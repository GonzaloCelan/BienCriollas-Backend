# Producción real

Base: `/api/v1/productions`. Todos los endpoints requieren Bearer JWT.

1. `POST /api/v1/productions` crea un `DRAFT` desde la receta activa.
2. `PUT /{id}` registra resultado, tiempo, personas y merma.
3. `PATCH /{id}/ingredients` corrige un consumo real y `POST /{id}/ingredients`
   agrega un extra para el análisis de costos.
4. `POST /{id}/finalize` finaliza la tanda y suma las unidades obtenidas al
   stock de empanadas terminadas.

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
  "actualCost": 1000
}
```

Los costos usan `actualQuantity * costPerBaseUnitSnapshot`. Al finalizar no se
valida disponibilidad ni se descuenta materia prima. El consumo real se conserva
para costos y desvíos, mientras que `finalUnits` se suma al stock de la variedad
terminada.

`POST /{id}/cancel` cancela un borrador sin devolver materia prima. No existe
eliminación porque las producciones se conservan como historial.

En estadísticas, la mano de obra estándar sale del `LABOR` de receta y la real
continúa saliendo de horas por personas por costo hora. Los descartables reales
se estiman con las unidades finales. La energía usa el porcentaje snapshot de la
receta sobre ingredientes reales, mano de obra real, descartables y otros costos
no porcentuales. Si una receta histórica no tenía costos adicionales, se mantiene
el cálculo anterior con la configuración global.
