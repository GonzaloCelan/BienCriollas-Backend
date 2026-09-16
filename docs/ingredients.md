# Ingredientes de Producción

El módulo se encuentra en `production/ingredient`, organizado por funcionalidad
con controller, DTOs, entity, interfaces, repository, service, mapper y exception.
Es la base para futuras recetas y producciones, sin implementar esos módulos.

## Contrato

Base: `/api/v1/ingredients`. Requiere el Bearer JWT existente. Conserva la política
general de stock: acceso para usuarios autenticados.

| Método | Ruta relativa | Operación |
| --- | --- | --- |
| POST | / | Crear, devuelve 201 y cabecera Location |
| GET | / | Activos paginados |
| GET | /{id} | Consultar cualquier estado |
| GET | /search?query=car | Buscar texto literal en nombres activos, sin distinguir mayúsculas |
| GET | /status?active=false | Listar por estado |
| GET | /low-stock | Activos con stock actual menor o igual al mínimo |
| GET | /summary | Resumen general |
| PUT | /{id} | Actualizar nombre, stock, mínimo y costo |
| PATCH | /{id}/stock | Establecer stock exacto |
| PATCH | /{id}/stock/increase | Sumar gramos |
| PATCH | /{id}/stock/decrease | Descontar gramos de un ingrediente activo |
| PATCH | /{id}/cost | Actualizar precio por kilogramo |
| PATCH | /{id}/minimum-stock | Actualizar mínimo |
| PATCH | /{id}/activate | Activar |
| PATCH | /{id}/deactivate | Desactivar |

En las rutas raíz se usa la base sin barra final. Los listados aceptan `page`
(desde cero), `size` (20 por defecto) y `sort` (nombre e id ascendentes por defecto).
Se puede ordenar por los campos persistidos, incluido `costPerKilogram`.
Por compatibilidad, ordenar por `costPerGram` equivale a ordenar por kilo.
Los campos calculados `stockValue`
y `lowStock` no se pueden utilizar como criterios de ordenamiento.

Creación y actualización general:

```json
{
  "name": "Carne",
  "currentStockGrams": 15000,
  "minimumStockGrams": 5000,
  "costPerKilogram": 12500.00
}
```

Cuerpos de los PATCH:

| Ruta | Cuerpo |
| --- | --- |
| stock | `{"stockGrams":18500}` |
| stock/increase o stock/decrease | `{"quantityGrams":2500}` |
| cost | `{"costPerKilogram":14350.00}` |
| minimum-stock | `{"minimumStockGrams":5000}` |
| activate o deactivate | Sin cuerpo |

Los DTOs mantienen los nombres JSON en inglés indicados en el contrato.
Los errores usan `ApiErrorResponse`: 400 para entrada inválida, 404 si no existe
el ingrediente y 409 para duplicados, stock insuficiente o consumo de inactivos.
Swagger muestra todas las operaciones bajo **Producción - Ingredientes**.

## Reglas y precisión

- Todas las cantidades se expresan en gramos, con hasta 12 dígitos enteros y
  2 decimales. Stock y mínimo pueden ser cero; movimientos desde 0.01 g.
- El precio se carga por kilogramo mediante `costPerKilogram`, con hasta
  11 dígitos enteros y 3 decimales, desde 0.001. Se guarda directamente en
  `cost_per_kilogram DECIMAL(14,3)`. Para calcular el costo de cantidades en
  gramos se divide por 1000 de forma exacta.
- Las respuestas incluyen `costPerKilogram` para mostrar el precio y
  `costPerGram` como dato calculado, sin columna propia. Por ejemplo, 12500 por kilo equivale
  a 12.50 por gramo; 15000 g tienen un valor de stock de 187500.
- POST, PUT y PATCH de costo reciben `costPerKilogram`; los clientes deben
  reemplazar el campo anterior `costPerGram` y multiplicar su valor por 1000.
  La migración V6 convierte los datos existentes de la base de gramos a kilos.
