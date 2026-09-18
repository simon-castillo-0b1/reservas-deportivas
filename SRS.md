# SRS — API REST de Gestión de Reservas Deportivas

**Versión:** 1.0
**Estado:** Especificación inicial
**Tipo:** Sistema Backend / API REST
**Tecnologías objetivo:** Java, Spring Boot, Spring Data JPA, Hibernate, MySQL, Maven, Docker
**Repositorio objetivo:** `springboot-reservas-deportivas`

---

## Introducción

### Propósito

Este documento define los requisitos funcionales, no funcionales, reglas de negocio, modelo de dominio, contrato HTTP, restricciones técnicas y criterios de aceptación para una API REST destinada a gestionar actividades deportivas y las reservas de usuarios.

La especificación constituye la **fuente de verdad funcional** del proyecto. La implementación debe ajustarse a este documento. Las decisiones que no estén definidas aquí podrán resolverse durante el desarrollo, pero deberán documentarse como decisiones arquitectónicas cuando tengan impacto significativo sobre el diseño.

El proyecto está pensado como pieza de portfolio para demostrar competencias de desarrollo backend con Java y Spring, especialmente:

* diseño de APIs REST;
* separación por capas;
* modelado relacional;
* persistencia con JPA/Hibernate;
* implementación de reglas de negocio;
* validación;
* manejo estructurado de errores;
* transacciones y consistencia;
* testing automatizado;
* documentación de API;
* Docker;
* CI/CD.

El objetivo no es construir una aplicación deportiva comercial completa, sino un backend pequeño pero técnicamente defendible.

### Metodología de desarrollo

El proyecto se desarrollará siguiendo un enfoque **AI-Driven Development (AI-DD)**: la IA participa de forma activa en las distintas etapas del ciclo de desarrollo, pero el proceso está gobernado por especificaciones, contexto explícito, reglas del proyecto, pruebas y revisión humana. Este enfoque se diferencia de utilizar la IA únicamente como autocompletado de código.

Por tanto:

**La especificación define qué debe construirse.**

**La arquitectura define las restricciones de cómo debe construirse.**

**Los tests verifican que se haya construido correctamente.**

**La IA propone e implementa soluciones dentro de esas restricciones.**

**El desarrollador mantiene la decisión final.**

---

# Descripción general

## Problema

Los centros deportivos suelen gestionar actividades con cupos limitados. Un usuario debe poder consultar actividades disponibles y reservar una plaza.

El sistema debe impedir situaciones inconsistentes como:

* un mismo usuario reservando dos veces la misma actividad;
* superar el cupo máximo de una actividad;
* reservar una actividad cancelada;
* reservar una actividad que ya comenzó;
* cancelar una reserva después de iniciada la actividad;
* registrar asistencia para una reserva inexistente;
* registrar asistencia sobre una reserva cancelada;
* realizar transiciones de asistencia inválidas.

El backend deberá centralizar estas reglas para que ningún cliente pueda evitarlas simplemente construyendo una petición HTTP distinta.

## Objetivos

El sistema debe permitir:

* gestionar usuarios;
* gestionar actividades deportivas;
* consultar disponibilidad;
* crear y cancelar reservas;
* consultar reservas;
* registrar asistencia;
* consultar asistencia;
* mantener la integridad de los datos;
* exponer una API documentada;
* ejecutarse de forma reproducible mediante Docker;
* ejecutar tests automáticamente mediante CI.

## Fuera de alcance

Las siguientes funcionalidades no forman parte de la versión inicial:

* aplicación web frontend;
* aplicación móvil;
* pagos;
* notificaciones por email o WhatsApp;
* integración con calendarios externos;
* autenticación JWT;
* autorización por roles;
* multi-tenancy;
* microservicios;
* mensajería asíncrona;
* despliegue en cloud;
* recomendaciones deportivas;
* gestión de entrenadores como usuarios especializados.

Estas funcionalidades pueden considerarse extensiones futuras, pero no deberán incorporarse automáticamente durante la implementación.

---

# Actores

## Usuario

Persona que utiliza el sistema para consultar actividades y realizar reservas.

## Administrador

Actor conceptual encargado de mantener usuarios y actividades.

La versión inicial no implementará autenticación/autorización. El backend podrá asumir que determinadas operaciones son administrativas a nivel de diseño, pero no deberá introducir Spring Security salvo que posteriormente se amplíe el alcance.

## Sistema

La propia aplicación backend, responsable de aplicar las reglas de negocio, validar datos y mantener la integridad.

---

# Alcance funcional

El sistema estará dividido conceptualmente en cuatro módulos:

```text
Usuarios
    │
    ├── realizan
    │
    ▼
Reservas ───────────► Actividades
    │                       │
    │                       └── poseen cupo
    │
    ▼
Asistencia
```

Las entidades principales serán:

```text
User
Activity
Reservation
Attendance
```

---

# Requisitos funcionales

## Gestión de usuarios

