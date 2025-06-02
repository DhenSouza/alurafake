package br.com.alura.AluraFake.globalHandler.dto;

import java.util.List;

public record ParsedExceptionDetails(String detail, List<ErrorField> field) {
}
