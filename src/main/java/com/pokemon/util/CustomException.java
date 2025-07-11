package com.pokemon.util;

/**
 * Exception usada para lanzar errores de negocio
 */
public class CustomException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int statusCode;

    public CustomException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public CustomException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
} 