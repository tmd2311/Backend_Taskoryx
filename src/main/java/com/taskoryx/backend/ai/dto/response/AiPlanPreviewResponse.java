package com.taskoryx.backend.ai.dto.response;

import lombok.Builder;
import lombok.Data;

/** Trả về client để xem trước kế hoạch trước khi xác nhận tạo. */
@Data
@Builder
public class AiPlanPreviewResponse {

    private AiProjectPlan plan;

    /** Tổng số task sẽ được tạo (bao gồm sub-task). */
    private int totalTaskCount;

    /** Model AI đã dùng để sinh kế hoạch. */
    private String modelUsed;
}
