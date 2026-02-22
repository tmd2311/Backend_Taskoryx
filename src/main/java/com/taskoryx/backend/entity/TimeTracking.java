package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity class representing Time Tracking
 * Maps to 'time_tracking' table in database
 */
@Entity
@Table(name = "time_tracking", indexes = {
    @Index(name = "idx_time_tracking_task", columnList = "task_id"),
    @Index(name = "idx_time_tracking_user", columnList = "user_id"),
    @Index(name = "idx_time_tracking_date", columnList = "workDate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, foreignKey = @ForeignKey(name = "fk_time_tracking_task"))
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_time_tracking_user"))
    private User user;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal hours;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Get formatted hours (e.g., "2h 30m")
     */
    @Transient
    public String getFormattedHours() {
        if (hours == null) return "0h";

        double totalHours = hours.doubleValue();
        int wholeHours = (int) totalHours;
        int minutes = (int) ((totalHours - wholeHours) * 60);

        if (minutes == 0) {
            return wholeHours + "h";
        }
        return wholeHours + "h " + minutes + "m";
    }

    /**
     * Check if this is today's work
     */
    @Transient
    public boolean isToday() {
        return workDate != null && workDate.equals(LocalDate.now());
    }

    /**
     * Check if work is in the past
     */
    @Transient
    public boolean isPast() {
        return workDate != null && workDate.isBefore(LocalDate.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeTracking)) return false;
        return id != null && id.equals(((TimeTracking) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "TimeTracking{" +
                "id=" + id +
                ", hours=" + hours +
                ", workDate=" + workDate +
                '}';
    }
}
