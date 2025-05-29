package br.com.alura.AluraFake.exceptionhandler.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private Integer status;
    private String type;
    private String title;
    private String detail;
    private String instance;
    private String userMessage;
    private LocalDateTime timestamp;
    private List<ErrorField> fields;

    public ErrorResponse(Integer status, String type, String title, String detail, String instance, String userMessage) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.type = type;
        this.title = title;
        this.detail = detail;
        this.instance = instance;
        this.userMessage = (userMessage != null && !userMessage.isEmpty()) ? userMessage : detail;
    }

    public ErrorResponse(Integer status, String type, String title, String detail, String instance, String userMessage, List<ErrorField> fields) {
        this(status, type, title, detail, instance, userMessage);
        this.fields = fields;
    }


    public Integer getStatus() { return status; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getDetail() { return detail; }
    public String getInstance() { return instance; }
    public String getUserMessage() { return userMessage; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<ErrorField> getFields() { return fields; }


    public void setStatus(Integer status) { this.status = status; }
    public void setType(String type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setDetail(String detail) { this.detail = detail; }
    public void setInstance(String instance) { this.instance = instance; }
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setFields(List<ErrorField> fields) { this.fields = fields; }
}
