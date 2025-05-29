package br.com.alura.AluraFake.exceptionhandler;

public class InvalidCourseTaskOperationException extends RuntimeException {
    public InvalidCourseTaskOperationException(String message) {
        super(message);
    }
}