El sistema deberá permitir crear, consultar, actualizar y eliminar usuarios.

### RF-USR-001 — Crear usuario

El sistema deberá permitir crear un usuario proporcionando:

* nombre;
* apellido;
* email.

El email deberá ser único.

No deberá permitirse crear dos usuarios con el mismo email.

### RF-USR-002 — Consultar usuario

El sistema deberá permitir consultar un usuario mediante su identificador.

Si el usuario no existe deberá devolverse un error `404 Not Found`.

### RF-USR-003 — Listar usuarios

El sistema deberá permitir obtener una colección de usuarios.

La respuesta deberá incluir únicamente los campos correspondientes al recurso público definido por el DTO.

### RF-USR-004 — Actualizar usuario

El sistema deberá permitir modificar los datos editables de un usuario.

El email continuará sujeto a la restricción de unicidad.

### RF-USR-005 — Eliminar usuario

El sistema deberá permitir eliminar un usuario únicamente cuando la eliminación no genere inconsistencias con el historial de reservas.

Una estrategia válida será rechazar la eliminación de usuarios que posean reservas históricas.

La implementación deberá elegir explícitamente una política y documentarla.

---

# Gestión de actividades

Una actividad representa una sesión deportiva concreta con fecha, hora y cupo.

Ejemplo:

```text
Yoga
2026-10-08 18:00
Duración: 60 minutos
Cupo: 20 personas
Estado: PROGRAMADA
```

### RF-ACT-001 — Crear actividad

El sistema deberá permitir crear una actividad con al menos:

* nombre;
* deporte;
* fecha y hora de inicio;
* duración;
* capacidad máxima.

La capacidad deberá ser un número entero positivo.

La duración deberá ser positiva.

La fecha de inicio no podrá estar en el pasado en el momento de creación.

### RF-ACT-002 — Consultar actividad

El sistema deberá permitir obtener una actividad mediante su ID.

La respuesta deberá incluir:

* datos básicos;
* estado;
* capacidad máxima;
* cantidad de plazas ocupadas;
* cantidad de plazas disponibles.

### RF-ACT-003 — Listar actividades

El sistema deberá permitir consultar actividades.

La implementación deberá contemplar al menos filtrado por:

* deporte;
* estado;
* rango temporal.

Los filtros podrán combinarse.

### RF-ACT-004 — Actualizar actividad

El sistema deberá permitir actualizar los datos editables de una actividad futura.

No deberá permitirse modificar arbitrariamente una actividad cuyo historial ya haya comenzado.

La implementación deberá separar explícitamente los campos modificables de los no modificables.

### RF-ACT-005 — Cancelar actividad

El sistema deberá permitir cancelar una actividad.

Cancelar una actividad no deberá eliminarla físicamente de la base de datos.

El historial deberá mantenerse para preservar la trazabilidad de las reservas.

---

# Gestión de reservas

Una reserva representa la relación entre un usuario y una actividad.

```text
Usuario 1 ─────── N Reserva N ─────── 1 Actividad
```

Un usuario no podrá tener más de una reserva activa para la misma actividad.

### RF-RES-001 — Crear reserva

El sistema deberá permitir crear una reserva indicando:

* usuario;
* actividad.

El sistema deberá verificar todas las reglas de negocio antes de persistirla.

### RF-RES-002 — Consultar reserva

El sistema deberá permitir consultar una reserva por ID.

### RF-RES-003 — Cancelar reserva

El sistema deberá permitir cancelar una reserva existente.

La cancelación deberá cambiar el estado de la reserva y no eliminarla físicamente.

### RF-RES-004 — Listar reservas de usuario

El sistema deberá permitir obtener las reservas correspondientes a un usuario.

El resultado deberá permitir distinguir:

* reservas confirmadas;
* reservas canceladas.

### RF-RES-005 — Listar reservas de actividad

El sistema deberá permitir consultar las reservas pertenecientes a una actividad.

La respuesta deberá permitir conocer:

* usuario;
* estado de reserva;
* estado de asistencia.

---

# Gestión de asistencia

Cada reserva tendrá asociada como máximo una entidad de asistencia.

Estados posibles:

```text
PENDIENTE
PRESENTE
AUSENTE
```

Transiciones permitidas:

```text
PENDIENTE
   ├──► PRESENTE
   └──► AUSENTE
```

No se permitirán transiciones posteriores:

```text
PRESENTE ──X──► AUSENTE
AUSENTE  ──X──► PRESENTE
```

La decisión de mantener este modelo deberá garantizarse tanto desde la lógica de negocio como mediante tests.

### RF-ATT-001 — Registrar asistencia

El sistema deberá permitir registrar el estado de asistencia de una reserva.

### RF-ATT-002 — Consultar asistencia de actividad

El sistema deberá permitir consultar la asistencia correspondiente a una actividad.

### RF-ATT-003 — Consultar asistencia de usuario

El sistema deberá permitir consultar el historial de asistencia de un usuario.

