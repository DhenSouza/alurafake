package br.com.alura.AluraFake.exceptionhandler.dto;

import lombok.Getter;

@Getter
public enum ProblemType {

    DEFAULT_USER_MESSAGE_VALIDATION(
            "/invalid-fields",                                      // path
            "Campos inválidos",                                     // title
            "Um ou mais campos estão inválidos. Faça o preenchimento correto e tente novamente."
    ),

    INVALID_DATA(
            "/invalid-data",
            "Dados inválidos",
            "Os dados fornecidos são inválidos. Verifique e tente novamente."
    ),

    RESOURCE_NOT_FOUND(
            "/resource-not-found",
            "Recurso não encontrado",
            "O recurso solicitado não foi encontrado."
    ),

    INVALID_OPERATION(
            "/invalid-operation",
            "Operação inválida",
            "A operação solicitada não é permitida."
    ),

    UNEXPECTED_ERROR(
            "/unexpected-error",
            "Erro inesperado",
            "Ocorreu um erro inesperado. Tente novamente mais tarde."
    ),

    MESSAGE_NOT_READABLE(
            "/message-not-readable",
            "Corpo ilegível",
            "Não foi possível processar a requisição devido a um problema no formato ou nos dados enviados. Por favor, verifique e tente novamente."
    ),

    TYPE_INVALID_REQUEST_FORMAT(
            "/invalid-request-format",
            "Formato de requisição inválido",
            "O formato do corpo da requisição é inválido. Verifique e tente novamente."
    ),

    GENERIC_MESSAGE_NOT_READABLE(
            "/generic-message-not-readable",
            "Corpo ilegível",
            "O corpo da requisição está ilegível ou malformado."
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

