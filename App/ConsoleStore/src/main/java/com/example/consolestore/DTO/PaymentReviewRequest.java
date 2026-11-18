package com.example.consolestore.DTO;

public class PaymentReviewRequest {

    private String action;
    private String expectedMethod;
    private String expectedAmount;
    private String receivedAmount;
    private boolean received;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getExpectedMethod() {
        return expectedMethod;
    }

    public void setExpectedMethod(String expectedMethod) {
        this.expectedMethod = expectedMethod;
    }

    public String getExpectedAmount() {
        return expectedAmount;
    }

    public void setExpectedAmount(String expectedAmount) {
        this.expectedAmount = expectedAmount;
    }

    public String getReceivedAmount() {
        return receivedAmount;
    }

    public void setReceivedAmount(String receivedAmount) {
        this.receivedAmount = receivedAmount;
    }

    public boolean isReceived() {
        return received;
    }

    public void setReceived(boolean received) {
        this.received = received;
    }
}