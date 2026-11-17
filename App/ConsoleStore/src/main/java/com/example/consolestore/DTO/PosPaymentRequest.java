package com.example.consolestore.Dto;

public class PosPaymentRequest {

    private String method = "TIEN_MAT";
    private String cashReceived = "";
    private boolean confirmed;

    public String getMethod() { return method; }
    public void setMethod(String value) { method = value; }

    public String getCashReceived() { return cashReceived; }
    public void setCashReceived(String value) { cashReceived = value; }

    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean value) { confirmed = value; }
}