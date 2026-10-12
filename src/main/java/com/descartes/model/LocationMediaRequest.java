package com.descartes.model;


import jakarta.validation.constraints.NotBlank;

public record LocationMediaRequest(
        @NotBlank String id,
        @NotBlank String url,
        @NotBlank String category,
        @NotBlank String attributes) {
}