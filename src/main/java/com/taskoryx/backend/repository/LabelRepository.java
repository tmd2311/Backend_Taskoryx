package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LabelRepository extends JpaRepository<Label, UUID> {

    List<Label> findByProjectIdOrderByNameAsc(UUID projectId);

    boolean existsByProjectIdAndName(UUID projectId, String name);
}
