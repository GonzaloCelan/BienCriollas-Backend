# Recetas de Producción

Base: `/api/v1/recipes`. Todos los endpoints requieren Bearer JWT.

| Método | Ruta | Operación |
| --- | --- | --- |
| POST | `/` | Crear primera receta de una variedad |
| GET | `/` | Listar activas |
| GET | `/{id}` | Obtener receta activa o histórica |
| GET | `/variety/{varietyId}` | Obtener receta activa |
| GET | `/variety/{varietyId}/history` | Obtener versiones |
| GET | `/status?active=true` | Listar por estado |
| POST | `/{id}/versions` | Crear versión nueva |
| GET | `/{id}/calculate?quantity=250` | Simular cantidades, faltantes y costos |

```json
{
  "varietyId": 1,
  "baseYieldUnits": 100,
  "notes": "Receta estándar de carne",
  "ingredients": [
    {"ingredientId": 1, "quantity": 8000},
    {"ingredientId": 3, "quantity": 150},
    {"ingredientId": 5, "quantity": 4}
  ],
  "additionalCosts": [
    {
      "costType": "LABOR",
      "name": "Mano de obra",
      "calculationMode": "FIXED_TOTAL",
      "value": 16250,
      "sortOrder": 1,
      "notes": null
    },
    {
      "costType": "PACKAGING",
      "name": "Descartables",
      "calculationMode": "PER_UNIT",
      "value": 44.83,
      "sortOrder": 2,
      "notes": null
    },
    {
      "costType": "ENERGY",
      "name": "Energía",
      "calculationMode": "PERCENTAGE",
      "value": 7,
      "sortOrder": 3,
      "notes": null
    }
  ]
}
```

`additionalCosts` puede omitirse o enviarse vacío. `LABOR` usa `FIXED_TOTAL`,
`PACKAGING` usa `PER_UNIT`, `ENERGY` usa `PERCENTAGE` y `OTHER` admite cualquier
modo. Solo `OTHER` puede repetirse. Todo valor debe ser mayor que cero y los
porcentajes no pueden superar 100.

`quantity` utiliza la unidad base definida por el ingrediente. La receta no
duplica la unidad. Las respuestas incluyen `measurementUnit` y
`currentCostPerBaseUnit` para que el frontend pueda mostrar `g`, `ml` o `u`.

La fórmula de escalado es
`requiredQuantity = quantity * requestedUnits / baseYieldUnits`, con cuatro
decimales. El costo estimado es
`requiredQuantity * currentCostPerBaseUnit`. Consultar o calcular una receta
no descuenta stock.

La respuesta incluye `additionalCosts[].calculatedCost` y `costSummary`.
Los porcentajes se calculan una sola vez sobre ingredientes más costos fijos y
por unidad; no se componen entre sí. `estimatedTotalCost` y
`estimatedCostPerUnit` se conservan por compatibilidad y contienen el total
completo de la receta.

Las versiones históricas son inmutables. Cambiar el costo actual del ingrediente
cambia la estimación de una receta, pero no los costos de producciones ya creadas,
que utilizan snapshots.

Al crear una versión nueva se envía el conjunto completo de ingredientes y
costos adicionales. Omitir un costo lo excluye de la versión nueva y nunca
modifica la versión histórica.
