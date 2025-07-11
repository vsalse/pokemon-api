package com.pokemon.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Modelo de error estándar para respuestas de la API")
public class ErrorModel {
    @Schema(description = "Severidad del error", example = "ERROR")
    private String severity;
    @Schema(description = "Mensaje de error", example = "No se encontró el recurso solicitado")
    private String message;
    @Schema(description = "Código de error", example = "404")
    private int codigo;
    @Schema(description = "Stack trace del error (solo en desarrollo)", example = "java.lang.NullPointerException ...")
    private String trace;

    public ErrorModel() {
    }

    public ErrorModel(String severity, String message, int codigo, String trace) {
        this.severity = severity;
        this.message = message;
        this.codigo = codigo;
        this.trace = trace;
    }
}