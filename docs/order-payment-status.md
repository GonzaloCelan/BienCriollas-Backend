# Estado de pago de pedidos

Cada pedido guarda el booleano `pagado`, independiente del tipo de pago, tipo de venta, caja, estadísticas y stock.

## Reglas

- En `POST /api/v2/pedido/crear`, `pagado` puede ser `true` o `false`. Si se omite, se guarda `false`.
- En `PUT /api/v2/pedido/actualizar/{id}`, un valor explícito corrige el estado de pago. Si un cliente antiguo omite el campo, se conserva el valor existente.
- Pasar a `PREPARADO` no modifica `pagado`.
- Pasar a `ENTREGADO` siempre guarda `pagado=true` en la misma transacción.
- Pasar a `CANCELADO` conserva el valor de `pagado`.
- La regularización administrativa que entrega pedidos también guarda `pagado=true`.
- Los pedidos históricos quedan con `pagado=false` al ejecutar la migración; no se infiere información que antes no existía.

## Actualización rápida

```http
PATCH /api/v2/pedido/{id}/pago
Content-Type: application/json
Authorization: Bearer <token>
```

```json
{
  "pagado": true
}
```

Devuelve el `OrderResponseDTO` actualizado y publica un evento `PAGO_ACTUALIZADO` en `/topic/pedidos`.

Un pedido `ENTREGADO` no puede actualizarse a `pagado=false`; el endpoint responde `409 Conflict` con:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Un pedido entregado no puede quedar pendiente de pago.",
  "path": "/api/v2/pedido/123/pago"
}
```

## Respuestas

`pagado` se incluye en los pedidos devueltos por listados, programación, creación y actualización, y también en el detalle utilizado por la comanda y en la consulta de regularización.

```json
{
  "idPedido": 123,
  "cliente": "Juan Pérez",
  "tipoPago": "EFECTIVO",
  "totalPedido": 25000.00,
  "pagado": false,
  "estadoPedido": "PREPARADO"
}
```

La representación visual queda a cargo del frontend:

- `true`: `PAGADO`.
- `false`: `PENDIENTE` o `COBRAR AL RETIRAR` en la comanda.
