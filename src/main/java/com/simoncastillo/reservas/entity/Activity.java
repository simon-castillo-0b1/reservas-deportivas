package com.simoncastillo.reservas.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
@Getter
@Setter
@NoArgsConstructor
public class Activity {

    public Activity(String name, String sport, LocalDateTime startAt, Integer durationMinutes, Integer capacity) {
        this.name = name;
        this.sport = sport;
        this.startAt = startAt;
        this.durationMinutes = durationMinutes;
        this.capacity = capacity;
        this.status = ActivityStatus.PROGRAMADA;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "sport", nullable = false)
    private String sport;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Positive
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;


    @Positive
    @Column(name = "capacity", nullable = false)
    private Integer capacity;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActivityStatus status;


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


}
