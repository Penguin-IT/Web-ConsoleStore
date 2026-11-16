package com.example.consolestore.DTO;

public record AccountProfile(
        String username,
        String email,
        String fullName,
        String phone,
        String address
) {
}