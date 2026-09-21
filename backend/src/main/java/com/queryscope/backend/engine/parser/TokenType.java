package com.queryscope.backend.engine.parser;

public enum TokenType {
    SELECT,
    FROM,
    WHERE,
    IDENTIFIER,
    NUMBER,
    STRING,
    BOOLEAN,
    STAR,
    COMMA,
    EQUAL,
    NOT_EQUAL,
    LESS_THAN,
    LESS_EQUAL,
    GREATER_THAN,
    GREATER_EQUAL,
    SEMICOLON,
    LPAREN,
    RPAREN,
    EOF
}
