package br.com.alura.AluraFake.globalHandler;

import br.com.alura.AluraFake.globalHandler.dto.ErrorField;
import br.com.alura.AluraFake.globalHandler.dto.ErrorResponse;
import br.com.alura.AluraFake.globalHandler.dto.ParsedExceptionDetails;
import br.com.alura.AluraFake.globalHandler.dto.ProblemType;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.add(new ErrorField(error.getField(), error.getDefaultMessage()))
        );
        ex.getBindingResult().getGlobalErrors().forEach(error ->
                fieldErrors.add(new ErrorField(error.getObjectName(), error.getDefaultMessage()))
        );

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorBaseUri + ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getPath(),
                ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getTitle(),
                ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getMessage(),
                ((ServletWebRequest) request).getRequest().getRequestURI(),
                ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getMessage(),
                fieldErrors
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String requestUri = ((ServletWebRequest) request).getRequest().getRequestURI();
        Throwable cause = ex.getMostSpecificCause();

        if (cause instanceof OptionalInvalidException oie) {
            return buildResponseForOptionalInvalid(oie, requestUri);
        } else if (cause instanceof JsonParseException jpe) {
            return buildResponseForJsonParse(jpe, requestUri, headers, status);
        } else if (cause instanceof InvalidFormatException ife) {
            return buildResponseForInvalidFormat(ife, requestUri, headers, status);
        } else if (cause instanceof IllegalArgumentException iae) {
            return buildResponseForIllegalArgument(iae, requestUri, status);
        } else {
            return buildFallbackForNotReadable(ex, requestUri, headers, status);
        }
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            WebRequest request) {

        String uri = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                errorBaseUri + ProblemType.RESOURCE_NOT_FOUND.getPath(),
                ProblemType.RESOURCE_NOT_FOUND.getTitle(),
                ex.getMessage(),
                uri,
                ProblemType.RESOURCE_NOT_FOUND.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OptionalInvalidException.class)
    public ResponseEntity<ErrorResponse> handleOptionalInvalid(
            OptionalInvalidException ex,
            WebRequest request) {

        String uri = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorBaseUri + ProblemType.MESSAGE_NOT_READABLE.getPath(),
                ProblemType.MESSAGE_NOT_READABLE.getTitle(),
                ex.getMessage(),
                uri,
                ProblemType.MESSAGE_NOT_READABLE.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidCourseTaskOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCourseTask(
            InvalidCourseTaskOperationException ex,
            WebRequest request) {

        String uri = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorBaseUri + ProblemType.INVALID_OPERATION.getPath(),
                ProblemType.INVALID_OPERATION.getTitle(),
                ex.getMessage(),
                uri,
                ProblemType.INVALID_OPERATION.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request) {

        List<ErrorField> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(cv -> {
                    String path = cv.getPropertyPath().toString();
                    String field = path.contains(".")
                            ? path.substring(path.lastIndexOf('.') + 1)
                            : path;
                    return new ErrorField(field, cv.getMessage());
                })
                .collect(Collectors.toList());

        String uri = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorBaseUri + ProblemType.INVALID_DATA.getPath(),
                ProblemType.INVALID_DATA.getTitle(),
                ProblemType.INVALID_DATA.getMessage(),
                uri,
                ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getMessage(),
                fieldErrors
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request) {

        String uri = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorBaseUri + ProblemType.INVALID_DATA.getPath(),
                ProblemType.INVALID_DATA.getTitle(),
                ex.getMessage(),
                uri,
                ProblemType.INVALID_DATA.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex,
            WebRequest request) {

        logger.error("Unexpected error captured: ", ex);
        String uri = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                errorBaseUri + ProblemType.UNEXPECTED_ERROR.getPath(),
                ProblemType.UNEXPECTED_ERROR.getTitle(),
                ProblemType.UNEXPECTED_ERROR.getMessage(),
                uri,
                ProblemType.UNEXPECTED_ERROR.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }


    private ResponseEntity<Object> buildResponseForOptionalInvalid(
            OptionalInvalidException ex,
            String requestUri) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorBaseUri + ProblemType.INVALID_DATA.getPath(),
                ProblemType.INVALID_DATA.getTitle(),
                ex.getMessage(),
                requestUri,
                ProblemType.INVALID_DATA.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Object> buildResponseForJsonParse(
            JsonParseException ex,
            String requestUri,
            HttpHeaders headers,
            HttpStatusCode status) {

        ParsedExceptionDetails details = handleJsonParseException(ex);
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                errorBaseUri + ProblemType.TYPE_INVALID_REQUEST_FORMAT.getPath(),
                ProblemType.TYPE_INVALID_REQUEST_FORMAT.getTitle(),
                details.detail(),
                requestUri,
                ProblemType.TYPE_INVALID_REQUEST_FORMAT.getMessage(),
                details.field()
        );
        return new ResponseEntity<>(errorResponse, headers, status);
    }

    private ResponseEntity<Object> buildResponseForInvalidFormat(
            InvalidFormatException ex,
            String requestUri,
            HttpHeaders headers,
            HttpStatusCode status) {

        ParsedExceptionDetails details = handleInvalidFormatException(ex);
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                errorBaseUri + ProblemType.TYPE_INVALID_REQUEST_FORMAT.getPath(),
                ProblemType.TYPE_INVALID_REQUEST_FORMAT.getTitle(),
                details.detail(),
                requestUri,
                ProblemType.TYPE_INVALID_REQUEST_FORMAT.getMessage(),
                details.field()
        );
        return new ResponseEntity<>(errorResponse, headers, status);
    }

    private ResponseEntity<Object> buildResponseForIllegalArgument(
            IllegalArgumentException ex,
            String requestUri,
            HttpStatusCode status) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorBaseUri + ProblemType.INVALID_DATA.getPath(),
                ProblemType.INVALID_DATA.getTitle(),
                ex.getMessage(),
                requestUri,
                ProblemType.INVALID_DATA.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Object> buildFallbackForNotReadable(
            HttpMessageNotReadableException ex,
            String requestUri,
            HttpHeaders headers,
            HttpStatusCode status) {

        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                errorBaseUri + ProblemType.GENERIC_MESSAGE_NOT_READABLE.getPath(),
                ProblemType.GENERIC_MESSAGE_NOT_READABLE.getTitle(),
                ex.getCause().getMessage(),
                requestUri,
                ProblemType.GENERIC_MESSAGE_NOT_READABLE.getMessage()
        );
        return new ResponseEntity<>(errorResponse, headers, status);
    }

    private ParsedExceptionDetails handleJsonParseException(JsonParseException json) {
        String detailMsg = String.format("JSON syntax error at location: line %d, column %d. Check the submitted JSON.",
                json.getLocation().getLineNr(), json.getLocation().getColumnNr());
        return new ParsedExceptionDetails(detailMsg, null);
    }

    private ParsedExceptionDetails handleInvalidFormatException(InvalidFormatException invalidFormat) {
        String fieldPath = formatFieldPath(invalidFormat.getPath());
        String detailMsg = String.format("The value '%s' provided for the field '%s' is not of the expected type (%s).",
                invalidFormat.getValue(), fieldPath, invalidFormat.getTargetType().getSimpleName());
        List<ErrorField> fields = List.of(
                new ErrorField(fieldPath, String.format("Invalid value '%s'. Please provide a value of type %s.",
                        invalidFormat.getValue(), invalidFormat.getTargetType().getSimpleName()))
        );

        return new ParsedExceptionDetails(detailMsg, fields);
    }

    private String formatFieldPath(List<com.fasterxml.jackson.databind.JsonMappingException.Reference> path) {
        return path.stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining("."));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolation(
            BusinessRuleException ex,
            WebRequest request) {
        String uri = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorBaseUri + ProblemType.INVALID_OPERATION.getPath(),
                ProblemType.INVALID_OPERATION.getTitle(),
                ex.getMessage(),
                uri,
                ex.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex,
            WebRequest request) {

        String uri = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                errorBaseUri + ProblemType.RESOURCE_NOT_FOUND.getPath(),
                ProblemType.RESOURCE_NOT_FOUND.getTitle(),
                ex.getMessage(),
                uri,
                ProblemType.RESOURCE_NOT_FOUND.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
}
