# Ingredientes de Producción

Base: `/api/v1/ingredients`. Todos los endpoints requieren Bearer JWT.

Ingredientes es el catálogo de materias primas y precios. No controla inventario físico,
stock mínimo, disponibilidad ni alertas.

Cada ingrediente usa una unidad base: `GRAM`, `MILLILITER` o `UNIT`. El backend
calcula:

`costPerBaseUnit = purchasePrice / purchaseQuantity`

También entrega un precio comercial de referencia:

- `GRAM`: `referencePrice = costPerBaseUnit * 1000`, unidad `KG`.
- `MILLILITER`: `referencePrice = costPerBaseUnit * 1000`, unidad `LITER`.
- `UNIT`: `referencePrice = costPerBaseUnit`, unidad `UNIT`.

| Método | Ruta | Operación |
| --- | --- | --- |
| POST | `/` | Crear ingrediente |
| GET | `/` | Listar activos paginados |
| GET | `/{id}` | Obtener por id |
| GET | `/search?query=car` | Buscar activos por nombre |
| GET | `/status?active=false` | Listar por estado |
| GET | `/summary` | Contadores del catálogo |
| PUT | `/{id}` | Actualizar nombre, unidad y presentación |
| PATCH | `/{id}/cost` | Actualizar presentación, contenido y precio |
| PATCH | `/{id}/activate` | Activar |
| PATCH | `/{id}/deactivate` | Desactivar |

## Crear o actualizar

```json
{
  "name": "Aceite",
  "measurementUnit": "MILLILITER",
  "purchasePresentation": "Botella",
  "purchaseQuantity": 900,
  "purchasePrice": 2740
}
```

## Respuesta

```json
{
  "id": 1,
  "name": "Aceite",
  "measurementUnit": "MILLILITER",
  "purchasePresentation": "Botella",
  "purchaseQuantity": 900,
  "purchasePrice": 2740,
  "costPerBaseUnit": 3.044444,
  "purchaseDataComplete": true,
  "referencePrice": 3044.44,
  "referencePriceUnit": "LITER",
  "active": true,
  "createdAt": "2026-09-28T17:00:00",
  "updatedAt": "2026-09-28T17:00:00"
}
```

Los clientes anteriores pueden enviar temporalmente `currentStock` y `minimumStock`;
el backend los ignora. Esos campos no aparecen en la respuesta.

Las columnas `current_stock` y `minimum_stock` continúan en la base como datos
legacy para evitar una migración destructiva, pero la aplicación no las consulta ni
las modifica durante el flujo normal.

Las rutas `/low-stock`, `/{id}/stock`, `/{id}/stock/increase`,
`/{id}/stock/decrease` y `/{id}/minimum-stock` están discontinuadas y responden
`410 Gone`.
