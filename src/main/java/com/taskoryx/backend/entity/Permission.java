package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entity class representing a Permission in the system
 * Maps to 'permissions' table in database
 */
@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permissions_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    /**
     * Nhóm permission để hiển thị UI (USER, PROJECT, TASK, BOARD, REPORT, ADMIN)
     */
    @Column(nullable = false, length = 50)
    private String resource;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission)) return false;
        return id != null && id.equals(((Permission) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Permission{name='" + name + "'}";
    }
}
