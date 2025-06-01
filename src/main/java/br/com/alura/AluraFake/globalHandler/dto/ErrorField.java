package br.com.alura.AluraFake.globalHandler.dto;

public class ErrorField {
    private String name;
    private String userMessage;

    public ErrorField(String name, String userMessage) {
        this.name = name;
        this.userMessage = userMessage;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
