package com.queryscope.backend.engine.parser;

public record Token(TokenType type, String lexeme, int position) {
}
