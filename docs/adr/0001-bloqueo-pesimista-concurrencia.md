# ADR-0001: Bloqueo Pesimista (SELECT FOR UPDATE) para Control de Cupos y Prevención de Overbooking

* **Estado:** Aceptado
* **Fecha:** 2026-09-19
* **Autores:** Simón Castillo & AI Pair Programmer

## Contexto
En un sistema de reservas deportivas donde las actividades tienen una capacidad finita (`capacity`), surge el riesgo crítico de **condición de carrera (race condition)** cuando dos o más usuarios intentan reservar el último cupo al mismo milisegundo. 

Si ambas transacciones leen la cantidad de reservas confirmadas concurrentemente antes de que alguna de ellas realice el `INSERT`, ambas leerán que queda 1 cupo disponible y procederán a insertar. El resultado sería una sobreventa de cupos (**overbooking**), violando una de las reglas de negocio primarias del sistema (RF-RES-001).

## Alternativas Consideradas
1. **Bloqueo Optimista (`@Version`):**
   - *Ventajas:* No bloquea filas en la base de datos, ideal para escenarios de baja contención.
   - *Desventajas:* Al detectarse colisión, arroja `OptimisticLockingFailureException`. En un checkout de alta demanda (como el último cupo de una clase de spinning o tenis), el usuario experimentaría fallos inesperados y requeriría mecanismos de reintento complejos.
2. **Semáforos / Locks a nivel de JVM (`ReentrantLock`, `synchronized`):**
   - *Ventajas:* Fácil de implementar en memoria.
   - *Desventajas:* No escala horizontalmente. Al desplegar múltiples réplicas del backend en un clúster o contenedor (Kubernetes/Docker), las instancias no comparten la memoria de la JVM.
3. **Bloqueo Pesimista de Escritura (`SELECT ... FOR UPDATE`):**
   - *Ventajas:* Delegado al motor de la base de datos relacional (ACID). Escala a múltiples réplicas del servicio. El hilo adquirente bloquea la fila de la entidad `Activity` hasta el `commit` o `rollback` de la transacción.

## Decisión
Se implementó **Bloqueo Pesimista de Escritura** en `ActivityRepository`:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM Activity a WHERE a.id = :id")
Optional<Activity> findByIdWithLock(@Param("id") Long id);
```

En `ReservationService.createReservation`:
1. Se abre una transacción `@Transactional`.
2. Se adquiere el bloqueo de la actividad con `findByIdWithLock(activityId)`.
3. Se verifica el recuento de reservas confirmadas activas (`countByActivityIdAndStatus`).
4. Si hay cupo, se guarda la reserva y se confirma la transacción, liberando el lock de inmediato.

## Consecuencias
* **Positivas:** Consistencia de datos absoluta sin riesgo de sobreventa. Probado empíricamente mediante un test de integración concurrente multihilo (`ReservationConcurrencyIntegrationTest`).
* **Negativas / Mitigaciones:** Puede generar latencia mínima durante picos de contención sobre una misma actividad específica, pero el ámbito de la transacción se mantuvo ultra-reducido (solo validación y persistencia de 2 entidades) para minimizar el tiempo de retención del lock.
