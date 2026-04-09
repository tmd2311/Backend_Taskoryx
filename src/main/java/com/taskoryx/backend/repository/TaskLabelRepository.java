package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.TaskLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TaskLabelRepository extends JpaRepository<TaskLabel, UUID> {
}