---

# Contrato de la API REST

La API utilizará el prefijo:

```text
/api
```

## Usuarios

```http
POST   /api/users
GET    /api/users
GET    /api/users/{id}
PUT    /api/users/{id}
DELETE /api/users/{id}
```

## Actividades

```http
POST   /api/activities
GET    /api/activities
GET    /api/activities/{id}
PUT    /api/activities/{id}
DELETE /api/activities/{id}
```

El `DELETE` de una actividad representará conceptualmente una **cancelación lógica**, no necesariamente un `DELETE` físico de la fila.

## Reservas

```http
POST   /api/reservations
GET    /api/reservations/{id}
DELETE /api/reservations/{id}
GET    /api/users/{userId}/reservations
GET    /api/activities/{activityId}/reservations
```

## Asistencia

```http
PUT    /api/reservations/{reservationId}/attendance
GET    /api/activities/{activityId}/attendance
GET    /api/users/{userId}/attendance
```

**Total objetivo: 18 endpoints.**

El número de endpoints no es un requisito de calidad por sí mismo. Cada endpoint deberá corresponder a una necesidad funcional real.

---

# Formatos de datos

## User

```json
{
  "id": 1,
  "firstName": "Juan",
  "lastName": "Pérez",
  "email": "juan@example.com"
}
```

### Creación

```json
{
  "firstName": "Juan",
  "lastName": "Pérez",
  "email": "juan@example.com"
}
```

---

# Activity

```json
{
  "id": 10,
  "name": "Clase de Yoga",
  "sport": "YOGA",
  "startAt": "2026-10-08T18:00:00",
  "durationMinutes": 60,
  "capacity": 20,
  "reservedSpots": 13,
  "availableSpots": 7,
  "status": "PROGRAMADA"
}
```

Los campos calculados no deberán almacenarse necesariamente de forma redundante si pueden obtenerse de las reservas.

---

# Reservation

```json
{
  "id": 50,
  "userId": 1,
  "activityId": 10,
  "status": "CONFIRMADA",
  "createdAt": "2026-10-01T12:30:00"
}
```

---

# Attendance

```json
{
  "reservationId": 50,
  "status": "PRESENTE",
  "registeredAt": "2026-10-08T18:20:00"
}
```

---

# Códigos HTTP

La API deberá utilizar códigos HTTP coherentes.

### Éxito

```text
200 OK
201 Created
204 No Content
```

### Errores del cliente

```text
400 Bad Request
404 Not Found
409 Conflict
```

El sistema deberá evitar devolver `500 Internal Server Error` para errores de dominio previsibles.

---

# Manejo de errores

El backend deberá utilizar un mecanismo centralizado mediante:

```java
@RestControllerAdvice
```

La respuesta de error deberá tener una estructura consistente.

Ejemplo:

```json
{
  "timestamp": "2026-10-01T15:30:00Z",
  "status": 409,
  "error": "RESERVATION_ALREADY_EXISTS",
  "message": "The user already has a reservation for this activity",
  "path": "/api/reservations"
}
```

Los errores deberán clasificarse como mínimo en:

```text
RESOURCE_NOT_FOUND
VALIDATION_ERROR
DUPLICATE_RESOURCE
BUSINESS_RULE_VIOLATION
CONFLICT
```

No deberán exponerse stack traces ni detalles internos al cliente.

---

# Reglas de negocio

Esta sección es crítica. Las reglas deberán implementarse en la capa de dominio/servicio y estar cubiertas por tests.

## Reserva duplicada

Un usuario no podrá tener dos reservas para la misma actividad.

La regla deberá estar protegida en dos niveles:

```text
Regla de negocio
        +
Restricción de base de datos
```

La base de datos deberá utilizar una restricción de unicidad apropiada sobre:

```text
(user_id, activity_id)
```

La aplicación deberá traducir una violación de esta restricción a un error funcional coherente.

## Control de cupo

Una actividad no podrá superar su capacidad máxima.

Ejemplo:

```text
capacidad = 20
reservas activas = 20

nueva reserva → RECHAZADA
```

La disponibilidad deberá calcularse excluyendo reservas canceladas.

## Concurrencia

El sistema deberá impedir el sobre-reservationamiento cuando dos peticiones intenten reservar simultáneamente la última plaza disponible.

La operación de creación de reserva deberá ejecutarse dentro de una transacción y deberá utilizar un mecanismo explícito de consistencia/concurrencia apropiado para JPA y MySQL.

Una solución aceptable será utilizar bloqueo pesimista sobre la actividad antes de comprobar y actualizar la disponibilidad.

No deberá considerarse suficiente implementar únicamente:

```java
if (availableSpots > 0) {
    createReservation();
}
```

sin contemplar condiciones de carrera.

## Actividad cancelada

No podrá crearse una reserva para una actividad cancelada.

## Actividad iniciada

No podrá crearse una nueva reserva después del inicio de la actividad.

