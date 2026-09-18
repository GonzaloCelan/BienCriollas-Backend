# Ranking de clientes — contrato frontend

`GET /api/v2/estadisticas/clientes-ranking`

Requiere `Authorization: Bearer <token>` y el permiso existente de Estadísticas: `ROLE_ADMINISTRADOR`.

## Parámetros

| Parámetro | Valores / formato | Requerido / default |
| --- | --- | --- |
| `periodo` | `DIA`, `ULTIMOS_7_DIAS`, `MES`, `ANIO` | Obligatorio |
| `fecha` | `yyyy-MM-dd` | Obligatorio para `DIA` y `ULTIMOS_7_DIAS` |
| `mes` | `yyyy-MM` | Obligatorio para `MES` |
| `anio` | Entero entre 1000 y 9998 | Obligatorio para `ANIO` |
| `orden` | `IMPORTE`, `PEDIDOS` | Default `PEDIDOS` |
| `limit` | Entero entre 1 y 100 | Default `5` |

Ejemplos:

```http
GET /api/v2/estadisticas/clientes-ranking?periodo=DIA&fecha=2026-09-18
GET /api/v2/estadisticas/clientes-ranking?periodo=ULTIMOS_7_DIAS&fecha=2026-09-18&limit=10
GET /api/v2/estadisticas/clientes-ranking?periodo=MES&mes=2026-09&orden=PEDIDOS&limit=5
GET /api/v2/estadisticas/clientes-ranking?periodo=MES&mes=2026-09&orden=PEDIDOS&limit=100
GET /api/v2/estadisticas/clientes-ranking?periodo=ANIO&anio=2026&orden=PEDIDOS&limit=5
```

## Reglas

- Solo participan pedidos `PARTICULAR` en estado `ENTREGADO`.
- La fecha comercial es `fechaPedido`, igual que en `/estadisticas/resumen`. No se utiliza `createdAt` ni `fechaEntrega`. Los pedidos históricos con `createdAt` nulo también participan si su fecha comercial corresponde al período.
- Se reutiliza `StatisticsPeriodResolver`: día seleccionado completo; últimos 7 días incluyendo la fecha seleccionada y sus 6 anteriores; mes calendario completo; año calendario completo del 1 de enero al 31 de diciembre. `desde` y `hasta` en la respuesta son inclusivos.
- Los nombres nulos, vacíos o compuestos exclusivamente por espacios se excluyen de `clientes` y `totalClientes`.
- Se agrupa ignorando mayúsculas, espacios repetidos (incluyendo espacios Unicode) y diacríticos: `Lucia Fernandez`, `LUCIA FERNANDEZ` y `Lucía Fernández` representan el mismo cliente. No existe una identificación adicional por DNI o teléfono: personas diferentes con el mismo nombre normalizado quedan agrupadas.
- Se muestra la variante del pedido de fecha comercial más reciente dentro del período; si comparten fecha, se utiliza el mayor ID. Para presentación se normalizan espacios y mayúsculas, conservando los acentos de esa variante. No se actualiza ningún Pedido.
- `IMPORTE`: importe descendente, cantidad de pedidos descendente y nombre ascendente (orden alfabético español).
- `PEDIDOS` (default): cantidad de pedidos descendente, importe descendente y nombre ascendente.
- Los importes, promedios y porcentajes se calculan con `BigDecimal`, con 2 decimales y redondeo `HALF_UP`.
- Cada pedido se cuenta una sola vez. `totalUnidades` suma las cantidades de sus detalles; pedidos sin detalles aportan 0 unidades.

## Respuesta HTTP 200

```json
{
  "periodo": {
    "tipo": "MES",
    "desde": "2026-09-01",
    "hasta": "2026-09-30"
  },
  "orden": "PEDIDOS",
  "totalClientes": 3,
  "ventasParticularesPeriodo": 100000.00,
  "totalTopClientes": 80000.00,
  "porcentajeVentasTop": 80.00,
  "clientes": [
    {
      "posicion": 1,
      "cliente": "Lucía Fernández",
      "cantidadPedidos": 2,
      "totalAcumulado": 50000.00,
      "ticketPromedio": 25000.00,
      "totalUnidades": 24
    },
    {
      "posicion": 2,
      "cliente": "Martín González",
      "cantidadPedidos": 1,
      "totalAcumulado": 30000.00,
      "ticketPromedio": 30000.00,
      "totalUnidades": 12
    }
  ]
}
```

Este ejemplo corresponde a `limit=2`. El tercer cliente y/o ventas anónimas completan el total del período.

| Campo | Significado |
| --- | --- |
| `totalClientes` | Clientes distintos con nombre válido antes de aplicar `limit` |
| `ventasParticularesPeriodo` | Suma de **todos** los pedidos particulares entregados del período, incluso los sin nombre |
| `totalTopClientes` | Suma de los importes de los clientes efectivamente devueltos |
| `porcentajeVentasTop` | `totalTopClientes * 100 / ventasParticularesPeriodo`; 0 si el denominador es 0 |
| `clientes[].posicion` | Posición correlativa desde 1 según el orden solicitado |
| `clientes[].cantidadPedidos` | Pedidos válidos del cliente durante el período |
| `clientes[].totalAcumulado` | Suma de `totalPedido` del cliente |
| `clientes[].ticketPromedio` | `totalAcumulado / cantidadPedidos` |
| `clientes[].totalUnidades` | Suma de `cantidad` en los detalles de esos pedidos |

El cliente destacado es `clientes[0]` cuando la lista contiene elementos.

## Sin datos y errores

Un período sin pedidos válidos responde HTTP 200 con `clientes: []`, `totalClientes: 0` y los tres totales/porcentaje en 0. No se devuelve 404.

Si existen únicamente ventas anónimas, la lista y el Top quedan vacíos/en 0, pero `ventasParticularesPeriodo` conserva esas ventas. Esto respeta la regla de sumar todos los particulares entregados (sección 23 del contrato original).

- HTTP 400: período/fecha/mes/año faltante o inválido, `orden` inválido, `limit` no entero o fuera de 1 a 100. Usa el formato de errores existente del backend.
- HTTP 401: token ausente o inválido.
- HTTP 403: usuario sin permiso de Estadísticas.

## Implementación y verificación

Integrado en `StatisticsController`, `StatisticsService` e `IStatisticsService`. Una consulta en `StatisticsRepository` filtra primero el período, tipo y estado, agrega las unidades por pedido y devuelve solo los campos necesarios. `CustomerRankingCalculator` agrupa los nombres en memoria para obtener el mismo comportamiento con MySQL y H2, sin depender de una collation específica ni generar N+1.

El DTO de período es compartido con Hora Pico; su JSON existente se conserva. No requiere migraciones ni cambios en los datos históricos.

Pruebas: `CustomerRankingCalculatorTest` y `CustomerRankingIntegrationTest` cubren nombres, acentos, espacios Unicode, importes, promedios, unidades, filtros, orden, Top, validaciones, autorización y ausencia de cambios en los pedidos.
