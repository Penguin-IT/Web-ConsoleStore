package com.example.consolestore.DTO;

public record StaffAccountForm(
        Long employeeId,
        String username,
        String password,
        String confirmPassword,
        String role,
        Boolean locked,
        String version
) {

    public static StaffAccountForm empty(Long employeeId) {
        return new StaffAccountForm(
                employeeId,
                "",
                "",
                "",
                "NHANVIEN",
                false,
                ""
        );
    }

    @Override
    public String toString() {
        return "StaffAccountForm";
    }
}