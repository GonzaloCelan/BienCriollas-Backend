# Producción real

Base: `/api/v1/productions`. Todos los endpoints requieren `Authorization: Bearer <token>` y usan JSON. Las listas son páginas Spring (`content`, `totalElements`, `totalPages`, `number`, `size`).

## Flujo

1. `POST /api/v1/productions` crea un `DRAFT` con la receta y el proceso activos de la variedad.
2. `PUT /{id}` carga el resultado, tiempo, personas y merma.
3. `PATCH /{id}/ingredients` corrige consumos y `POST /{id}/ingredients` agrega extras.
4. `POST /{id}/finalize` descuenta todos los ingredientes y suma las unidades terminadas al stock en una sola transacción.

La receta es obligatoria. El proceso es opcional; sin proceso, las métricas de comparación estándar son `null`. En un borrador, una cantidad real no informada se presenta y finaliza con la cantidad esperada. Los costos usan el precio por gramo congelado al crear la producción, calculado desde `costPerKilogram / 1000`.

## Crear

`POST /api/v1/productions`

```json
{
  "varietyId": 1,
  "productionDate": "2026-09-10",
  "plannedUnits": 100,
  "notes": "Tanda de la tarde"
}
```

Responde `201 Created`, encabezado `Location` y el detalle completo.

## Actualizar datos reales

`PUT /api/v1/productions/{id}`

```json
{
  "finalUnits": 95,
  "totalMinutes": 195,
  "peopleCount": 2,
  "wasteUnits": 5,
  "wasteReason": "Tapas rotas",
  "notes": "Tanda medida"
}
```

Los campos son opcionales. Los valores `null` conservan el dato anterior; se puede enviar `""` para limpiar `wasteReason` o `notes`. Solo se admite mientras el estado sea `DRAFT`.

## Consumos

Actualizar un ingrediente ya incluido:

`PATCH /api/v1/productions/{id}/ingredients`

```json
{
  "ingredientId": 3,
  "actualQuantityGrams": 8400.00
}
```

Agregar un ingrediente activo que no estaba en la receta:

`POST /api/v1/productions/{id}/ingredients`

```json
{
  "ingredientId": 9,
  "actualQuantityGrams": 250.00
}
```

El extra se guarda con `expectedQuantityGrams: 0.00`. Ambos endpoints responden `200 OK` con la producción completa y solo admiten borradores.

## Finalizar o cancelar

- `POST /api/v1/productions/{id}/finalize`: exige `finalUnits`, valida todo el stock, descuenta gramos, suma empanadas mediante el servicio de Stock y devuelve `FINALIZED`.
- `POST /api/v1/productions/{id}/cancel`: pasa un borrador a `CANCELED` sin mover stock.

No existe `DELETE`: las producciones quedan como historial.

## Consultas

- `GET /api/v1/productions?page=0&size=20&sort=productionDate,desc`
- `GET /api/v1/productions/{id}`
- `GET /api/v1/productions/status?status=FINALIZED&page=0&size=20`
- `GET /api/v1/productions/date-range?from=2026-09-01&to=2026-09-10&page=0&size=20`

Campos de orden permitidos: `id`, `productionDate`, `varietyName`, `plannedUnits`, `finalUnits`, `totalMinutes`, `peopleCount`, `wasteUnits`, `status`, `createdAt`, `updatedAt`, `finalizedAt`.

## Respuesta

```json
{
  "id": 15,
  "productionDate": "2026-09-10",
  "varietyId": 1,
  "varietyName": "Carne",
  "recipeId": 7,
  "recipeVersion": 3,
  "processId": 4,
  "processVersion": 2,
  "plannedUnits": 100,
  "finalUnits": 95,
  "wasteUnits": 5,
  "wasteReason": "Tapas rotas",
  "totalMinutes": 195,
  "peopleCount": 2,
  "status": "FINALIZED",
  "notes": "Tanda medida",
  "ingredients": [
    {
      "ingredientId": 3,
      "ingredientName": "Carne picada",
      "expectedQuantityGrams": 8000.00,
      "actualQuantityGrams": 8400.00,
      "differenceGrams": 400.00,
      "differencePercentage": 5.00,
      "costPerGramSnapshot": 12.500000,
      "expectedCost": 100000.00,
      "actualCost": 105000.00,
      "currentStockGrams": 16600.00,
      "projectedStockGrams": 8200.00,
      "enoughStock": true
    }
  ],
  "expectedIngredientCost": 106000.00,
  "actualIngredientCost": 111000.00,
  "actualIngredientCostPerUnit": 1168.42,
  "standardUnitsPerHour": 33.33,
  "actualUnitsPerHour": 29.23,
  "productivityVariationPercentage": -12.30,
  "createdAt": "2026-09-10T14:20:00.123456",
  "finalizedAt": "2026-09-10T18:10:00.654321"
}
```

`actualIngredientCostPerUnit` solo contempla ingredientes. Si `finalUnits` es `0` o todavía no se informó, es `null`. `actualUnitsPerHour` requiere `finalUnits` y `totalMinutes`. `productivityVariationPercentage` también requiere un proceso estándar.

## Validaciones y errores

- `400 Bad Request`: JSON, fechas, cantidades, paginación u orden inválidos.
- `404 Not Found`: producción, variedad o ingrediente inexistente.
- `409 Conflict`: falta receta activa, ingrediente inactivo, stock insuficiente o estado incompatible.

El error conserva el formato global: `timestamp`, `status`, `error`, `message` y `path`.

La carga manual existente en Stock sigue separada. Solo suma empanadas y no crea una producción, por lo que no afecta las métricas de este módulo.
