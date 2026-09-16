# Recetas de Producción

El módulo está en `production/recipe` y define la composición estándar y
versionada de las variedades existentes. Usa relaciones JPA con
`EmpanadaVariety` e `Ingredient`; no duplica esos datos.

## Endpoints

La base es `/api/v1/recipes` y todos los endpoints requieren Bearer JWT.

| Método | Ruta relativa | Operación |
| --- | --- | --- |
| POST | `/` | Crear la primera receta de una variedad |
| GET | `/` | Listar recetas activas paginadas |
| GET | `/{id}` | Obtener una receta activa o histórica |
| GET | `/variety/{varietyId}` | Obtener la receta activa de una variedad |
| GET | `/variety/{varietyId}/history` | Historial por versión descendente |
| GET | `/status?active=true` | Listar recetas por estado |
| POST | `/{id}/versions` | Crear una nueva versión |
| GET | `/{id}/calculate?quantity=250` | Simular cantidades, costos y faltantes |

No existe DELETE ni actualización de una versión histórica.

Creación:

```json
{
  "varietyId": 1,
  "baseYieldUnits": 100,
  "notes": "Receta estándar de carne",
  "ingredients": [
    {"ingredientId": 1, "quantityGrams": 8000},
    {"ingredientId": 3, "quantityGrams": 4000}
  ]
}
```

Para crear una versión nueva se usa el mismo contenido sin `varietyId`. La
variedad se toma de la receta indicada en la URL. POST devuelve 201 y `Location`.

Los listados aceptan `page`, `size` y `sort`. El orden predeterminado es por
nombre de variedad e id. Los campos permitidos son `id`, `varietyName`,
`version`, `baseYieldUnits`, `active`, `createdAt` y `updatedAt`.

## Reglas

- La variedad y todos los ingredientes deben existir.
- Los ingredientes deben estar activos al crear cada versión y no pueden repetirse.
- La receta requiere al menos un ingrediente; gramos y rendimiento deben ser positivos.
- La primera versión es 1. Cada cambio crea la versión máxima más uno, desactiva
  la vigente y deja activa la nueva. Las versiones anteriores no se modifican.
- Solo puede haber una versión activa por variedad. La aplicación bloquea la
  variedad durante la creación y la base refuerza la regla con un índice único.
- Los ingredientes se bloquean en lectura mientras se valida una nueva versión,
  evitando que sean desactivados antes del commit.
- Crear, consultar, versionar y calcular nunca modifica el stock.
- Los costos son estimaciones actuales. Se calculan con `costPerKilogram / 1000`
  y pueden cambiar cuando cambia el precio actual del ingrediente.

## Cálculo

`scaleFactor = requestedUnits / baseYieldUnits`, con 6 decimales y HALF_UP.
Cada cantidad requerida se redondea a 2 decimales con HALF_UP. `enoughStock` y
`missingGrams` comparan esa cantidad con el stock actual, sin descontarlo.

El costo estimado es `requiredQuantityGrams * costPerKilogram / 1000`. El costo
por unidad se divide con 6 decimales y HALF_UP.

## Persistencia

Flyway aplica `V7__crear_tablas_recetas.sql`. La FK de variedad reproduce el
tipo real `INT UNSIGNED` y apunta a `variedad_empanada(id_variedad)`. La migración crea `recipes` y
`recipe_ingredients`, restricciones positivas, claves foráneas e índices.

La columna generada `active_variety_id` permite un índice único que admite
múltiples versiones históricas inactivas y solo una activa por variedad.

## Verificación

```powershell
.\mvnw.cmd -Dtest=RecipeIntegrationTest test
.\mvnw.cmd test
```

`RecipeIntegrationTest` cubre el contrato HTTP, reglas, cálculos, costos actuales,
inmutabilidad, stock intacto y creaciones/versiones concurrentes. La migración se
puede comprobar en un esquema MySQL vacío con:

```powershell
.\mvnw.cmd "-Drecipe.mysqlTestUrl=jdbc:mysql://127.0.0.1:33317/recipe_v7_migration_test" -Dtest=RecipeMigrationMySqlTest test
```