## Cancelación de reserva

Una reserva confirmada podrá cancelarse mientras la actividad no haya comenzado.

Una reserva cancelada no podrá volver a estado confirmada.

## Reserva sobre actividad inexistente

Una reserva deberá rechazarse con `404 Not Found` cuando la actividad indicada no exista.

## Reserva sobre usuario inexistente

Una reserva deberá rechazarse con `404 Not Found` cuando el usuario no exista.

## Asistencia

Solo podrá registrarse asistencia para una reserva válida y no cancelada.

La asistencia no podrá registrarse antes del comienzo de la actividad.

Las transiciones deberán respetar:

```text
PENDIENTE → PRESENTE
PENDIENTE → AUSENTE
```

Cualquier otra transición deberá provocar un error de dominio.

## Integridad histórica

Las reservas y asistencias no deberán eliminarse físicamente como consecuencia de una cancelación normal.

El sistema debe conservar historial.

---

# Modelo de dominio

## User

```text
id
firstName
lastName
email
createdAt
updatedAt
```

Restricciones:

```text
id          PK
email       UNIQUE
firstName   NOT NULL
lastName    NOT NULL
email       NOT NULL
```

## Activity

```text
id
name
sport
startAt
durationMinutes
capacity
status
createdAt
updatedAt
```

Estados:

```text
PROGRAMADA
CANCELADA
FINALIZADA
```

## Reservation

```text
id
user
activity
status
createdAt
cancelledAt
```

Estados:

```text
CONFIRMADA
CANCELADA
```

Restricción:

```text
UNIQUE(user_id, activity_id)
```

## Attendance

```text
id
reservation
status
registeredAt
```

Estados:

```text
PENDIENTE
PRESENTE
AUSENTE
```

Restricción:

```text
UNIQUE(reservation_id)
```

---

# Relaciones

```text
User
 │
 │ 1
 │
 │ N
 ▼
Reservation
 │
 │ N
 │
 │ 1
 ▼
Activity

Reservation
 │
 │ 1
 │
 │ 0..1
 ▼
Attendance
```

Una actividad puede tener muchas reservas.

Un usuario puede tener muchas reservas.

Una reserva pertenece exactamente a un usuario y una actividad.

Una reserva puede tener como máximo una asistencia.

---

# Persistencia

## Base de datos

La aplicación utilizará MySQL como base de datos principal.

Las entidades JPA deberán mapearse utilizando:

```text
@Entity
@Id
@GeneratedValue
@ManyToOne
@OneToOne
```

o las anotaciones equivalentes que correspondan al modelo final.

La implementación deberá evitar exponer directamente entidades JPA desde los controladores.

La API deberá utilizar DTOs.

---

# Arquitectura

Se utilizará una arquitectura por capas:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

Con objetos de transferencia y manejo de errores transversal:

```text
                  ┌───────────────┐
                  │   Controller  │
                  └───────┬───────┘
                          ↓
                  ┌───────────────┐
                  │    Service    │
                  └───────┬───────┘
                          ↓
                  ┌───────────────┐
                  │  Repository   │
                  └───────┬───────┘
                          ↓
                     ┌────────┐
                     │ MySQL  │
                     └────────┘

DTOs
Validation
Exceptions
Mapping
Configuration
```

## Responsabilidades

### Controller

Debe encargarse de:

* HTTP;
* path variables;
* query parameters;
* request/response DTOs;
* códigos HTTP;
* validación de entrada.

No deberá contener reglas de negocio complejas.

### Service

Debe contener:

* casos de uso;
* reglas de negocio;
* transacciones;
* coordinación entre repositorios.

### Repository

Debe encargarse del acceso a persistencia.

No deberá contener reglas de negocio.

### Entity

Representará el estado persistente del dominio.

### DTO

Representará el contrato externo de la API.

### Exception

Representará errores de dominio o aplicación.

---

# Estructura sugerida del proyecto

```text
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── simoncastillo/
│   │           └── reservas/
│   │               ├── controller/
│   │               ├── dto/
│   │               │   ├── user/
│   │               │   ├── activity/
│   │               │   ├── reservation/
│   │               │   └── attendance/
│   │               ├── entity/
│   │               ├── repository/
│   │               ├── service/
│   │               ├── exception/
│   │               ├── mapper/
│   │               ├── config/
│   │               └── ReservaApplication.java
│   │
│   └── resources/
│       ├── application.yml
│       ├── application-local.yml
│       └── application-test.yml
│
└── test/
    └── java/
        └── com/
            └── simoncastillo/
                └── reservas/
                    ├── controller/
                    ├── service/
                    └── repository/
```

Esta estructura podrá modificarse mediante una decisión arquitectónica documentada si durante la implementación aparece una justificación técnica relevante.

---

# Validación

Se utilizará Bean Validation.

Ejemplos:

```text
@NotBlank
@Email
@Size
@NotNull
@Positive
@Future
```

