package com.pokemon.controller;

import com.pokemon.util.CustomException;
import com.pokemon.model.ErrorModel;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;

public abstract class BaseExceptionHandler {
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorModel> handleCustomException(CustomException ex) {
        ErrorModel error = new ErrorModel(
            "error",
            ex.getMessage(),
            ex.getStatusCode(),
            ex.getMessage()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorModel> handleException(Exception ex) {
        ErrorModel error = new ErrorModel(
            "fatal",
            ex.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
} 