# Módulo de empleados y jornadas

El módulo administra empleados, sus valores por hora y las jornadas efectivamente trabajadas. Todas las rutas requieren un JWT con el rol `ADMINISTRADOR`.

## Empleados

- `POST /api/v2/employees`: crea un empleado activo.
- `GET /api/v2/employees?status=ACTIVE|INACTIVE|ALL`: lista por estado; el valor predeterminado es `ACTIVE`.
- `GET /api/v2/employees/search?q=ana`: busca por nombre entre empleados activos.
- `GET /api/v2/employees/{id}`: obtiene un empleado.
- `PUT /api/v2/employees/{id}`: actualiza nombre, valor hora y notas.
- `PATCH /api/v2/employees/{id}/activate`: activa.
- `PATCH /api/v2/employees/{id}/deactivate`: desactiva sin eliminar su historial.
- `GET /api/v2/employees/{id}/workdays`: historial paginado y totales; acepta `from`, `to`, `page`, `size` y `sort`.
- `GET /api/v2/employees/dashboard`: métricas de hoy, semana y mes.

Solicitud para alta o modificación:

```json
{
  "name": "Ana Pérez",
  "hourlyRate": 3500.00,
  "notes": "Cocina"
}
```

## Jornadas

- `POST /api/v2/employee-workdays`: crea una jornada.
- `POST /api/v2/employee-workdays/bulk`: crea la misma jornada para varios empleados de forma atómica.
- `GET /api/v2/employee-workdays/{id}`: obtiene la jornada con sus turnos.
- `PUT /api/v2/employee-workdays/{id}`: corrige fecha, notas y turnos; conserva el valor hora histórico.
- `POST /api/v2/employee-workdays/{id}/copy`: copia turnos y notas a otra fecha usando el valor hora actual.
- `DELETE /api/v2/employee-workdays/{id}`: elimina una carga errónea.
- `GET /api/v2/employee-workdays/history`: historial paginado. Acepta `employeeId`, `date`, `from`, `to`, `period=TODAY|WEEK|MONTH|CUSTOM`, `page`, `size` y `sort`.
- `GET /api/v2/employee-workdays/week?date=2026-09-23`: planilla lunes a domingo.
- `GET /api/v2/employee-workdays/summary/week?date=2026-09-23`: resumen semanal.
- `GET /api/v2/employee-workdays/summary/month?year=2026&month=9`: resumen mensual.
- `GET /api/v2/employee-workdays/summary/day?date=2026-09-23`: resumen diario.

Solicitud individual:

```json
{
  "employeeId": 1,
  "workDate": "2026-09-23",
  "notes": "Turno normal",
  "shifts": [
    {
      "startTime": "08:00",
      "endTime": "12:00",
      "breakMinutes": 0
    },
    {
      "startTime": "16:00",
      "endTime": "20:00",
      "breakMinutes": 15
    }
  ]
}
```

Solicitud masiva:

```json
{
  "employeeIds": [1, 2, 3],
  "workDate": "2026-09-23",
  "notes": "Producción general",
  "shifts": [
    {
      "startTime": "08:00",
      "endTime": "12:00",
      "breakMinutes": 0
    }
  ]
}
```

Cada empleado solo puede tener una jornada por fecha. Los turnos se ordenan por hora, pueden ser contiguos, no pueden superponerse ni cruzar medianoche y el descanso debe ser menor que la duración del turno. El backend calcula minutos y monto con `valorHoraSnapshot × minutos / 60`, redondeado a dos decimales con `HALF_UP`.

Cuando una carga masiva encuentra jornadas ya existentes, responde `409` con los empleados en conflicto y no crea ninguna jornada:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Ya existen jornadas para uno o más empleados en la fecha indicada.",
  "path": "/api/v2/employee-workdays/bulk",
  "conflicts": [
    { "employeeId": 2, "employeeName": "Beatriz Gómez" }
  ]
}
```

El tamaño máximo de página es `100`. Las fechas relativas se resuelven con la zona `America/Argentina/Buenos_Aires`.