La validación deberá ejecutarse en el borde de la aplicación, pero las reglas de negocio deberán seguir verificándose en la capa de servicio.

La validación de formato y la validación de dominio no deben confundirse.

Ejemplo:

```text
"capacity": -4
```

es un error de validación.

```text
capacity = 20
reservas = 20
nueva reserva
```

es una violación de regla de negocio.

---

# Transacciones

Las operaciones que modifiquen más de una entidad relacionada deberán utilizar transacciones cuando sea necesario para garantizar consistencia.

La creación de una reserva deberá ser transaccional.

El registro de asistencia deberá ser transaccional.

La cancelación de una actividad deberá mantener una operación consistente sobre su estado y las consecuencias de negocio asociadas.

No deberá utilizarse `@Transactional` indiscriminadamente en todos los métodos.

---

# Testing

El proyecto deberá utilizar:

```text
JUnit 5
Mockito
MockMvc
```

El objetivo es cubrir comportamiento y reglas de negocio, no simplemente aumentar el número de tests.

## Tests unitarios

Los servicios deberán contar con tests unitarios que cubran:

* casos exitosos;
* recursos inexistentes;
* conflictos;
* reglas de negocio;
* estados inválidos;
* casos límite.

## Tests de controller

Deberán verificarse:

* status HTTP;
* estructura de respuesta;
* validación;
* serialización;
* errores.

## Tests de integración

Deberán comprobar la interacción real entre:

```text
Controller
Service
Repository
Database
```

para los casos críticos.

La integración con MySQL deberá probarse mediante un mecanismo reproducible. H2 podrá utilizarse para tests rápidos, pero no deberá ser el único mecanismo de validación de persistencia cuando existan diferencias relevantes con MySQL.

Una evolución recomendable es introducir **Testcontainers** para pruebas de integración contra MySQL real.

## Casos mínimos de testing

La suite deberá cubrir como mínimo:

```text
Usuario
    creación correcta
    email duplicado
    validación
    búsqueda inexistente

Actividad
    creación
    capacidad inválida
    fecha inválida
    cancelación
    modificación inválida

Reserva
    creación correcta
    usuario inexistente
    actividad inexistente
    reserva duplicada
    actividad llena
    actividad cancelada
    actividad iniciada
    cancelación
    doble cancelación

Asistencia
    registro presente
    registro ausente
    reserva cancelada
    actividad aún no iniciada
    transición inválida
```

El número final de tests deberá surgir de la implementación. El CV deberá reflejar el número real de tests existentes en el repositorio.

---

# Cobertura de código

Se establecerá como objetivo:

```text
Cobertura global mínima: 80%
```

La cobertura no deberá considerarse suficiente por sí misma. Un test que ejecuta código pero no verifica comportamiento significativo no cumple el objetivo de calidad.

La capa de servicios deberá recibir especial atención.

---

# Documentación de API

La API deberá documentarse mediante OpenAPI/Swagger.

La documentación deberá incluir:

* endpoints;
* parámetros;
* cuerpos de request;
* respuestas;
* códigos HTTP;
* ejemplos;
* errores conocidos.

La documentación deberá ser consistente con la implementación.

No se deberá mantener manualmente una especificación que contradiga el código.

---

# Configuración

La configuración deberá estar externalizada.

No deberán almacenarse credenciales reales en Git.

Variables esperadas:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
```

La aplicación deberá permitir configurar diferentes entornos mediante perfiles.

Ejemplo conceptual:

```text
local
test
docker
```

---

# Docker

El proyecto deberá ejecutarse mediante Docker Compose.

La composición mínima será:

```text
┌─────────────────────┐
│ Spring Boot App     │
│       :8080         │
└──────────┬──────────┘
           │
           │ JDBC
           ▼
┌─────────────────────┐
│       MySQL         │
│        :3306        │
└─────────────────────┘
```

El usuario deberá poder levantar el sistema utilizando una secuencia equivalente a:

```bash
docker compose up --build
```

El README deberá explicar:

* requisitos;
* configuración;
* ejecución;
* acceso a Swagger;
* ejecución de tests;
* apagado del entorno.

---

# GitHub Actions

Cada push y pull request deberá ejecutar automáticamente:

```text
checkout
    ↓
setup Java
    ↓
Maven test
    ↓
