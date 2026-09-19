# ADR-0003: Cancelación Lógica (Soft Delete) para Actividades y Reservas

* **Estado:** Aceptado
* **Fecha:** 2026-09-19
* **Autores:** Simón Castillo & AI Pair Programmer

## Contexto
En un sistema transaccional de reservas, ejecutar operaciones destructivas físicas (`DELETE FROM activities` o `DELETE FROM reservations`) destruye el historial contable, referencial y analítico del negocio. Además, violaría la integridad referencial (`foreign keys`) de asistencias y reservas previas.

Por otro lado, cuando un usuario cancela su reserva, el cupo ocupado debe liberarse de inmediato para que otros usuarios puedan tomarlo, pero sin que el registro original desaparezca del historial del usuario.

## Decisión
Se implementó **Cancelación Lógica (Soft Delete)** tanto para `Activity` como para `Reservation`:

1. **Entidad `Activity`:**
   - La cancelación mediante `DELETE /api/activities/{id}` transiciona el estado a `ActivityStatus.CANCELADA`.
   - No se borra ninguna reserva asociada; se preserva toda la historia para auditoría.
   - Si la actividad ya comenzó o finalizó, se rechaza la cancelación.
2. **Entidad `Reservation`:**
   - La cancelación mediante `DELETE /api/reservations/{id}` transiciona el estado a `ReservationStatus.CANCELADA` y fija la marca temporal `cancelledAt`.
   - El cálculo de cupos ocupados se basa exclusivamente en reservas activas:
     ```java
     countByActivityIdAndStatus(activityId, ReservationStatus.CONFIRMADA)
     ```
   - Al cancelarse una reserva, el recuento disminuye automáticamente en 1, liberando la plaza para futuros usuarios de forma transparente e instantánea.
3. **Entidad `User`:**
   - Para `User`, se aplica una regla de protección de integridad: un usuario solo puede ser eliminado físicamente si no tiene reservas históricas registradas. Si tiene reservas, se prohíbe el borrado para salvaguardar la trazabilidad de asistencias y cobros.

## Consecuencias
* **Positivas:** Máxima trazabilidad histórica, soporte para reportería y analítica, liberación dinámica y consistente de cupos.
* **Negativas / Mitigaciones:** Requiere que las consultas de cupos filtren explícitamente por `status = CONFIRMADA`, lo cual quedó encapsulado en los métodos de repositorio y probado exhaustivamente.
