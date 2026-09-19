package com.simoncastillo.reservas.controller;

import com.simoncastillo.reservas.dto.reservation.CreateReservationRequest;
import com.simoncastillo.reservas.dto.reservation.ReservationResponse;
import com.simoncastillo.reservas.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Reservas", description = "Gestión de reservas deportivas usando Lock Pesimista para concurrencia")
@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(summary = "Crear reserva", description = "Crea una reserva para un usuario")
    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Obtener reserva por ID", description = "Retorna el detalle de una reserva específica")
    @GetMapping("/reservations/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    @Operation(summary = "Cancelar reserva", description = "Cancela una reserva confirmada si la actividad aún no ha iniciado")
    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar reservas por usuario", description = "Consulta todas las reservas efectuadas por un usuario determinado")
    @GetMapping("/users/{userId}/reservations")
    public ResponseEntity<List<ReservationResponse>> getReservationsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(reservationService.getReservationsByUserId(userId));
    }

    @Operation(summary = "Listar reservas por actividad", description = "Consulta todas las reservas registradas en una actividad deportiva")
    @GetMapping("/activities/{activityId}/reservations")
    public ResponseEntity<List<ReservationResponse>> getReservationsByActivityId(@PathVariable Long activityId) {
        return ResponseEntity.ok(reservationService.getReservationsByActivityId(activityId));
    }
}