build
```

La pipeline deberá fallar si:

* no compila;
* algún test falla;
* una comprobación obligatoria de calidad falla.

La aplicación no deberá requerir servicios externos para ejecutar el pipeline salvo aquellos declarados explícitamente.

---

# Logging

La aplicación deberá disponer de logging mediante SLF4J/Logback.

Deberán registrarse eventos útiles como:

* creación de reserva;
* cancelación de reserva;
* cancelación de actividad;
* errores de negocio;
* errores inesperados.

No deberán registrarse:

* contraseñas;
* credenciales;
* tokens;
* información sensible innecesaria.

Los logs deberán facilitar el diagnóstico de errores sin depender del debugger.

---

# Observabilidad

Como extensión del núcleo del proyecto, la aplicación podrá utilizar Spring Boot Actuator.

Deberá existir al menos un health check accesible de forma controlada.

Ejemplo:

```text
/actuator/health
```

El endpoint no deberá exponer innecesariamente información interna de la infraestructura.

---

# Requisitos no funcionales

## RNF-001 — Mantenibilidad

El código deberá estar organizado de forma que cada clase tenga una responsabilidad clara.

## RNF-002 — Separación de responsabilidades

Los controladores no deberán implementar reglas de negocio.

Los repositorios no deberán implementar decisiones de negocio.

## RNF-003 — Legibilidad

El código deberá priorizar nombres descriptivos y estructuras simples sobre abstracciones innecesarias.

## RNF-004 — Configuración

Las configuraciones dependientes del entorno deberán estar fuera del código fuente.

## RNF-005 — Reproducibilidad

Una persona que clone el repositorio deberá poder levantar el proyecto siguiendo únicamente el README.

## RNF-006 — Integridad

La base de datos deberá colaborar con la aplicación en la protección de invariantes críticas.

## RNF-007 — Testabilidad

Las reglas de negocio deberán poder probarse sin depender necesariamente de la base de datos real.

## RNF-008 — Documentación

La API pública deberá contar con documentación OpenAPI.

## RNF-009 — Automatización

Los tests deberán ejecutarse automáticamente en CI.

---

# Requisitos de diseño

## DTOs

No se deberán devolver entidades JPA directamente desde los controladores.

Debe utilizarse una separación:

```text
Request DTO
     ↓
Service
     ↓
Entity
     ↓
Repository
     ↓
Entity
     ↓
Response DTO
```

## Inyección de dependencias

Se deberá utilizar inyección por constructor.

Evitar:

```java
@Autowired
private ReservationService reservationService;
```

Preferir:

```java
public ReservationController(ReservationService reservationService) {
    this.reservationService = reservationService;
}
```

## Lombok

Lombok podrá utilizarse cuando reduzca boilerplate sin ocultar comportamiento relevante.

No deberá utilizarse indiscriminadamente.

## MapStruct

Podrá incorporarse si el volumen de mapping lo justifica, pero no forma parte del alcance obligatorio inicial.

---

# Seguridad básica

Aunque la autenticación queda fuera del alcance, deberán respetarse prácticas básicas:

* validar toda entrada externa;
* no confiar en IDs recibidos;
* no exponer excepciones internas;
* no almacenar secretos en Git;
* evitar SQL construido mediante concatenación;
* utilizar los mecanismos parametrizados de JPA;
* no devolver información interna innecesaria.

Spring Security no deberá incorporarse únicamente para hacer el proyecto “más grande”.

Será responsabilidad de una futura versión.

---

# Criterios de aceptación

El proyecto podrá considerarse funcionalmente completo cuando:

* se puedan crear usuarios;
* se puedan consultar usuarios;
* se puedan modificar usuarios;
* se puedan eliminar según la política definida;
* se puedan crear actividades;
* se puedan consultar actividades;
* se puedan filtrar actividades;
* se puedan modificar actividades válidas;
* se puedan cancelar actividades;
* se puedan crear reservas;
* se rechacen reservas duplicadas;
* se controle correctamente el cupo;
* se rechacen reservas sobre actividades inválidas;
* se puedan cancelar reservas;
* se puedan consultar reservas;
* se pueda registrar asistencia;
* se rechacen transiciones de asistencia inválidas;
* exista manejo centralizado de errores;
* exista documentación OpenAPI;
* existan tests automatizados;
* exista pipeline CI;
* exista Docker Compose;
* exista documentación suficiente para ejecutar el sistema.

---

# Casos de uso principales

## Crear reserva

```text
Usuario
   │
   ▼
POST /api/reservations
   │
   ▼
Validar request
   │
   ▼
Buscar usuario
   │
   ▼
Buscar actividad
   │
   ▼
¿Actividad reservable?
   │
   ├── NO ──► Error
   │
   ▼
Comprobar duplicado
   │
   ├── EXISTE ──► Conflict
   │
   ▼
Obtener bloqueo/transacción
   │
   ▼
Comprobar cupo
   │
   ├── SIN CUPO ──► Conflict
   │
   ▼
Crear Reservation
   │
   ▼
Crear Attendance(PENDIENTE)
   │
   ▼
Commit
   │
   ▼
201 Created
```

## Registrar asistencia

```text
PUT /api/reservations/{id}/attendance
                │
                ▼
        Buscar Reservation
                │
                ▼
        ¿Existe y está activa?
                │
                ├── NO → Error
                │
                ▼
        ¿Actividad iniciada?
                │
                ├── NO → Error
                │
                ▼
        ¿Transición válida?
                │
                ├── NO → Conflict
                │
                ▼
        Actualizar Attendance
                │
                ▼
             200 OK
