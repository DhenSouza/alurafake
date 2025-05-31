package br.com.alura.AluraFake.exceptionhandler;

public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
