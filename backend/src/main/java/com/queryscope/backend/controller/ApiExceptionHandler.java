package com.queryscope.backend.controller;

import com.queryscope.backend.dto.ApiError;
import com.queryscope.backend.engine.parser.ParserException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ParserException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleParserException(ParserException exception) {
        return new ApiError(exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMalformedRequest() {
        return new ApiError("Malformed request body");
    }
}
