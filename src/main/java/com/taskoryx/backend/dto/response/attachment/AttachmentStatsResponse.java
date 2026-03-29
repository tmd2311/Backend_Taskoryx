package com.taskoryx.backend.dto.response.attachment;

import com.taskoryx.backend.entity.FileCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentStatsResponse {

    /** Tổng số file đính kèm */
    private long totalCount;

    /** Số lượng file theo từng danh mục (luôn có đủ 9 giá trị, 0 nếu không có) */
    private Map<FileCategory, Long> byCategory;
}
