# Producción real

Base: `/api/v1/productions`. Todos los endpoints requieren Bearer JWT.

1. `POST /api/v1/productions` crea un `DRAFT` desde la receta activa.
2. `PUT /{id}` registra resultado, tiempo, personas y merma.
3. `PATCH /{id}/ingredients` corrige un consumo y `POST /{id}/ingredients`
   agrega un extra.
4. `POST /{id}/finalize` valida y descuenta todos los ingredientes en una
   transacción.

Al crear el borrador se guardan `measurementUnitSnapshot` y
`costPerBaseUnitSnapshot`. El historial no cambia si luego se edita el ingrediente.
También se guarda `additionalCosts` con el tipo, modo, valor y costo esperado
escalado para la cantidad planificada. Esos valores pertenecen a la versión de
receta usada por la producción y no cambian al crear versiones posteriores.

## Crear

```json
{
  "varietyId": 1,
  "productionDate": "2026-09-10",
  "plannedUnits": 100,
  "notes": "Tanda de la tarde"
}
```

## Cargar consumo real

```json
{
  "ingredientId": 5,
  "actualQuantity": 4
}
```

La cantidad usa la unidad del ingrediente: `4 UNIT`, `150 MILLILITER` o
`3000 GRAM`. Si no se informa una cantidad real, al finalizar se utiliza la
esperada.

## Respuesta de ingrediente

```json
{
  "ingredientId": 5,
  "ingredientName": "Huevo",
  "expectedQuantity": 4,
  "actualQuantity": 5,
  "differenceQuantity": 1,
  "differencePercentage": 25,
  "measurementUnit": "UNIT",
  "costPerBaseUnitSnapshot": 200,
  "expectedCost": 800,
  "actualCost": 1000,
  "currentStock": 30,
  "projectedStock": 25,
  "enoughStock": true
}
```

Los costos usan `actualQuantity * costPerBaseUnitSnapshot`. Al finalizar, el
stock se reduce con `currentStock - actualQuantity` sin conversiones según el
tipo de ingrediente. La validación se completa para todos los ingredientes antes
de modificar datos; cualquier faltante provoca rollback completo y deja la
producción en `DRAFT`.

`POST /{id}/cancel` cancela un borrador sin mover stock. No existe eliminación
porque las producciones se conservan como historial.

En estadísticas, la mano de obra estándar sale del `LABOR` de receta y la real
continúa saliendo de horas por personas por costo hora. Los descartables reales
se estiman con las unidades finales. La energía usa el porcentaje snapshot de la
receta sobre ingredientes reales, mano de obra real, descartables y otros costos
no porcentuales. Si una receta histórica no tenía costos adicionales, se mantiene
el cálculo anterior con la configuración global.
