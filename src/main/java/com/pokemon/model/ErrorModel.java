package com.pokemon.model;

public class ErrorModel {
    private String severity;
    private String message;
    private int codigo;
    private String trace;

    public ErrorModel() {}

    public ErrorModel(String severity, String message, int codigo, String trace) {
        this.severity = severity;
        this.message = message;
        this.codigo = codigo;
        this.trace = trace;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCodigo() {
        return codigo;
    }

    public void setCodigo(int codigo) {
        this.codigo = codigo;
    }

    public String getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        this.trace = trace;
    }
} 