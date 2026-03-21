package com.taskoryx.backend.dto.request.timetracking;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTimeEntryRequest {

    @NotNull(message = "taskId không được để trống")
    private UUID taskId;

    @NotNull(message = "Số giờ không được để trống")
    @DecimalMin(value = "0.1", message = "Số giờ phải lớn hơn hoặc bằng 0.1")
    @DecimalMax(value = "24.0", message = "Số giờ không được vượt quá 24")
    private BigDecimal hours;

    private String description;

    // Nếu null thì dùng ngày hôm nay
    private LocalDate workDate;
}
