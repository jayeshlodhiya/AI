package com.retailai.api.dto;



public record UserProfileResponse(
        Long id,
        String username,
        String email,         // derived as username if it looks like an email
        String fullName,
        boolean enabled,
        String maskedQcallKey // last 4 chars only, or null
) {}
