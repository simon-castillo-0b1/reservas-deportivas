package com.simoncastillo.reservas.controller;

import com.simoncastillo.reservas.dto.activity.ActivityResponse;
import com.simoncastillo.reservas.dto.activity.CreateActivityRequest;
import com.simoncastillo.reservas.dto.activity.UpdateActivityRequest;
import com.simoncastillo.reservas.entity.ActivityStatus;
import com.simoncastillo.reservas.service.ActivityService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    public ResponseEntity<ActivityResponse> createActivity(@Valid @RequestBody CreateActivityRequest request) {
        ActivityResponse response = activityService.createActivity(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityResponse> getActivityById(@PathVariable Long id) {
        return ResponseEntity.ok(activityService.getActivityById(id));
    }

    @GetMapping
    public ResponseEntity<List<ActivityResponse>> getActivities(
            @RequestParam(required = false) String sport,
            @RequestParam(required = false) ActivityStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ResponseEntity.ok(activityService.getActivities(sport, status, from, to));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActivityResponse> updateActivity(
            @PathVariable Long id,
            @Valid @RequestBody UpdateActivityRequest request
    ) {
        return ResponseEntity.ok(activityService.updateActivity(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelActivity(@PathVariable Long id) {
        activityService.cancelActivity(id);
        return ResponseEntity.noContent().build();
    }
}
