# Ingredientes de Producción

Base: `/api/v1/ingredients`. Todos los endpoints requieren Bearer JWT.

Cada ingrediente usa una única `measurementUnit`: `GRAM` (`g`), `MILLILITER`
(`ml`) o `UNIT` (`u`). El stock, el mínimo y las cantidades se expresan siempre
en esa unidad base. La presentación de compra es descriptiva y su contenido
también se expresa en la unidad base. El backend calcula
`costPerBaseUnit = purchasePrice / purchaseQuantity` con seis decimales.

| Método | Ruta | Operación |
| --- | --- | --- |
| POST | `/` | Crear ingrediente |
| GET | `/` | Listar activos paginados |
| GET | `/{id}` | Obtener por id |
| GET | `/search?query=car` | Buscar activos por nombre |
| GET | `/status?active=false` | Listar por estado |
| GET | `/low-stock` | Listar activos debajo del mínimo |
| GET | `/summary` | Resumen e importe total del inventario |
| PUT | `/{id}` | Actualizar ingrediente |
| PATCH | `/{id}/stock` | Establecer stock exacto |
| PATCH | `/{id}/stock/increase` | Sumar stock |
| PATCH | `/{id}/stock/decrease` | Descontar stock |
| PATCH | `/{id}/cost` | Actualizar presentación, contenido y precio de compra |
| PATCH | `/{id}/minimum-stock` | Actualizar mínimo |
| PATCH | `/{id}/activate` | Activar |
| PATCH | `/{id}/deactivate` | Desactivar |

```json
{
  "name": "Aceite",
  "measurementUnit": "MILLILITER",
  "purchasePresentation": "Botella",
  "purchaseQuantity": 900,
  "purchasePrice": 2740,
  "currentStock": 3600,
  "minimumStock": 900
}
```

Los PATCH reciben `{"currentStock":5000}`, `{"quantity":250}`,
`{"purchasePresentation":"Botella","purchaseQuantity":900,"purchasePrice":2740}`
o `{"minimumStock":1000}`, según la ruta.

La respuesta incluye `measurementUnit`, `purchasePresentation`, `purchaseQuantity`,
`purchasePrice`, `purchaseDataComplete`, `currentStock`, `minimumStock`,
`costPerBaseUnit`, `stockValue`, `lowStock`, `active`, `createdAt` y `updatedAt`.
`stockValue = currentStock * costPerBaseUnit` y
`lowStock = currentStock <= minimumStock`.

Stock, mínimo y contenido de compra admiten cuatro decimales. El precio de la
presentación admite dos decimales y el costo calculado por unidad base, seis.
La unidad no puede modificarse si el ingrediente ya tiene stock o está relacionado
con recetas o producciones.

Flyway V14 migra los registros existentes como `GRAM`, copia stock y mínimo,
convierte `cost_per_kilogram / 1000` a `cost_per_base_unit` y sustituye las
columnas legacy. Flyway V17 agrega los datos de compra como columnas nullable:
los ingredientes históricos mantienen su costo y responden
`purchaseDataComplete: false` hasta que se carguen datos reales. La migración no
inventa presentaciones, cantidades ni precios.

Los ingredientes históricos que realmente sean líquidos o unidades requieren una
corrección administrativa con valores reales; la migración no inventa conversiones
entre gramos, mililitros y unidades.
