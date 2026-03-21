package com.taskoryx.backend.dto.request.timetracking;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTimeEntryRequest {

    @DecimalMin(value = "0.1", message = "Số giờ phải lớn hơn hoặc bằng 0.1")
    @DecimalMax(value = "24.0", message = "Số giờ không được vượt quá 24")
    private BigDecimal hours;

    private String description;

    private LocalDate workDate;
}