- Se rechazan valores con mayor precisión o fuera del rango, sin redondearlos.
- Los nombres se guardan sin espacios exteriores, con máximo de 100 caracteres.
  Su unicidad incluye ingredientes inactivos y no distingue mayúsculas.
- `stockValue = currentStockGrams * costPerKilogram / 1000`, con precisión decimal completa.
- `lowStock = currentStockGrams <= minimumStockGrams`.
- El resumen cuenta todos los ingredientes y desglosa activos/inactivos.
  `lowStockIngredients` cuenta solo activos. `totalStockValue` incluye activos
  e inactivos, porque desactivar no elimina el inventario. Sin registros devuelve ceros.
- La creación inicia activa; PUT conserva id, estado y fecha de creación.
  Los callbacks JPA asignan fechas con precisión de microsegundos.
- Desactivar conserva el registro. Se permiten correcciones administrativas,
  reposiciones y cambios de costo/mínimo; para consumir se debe reactivar.
- No existe endpoint de eliminación.

## Persistencia y concurrencia

Flyway aplica `V5__crear_tabla_ingredientes.sql` y luego
`V6__migrar_costo_ingredientes_a_kilogramo.sql` al iniciar sobre MySQL.
V5 se conserva intacta para las bases donde ya fue aplicada.
V6 crea `cost_per_kilogram`, copia `cost_per_gram * 1000` y sustituye la columna
anterior junto con su restricción de costo positivo. Por ejemplo, 12.500000
pasa a 12500.000. Conserva toda la precisión, incluso en los límites del rango,
y no cambia stocks, estados, fechas ni el valor monetario del inventario.
No hay que renombrar columnas o multiplicar precios manualmente antes de iniciar.
La columna del nombre usa `utf8mb4_unicode_ci`, incluyendo su índice único;
esto también equipara acentos según las reglas de esa collation.
Los controles CHECK de MySQL requieren 8.0.16 o posterior para ser aplicados.

Todas las escrituras sobre un ingrediente existente toman un bloqueo pesimista
dentro de una transacción. Así los descuentos verifican el saldo vigente y las
sumas no pierden actualizaciones simultáneas. El índice único también protege
las creaciones concurrentes; los duplicados se traducen a errores de negocio.
Las validaciones se ejecutan tanto en HTTP como en el servicio.

La entidad tiene una clave Long estable, apta para una futura relación
`RecipeIngredient -> Ingredient` mediante `ManyToOne`. El método
`Ingredient.requireActive()` centraliza la validación que deberán invocar los
futuros servicios al incorporar ingredientes a recetas o consumirlos en producción.
Las futuras operaciones sobre varios ingredientes deberán bloquearlos por id
en orden ascendente dentro de la misma transacción.

## Verificación

```powershell
.\mvnw.cmd -Dtest=IngredientIntegrationTest test
.\mvnw.cmd test
```

Las pruebas de integración usan H2 aislado y cubren todos los endpoints,
validaciones, preservación de datos, cálculos, búsquedas y modificaciones concurrentes.
La collation específica de MySQL se define en la migración, no en el esquema
generado por Hibernate para H2.

La prueba `IngredientMigrationMySqlTest` valida V5 → V6 con Flyway sobre MySQL
real: conversión de precios mínimos, máximos y fraccionarios, conservación del
valor de stock, eliminación de la columna anterior y restricciones vigentes.
Se habilita únicamente indicando un esquema MySQL vacío y exclusivo de prueba:

```powershell
.\mvnw.cmd "-Dingredient.mysqlTestUrl=jdbc:mysql://127.0.0.1:33317/ingredient_v6_migration_test" -Dtest=IngredientMigrationMySqlTest test
```

El usuario predeterminado de esta prueba es `root`, sin contraseña; se puede
configurar con `ingredient.mysqlTestUser` y `ingredient.mysqlTestPassword`.
Sin URL explícita, la prueba de migración se omite y el resto se ejecuta en H2.
