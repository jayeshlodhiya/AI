package com.retailai.api.dto;



import jakarta.validation.constraints.NotBlank;

public record UpdateQcallKeyRequest(@NotBlank String qcallApiKey) {}
