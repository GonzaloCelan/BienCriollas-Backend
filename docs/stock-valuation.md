# Valorizacion del stock terminado

La valorizacion utiliza el costo actual por unidad de la receta vigente. No guarda
costos ni valores monetarios en la tabla de stock y no modifica la mecanica de
produccion, ventas, perdidas o ajustes.

## Stock actual

`GET /api/v2/stock/obtener-stock-actual`

Cada registro activo incluye los campos existentes y dos campos calculados:

```json
[
  {
    "id_variedad": 2,
    "fecha_elaboracion": "2026-09-29",
    "stock_total": 165,
    "stock_disponible": 100,
    "costo_unitario_actual": 618.67,
    "valor_stock_actual": 61867.00
  }
]
```

`valor_stock_actual` es `stock_disponible * costo_unitario_actual`. Ambos importes
se responden con dos decimales y redondeo `HALF_UP`. Si no existe una receta vigente
o sus datos no permiten calcular el costo, ambos campos monetarios se responden como
`null`. Si el stock disponible es cero y el costo puede calcularse, el valor es
`0.00`.

El costo se vuelve a calcular al consultar, por lo que refleja cambios posteriores
en precios de ingredientes o en la receta vigente.

## Resumen

`GET /api/v2/stock/resumen`

```json
{
  "total_unidades_disponibles": 1245,
  "valor_total_stock": 782450.20,
  "variedades_con_stock": 12,
  "variedades_sin_valoracion": 1
}
```

- `total_unidades_disponibles`: suma del stock disponible de todos los registros activos.
- `valor_total_stock`: suma de los valores que pudieron calcularse.
- `variedades_con_stock`: registros activos con al menos una unidad disponible.
- `variedades_sin_valoracion`: registros activos sin costo vigente calculable.

El historial `GET /api/v2/stock/obtener-variedad/{idVariedad}` conserva su contrato
anterior y no expone valorizaciones historicas.
