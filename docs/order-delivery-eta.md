# ETA opcional de PedidosYa

Los pedidos `PEDIDOS_YA` pueden guardar una fecha y hora objetivo nullable en
`fechaHoraEstimadaDelivery`. El backend calcula este valor usando la zona
`America/Argentina/Buenos_Aires`; no ejecuta cronómetros, tareas programadas ni
actualizaciones periódicas.

## Crear o reemplazar un pedido

`POST /api/v2/pedido/crear` y `PUT /api/v2/pedido/actualizar/{id}` aceptan:

```json
{
  "tipoVenta": "PEDIDOS_YA",
  "tiempoEstimadoDeliveryMinutos": 31
}
```

Si los minutos son positivos, la respuesta contiene la hora actual más ese
intervalo:

```json
{
  "fechaHoraEstimadaDelivery": "2026-09-28T13:51:00"
}
```

El campo es opcional. En creación, un valor `null` deja el ETA en `null`. En la
edición completa, omitirlo o enviarlo en `null` conserva el ETA existente para
mantener compatibilidad con clientes anteriores; el PATCH específico permite
quitarlo explícitamente. En un pedido `PARTICULAR`, el ETA siempre se guarda
como `null`. Cero y los valores negativos producen `400 Bad Request` con el
mensaje `El tiempo estimado del delivery debe ser mayor a cero.`

## Actualización rápida

```http
PATCH /api/v2/pedido/{id}/eta-delivery
Content-Type: application/json
Authorization: Bearer <token>
```

Para establecer o recalcular:

```json
{
  "minutos": 20
}
```

Para quitar:

```json
{
  "minutos": null
}
```

El PATCH devuelve el `OrderResponseDTO` completo. Solo admite pedidos
`PEDIDOS_YA`; sobre un pedido `PARTICULAR` responde `409 Conflict` con `El
tiempo estimado del delivery solo aplica a pedidos de PedidosYa.`

Cada cambio por PATCH publica en `/topic/pedidos`:

```json
{
  "tipo": "ETA_DELIVERY_ACTUALIZADO",
  "idPedido": 2802,
  "estado": "PREPARADO"
}
```

Modificar el ETA no cambia estado, pago, stock, caja ni estadísticas. Los
cambios a `PREPARADO`, `ENTREGADO` o `CANCELADO` conservan el valor almacenado.
Todos los endpoints que responden con `OrderResponseDTO` incluyen
`fechaHoraEstimadaDelivery`, que es `null` cuando no existe ETA.
