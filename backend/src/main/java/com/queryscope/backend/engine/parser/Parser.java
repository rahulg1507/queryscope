package com.queryscope.backend.engine.parser;

import com.queryscope.backend.engine.ast.BooleanLiteral;
import com.queryscope.backend.engine.ast.ColumnExpression;
import com.queryscope.backend.engine.ast.ColumnSelectItem;
import com.queryscope.backend.engine.ast.ComparisonExpression;
import com.queryscope.backend.engine.ast.ComparisonOperator;
import com.queryscope.backend.engine.ast.Expression;
import com.queryscope.backend.engine.ast.FromSource;
import com.queryscope.backend.engine.ast.JoinCondition;
import com.queryscope.backend.engine.ast.JoinSource;
import com.queryscope.backend.engine.ast.NumberLiteral;
import com.queryscope.backend.engine.ast.SelectItem;
import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.engine.ast.StringLiteral;
import com.queryscope.backend.engine.ast.TableReference;
import com.queryscope.backend.engine.ast.WildcardSelectItem;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    private final List<Token> tokens;
    private int current;

    public Parser(List<Token> tokens) {
        this.tokens = List.copyOf(tokens);
    }

    public SelectStatement parse() {
        if (check(TokenType.EOF)) {
            throw error("Unexpected end of query; expected SELECT", peek());
        }
        consume(TokenType.SELECT, "Expected SELECT at the start of the query");
        List<SelectItem> columns = parseSelectItems();
        consume(TokenType.FROM, "Expected FROM after SELECT list");
        String tableName = consume(TokenType.IDENTIFIER, "Expected table identifier after FROM").lexeme();
        TableReference leftTable = new TableReference(tableName);
        FromSource source = leftTable;
        if (match(TokenType.JOIN)) {
            String rightTableName = consume(TokenType.IDENTIFIER, "Expected right table identifier after JOIN").lexeme();
            consume(TokenType.ON, "Expected ON after JOIN table");
            ColumnExpression leftColumn = parseColumnReference("Expected left column in JOIN condition");
            if (!match(TokenType.EQUAL)) {
                throw error("JOIN conditions only support equality comparisons; expected '='", peek());
            }
            ColumnExpression rightColumn = parseColumnReference("Expected right column in JOIN condition");
            source = new JoinSource(leftTable, new TableReference(rightTableName), new JoinCondition(leftColumn, rightColumn));
            if (check(TokenType.JOIN)) {
                throw error("Multiple JOIN clauses are not supported yet", peek());
            }
        }

        Expression where = null;
        if (match(TokenType.WHERE)) {
            where = parseCondition();
        }

        match(TokenType.SEMICOLON);
        if (!check(TokenType.EOF)) {
            throw error("Unexpected trailing token '" + peek().lexeme() + "'", peek());
        }
        return new SelectStatement(columns, source, where);
    }

    private List<SelectItem> parseSelectItems() {
        if (match(TokenType.STAR)) {
            if (check(TokenType.COMMA)) {
                throw error("Wildcard cannot be combined with other SELECT columns", peek());
            }
            return List.of(new WildcardSelectItem());
        }
        if (!check(TokenType.IDENTIFIER)) {
            throw error("Expected SELECT column or '*'", peek());
        }

        List<SelectItem> columns = new ArrayList<>();
        columns.add(parseColumnSelectItem());
        while (match(TokenType.COMMA)) {
            if (!check(TokenType.IDENTIFIER)) {
                throw error("Expected identifier after ','", peek());
            }
            columns.add(parseColumnSelectItem());
        }
        return List.copyOf(columns);
    }

    private Expression parseCondition() {
        ColumnExpression column = parseColumnReference("Expected identifier after WHERE");
        ComparisonOperator operator = parseComparisonOperator();
        Expression right = parseLiteral();
        return new ComparisonExpression(column, operator, right);
    }

    private ColumnSelectItem parseColumnSelectItem() {
        Token first = consume(TokenType.IDENTIFIER, "Expected column identifier");
        if (match(TokenType.DOT)) {
            Token second = consume(TokenType.IDENTIFIER, "Expected column identifier after '.'");
            return new ColumnSelectItem(first.lexeme(), second.lexeme());
        }
        return new ColumnSelectItem(first.lexeme());
    }

    private ColumnExpression parseColumnReference(String message) {
        Token first = consume(TokenType.IDENTIFIER, message);
        if (match(TokenType.DOT)) {
            Token second = consume(TokenType.IDENTIFIER, "Expected column identifier after '.'");
            return new ColumnExpression(first.lexeme(), second.lexeme());
        }
        return new ColumnExpression(first.lexeme());
    }

    private ComparisonOperator parseComparisonOperator() {
        Token token = peek();
        if (!check(TokenType.EOF)) {
            advance();
        }
        return switch (token.type()) {
            case EQUAL -> ComparisonOperator.EQUAL;
            case NOT_EQUAL -> ComparisonOperator.NOT_EQUAL;
            case LESS_THAN -> ComparisonOperator.LESS_THAN;
            case LESS_EQUAL -> ComparisonOperator.LESS_EQUAL;
            case GREATER_THAN -> ComparisonOperator.GREATER_THAN;
            case GREATER_EQUAL -> ComparisonOperator.GREATER_EQUAL;
            default -> throw error("Expected comparison operator after WHERE column", token);
        };
    }

    private Expression parseLiteral() {
        Token token = peek();
        if (match(TokenType.NUMBER)) {
            try {
                return new NumberLiteral(Long.parseLong(token.lexeme()));
            } catch (NumberFormatException exception) {
                throw error("Invalid integer literal '" + token.lexeme() + "'", token);
            }
        }
        if (match(TokenType.STRING)) {
            return new StringLiteral(token.lexeme());
        }
        if (match(TokenType.BOOLEAN)) {
            return new BooleanLiteral(Boolean.parseBoolean(token.lexeme()));
        }
        throw error("Expected expression on the right side of comparison", token);
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(message, peek());
    }

    private boolean match(TokenType type) {
        if (!check(type)) {
            return false;
        }
        advance();
        return true;
    }

    private boolean check(TokenType type) {
        return peek().type() == type;
    }

    private Token advance() {
        if (!check(TokenType.EOF)) {
            current++;
        }
        return previous();
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private static ParserException error(String message, Token token) {
        return new ParserException(message + " at position " + token.position());
    }
}
