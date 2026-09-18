package com.simoncastillo.reservas.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_reservation_user_activity",
                columnNames = {"user_id", "activity_id"}
        )
})
@Getter
@Setter
@NoArgsConstructor
public class Reservation {

    public Reservation(User user, Activity activity) {
        this.user = user;
        this.activity = activity;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private  User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status = ReservationStatus.CONFIRMADA;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELADA;
        this.cancelledAt = LocalDateTime.now();
    }
}
