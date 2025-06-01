package br.com.alura.AluraFake.globalHandler.dto;

import lombok.Getter;

@Getter
public enum ProblemType {

    DEFAULT_USER_MESSAGE_VALIDATION(
            "/invalid-fields",
            "Invalid fields",
            "One or more fields are invalid. Please fill them out correctly and try again."
    ),

    INVALID_DATA(
            "/invalid-data",
            "Invalid data",
            "The provided data is invalid. Please check and try again."
    ),

    RESOURCE_NOT_FOUND(
            "/resource-not-found",
            "Resource not found",
            "The requested resource was not found."
    ),

    INVALID_OPERATION(
            "/invalid-operation",
            "Invalid operation",
            "The requested operation is not allowed."
    ),

    UNEXPECTED_ERROR(
            "/unexpected-error",
            "Unexpected error",
            "An unexpected error occurred. Please try again later."
    ),

    MESSAGE_NOT_READABLE(
            "/message-not-readable",
            "Unreadable body",
            "The request could not be processed due to a problem with the format or the data sent. Please check and try again."
    ),

    TYPE_INVALID_REQUEST_FORMAT(
            "/invalid-request-format",
            "Invalid request format",
            "The request body format is invalid. Please check and try again."
    ),

    GENERIC_MESSAGE_NOT_READABLE(
            "/generic-message-not-readable",
            "Unreadable body",
            "The request body is unreadable or malformed."
    );

    private final String path;
    private final String title;
    private final String message;

    ProblemType(String path, String title, String message) {
        this.path = path;
        this.title = title;
        this.message = message;
    }
}
