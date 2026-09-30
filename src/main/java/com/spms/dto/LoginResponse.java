package com.spms.dto;

public record LoginResponse(
        String token,
        UserDto user
) {
}
