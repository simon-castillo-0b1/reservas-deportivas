package com.simoncastillo.reservas.controller;

import com.simoncastillo.reservas.dto.attendance.AttendanceResponse;
import com.simoncastillo.reservas.dto.attendance.UpdateAttendanceRequest;
import com.simoncastillo.reservas.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Asistencia", description = "Control del registro de asistencia")
@RestController
@RequestMapping("/api")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @Operation(summary = "Registrar asistencia", description = "Registra el estado de asistencia de una reserva confirmada")
    @PutMapping("/reservations/{reservationId}/attendance")
    public ResponseEntity<AttendanceResponse> registerAttendance(
            @PathVariable Long reservationId,
            @Valid @RequestBody UpdateAttendanceRequest request
    ) {
        AttendanceResponse response = attendanceService.registerAttendance(reservationId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consultar asistencia por actividad", description = "Obtiene el registro de asistencia de todos los participantes de una actividad")
    @GetMapping("/activities/{activityId}/attendance")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByActivityId(@PathVariable Long activityId) {
        return ResponseEntity.ok(attendanceService.getAttendanceByActivityId(activityId));
    }

    @Operation(summary = "Consultar asistencia por usuario", description = "Obtiene el historial de asistencia de un usuario")
    @GetMapping("/users/{userId}/attendance")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(attendanceService.getAttendanceByUserId(userId));
    }
}
