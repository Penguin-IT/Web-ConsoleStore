package com.example.consolestore.Dto;

import java.util.ArrayList;
import java.util.List;

public class PosRequest {

    private Long warehouseId;
    private String phone = "";
    private String fullName = "";
    private String address = "";
    private String note = "";
    private boolean newCustomer;
    private Long tradeInId;

    private List<Item> lines =
            new ArrayList<>(List.of(new Item()));

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long value) { warehouseId = value; }

    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value; }

    public String getFullName() { return fullName; }
    public void setFullName(String value) { fullName = value; }

    public String getAddress() { return address; }
    public void setAddress(String value) { address = value; }

    public String getNote() { return note; }
    public void setNote(String value) { note = value; }

    public boolean isNewCustomer() { return newCustomer; }
    public void setNewCustomer(boolean value) { newCustomer = value; }

    public Long getTradeInId() { return tradeInId; }
    public void setTradeInId(Long value) { tradeInId = value; }

    public List<Item> getLines() { return lines; }
    public void setLines(List<Item> value) { lines = value; }

    public static PosRequest from(PosData.Quote quote) {
        PosRequest form = new PosRequest();

        form.warehouseId = quote.warehouseId();
        form.phone = quote.customer().phone();
        form.fullName = quote.customer().name();
        form.address = quote.customer().address();
        form.newCustomer = quote.customer().newCustomer();
        form.tradeInId = quote.tradeInId();
        form.note = quote.note();
        form.lines = new ArrayList<>();

        for (var line : quote.lines()) {
            Item item = new Item();
            item.productId = line.productId();
            item.serialId = line.serialId();
            item.quantity = line.quantity();
            form.lines.add(item);
        }

        return form;
    }

    public static class Item {

        private Long productId;
        private Long serialId;
        private int quantity = 1;

        public Long getProductId() { return productId; }
        public void setProductId(Long value) { productId = value; }

        public Long getSerialId() { return serialId; }
        public void setSerialId(Long value) { serialId = value; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int value) { quantity = value; }
    }
}