package com.crypto.trading.trading.event;

public class ExternalApiDataEvent {

    private final String data;

    public ExternalApiDataEvent(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
}
