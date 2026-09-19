# ADR-0002: Máquina de Estados Finita Unidireccional para Control de Asistencia

* **Estado:** Aceptado
* **Fecha:** 2026-09-19
* **Autores:** Simón Castillo & AI Pair Programmer

## Contexto
El registro de asistencia a las actividades deportivas representa la validación en el mundo real de un servicio prestado. En auditorías de gestión de complejos deportivos o gimnasios, permitir modificaciones arbitrarias de asistencia (por ejemplo, cambiar de `PRESENTE` a `AUSENTE` días después) abre la puerta a fraudes, manipulación de métricas de presentismo y reclamos indebidos.

## Decisión
Se implementó una **Máquina de Estados Finita (FSM)** con flujo unidireccional estricto:

```text
       ┌───────────► PRESENTE (Estado terminal)
PENDIENTE 
       └───────────► AUSENTE  (Estado terminal)
```

1. **Precondición Temporal:** Solo se puede alterar el estado de asistencia una vez que la actividad deportiva haya comenzado (`activity.getStartAt().isBefore(now())`). Intentar registrar presentismo antes del inicio arroja `BusinessRuleViolationException` (HTTP 409).
2. **Precondición de Reserva:** La reserva asociada debe encontrarse en estado `CONFIRMADA`. Si fue cancelada, se rechaza la operación.
3. **Irreversibilidad:** Los estados `PRESENTE` y `AUSENTE` son terminales. Una vez que una asistencia abandona el estado `PENDIENTE`, cualquier petición posterior es rechazada con HTTP 409.
4. **Imposibilidad de Retorno:** No se permite restablecer el estado a `PENDIENTE`.

## Consecuencias
* **Positivas:** Trazabilidad inmutable de la asistencia física de los socios. Código de dominio desacoplado y blindado contra inconsistencias.
* **Negativas / Mitigaciones:** Si un instructor comete un error manual de registro, el sistema no permite sobrescribirlo vía API estándar; se requeriría un procedimiento administrativo auditado específico en futuras versiones.
