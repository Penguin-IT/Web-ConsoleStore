package com.example.consolestore.DTO;

public class PasswordChangeRequest {

    private String currentPassword = "";
    private String newPassword = "";
    private String confirmPassword = "";

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String value) {
        currentPassword = value;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String value) {
        newPassword = value;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String value) {
        confirmPassword = value;
    }

    public void clear() {
        currentPassword = "";
        newPassword = "";
        confirmPassword = "";
    }
}