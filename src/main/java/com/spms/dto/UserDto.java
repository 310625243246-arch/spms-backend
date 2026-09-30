package com.spms.dto;

public record UserDto(
        Long id,
        String name,
        String email,
        String role
) {
}
