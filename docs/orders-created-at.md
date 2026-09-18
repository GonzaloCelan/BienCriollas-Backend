# Fecha y hora de creación de pedidos

Todos los endpoints que devuelven `OrderResponseDTO` incluyen `createdAt`:

```json
{
  "fechaPedido": "2026-09-18",
  "fechaEntrega": "2026-09-20",
  "horaEntrega": "21:00:00",
  "createdAt": "2026-09-18T21:37:42"
}
```

`createdAt` registra la fecha y hora local de creación en `America/Argentina/Buenos_Aires`,
con precisión de segundos. Se genera en el backend al persistir el pedido, sin depender
del horario del servidor ni de los datos enviados por el frontend. No es un campo del
request y permanece igual al editar el pedido o cambiar su estado.

Los pedidos históricos devuelven `"createdAt": null`. Editarlos no completa ese valor.
`fechaPedido`, `fechaEntrega` y `horaEntrega` mantienen su significado y comportamiento.

La migración Flyway `V13__agregar_created_at_pedidos.sql` agrega `created_at DATETIME NULL`
y el índice `idx_pedido_created_at`, sin defaults ni backfill. Se aplica al iniciar el
backend con Flyway habilitado. Este cambio no incorpora estadísticas nuevas.
