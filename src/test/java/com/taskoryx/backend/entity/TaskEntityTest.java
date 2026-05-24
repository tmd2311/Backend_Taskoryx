package com.taskoryx.backend.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Task Entity - Business Logic Tests")
class TaskEntityTest {

    // ─── getDepth() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Task không có cha → depth = 1")
    void getDepth_noParent_returnsOne() {
        Task task = Task.builder().title("Epic").build();
        assertThat(task.getDepth()).isEqualTo(1);
    }

    @Test
    @DisplayName("Task có cha cấp 1 → depth = 2")
    void getDepth_oneParent_returnsTwo() {
        Task parent = Task.builder().title("Epic").build();
        Task child = Task.builder().title("Story").parentTask(parent).build();
        assertThat(child.getDepth()).isEqualTo(2);
    }

    @Test
    @DisplayName("Task có cha cấp 2 → depth = 3")
    void getDepth_twoParents_returnsThree() {
        Task grandParent = Task.builder().title("Epic").build();
        Task parent = Task.builder().title("Story").parentTask(grandParent).build();
        Task child = Task.builder().title("Subtask").parentTask(parent).build();
        assertThat(child.getDepth()).isEqualTo(3);
    }

    // ─── canHaveChildren() ────────────────────────────────────────────────────

    @Test
    @DisplayName("Task cấp 1 có thể có con")
    void canHaveChildren_level1_returnsTrue() {
        Task task = Task.builder().title("Epic").build();
        assertThat(task.canHaveChildren()).isTrue();
    }

    @Test
    @DisplayName("Task cấp 2 có thể có con")
    void canHaveChildren_level2_returnsTrue() {
        Task parent = Task.builder().title("Epic").build();
        Task child = Task.builder().title("Story").parentTask(parent).build();
        assertThat(child.canHaveChildren()).isTrue();
    }

    @Test
    @DisplayName("Task cấp 3 KHÔNG thể có con")
    void canHaveChildren_level3_returnsFalse() {
        Task grandParent = Task.builder().title("Epic").build();
        Task parent = Task.builder().title("Story").parentTask(grandParent).build();
        Task child = Task.builder().title("Subtask").parentTask(parent).build();
        assertThat(child.canHaveChildren()).isFalse();
    }

    // ─── getTaskKey() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTaskKey trả về projectKey-taskNumber")
    void getTaskKey_withProjectAndNumber_returnsCorrectKey() {
        Project project = Project.builder().key("TX").build();
        Task task = Task.builder().project(project).taskNumber(42).title("Test").build();
        assertThat(task.getTaskKey()).isEqualTo("TX-42");
    }

    @Test
    @DisplayName("getTaskKey khi project null → trả về null")
    void getTaskKey_noProject_returnsNull() {
        Task task = Task.builder().taskNumber(1).title("Test").build();
        assertThat(task.getTaskKey()).isNull();
    }

    // ─── isOverdue() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Task quá hạn và chưa hoàn thành → isOverdue = true")
    void isOverdue_pastDueAndNotCompleted_returnsTrue() {
        Task task = Task.builder()
                .title("Overdue task")
                .dueDate(LocalDate.now().minusDays(1))
                .build();
        assertThat(task.isOverdue()).isTrue();
    }

    @Test
    @DisplayName("Task quá hạn nhưng đã hoàn thành → isOverdue = false")
    void isOverdue_pastDueButCompleted_returnsFalse() {
        Task task = Task.builder()
                .title("Completed task")
                .dueDate(LocalDate.now().minusDays(1))
                .completedAt(java.time.LocalDateTime.now())
                .build();
        assertThat(task.isOverdue()).isFalse();
    }

    @Test
    @DisplayName("Task chưa đến hạn → isOverdue = false")
    void isOverdue_futureDue_returnsFalse() {
        Task task = Task.builder()
                .title("Future task")
                .dueDate(LocalDate.now().plusDays(5))
                .build();
        assertThat(task.isOverdue()).isFalse();
    }

    @Test
    @DisplayName("Task không có dueDate → isOverdue = false")
    void isOverdue_noDueDate_returnsFalse() {
        Task task = Task.builder().title("No deadline").build();
        assertThat(task.isOverdue()).isFalse();
    }

    // ─── isCompleted() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Task có completedAt → isCompleted = true")
    void isCompleted_withCompletedAt_returnsTrue() {
        Task task = Task.builder()
                .title("Done")
                .completedAt(java.time.LocalDateTime.now())
                .build();
        assertThat(task.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("Task không có completedAt → isCompleted = false")
    void isCompleted_withoutCompletedAt_returnsFalse() {
        Task task = Task.builder().title("In progress").build();
        assertThat(task.isCompleted()).isFalse();
    }

    // ─── Default values ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Task mặc định có priority = MEDIUM và status = TODO")
    void defaults_priorityMedium_statusTodo() {
        Task task = Task.builder().title("Default task").build();
        assertThat(task.getPriority()).isEqualTo(Task.TaskPriority.MEDIUM);
        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.TODO);
    }

    // ─── equals / hashCode ────────────────────────────────────────────────────

    @Test
    @DisplayName("Hai Task cùng id → equals = true")
    void equals_sameId_returnsTrue() {
        java.util.UUID id = java.util.UUID.randomUUID();
        Task t1 = new Task();
        t1.setId(id);
        Task t2 = new Task();
        t2.setId(id);
        assertThat(t1).isEqualTo(t2);
    }

    @Test
    @DisplayName("Task id null → không equals Task khác")
    void equals_nullId_returnsFalse() {
        Task t1 = new Task();
        Task t2 = new Task();
        assertThat(t1).isNotEqualTo(t2);
    }
}
