package com.descartes.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LocationRequest(
        UUID id,
        @NotBlank String name,
        @NotBlank String country,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double lon,
        @NotBlank String mapsLink,
        @NotBlank String attributes
) {
}
