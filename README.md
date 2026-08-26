# Bien Criollas Backend

Backend del sistema de gestión de **Bien Criollas**, desarrollado con Java y Spring Boot para centralizar pedidos, catálogo, stock, ingresos, egresos, mermas y estadísticas de la operación diaria.

La API está organizada por módulos de negocio, ofrece documentación interactiva con Swagger/OpenAPI y publica eventos WebSocket para mantener sincronizados los pedidos entre computadoras, celulares y tablets.

## Contenido

- [Funcionalidades](#funcionalidades)
- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Requisitos](#requisitos)
- [Configuración local](#configuración-local)
- [Variables de entorno](#variables-de-entorno)
- [Swagger y OpenAPI](#swagger-y-openapi)
- [Resumen de endpoints](#resumen-de-endpoints)
- [Reglas principales de pedidos](#reglas-principales-de-pedidos)
- [Ejemplos de uso](#ejemplos-de-uso)
- [WebSocket en tiempo real](#websocket-en-tiempo-real)
- [Pruebas](#pruebas)
- [Despliegue](#despliegue)
- [Seguridad](#seguridad)

## Funcionalidades

### Pedidos

- Creación de pedidos particulares y de PedidosYa.
- Edición completa de pedidos pendientes o preparados.
- Estados `PENDIENTE`, `PREPARADO`, `ENTREGADO` y `CANCELADO`.
- Pagos en efectivo, transferencia o modalidad combinada.
- Registro del horario de entrega para pedidos particulares.
- Consulta por estado, fecha y paginación.
- Consulta del detalle de variedades y cantidades.
- Notificaciones WebSocket al crear, editar, cancelar o cambiar el estado.

### Stock y catálogo

- Registro de producción por variedad y fecha de elaboración.
- Descuento automático de stock al crear o modificar pedidos.
- Reposición del stock anterior al editar o cancelar un pedido.
- Ajustes manuales de disponibilidad.
- Registro de pérdidas y mermas.
- Consulta del stock general o por variedad.
- Administración de precios por unidad, media docena y docena.

### Gestión financiera

- Registro y categorización de egresos.
- Historial paginado por mes y tipo.
- Comparación del mes actual contra el anterior.
- Totales de egresos por categoría.
- Resumen de ingresos por período.
- Registro de liquidaciones de PedidosYa.
- Estadísticas consolidadas de la operación.

## Arquitectura

El proyecto utiliza una organización **package by feature**: cada módulo reúne sus controllers, DTOs, entidades, interfaces, repositorios, servicios y enums. De esta manera, las clases de una misma funcionalidad permanecen juntas y el sistema puede crecer sin volver a una estructura global difícil de mantener.

```text
src/main/java/com/bienCriollas/stock/
├── config/
│   ├── CorsConfig.java
│   ├── OpenApiConfig.java
│   ├── SecurityConfig.java
│   └── WebSocketConfig.java
├── egreso/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── enums/
│   ├── interfaces/
│   ├── repository/
│   └── service/
├── estadistica/
├── ingreso/
├── merma/
├── pedido/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── enums/
│   ├── interfaces/
│   ├── repository/
│   └── service/
├── stock/
├── variedad/
└── StockApplication.java
```

### Responsabilidad de cada capa

| Capa | Responsabilidad |
|---|---|
| `controller` | Expone endpoints HTTP y adapta la entrada y salida de la API. |
| `service` | Aplica reglas de negocio y coordina operaciones transaccionales. |
| `repository` | Accede a MySQL mediante Spring Data JPA. |
| `entity` | Representa el modelo persistido. |
| `dto` | Define los contratos de entrada y respuesta sin acoplar el frontend a las entidades. |
| `interfaces` | Declara contratos de servicios y proyecciones. |
| `enums` | Centraliza valores válidos del dominio. |
| `config` | Configura CORS, seguridad, Swagger/OpenAPI y WebSocket. |

## Tecnologías

| Tecnología | Uso |
|---|---|
| Java 17 | Lenguaje y versión configurada para compilación. |
| Spring Boot 4.0.0 | Base de la aplicación. |
| Spring Web MVC | API REST. |
| Spring Data JPA | Persistencia y repositorios. |
| Hibernate | Implementación ORM. |
| MySQL | Base de datos productiva y local. |
| Spring Security | Cadena de seguridad HTTP. |
| Spring WebSocket + STOMP | Actualizaciones de pedidos en tiempo real. |
| springdoc-openapi 3.0.3 | Generación de OpenAPI y Swagger UI para Spring Boot 4. |
| Jakarta Validation | Validación declarativa de requests. |
| Lombok | Reducción de código repetitivo. |
| Maven Wrapper | Compilación, pruebas y ejecución reproducibles. |
| JUnit 5 + Mockito + H2 | Pruebas automatizadas. |

## Requisitos

- Java Development Kit 17 o superior.
- MySQL 8 o un servicio MySQL compatible.
- Git, únicamente si se desea clonar el repositorio.
- No es necesario instalar Maven globalmente: el proyecto incluye Maven Wrapper.

## Configuración local

### 1. Clonar el repositorio

```bash
git clone https://github.com/GonzaloCelan/BienCriollas-Backend.git
cd BienCriollas-Backend
```

### 2. Crear la base de datos

```sql
CREATE DATABASE biencriollas_local
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

La propiedad `spring.jpa.hibernate.ddl-auto` está configurada como `none`. Por lo tanto, Hibernate no crea ni modifica las tablas automáticamente y el esquema debe existir antes de iniciar la aplicación.

### 3. Configurar las credenciales

La configuración compartida y sin secretos está en `application.yml`. Las credenciales reales se leen desde variables de entorno.

PowerShell:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/biencriollas_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Argentina/Buenos_Aires&characterEncoding=utf8"
$env:DB_USER="root"
$env:DB_PASSWORD="tu_password"
```

Bash:

```bash
export DB_URL='jdbc:mysql://localhost:3306/biencriollas_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Argentina/Buenos_Aires&characterEncoding=utf8'
export DB_USER='root'
export DB_PASSWORD='tu_password'
```

Como alternativa únicamente para desarrollo local, se puede copiar `src/main/resources/application.properties.example` como `src/main/resources/application.properties` y completar los valores. Ese archivo está incluido en `.gitignore` y no debe agregarse al repositorio.

### 4. Ejecutar la aplicación

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux o macOS:

```bash
./mvnw spring-boot:run
```

Por defecto, el backend queda disponible en `http://localhost:8080`.

## Variables de entorno

| Variable | Obligatoria | Valor local predeterminado | Descripción |
|---|---:|---|---|
| `PORT` | No | `8080` | Puerto HTTP utilizado por la aplicación. |
| `DB_URL` | En producción | URL de `biencriollas_local` | URL JDBC completa de MySQL. |
| `DB_USER` | En producción | `root` | Usuario de la base de datos. |
| `DB_PASSWORD` | Sí | Sin valor | Contraseña de la base de datos. |

Las credenciales se deben definir siempre mediante variables de entorno o en el archivo local ignorado, nunca en archivos versionados.

## Swagger y OpenAPI

Con la aplicación iniciada, la documentación interactiva se encuentra en:

```text
Swagger UI:   http://localhost:8080/swagger-ui.html
OpenAPI JSON: http://localhost:8080/v3/api-docs
OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml
```

Swagger UI permite explorar los endpoints por módulo, consultar parámetros y esquemas, ver ejemplos, ejecutar operaciones con **Try it out** y descargar el contrato OpenAPI.

La configuración general se encuentra en `config/OpenApiConfig.java`. Springdoc publica las rutas estándar de Swagger UI y OpenAPI automáticamente.

## Resumen de endpoints

Todas las rutas REST utilizan la versión `/api/v2`.

### Pedidos — `/api/v2/pedido`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/crear` | Crea un pedido, descuenta stock y publica un evento WebSocket. |
| `PUT` | `/actualizar/{id}` | Reemplaza los datos y detalles completos del pedido. |
| `PUT` | `/actualizar-estado/{id}/{nuevoEstado}` | Cambia el estado y lo comunica en tiempo real. |
| `PUT` | `/actualizar-pago/{id}/{nuevoPago}` | Cambia el medio de pago; `COMBINADO` recibe los dos importes en JSON. |
| `GET` | `/pedido-estado/{estado}?page=0&size=10` | Lista pedidos del día por estado. |
| `GET` | `/por-fecha/{fecha}` | Lista pedidos de una fecha. |
| `GET` | `/detalle/{id}` | Obtiene las variedades y cantidades del pedido. |
| `GET` | `/paginado?estado=PENDIENTE&page=0&size=10` | Consulta paginada por estado. |

### Stock — `/api/v2/stock`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/actualizar` | Registra producción para varias variedades. |
| `GET` | `/obtener-stock-actual` | Obtiene el stock activo de todas las variedades. |
| `GET` | `/obtener-variedad/{idVariedad}` | Consulta los registros de una variedad. |
| `POST` | `/descontarStock/{idVariedad}/{cantidad}` | Realiza un descuento manual. |
| `POST` | `/perdidas` | Registra mermas o pérdidas. |
| `POST` | `/ajustar` | Corrige el stock disponible. |

### Catálogo — `/api/v2/catalogo`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/` | Lista las variedades y sus precios. |
| `PUT` | `/{id}` | Actualiza los precios de una variedad. |

### Ingresos — `/api/v2/ingresos`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/resumen?desde=2026-08-01&hasta=2026-08-31` | Obtiene el resumen del período. |
| `POST` | `/liquidaciones-pedidos-ya` | Registra una liquidación de PedidosYa. |

### Egresos — `/api/v2/egreso`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/registrar` | Registra un gasto. |
| `GET` | `/acumulado` | Obtiene el total acumulado. |
| `GET` | `/porcentajes` | Compara el mes actual contra el anterior. |
| `GET` | `/totales-tipo?anio=2026&mes=8` | Agrupa totales por categoría. |
| `GET` | `/diario` | Lista los gastos de hoy. |
| `GET` | `/tipo/{tipo}` | Lista gastos paginados por categoría. |
| `GET` | `/historial?anio=2026&mes=8&tipo=PRODUCCION` | Consulta el historial mensual; `tipo` es opcional. |
| `GET` | `/ultimos` | Obtiene los movimientos más recientes. |

### Estadísticas — `/api/v2/estadisticas`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/resumen?desde=2026-08-01&hasta=2026-08-31` | Devuelve métricas consolidadas del período. |

## Valores del dominio

| Campo | Valores permitidos |
|---|---|
| Estado del pedido | `PENDIENTE`, `PREPARADO`, `ENTREGADO`, `CANCELADO` |
| Tipo de venta | `PARTICULAR`, `PEDIDOS_YA` |
| Tipo de pago | `EFECTIVO`, `TRANSFERENCIA`, `COMBINADO` |
| Tipo de egreso | `PERSONAL`, `PRODUCCION`, `OTROS` |

Las fechas se envían en formato `yyyy-MM-dd` y los horarios en formato `HH:mm:ss`.

## Reglas principales de pedidos

- Todo pedido nuevo se crea con estado `PENDIENTE`.
- La creación descuenta del stock las cantidades de cada variedad.
- Un pedido completo solo puede editarse mientras esté `PENDIENTE` o `PREPARADO`.
- La creación, edición, cancelación, producción, ajuste y merma bloquean los registros de stock involucrados antes de modificarlos.
- La edición completa aplica la diferencia neta entre el detalle anterior y el nuevo.
- La actualización es transaccional: si alguna variedad o cantidad falla, no quedan cambios parciales.
- Las variedades se bloquean siempre en el mismo orden para evitar condiciones de carrera y deadlocks.
- Al cancelar un pedido se devuelve su stock disponible.
- En pagos `EFECTIVO`, el total se asigna a efectivo.
- En pagos `TRANSFERENCIA`, el total se asigna a transferencia.
- En pagos `COMBINADO`, efectivo más transferencia deben coincidir exactamente con `totalPedido`.
- Las transiciones permitidas son `PENDIENTE → PREPARADO → ENTREGADO` y `PENDIENTE/PREPARADO → CANCELADO`.
- Un pedido `ENTREGADO` o `CANCELADO` no puede pasar a otro estado.
- `numeroPedidoPedidosYa` se utiliza para ventas provenientes de `PEDIDOS_YA`.
- `horaEntrega` utiliza el formato `HH:mm:ss` y se guarda al crear y al editar.

## Ejemplos de uso

### Crear o actualizar un pedido completo

El mismo contrato se utiliza para crear y actualizar:

```json
{
  "cliente": "Juan Pérez",
  "tipoVenta": "PARTICULAR",
  "tipoPago": "COMBINADO",
  "numeroPedidoPedidosYa": null,
  "horaEntrega": "21:30:00",
  "montoEfectivo": 5000,
  "montoTransferencia": 4000,
  "totalPedido": 9000,
  "detalles": [
    {
      "idVariedad": 2,
      "cantidad": 6
    },
    {
      "idVariedad": 5,
      "cantidad": 6
    }
  ]
}
```

Creación:

```http
POST /api/v2/pedido/crear
Content-Type: application/json
```

Actualización:

```http
PUT /api/v2/pedido/actualizar/123
Content-Type: application/json
```

Respuesta:

```json
{
  "idPedido": 123,
  "cliente": "Juan Pérez",
  "tipoVenta": "PARTICULAR",
  "tipoPago": "COMBINADO",
  "numeroPedidoPedidosYa": null,
  "horaEntrega": "21:30:00",
  "totalPedido": 9000,
  "estadoPedido": "PENDIENTE"
}
```

### Cambiar el estado

```http
PUT /api/v2/pedido/actualizar-estado/123/PREPARADO
```

Respuesta:

```json
true
```

### Cambiar a pago combinado

```http
PUT /api/v2/pedido/actualizar-pago/123/COMBINADO
Content-Type: application/json
```

```json
{
  "montoEfectivo": 5000,
  "montoTransferencia": 4000
}
```

Para cambiar a `EFECTIVO` o `TRANSFERENCIA` el body es opcional. Para `COMBINADO`, ambos importes son obligatorios, no pueden ser negativos y su suma debe coincidir con `totalPedido`.

### Respuestas de error

Los errores se devuelven sin stack trace ni información interna. Por ejemplo, un pedido inexistente responde `404 Not Found`:

```json
{
  "timestamp": "2026-08-26T12:04:13-03:00",
  "status": 404,
  "error": "Not Found",
  "message": "No se encontró el pedido con id 999999",
  "path": "/api/v2/pedido/detalle/999999"
}
```

Las validaciones de entrada responden `400 Bad Request`; falta de stock y transiciones de estado inválidas responden `409 Conflict`.

### Registrar producción

```json
[
  {
    "id_variedad": 2,
    "fecha_elaboracion": "2026-08-26",
    "stock_total": 48
  }
]
```

```http
POST /api/v2/stock/actualizar
Content-Type: application/json
```

## WebSocket en tiempo real

La aplicación utiliza STOMP sobre SockJS.

| Recurso | Valor |
|---|---|
| Endpoint de conexión | `/ws` |
| Canal de pedidos | `/topic/pedidos` |
| Prefijo de mensajes hacia el servidor | `/app` |

Eventos publicados:

| Tipo | Momento |
|---|---|
| `CREADO` | Se registra un pedido nuevo. |
| `ACTUALIZADO` | Se edita el pedido o cambia su estado. |
| `CANCELADO` | El pedido cambia al estado cancelado. |

Ejemplo:

```json
{
  "tipo": "ACTUALIZADO",
  "idPedido": 123,
  "estado": "PREPARADO"
}
```

Ejemplo mínimo para un frontend TypeScript:

```ts
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const client = new Client({
  webSocketFactory: () => new SockJS(`${BACKEND_URL}/ws`),
  reconnectDelay: 5000
});

client.onConnect = () => {
  client.subscribe("/topic/pedidos", (message) => {
    const evento = JSON.parse(message.body);
    console.log("Pedido actualizado", evento);
  });
};

client.activate();
```

El frontend puede actualizar su estado local o invalidar la consulta de pedidos cuando recibe el evento. Así, un cambio realizado desde un celular o tablet aparece en la computadora sin actualizar manualmente la página.

## Pruebas

Ejecutar todas las pruebas:

Windows:

```powershell
.\mvnw.cmd test
```

Linux o macOS:

```bash
./mvnw test
```

La suite incluye:

- Carga del contexto de Spring Boot.
- Actualización completa de pedidos y movimientos de stock.
- Rechazo de edición para pedidos entregados.
- Reglas de transición de estados y pagos combinados.
- Respuestas específicas para pedidos inexistentes.
- Prueba de 40 descuentos simultáneos que verifica que no se pierda ninguna actualización de stock.
- Persistencia y respuesta del horario de entrega durante la creación.

## Despliegue

El backend está preparado para plataformas que inyectan configuración mediante variables de entorno, como Railway.

Variables mínimas de producción:

```env
PORT=8080
DB_URL=jdbc:mysql://HOST:PUERTO/BASE_DE_DATOS
DB_USER=usuario
DB_PASSWORD=contraseña
```

Una vez desplegado:

```text
API:         https://TU-DOMINIO/api/v2/...
Swagger UI:  https://TU-DOMINIO/swagger-ui.html
OpenAPI:     https://TU-DOMINIO/v3/api-docs
WebSocket:   https://TU-DOMINIO/ws
```

Los orígenes habilitados para el frontend se administran en `config/CorsConfig.java`.

## Seguridad

El proyecto contiene Spring Security, pero actualmente las solicitudes están configuradas con acceso público mediante `permitAll()`. Swagger también queda accesible para facilitar el desarrollo y las pruebas.

Antes de exponer el sistema a usuarios no confiables se recomienda:

- Incorporar autenticación y autorización.
- Restringir Swagger en producción si la documentación no debe ser pública.
- Limitar los orígenes CORS a los dominios necesarios.
- No publicar credenciales ni secretos en el repositorio o el frontend.
- Utilizar HTTPS para REST y WebSocket.

## Autor

Desarrollado por [Gonzalo Celan](https://github.com/GonzaloCelan).

Repositorio: [GonzaloCelan/BienCriollas-Backend](https://github.com/GonzaloCelan/BienCriollas-Backend)
