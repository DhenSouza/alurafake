package br.com.alura.AluraFake.exceptionhandler;

import br.com.alura.AluraFake.exceptionhandler.dto.ErrorField;
import br.com.alura.AluraFake.exceptionhandler.dto.ErrorResponse;
import br.com.alura.AluraFake.exceptionhandler.dto.ParsedExceptionDetails;
import br.com.alura.AluraFake.exceptionhandler.dto.ProblemType;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Value("${api.error.base-uri:https://api.seusite.com/erros}")
    private String errorBaseUri;



    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<ErrorField> fieldErrors = new ArrayList<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(new ErrorField(error.getField(), error.getDefaultMessage()));
        }

        ex.getBindingResult().getGlobalErrors().forEach(error -> {
            fieldErrors.add(new ErrorField(error.getObjectName(), error.getDefaultMessage()));
        });

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorBaseUri + ProblemType.GENERIC_MESSAGE_NOT_READABLE.getPath(),
                ProblemType.INVALID_DATA.getMessage(),
                ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getTitle(),
                ((ServletWebRequest) request).getRequest().getRequestURI(),
                ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getMessage(),
                fieldErrors
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Throwable mostSpecificCause = ex.getMostSpecificCause();
        String requestUri = ((ServletWebRequest) request).getRequest().getRequestURI();

        if (mostSpecificCause instanceof OptionalInvalidException oie) {
            return buildResponseForOptionalInvalidCause(oie, requestUri);
        } else if (mostSpecificCause instanceof JsonParseException jpe) {
            return buildResponseForJsonParseCause(jpe, requestUri, headers, status);
        } else if (mostSpecificCause instanceof InvalidFormatException ife) {
            return buildResponseForInvalidFormatCause(ife, requestUri, headers, status);
        } else if (mostSpecificCause instanceof IllegalArgumentException iae) {
            return buildResponseForIllegalArgumentCause(iae, requestUri, status);
        } else {
            return buildFallbackResponseForHttpMessageNotReadable(ex, requestUri, headers, status);
        }
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                errorBaseUri + "/recurso-nao-encontrado",
                ProblemType.RESOURCE_NOT_FOUND.getTitle(),
                ex.getMessage(),
                ((ServletWebRequest)request).getRequest().getRequestURI(),
                ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OptionalInvalidException.class)
    public ResponseEntity<ErrorResponse> handleTopLevelOptionalInvalidException(
            OptionalInvalidException ex,
            WebRequest request) {
        logger.warn("Handler OptionalInvalidException: {}", ex);
        String requestUri = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorBaseUri + ProblemType.MESSAGE_NOT_READABLE.getTitle(),
                ProblemType.MESSAGE_NOT_READABLE.getMessage(),
                ex.getMessage(),
                requestUri,
                ex.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidCourseTaskOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCourseTaskOperationException(
            InvalidCourseTaskOperationException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorBaseUri + "/operacao-invalida",
                ProblemType.INVALID_OPERATION.getTitle(),
                ex.getMessage(),
                ((ServletWebRequest)request).getRequest().getRequestURI(),
                ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        List<ErrorField> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(cv -> {
                    String propertyPath = cv.getPropertyPath().toString();
                    String fieldName = propertyPath.contains(".") ? propertyPath.substring(propertyPath.lastIndexOf('.') + 1) : propertyPath;
                    return new ErrorField(fieldName, cv.getMessage());
                })
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorBaseUri + "/dados-invalidos",
                ProblemType.INVALID_DATA.getTitle(),
                ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getTitle(),
                ((ServletWebRequest)request).getRequest().getRequestURI(),
                ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getMessage(),
                fieldErrors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorBaseUri + "/argumento-invalido",
                ProblemType.INVALID_OPERATION.getTitle(),
                ex.getMessage(),
                ((ServletWebRequest)request).getRequest().getRequestURI(),
                ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        logger.error("Exceção não esperada capturada pelo GlobalExceptionHandler: ", ex);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                errorBaseUri + "/erro-de-sistema",
                ProblemType.UNEXPECTED_ERROR.getTitle(),
                "Ocorreu um erro interno inesperado no sistema. Tente novamente mais tarde.",
                ((ServletWebRequest)request).getRequest().getRequestURI(),
                "Ocorreu um erro interno inesperado no sistema. Tente novamente mais tarde ou contate o suporte."
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Object> buildResponseForOptionalInvalidCause(
            OptionalInvalidException ex, String requestUri) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorBaseUri + ProblemType.INVALID_DATA.getTitle(),
                ProblemType.INVALID_DATA.getMessage(),
                ex.getMessage(),
                requestUri,
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Object> buildResponseForJsonParseCause(
            JsonParseException ex, String requestUri, HttpHeaders headers, HttpStatusCode originalStatus) {
        ParsedExceptionDetails details = handleJsonParseException(ex);
        ErrorResponse errorResponse = new ErrorResponse(
                originalStatus.value(),
                errorBaseUri + ProblemType.TYPE_INVALID_REQUEST_FORMAT.getTitle(),
                ProblemType.TYPE_INVALID_REQUEST_FORMAT.getTitle(),
                details.detail(),
                requestUri,
                ProblemType.TYPE_INVALID_REQUEST_FORMAT.getMessage(),
                details.field()
        );
        return new ResponseEntity<>(errorResponse, headers, originalStatus);
    }

    private ResponseEntity<Object> buildResponseForInvalidFormatCause(
            InvalidFormatException ex, String requestUri, HttpHeaders headers, HttpStatusCode originalStatus) {
        ParsedExceptionDetails details = handleInvalidFormatException(ex);
        ErrorResponse errorResponse = new ErrorResponse(
                originalStatus.value(),
                errorBaseUri + ProblemType.TYPE_INVALID_REQUEST_FORMAT.getTitle(),
                ProblemType.TYPE_INVALID_REQUEST_FORMAT.getTitle(),
                details.detail(),
                requestUri,
                ProblemType.TYPE_INVALID_REQUEST_FORMAT.getMessage(),
                details.field()
        );
        return new ResponseEntity<>(errorResponse, headers, originalStatus);
    }

    private ResponseEntity<Object> buildResponseForIllegalArgumentCause(
            IllegalArgumentException ex, String requestUri, HttpStatusCode originalStatus) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorBaseUri + "/parametro-invalido",
                ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getTitle(),
                ex.getMessage(),
                requestUri,
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Object> buildFallbackResponseForHttpMessageNotReadable(
            HttpMessageNotReadableException ex, String requestUri, HttpHeaders headers, HttpStatusCode status) {
        String fallbackDetail = "O corpo da requisição está inválido, ilegível ou ocorreu um problema durante o processamento inicial.";

        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                errorBaseUri + ProblemType.GENERIC_MESSAGE_NOT_READABLE.getPath(),
                ProblemType.GENERIC_MESSAGE_NOT_READABLE.getTitle(),
                fallbackDetail,
                requestUri,
                ProblemType.GENERIC_MESSAGE_NOT_READABLE.getMessage(),
                null
        );
        return new ResponseEntity<>(errorResponse, headers, status);
    }

    private ParsedExceptionDetails handleJsonParseException(JsonParseException json) {
        String detailMsg = String.format("Erro de sintaxe JSON na localização: linha %d, coluna %d. Verifique o JSON enviado.",
                json.getLocation().getLineNr(), json.getLocation().getColumnNr());
        return new ParsedExceptionDetails(detailMsg, null);
    }

    private ParsedExceptionDetails handleInvalidFormatException(InvalidFormatException invalidFormat) {
        String fieldPath = formatFieldPath(invalidFormat.getPath());
        String detailMsg = String.format("O valor '%s' fornecido para o campo '%s' não é do tipo esperado (%s).",
                invalidFormat.getValue(), fieldPath, invalidFormat.getTargetType().getSimpleName());
        List<ErrorField> fields = List.of(
                new ErrorField(fieldPath, String.format("Valor '%s' inválido. Forneça um valor do tipo %s.",
                        invalidFormat.getValue(), invalidFormat.getTargetType().getSimpleName()))
        );

        return new ParsedExceptionDetails(detailMsg, fields);
    }

    private String formatFieldPath(List<com.fasterxml.jackson.databind.JsonMappingException.Reference> path) {
        return path.stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining("."));
    }
}
