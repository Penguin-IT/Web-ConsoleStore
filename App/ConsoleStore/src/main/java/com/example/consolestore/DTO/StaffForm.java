package com.example.consolestore.DTO;

public record StaffForm(
        String fullName,
        String phone,
        String email,
        String address,
        String position,
        Boolean retired,
        String version
) {

    public static StaffForm empty() {
        return new StaffForm(
                "",
                "",
                "",
                "",
                "",
                false,
                ""
        );
    }
}