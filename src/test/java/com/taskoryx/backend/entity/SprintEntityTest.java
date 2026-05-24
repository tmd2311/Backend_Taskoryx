package com.taskoryx.backend.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Sprint Entity - Business Logic Tests")
class SprintEntityTest {

    @Test
    @DisplayName("Sprint ACTIVE → isActive() = true")
    void isActive_activeStatus_returnsTrue() {
        Sprint sprint = Sprint.builder()
                .name("Sprint 1")
                .status(Sprint.SprintStatus.ACTIVE)
                .build();
        assertThat(sprint.isActive()).isTrue();
    }

    @Test
    @DisplayName("Sprint PLANNED → isActive() = false")
    void isActive_plannedStatus_returnsFalse() {
        Sprint sprint = Sprint.builder()
                .name("Sprint 1")
                .status(Sprint.SprintStatus.PLANNED)
                .build();
        assertThat(sprint.isActive()).isFalse();
    }

    @Test
    @DisplayName("Sprint COMPLETED → isCompleted() = true")
    void isCompleted_completedStatus_returnsTrue() {
        Sprint sprint = Sprint.builder()
                .name("Sprint 1")
                .status(Sprint.SprintStatus.COMPLETED)
                .build();
        assertThat(sprint.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("Sprint ACTIVE → isCompleted() = false")
    void isCompleted_activeStatus_returnsFalse() {
        Sprint sprint = Sprint.builder()
                .name("Sprint 1")
                .status(Sprint.SprintStatus.ACTIVE)
                .build();
        assertThat(sprint.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("getDuration trả về số ngày giữa startDate và endDate")
    void getDuration_withBothDates_returnsCorrectDays() {
        Sprint sprint = Sprint.builder()
                .name("Sprint 1")
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 15))
                .build();
        assertThat(sprint.getDuration()).isEqualTo(14);
    }

    @Test
    @DisplayName("getDuration khi thiếu startDate → trả về -1")
    void getDuration_noStartDate_returnsMinusOne() {
        Sprint sprint = Sprint.builder()
                .name("Sprint 1")
                .endDate(LocalDate.of(2026, 5, 15))
                .build();
        assertThat(sprint.getDuration()).isEqualTo(-1);
    }

    @Test
    @DisplayName("getDuration khi thiếu endDate → trả về -1")
    void getDuration_noEndDate_returnsMinusOne() {
        Sprint sprint = Sprint.builder()
                .name("Sprint 1")
                .startDate(LocalDate.of(2026, 5, 1))
                .build();
        assertThat(sprint.getDuration()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Sprint mặc định có status = PLANNED")
    void defaults_statusIsPlanned() {
        Sprint sprint = Sprint.builder().name("Sprint 1").build();
        assertThat(sprint.getStatus()).isEqualTo(Sprint.SprintStatus.PLANNED);
    }

    @Test
    @DisplayName("Hai Sprint cùng id → equals = true")
    void equals_sameId_returnsTrue() {
        java.util.UUID id = java.util.UUID.randomUUID();
        Sprint s1 = new Sprint();
        s1.setId(id);
        Sprint s2 = new Sprint();
        s2.setId(id);
        assertThat(s1).isEqualTo(s2);
    }
}
