package com.taskoryx.backend.dto.response.checklist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistSummaryResponse {

    private long totalItems;
    private long checkedItems;
    private int completionPercentage;
    private List<ChecklistItemResponse> items;
}
