package com.example.consolestore.Service;

public class PosRejectedException extends RuntimeException {

    public PosRejectedException(String message) {
        super(message);
    }
}