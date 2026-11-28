package com.example.consolestore.Service;

import java.util.Map;

public class AddressRejectedException extends RuntimeException {
    private final Map<String, String> errors;

    public AddressRejectedException(Map<String, String> errors) {
        super(errors.values().stream().findFirst().orElse("Địa chỉ không hợp lệ."));
        this.errors = errors;
    }

    public Map<String, String> errors() { return errors; }
}