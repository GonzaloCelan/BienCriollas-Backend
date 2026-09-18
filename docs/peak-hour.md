# Hora pico del negocio

`GET /api/v2/estadisticas/hora-pico` requiere un Bearer token de un usuario con rol
`ADMINISTRADOR`, igual que el resto de Estadísticas.

## Filtros del análisis comercial

| Consulta | Período analizado |
| --- | --- |
| `?periodo=DIA&fecha=2026-09-18` | 18 de septiembre |
| `?periodo=ULTIMOS_7_DIAS&fecha=2026-09-18` | 12 al 18 de septiembre, inclusive |
| `?periodo=MES&mes=2026-09` | 1 al 30 de septiembre, inclusive |
| `?periodo=ANIO&anio=2026` | 1 de enero al 31 de diciembre, inclusive |

`periodo` es obligatorio. `fecha` es obligatoria para `DIA` y `ULTIMOS_7_DIAS`;
`mes` es obligatorio para `MES` y usa `yyyy-MM`; `anio` es obligatorio para `ANIO`
y es un entero entre 1000 y 9998. Los parámetros faltantes o inválidos
devuelven HTTP 400 con el formato de error habitual.

Las consultas usan inicio inclusivo y fin exclusivo, como `/estadisticas/resumen`.
El resumen también acepta los mismos parámetros `periodo`, `fecha`, `mes` y `anio`.
Conserva además su consulta por `desde` y `hasta` (sin combinar ambos modos).
La respuesta de Hora Pico
presenta `periodo.desde` y `periodo.hasta` como fechas inclusivas.

## Respuesta

| Campo | Contenido |
| --- | --- |
| `periodo` | `tipo`, `desde`, `hasta` |
| `totalPedidosAnalizados` | Pedidos entregados con `createdAt` dentro del período |
| `totalMontoVendido` | Suma de `totalPedido` de esos pedidos |
| `horaPico` | `inicio`, `fin`, `pedidos`, `montoVendido`, `porcentajeDelTotal`, `turno` |
| `turnos.mediodia` | Turno desde `11:30` hasta `14:30` |
| `turnos.noche` | Turno desde `20:30` hasta `23:30` |
| `pedidosFueraDeHorario` | Cantidad de pedidos fuera de ambos turnos |
| `montoFueraDeHorario` | Suma de sus `totalPedido` |

Cada turno incluye `desde`, `hasta`, `totalPedidos`, `totalMontoVendido` y `franjas`.
Siempre se devuelven seis franjas ordenadas de 30 minutos por turno, incluso las vacías.
Cada franja incluye `inicio`, `fin`, `pedidos`, `montoVendido` y `porcentajeDelTotal`.
Las horas usan `HH:mm` y los importes y porcentajes son números JSON.

Ejemplo de una franja:

```json
{
  "inicio": "21:30",
  "fin": "22:00",
  "pedidos": 2,
  "montoVendido": 36000,
  "porcentajeDelTotal": 66.67
}
```

## Reglas de cálculo

- Solo participan pedidos `ENTREGADO` de ambos canales, con `createdAt` no nulo.
- La clasificación usa la fecha y hora local argentina almacenada en `createdAt`.
  Las fechas comerciales y de entrega y la hora de entrega no se usan como reemplazo.
- Un pedido programado participa según su creación, una vez entregado.
- Cada pedido cuenta una vez, independientemente de sus detalles y cantidades.
- Los turnos y franjas incluyen el inicio y excluyen el fin. `14:30` y `23:30` ya están fuera.
- Día, siete días, mes y año acumulan la misma franja horaria de todas las fechas analizadas.
- Hora Pico se elige entre las doce franjas de los turnos: mayor cantidad de pedidos,
  luego mayor monto vendido y, en empate completo, la franja cronológicamente más temprana.
- Todos los porcentajes usan como denominador `totalPedidosAnalizados`, incluyendo
  los pedidos fuera de horario, y se redondean a dos decimales.
- Los pedidos fuera de horario participan en los totales y sus contadores; no alteran
  las barras de los turnos. Si no hay pedidos dentro de los turnos, `horaPico` es `null`.
- Sin pedidos válidos se responde HTTP 200 con totales en cero, `horaPico: null` y las
  doce franjas completas en cero.

El endpoint depende de la columna y el índice de `created_at`, incorporados por la
migración Flyway V13. No requiere una nueva migración y no modifica pedidos históricos.
