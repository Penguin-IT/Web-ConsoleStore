package com.example.consolestore.Service;

public class CheckoutRejectedException
        extends RuntimeException {

    public CheckoutRejectedException(String message) {
        super(message);
    }
}