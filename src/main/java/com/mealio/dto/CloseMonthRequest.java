package com.mealio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CloseMonthRequest(

        @NotNull(message = "messId is required") UUID messId,

        @NotNull(message = "adminId is required") UUID adminId,

        @NotBlank(message = "yearMonth is required") @Pattern(regexp = "\\d{4}-\\d{2}", message = "yearMonth must be in format YYYY-MM") String yearMonth) {
}
