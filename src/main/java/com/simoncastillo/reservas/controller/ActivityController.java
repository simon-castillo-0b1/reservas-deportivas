package com.simoncastillo.reservas.controller;

import com.simoncastillo.reservas.dto.activity.ActivityResponse;
import com.simoncastillo.reservas.dto.activity.CreateActivityRequest;
import com.simoncastillo.reservas.dto.activity.UpdateActivityRequest;
import com.simoncastillo.reservas.entity.ActivityStatus;
import com.simoncastillo.reservas.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Actividades", description = "Gestión de actividades")
@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @Operation(summary = "Crear una nueva actividad", description = "Programa una actividad deportiva validando fecha futura")
    @PostMapping
    public ResponseEntity<ActivityResponse> createActivity(@Valid @RequestBody CreateActivityRequest request) {
        ActivityResponse response = activityService.createActivity(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Obtener actividad por id", description = "Retorna el detalle de una actividad con cupos calculados")
    @GetMapping("/{id}")
    public ResponseEntity<ActivityResponse> getActivityById(@PathVariable Long id) {
        return ResponseEntity.ok(activityService.getActivityById(id));
    }

    @Operation(summary = "Listar actividades con filtros", description = "Consulta actividades filtrando dinámicamente por deporte, estado y rango de fechas")
    @GetMapping
    public ResponseEntity<List<ActivityResponse>> getActivities(
            @RequestParam(required = false) String sport,
            @RequestParam(required = false) ActivityStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ResponseEntity.ok(activityService.getActivities(sport, status, from, to));
    }

    @Operation(summary = "Actualizar actividad", description = "Modifica los datos editables de una actividad futura no iniciada")
    @PutMapping("/{id}")
    public ResponseEntity<ActivityResponse> updateActivity(
            @PathVariable Long id,
            @Valid @RequestBody UpdateActivityRequest request
    ) {
        return ResponseEntity.ok(activityService.updateActivity(id, request));
    }

    @Operation(summary = "Cancelar actividad", description = "Cancela lógicamente una actividad preservando la trazabilidad de reservas")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelActivity(@PathVariable Long id) {
        activityService.cancelActivity(id);
        return ResponseEntity.noContent().build();
    }
}
