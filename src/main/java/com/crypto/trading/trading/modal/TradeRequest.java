package com.crypto.trading.trading.modal;

public class TradeRequest {
    private String userName;
    private String symbol;
    private double quantity;
    private boolean isBuyOrder;

    // Getters and Setters
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public boolean isBuyOrder() {
        return isBuyOrder;
    }

    public void setBuyOrder(boolean isBuyOrder) {
        this.isBuyOrder = isBuyOrder;
    }
}
