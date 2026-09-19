package com.simoncastillo.reservas.controller;

import com.simoncastillo.reservas.dto.attendance.AttendanceResponse;
import com.simoncastillo.reservas.dto.attendance.UpdateAttendanceRequest;
import com.simoncastillo.reservas.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PutMapping("/reservations/{reservationId}/attendance")
    public ResponseEntity<AttendanceResponse> registerAttendance(
            @PathVariable Long reservationId,
            @Valid @RequestBody UpdateAttendanceRequest request
    ) {
        AttendanceResponse response = attendanceService.registerAttendance(reservationId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/activities/{activityId}/attendance")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByActivityId(@PathVariable Long activityId) {
        return ResponseEntity.ok(attendanceService.getAttendanceByActivityId(activityId));
    }

    @GetMapping("/users/{userId}/attendance")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(attendanceService.getAttendanceByUserId(userId));
    }
}
