package br.com.alura.AluraFake.exceptionhandler.dto;

import java.util.List;

public record ParsedExceptionDetails(String detail, List<ErrorField> field) {
}
