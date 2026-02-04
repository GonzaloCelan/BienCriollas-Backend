# Sistema de Gestión Gastronómica

Backend desarrollado en **Java + Spring Boot** para la gestión integral de un emprendimiento gastronómico.
El sistema centraliza pedidos, control de stock, caja diaria, egresos y estadísticas de negocio, modelando
situaciones reales de operación diaria.

---

## 🎯 Problema que resuelve

En muchos emprendimientos gastronómicos la gestión diaria se realiza de forma manual o con herramientas
desconectadas entre sí (pedidos, pagos, stock, caja).

**Bien Criollas** surge para resolver este problema mediante un sistema que:

- Centraliza los pedidos en un único flujo
- Controla el stock de productos en tiempo real
- Registra ingresos y egresos en la caja diaria
- Maneja distintos tipos de pago
- Genera métricas claras para la toma de decisiones

---

## ⚙️ Funcionalidades principales

- Gestión de pedidos (Particular / PedidosYa)
- Estados de pedido (Pendiente, Preparado, Cancelado, etc.)
- Manejo de pagos:
  - Efectivo
  - Transferencia
  - Pago combinado
- Control de stock por variedad de producto
- Descuento automático de stock al crear pedidos
- Devolución de stock al cancelar pedidos
- Caja diaria:
  - Apertura automática por fecha
  - Registro de ingresos y egresos
  - Cálculo de balance final
- Gestión de egresos y mermas
- Estadísticas y resúmenes:
  - Totales diarios y semanales
  - Resúmenes acumulados
  - Métricas por tipo de ingreso
- Paginación y filtros por estado y fecha
- Uso de DTOs para separar dominio interno de contratos externos

---

## 🧱 Arquitectura

El proyecto sigue una arquitectura en capas clara:

- **Controller**: expone los endpoints REST
- **Service**: contiene la lógica de negocio
- **Repository**: acceso a datos mediante Spring Data JPA
- **DTOs**: separación entre entidades y datos expuestos

Principios aplicados:

- Inyección de dependencias por constructor
- Uso de `@Transactional` para consistencia de datos
- Manejo explícito de reglas de negocio
- Evita exponer entidades directamente

---

## 🛠️ Stack tecnológico

- Java 17
- Spring Boot
- Spring Data JPA
- Hibernate
- MySQL
- Lombok
- Maven

---

## 🗃️ Base de datos

- Base de datos relacional (MySQL)
- Modelado orientado a negocio real
- Restricciones de integridad
- Manejo de concurrencia en operaciones críticas (caja diaria)

---

## 🎨 Frontend

El frontend fue desarrollado como una capa de presentación básica para
consumir la API, utilizando JavaScript y herramientas de asistencia
basadas en inteligencia artificial.

El objetivo del proyecto se centra en el backend y en la lógica de negocio.


## 🧪 Estado del proyecto

Proyecto en evolución continua.

Actualmente se encuentra en una etapa funcional sólida, con foco en:
- Robustez de la lógica de negocio
- Escalabilidad del diseño
- Preparación para una posible conversión a modelo SaaS

---

## 🚀 Objetivo del proyecto

Este proyecto fue desarrollado con fines:

- Educativos
- Profesionales
- Portfolio backend

Apuntando a demostrar:
- Conocimiento real de Spring Boot
- Diseño orientado a dominio
- Manejo de lógica de negocio compleja
- Buenas prácticas en aplicaciones backend

---

---

## 🔒 Acceso y entorno

Este sistema se encuentra actualmente desplegado y en uso real
en un entorno productivo.

Por razones de seguridad y consistencia de datos:
- No se expone una URL pública de prueba
- No se proveen credenciales de acceso
- No se recomienda consumir la API directamente

El objetivo de este repositorio es mostrar el diseño, la arquitectura
y la lógica de negocio del backend.