```

---

# Escenarios de error críticos

## Reserva duplicada

```text
Given:
usuario 1 ya posee una reserva para actividad 10

When:
POST /api/reservations
{
  "userId": 1,
  "activityId": 10
}

Then:
409 Conflict
```

## Actividad llena

```text
Given:
actividad.capacity = 20
20 reservas activas

When:
se crea una nueva reserva

Then:
409 Conflict
```

## Condición de carrera

```text
Given:
queda exactamente 1 plaza

When:
dos requests concurrentes intentan reservar

Then:
una operación debe tener éxito
y la otra debe ser rechazada
```

Este escenario es especialmente importante porque demuestra conocimiento de consistencia y concurrencia, no solamente de CRUD.

---

# Organización del repositorio

El repositorio deberá contener como mínimo:

```text
README.md
LICENSE
.gitignore
pom.xml
docker-compose.yml

src/

.github/
└── workflows/
    └── ci.yml

docs/
├── srs.md
├── api/
└── adr/
```

Para el flujo AI-DD se recomienda además:

```text
AGENTS.md
.ai/
├── context/
├── rules/
├── prompts/
└── decisions/
```

---

# AI-DD — Fuente de contexto

## `AGENTS.md`

El agente de IA deberá recibir instrucciones permanentes sobre:

```text
objetivo del proyecto
stack tecnológico
arquitectura
convenciones
reglas de negocio
estrategia de testing
restricciones
comandos disponibles
```

La IA no deberá asumir que puede modificar libremente el alcance.

## Jerarquía de autoridad

En caso de conflicto:

```text
SRS
 ↓
ADR
 ↓
Tests / contratos verificables
 ↓
Arquitectura
 ↓
Implementación actual
 ↓
Sugerencia de la IA
```

Una sugerencia del agente nunca deberá modificar silenciosamente un requisito del SRS.

---

# Flujo AI-DD

Cada funcionalidad deberá desarrollarse aproximadamente según este ciclo:

```text
Requisito
   ↓
Contexto
   ↓
Diseño
   ↓
Plan
   ↓
Implementación
   ↓
Tests
   ↓
Revisión
   ↓
Refactor
   ↓
Commit
```

La IA no deberá recibir una orden masiva del tipo:

```text
"Construime toda la API."
```

En cambio, deberá recibir tareas acotadas.

Ejemplo:

```text
Implementá RF-USR-001.

Antes de modificar código:
1. inspeccioná la estructura actual;
2. indicá qué archivos deberán modificarse;
3. proponé el diseño;
4. verificá que no contradiga el SRS.

Luego implementá únicamente esa funcionalidad.

Finalmente:
1. ejecutá los tests;
2. agregá los tests correspondientes;
3. explicá cualquier decisión relevante;
4. indicá qué requisitos quedaron cubiertos.
```

---

# Política de uso de IA

La IA podrá:

* generar código;
* proponer arquitectura;
* generar tests;
* detectar errores;
* refactorizar;
* generar documentación;
* analizar logs;
* proponer consultas JPA;
* explicar errores del compilador.

La IA no deberá:

* ampliar el alcance sin autorización;
* introducir dependencias innecesarias;
* eliminar tests para hacer pasar la build;
* cambiar reglas de negocio para adaptarlas al código;
* reemplazar errores reales por manejo genérico;
* eliminar restricciones de base de datos;
* copiar código sin comprender su propósito;
* modificar varios módulos no relacionados sin justificarlo.

Toda modificación generada por IA deberá pasar por:

```text
compilación
+
tests
+
revisión humana
```

---

# Definition of Done

Una funcionalidad estará terminada únicamente cuando:

```text
[ ] requisito identificado por ID
[ ] implementación terminada
[ ] validaciones implementadas
[ ] reglas de negocio cubiertas
[ ] tests agregados
[ ] tests existentes siguen pasando
[ ] documentación actualizada si corresponde
[ ] OpenAPI actualizada si corresponde
[ ] código revisado
[ ] no existen credenciales hardcodeadas
[ ] build correcta
```

---

# Plan de implementación

## Fase de bootstrap

Objetivo:

```text
proyecto ejecutable
```

Tareas:

* crear proyecto Maven;
* configurar Java;
* agregar Spring Boot;
* configurar estructura de paquetes;
* configurar application profiles;
* configurar Git;
* crear README inicial;
* crear CI mínimo.

Resultado:

```text
./mvnw test
```

deberá ejecutarse correctamente.

---

# Fase de dominio

Objetivo:

```text
modelo persistente completo
```

Implementar:

```text
User
Activity
Reservation
Attendance
```

Definir:

* enums;
* relaciones;
* restricciones;
* índices;
* columnas;
* timestamps.

No implementar todavía todos los endpoints.

---

# Fase de usuarios

Implementar:

```text
RF-USR-001
RF-USR-002
RF-USR-003
RF-USR-004
RF-USR-005
```

Completar:

* DTOs;
* controller;
* service;
* repository;
* validación;
* errores;
* tests.

---

# Fase de actividades

Implementar:

```text
RF-ACT-001
RF-ACT-002
RF-ACT-003
RF-ACT-004
RF-ACT-005
```

Especial atención a:

* fechas;
* estados;
* capacidad;
* filtros;
* cancelación lógica.

---

# Fase de reservas

Es la fase técnicamente más importante.

Implementar:

```text
RF-RES-001
RF-RES-002
RF-RES-003
RF-RES-004
RF-RES-005
```

Después implementar:

```text
reserva duplicada
cupo
transacciones
concurrencia
restricción UNIQUE
```

No considerar terminada la funcionalidad hasta disponer de tests que demuestren que una condición de carrera no permite superar el cupo.

---

# Fase de asistencia

Implementar:

```text
RF-ATT-001
RF-ATT-002
RF-ATT-003
```

Y verificar la máquina de estados:

```text
PENDIENTE → PRESENTE
PENDIENTE → AUSENTE
```

---

# Fase de calidad

Implementar:

* `@RestControllerAdvice`;
* DTOs de error;
* OpenAPI;
* logging;
* Actuator;
* revisión de transacciones;
* índices;
* manejo de excepciones de persistencia.

---

# Fase de integración

Implementar:

```text
Docker Compose
MySQL
application-docker.yml
```

Probar el sistema desde un entorno limpio.

---

# Fase de CI/CD

Pipeline:

```text
checkout
    ↓
