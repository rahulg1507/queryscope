package com.queryscope.backend.engine.parser;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LexerTest {

    @Test
    void tokenizesKeywordsIdentifiersAndPunctuation() {
        List<Token> tokens = lex("SELECT name, age FROM users WHERE age >= 18;");

        assertThat(tokens).extracting(Token::type).containsExactly(
                TokenType.SELECT, TokenType.IDENTIFIER, TokenType.COMMA,
                TokenType.IDENTIFIER, TokenType.FROM, TokenType.IDENTIFIER,
                TokenType.WHERE, TokenType.IDENTIFIER, TokenType.GREATER_EQUAL,
                TokenType.NUMBER, TokenType.SEMICOLON, TokenType.EOF
        );
        assertThat(tokens.get(1).lexeme()).isEqualTo("name");
        assertThat(tokens.get(5).lexeme()).isEqualTo("users");
    }

    @Test
    void tokenizesWildcardAllComparisonOperatorsAndParentheses() {
        List<Token> tokens = lex("* = != < <= > >= ( )");

        assertThat(tokens).extracting(Token::type).containsExactly(
                TokenType.STAR, TokenType.EQUAL, TokenType.NOT_EQUAL,
                TokenType.LESS_THAN, TokenType.LESS_EQUAL, TokenType.GREATER_THAN,
                TokenType.GREATER_EQUAL, TokenType.LPAREN, TokenType.RPAREN,
                TokenType.EOF
        );
    }

    @Test
    void treatsKeywordsAndBooleansCaseInsensitively() {
        List<Token> tokens = lex("SeLeCt select SELECT FrOm WHERE true FALSE");

        assertThat(tokens).extracting(Token::type).containsExactly(
                TokenType.SELECT, TokenType.SELECT, TokenType.SELECT,
                TokenType.FROM, TokenType.WHERE, TokenType.BOOLEAN,
                TokenType.BOOLEAN, TokenType.EOF
        );
        assertThat(tokens.get(5).lexeme()).isEqualTo("true");
        assertThat(tokens.get(6).lexeme()).isEqualTo("FALSE");
    }

    @Test
    void tokenizesIntegerAndStringLiteralsIncludingEscapedQuote() {
        List<Token> tokens = lex("-5 'Rahul''s'");

        assertThat(tokens.get(0)).isEqualTo(new Token(TokenType.NUMBER, "-5", 0));
        assertThat(tokens.get(1)).isEqualTo(new Token(TokenType.STRING, "Rahul's", 3));
    }

    @Test
    void ignoresWhitespaceAndAcceptsTrailingSemicolon() {
        List<Token> tokens = lex("  SELECT\n name\tFROM users ;  ");

        assertThat(tokens.get(0).type()).isEqualTo(TokenType.SELECT);
        assertThat(tokens.get(tokens.size() - 2).type()).isEqualTo(TokenType.SEMICOLON);
        assertThat(tokens.get(tokens.size() - 1).type()).isEqualTo(TokenType.EOF);
    }

    @Test
    void rejectsUnterminatedStringsAndUnknownCharacters() {
        assertThatThrownBy(() -> lex("'Rahul"))
                .isInstanceOf(ParserException.class)
                .hasMessageContaining("Unterminated string")
                .hasMessageContaining("position 0");
        assertThatThrownBy(() -> lex("SELECT #"))
                .isInstanceOf(ParserException.class)
                .hasMessageContaining("Unknown character '#'")
                .hasMessageContaining("position 7");
    }

    private static List<Token> lex(String sql) {
        return new Lexer(sql).tokenize();
    }
}
