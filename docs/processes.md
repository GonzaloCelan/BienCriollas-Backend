# Procesos estándar de Producción

El módulo está en `production/process` y documenta cómo se elabora cada
variedad. No representa una ejecución real y nunca modifica recetas,
ingredientes ni stock.

## Endpoints

La base es `/api/v1/processes` y todos los endpoints requieren Bearer JWT.

| Método | Ruta relativa | Operación |
| --- | --- | --- |
| POST | `/` | Crear el primer proceso de una variedad |
| GET | `/` | Listar procesos vigentes paginados |
| GET | `/{id}` | Obtener un proceso vigente o histórico |
| GET | `/variety/{varietyId}` | Obtener el proceso vigente de una variedad |
| GET | `/variety/{varietyId}/history` | Historial por versión descendente |
| GET | `/status?active=true` | Listar procesos por estado |
| POST | `/{id}/versions` | Crear una nueva versión |

No existe actualización directa ni `DELETE`. Los cambios se registran creando
una nueva versión.

## Creación

```json
{
  "varietyId": 1,
  "referenceYieldUnits": 100,
  "notes": "Proceso estándar para empanadas de carne.",
  "steps": [
    {
      "name": "Preparar verduras",
      "description": "Cortar cebolla y morrón.",
      "estimatedMinutes": 20,
      "requiredPeople": 1,
      "timeType": "ACTIVE",
      "notes": null
    },
    {
      "name": "Enfriar relleno",
      "description": "Dejar enfriar completamente.",
      "estimatedMinutes": 60,
      "requiredPeople": 0,
      "timeType": "WAITING",
      "notes": "No armar con el relleno caliente."
    }
  ]
}
```

Para crear una versión se envía el mismo contenido sin `varietyId` a
`POST /api/v1/processes/{id}/versions`. El ID puede corresponder a una versión
vigente o histórica; siempre se incrementa la última versión de la variedad.

## Reglas

- La variedad debe existir y sólo puede tener un proceso vigente.
- El rendimiento de referencia debe ser mayor a cero.
- Debe existir al menos un paso.
- `stepOrder` se genera desde la posición del array, comenzando en 1.
- El nombre del paso admite 120 caracteres, la descripción 1000 y sus notas 500.
- `estimatedMinutes` debe ser mayor a cero.
- `requiredPeople` no puede ser negativo.
- Un paso `ACTIVE` necesita al menos una persona; uno `WAITING` puede usar cero.
- Los tiempos son valores de referencia y no se escalan automáticamente.
- Una versión nueva desactiva la vigente y conserva intacto el histórico.

## Métricas calculadas

- `stepCount`: cantidad de pasos.
- `totalEstimatedMinutes`: suma de todos los tiempos, considerados secuenciales.
- `activeMinutes`: suma de los pasos `ACTIVE`.
- `waitingMinutes`: suma de los pasos `WAITING`.
- `estimatedPersonMinutes`: suma de `estimatedMinutes * requiredPeople`.
- `estimatedPersonHours`: minutos-persona divididos por 60, con dos decimales.

Estas métricas no se guardan en la base. Se calculan al construir la respuesta.

## Persistencia

Flyway aplica `V8__crear_tablas_procesos_produccion.sql`. La FK de variedad usa
el tipo real `INT UNSIGNED`. La migración crea `production_processes` y
`production_process_steps`, agrega sus restricciones y utiliza una columna
generada para garantizar un solo proceso activo por variedad.

## Verificación

```powershell
.\mvnw.cmd -Dtest=ProductionProcessIntegrationTest test
.\mvnw.cmd test
```

La migración puede comprobarse en un esquema MySQL vacío y dedicado mediante:

```powershell
.\mvnw.cmd "-Dprocess.mysqlTestUrl=jdbc:mysql://127.0.0.1:3306/process_v8_migration_test" -Dtest=ProductionProcessMigrationMySqlTest test
```
