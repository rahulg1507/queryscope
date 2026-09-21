package com.queryscope.backend.engine.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Lexer {

    private final String source;
    private int current;

    public Lexer(String source) {
        this.source = source == null ? "" : source;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (!isAtEnd()) {
            skipWhitespace();
            if (isAtEnd()) {
                break;
            }

            int position = current;
            char character = advance();
            switch (character) {
                case '*' -> tokens.add(token(TokenType.STAR, "*", position));
                case ',' -> tokens.add(token(TokenType.COMMA, ",", position));
                case ';' -> tokens.add(token(TokenType.SEMICOLON, ";", position));
                case '(' -> tokens.add(token(TokenType.LPAREN, "(", position));
                case ')' -> tokens.add(token(TokenType.RPAREN, ")", position));
                case '.' -> tokens.add(token(TokenType.DOT, ".", position));
                case '=' -> tokens.add(token(TokenType.EQUAL, "=", position));
                case '!' -> tokens.add(readBangOperator(position));
                case '<' -> tokens.add(readLessOperator(position));
                case '>' -> tokens.add(readGreaterOperator(position));
                case '\'' -> tokens.add(readString(position));
                default -> {
                    if (isIdentifierStart(character)) {
                        tokens.add(readIdentifier(character, position));
                    } else if (isDigit(character) || (character == '-' && hasNextDigit())) {
                        tokens.add(readNumber(character, position));
                    } else {
                        throw error("Unknown character '" + character + "'", position);
                    }
                }
            }
        }
        tokens.add(new Token(TokenType.EOF, "", source.length()));
        return List.copyOf(tokens);
    }

    private Token readBangOperator(int position) {
        if (!match('=')) {
            throw error("Expected '=' after '!'", position);
        }
        return token(TokenType.NOT_EQUAL, "!=", position);
    }

    private Token readLessOperator(int position) {
        return match('=')
                ? token(TokenType.LESS_EQUAL, "<=", position)
                : token(TokenType.LESS_THAN, "<", position);
    }

    private Token readGreaterOperator(int position) {
        return match('=')
                ? token(TokenType.GREATER_EQUAL, ">=", position)
                : token(TokenType.GREATER_THAN, ">", position);
    }

    private Token readString(int position) {
        StringBuilder value = new StringBuilder();
        while (!isAtEnd()) {
            char character = advance();
            if (character == '\'') {
                if (match('\'')) {
                    value.append('\'');
                } else {
                    return token(TokenType.STRING, value.toString(), position);
                }
            } else {
                value.append(character);
            }
        }
        throw error("Unterminated string", position);
    }

    private Token readIdentifier(char first, int position) {
        StringBuilder value = new StringBuilder().append(first);
        while (!isAtEnd() && isIdentifierPart(peek())) {
            value.append(advance());
        }
        String lexeme = value.toString();
        String normalized = lexeme.toLowerCase(Locale.ROOT);
        TokenType type = switch (normalized) {
            case "select" -> TokenType.SELECT;
            case "create" -> TokenType.CREATE;
            case "index" -> TokenType.INDEX;
            case "from" -> TokenType.FROM;
            case "join" -> TokenType.JOIN;
            case "on" -> TokenType.ON;
            case "where" -> TokenType.WHERE;
            case "group" -> TokenType.GROUP;
            case "by" -> TokenType.BY;
            case "count" -> TokenType.COUNT;
            case "sum" -> TokenType.SUM;
            case "avg" -> TokenType.AVG;
            case "true", "false" -> TokenType.BOOLEAN;
            default -> TokenType.IDENTIFIER;
        };
        return token(type, lexeme, position);
    }

    private Token readNumber(char first, int position) {
        StringBuilder value = new StringBuilder().append(first);
        while (!isAtEnd() && isDigit(peek())) {
            value.append(advance());
        }
        return token(TokenType.NUMBER, value.toString(), position);
    }

    private Token token(TokenType type, String lexeme, int position) {
        return new Token(type, lexeme, position);
    }

    private void skipWhitespace() {
        while (!isAtEnd() && Character.isWhitespace(peek())) {
            advance();
        }
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) {
            return false;
        }
        current++;
        return true;
    }

    private char advance() {
        return source.charAt(current++);
    }

    private char peek() {
        return isAtEnd() ? '\0' : source.charAt(current);
    }

    private boolean hasNextDigit() {
        return current < source.length() && isDigit(source.charAt(current));
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private static boolean isIdentifierStart(char character) {
        return Character.isLetter(character) || character == '_';
    }

    private static boolean isIdentifierPart(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }

    private static boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }

    private static ParserException error(String message, int position) {
        return new ParserException(message + " at position " + position);
    }
}