Java
    ↓
Maven
    ↓
Tests
    ↓
Build
```

Opcionalmente:

```text
coverage
    ↓
static analysis
    ↓
Docker build
```

---

# Fase de documentación

El README final deberá responder:

```text
¿Qué problema resuelve?
¿Cómo está diseñado?
¿Qué tecnologías utiliza?
¿Cómo se ejecuta?
¿Cómo se prueba?
¿Cómo está documentada la API?
¿Qué decisiones arquitectónicas importantes existen?
¿Qué funcionalidades quedan fuera del alcance?
```

Deberá incluir además:

```text
arquitectura
modelo de datos
ejemplos de requests
ejemplos de errores
comandos de ejecución
```

---

# ADRs recomendadas

Las decisiones arquitectónicas relevantes deberán registrarse.

Primeros ADR posibles:

```text
ADR-001 — Arquitectura por capas
ADR-002 — Uso de DTOs
ADR-003 — Restricción UNIQUE para reservas
ADR-004 — Estrategia de concurrencia para cupos
ADR-005 — Cancelación lógica de actividades
ADR-006 — Política de eliminación de usuarios
ADR-007 — H2 vs MySQL/Testcontainers para integración
```

No es obligatorio crear un ADR para decisiones triviales.

---

# Métricas finales del proyecto

Al finalizar se deberá medir, no inventar:

```text
cantidad de endpoints
cantidad de tests
cobertura
cantidad de entidades
cantidad de reglas de negocio
cantidad de ADRs
cantidad de workflows CI
```

El CV deberá utilizar estos números reales.

El objetivo inicial es:

```text
18 endpoints
80%+ cobertura
tests unitarios + integración
CI automática
Docker Compose
Swagger/OpenAPI
MySQL
manejo centralizado de errores
```

Los números de tests y cobertura exactos dependerán del resultado real de la implementación.

---

# Criterio de calidad para portfolio

El proyecto no se considerará exitoso únicamente porque:

```text
compila
+
tiene muchos endpoints
```

Deberá demostrar que el desarrollador comprende:

```text
HTTP
REST
Spring
JPA
SQL
transacciones
consistencia
testing
validación
arquitectura
Docker
CI/CD
```

La parte más valiosa del proyecto será la capacidad de explicar **por qué cada decisión existe**.

Por ejemplo:

```text
¿Por qué DTOs?
¿Por qué Service?
¿Por qué @Transactional?
¿Por qué UNIQUE(user_id, activity_id)?
¿Por qué no eliminar físicamente una reserva?
¿Por qué bloquear la actividad?
¿Por qué H2 no es suficiente para determinados tests?
¿Por qué la regla vive en Service y no Controller?
```

Una respuesta técnicamente razonada a estas preguntas tiene más valor para una entrevista que aumentar artificialmente el número de clases o dependencias.

---

# Resultado esperado

Al finalizar, el repositorio deberá representar una API backend completa y reproducible que permita:

```text
gestionar usuarios
       ↓
gestionar actividades
       ↓
consultar disponibilidad
       ↓
crear reservas
       ↓
controlar cupos
       ↓
cancelar reservas
       ↓
registrar asistencia
       ↓
consultar historial
```

Todo ello mediante una API REST documentada, testeada y ejecutable mediante Docker.

El proyecto deberá ser suficientemente pequeño para comprenderlo completamente, pero suficientemente realista para demostrar conocimientos de backend más allá de un CRUD básico.
