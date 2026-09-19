# Filtro por año — Estadísticas

Todos los endpoints de `/api/v2/estadisticas` permiten analizar un año calendario completo usando `periodo=ANIO&anio=2026`:

```http
GET /api/v2/estadisticas/resumen?periodo=ANIO&anio=2026
GET /api/v2/estadisticas/hora-pico?periodo=ANIO&anio=2026
GET /api/v2/estadisticas/clientes-ranking?periodo=ANIO&anio=2026&orden=PEDIDOS&limit=5
```

`anio` es obligatorio para `ANIO`, debe ser entero y estar entre 1000 y 9998. Este rango garantiza que tanto el inicio como el fin exclusivo de la consulta sean fechas válidas de MySQL. Parámetros faltantes, no numéricos o fuera de rango devuelven HTTP 400. La autenticación y autorización existentes se conservan.

Se reutiliza `StatisticsPeriodResolver`. El rango consultado es `[2026-01-01, 2027-01-01)`: incluye todo el 31 de diciembre y excluye los años vecinos. En años bisiestos incluye el 29 de febrero.

Ranking y Hora Pico devuelven:

```json
{
  "periodo": {
    "tipo": "ANIO",
    "desde": "2026-01-01",
    "hasta": "2026-12-31"
  }
}
```

El resto de sus campos conserva el contrato existente. Ranking siempre ordena por cantidad de pedidos, incluso si recibe `orden=IMPORTE` de un frontend anterior, y responde `orden: "PEDIDOS"`. `limit` conserva el default de 5 y permite cambiarlo. Hora Pico acumula cada franja horaria durante todas las fechas del año; no genera una franja por fecha.

Resumen conserva su JSON actual, sin agregar campos. El filtro anual se aplica a todas sus métricas: pedidos entregados, empanadas vendidas, ticket promedio, variedad más vendida, ranking de variedades, ventas por día de semana, tipos de venta, medios de pago y mermas por variedad.

Resumen y Ranking usan `fechaEntrega` para pedidos programados y `fechaPedido` cuando no existe fecha de entrega, igual que Ingresos. Hora Pico conserva `createdAt` porque mide la hora real en que ingresó el pedido; las mermas del Resumen usan `fechaRegistro`. No se modifican pedidos ni datos históricos y no requiere migraciones.

## Compatibilidad de filtros

`DIA`, `ULTIMOS_7_DIAS` y `MES` siguen funcionando con sus mismos parámetros. Resumen ahora también acepta esos filtros globales:

```http
GET /api/v2/estadisticas/resumen?periodo=DIA&fecha=2026-09-18
GET /api/v2/estadisticas/resumen?periodo=ULTIMOS_7_DIAS&fecha=2026-09-18
GET /api/v2/estadisticas/resumen?periodo=MES&mes=2026-09
```

La consulta anterior de Resumen sigue válida:

```http
GET /api/v2/estadisticas/resumen?desde=2026-01-01&hasta=2027-01-01
```

En este modo `desde` es inclusivo y `hasta` exclusivo. No combinar `periodo` con `desde`/`hasta`; devuelve HTTP 400 por filtro ambiguo. Enviar `anio`, `fecha` o `mes` sin `periodo` también devuelve HTTP 400.

Un año sin datos devuelve HTTP 200 con los valores vacíos/en cero habituales de cada endpoint.
