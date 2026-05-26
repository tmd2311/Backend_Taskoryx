package com.taskoryx.backend.ai.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AiExecuteResult {

    private UUID projectId;
    private String projectKey;
    private String projectName;

    private int sprintsCreated;
    private int tasksCreated;
    private int subTasksCreated;
}
