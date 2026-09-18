package com.simoncastillo.reservas.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendances")
@Getter
@Setter
@NoArgsConstructor
public class Attendance {

    public Attendance(Reservation reservation) {
        this.reservation = reservation;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", unique = true, nullable = false)
    private Reservation reservation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AttendanceStatus status = AttendanceStatus.PENDIENTE;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    public void markPresent() {
        this.status = AttendanceStatus.PRESENTE;
        this.registeredAt = LocalDateTime.now();
    }

    public void markAbsent() {
        this.status = AttendanceStatus.AUSENTE;
        this.registeredAt = LocalDateTime.now();
    }

}
