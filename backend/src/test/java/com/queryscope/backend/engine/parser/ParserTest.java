package com.queryscope.backend.engine.parser;

import com.queryscope.backend.engine.ast.BooleanLiteral;
import com.queryscope.backend.engine.ast.ColumnExpression;
import com.queryscope.backend.engine.ast.ColumnSelectItem;
import com.queryscope.backend.engine.ast.ComparisonExpression;
import com.queryscope.backend.engine.ast.ComparisonOperator;
import com.queryscope.backend.engine.ast.JoinCondition;
import com.queryscope.backend.engine.ast.JoinSource;
import com.queryscope.backend.engine.ast.NumberLiteral;
import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.engine.ast.StringLiteral;
import com.queryscope.backend.engine.ast.TableReference;
import com.queryscope.backend.engine.ast.WildcardSelectItem;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParserTest {

    @Test
    void parsesWildcardSelect() {
        assertThat(parse("SELECT * FROM users;"))
                .isEqualTo(new SelectStatement(
                        java.util.List.of(new WildcardSelectItem()),
                        new TableReference("users"), null));
    }

    @Test
    void parsesOneAndMultipleColumns() {
        assertThat(parse("SELECT name FROM users"))
                .isEqualTo(new SelectStatement(
                        java.util.List.of(new ColumnSelectItem("name")),
                        new TableReference("users"), null));
        assertThat(parse("SELECT name, age FROM users"))
                .isEqualTo(new SelectStatement(
                        java.util.List.of(new ColumnSelectItem("name"), new ColumnSelectItem("age")),
                        new TableReference("users"), null));
    }

    @Test
    void parsesNumericStringAndBooleanComparisons() {
        assertThat(parse("SELECT name FROM users WHERE age > 18").where())
                .isEqualTo(new ComparisonExpression(
                        new ColumnExpression("age"), ComparisonOperator.GREATER_THAN, new NumberLiteral(18)));
        assertThat(parse("SELECT name FROM users WHERE name = 'Rahul'").where())
                .isEqualTo(new ComparisonExpression(
                        new ColumnExpression("name"), ComparisonOperator.EQUAL, new StringLiteral("Rahul")));
        assertThat(parse("SELECT name FROM users WHERE active = true").where())
                .isEqualTo(new ComparisonExpression(
                        new ColumnExpression("active"), ComparisonOperator.EQUAL, new BooleanLiteral(true)));
    }

    @Test
    void parsesEveryComparisonOperator() {
        assertThat(parse("SELECT name FROM users WHERE age = 18").where())
                .isEqualTo(new ComparisonExpression(new ColumnExpression("age"), ComparisonOperator.EQUAL, new NumberLiteral(18)));
        assertThat(parse("SELECT name FROM users WHERE age != 18").where())
                .isEqualTo(new ComparisonExpression(new ColumnExpression("age"), ComparisonOperator.NOT_EQUAL, new NumberLiteral(18)));
        assertThat(parse("SELECT name FROM users WHERE age < 18").where())
                .isEqualTo(new ComparisonExpression(new ColumnExpression("age"), ComparisonOperator.LESS_THAN, new NumberLiteral(18)));
        assertThat(parse("SELECT name FROM users WHERE age <= 18").where())
                .isEqualTo(new ComparisonExpression(new ColumnExpression("age"), ComparisonOperator.LESS_EQUAL, new NumberLiteral(18)));
        assertThat(parse("SELECT name FROM users WHERE age >= 18").where())
                .isEqualTo(new ComparisonExpression(new ColumnExpression("age"), ComparisonOperator.GREATER_EQUAL, new NumberLiteral(18)));
    }

    @Test
    void treatsKeywordsCaseInsensitivelyAndPreservesIdentifiers() {
        assertThat(parse("sElEcT firstName fRoM UserTable")).isEqualTo(
                new SelectStatement(java.util.List.of(new ColumnSelectItem("firstName")),
                        new TableReference("UserTable"), null));
    }

    @Test
    void parsesQualifiedColumnsAndInnerJoin() {
        SelectStatement statement = parse("SELECT users.name, expenses.amount FROM users JOIN expenses ON users.id = expenses.user_id;");

        assertThat(statement.from()).isEqualTo(new JoinSource(
                new TableReference("users"),
                new TableReference("expenses"),
                new JoinCondition(
                        new ColumnExpression("users", "id"),
                        new ColumnExpression("expenses", "user_id"))));
        assertThat(statement.columns()).containsExactly(
                new ColumnSelectItem("users", "name"),
                new ColumnSelectItem("expenses", "amount"));
    }

    @Test
    void parsesJoinAndOnCaseInsensitively() {
        assertThat(parse("select users.id from USERS join expenses on users.id = expenses.user_id").from())
                .isEqualTo(new JoinSource(
                        new TableReference("USERS"),
                        new TableReference("expenses"),
                        new JoinCondition(new ColumnExpression("users", "id"),
                                new ColumnExpression("expenses", "user_id"))));
    }

    @Test
    void reportsUsefulErrorsForInvalidQueries() {
        assertThatThrownBy(() -> parse("SELECT FROM users"))
                .isInstanceOf(ParserException.class)
                .hasMessageContaining("Expected SELECT column");
        assertThatThrownBy(() -> parse("SELECT name users"))
                .isInstanceOf(ParserException.class)
                .hasMessageContaining("Expected FROM");
        assertThatThrownBy(() -> parse("SELECT name FROM users WHERE age"))
                .isInstanceOf(ParserException.class)
                .hasMessageContaining("Expected comparison operator");
        assertThatThrownBy(() -> parse("SELECT name FROM users WHERE age >"))
                .isInstanceOf(ParserException.class)
                .hasMessageContaining("Expected expression");
        assertThatThrownBy(() -> parse("SELECT name FROM users WHERE age ! 18"))
                .isInstanceOf(ParserException.class)
                .hasMessageContaining("Expected '=' after '!'");
        assertThatThrownBy(() -> parse("SELECT name FROM users extra"))
                .isInstanceOf(ParserException.class)
                .hasMessageContaining("Unexpected trailing token");
        assertThatThrownBy(() -> parse("SELECT * FROM users JOIN expenses"))
                .isInstanceOf(ParserException.class)
                .hasMessageContaining("Expected ON after JOIN table");
        assertThatThrownBy(() -> parse("SELECT * FROM users JOIN ON users.id = expenses.user_id"))
                .isInstanceOf(ParserException.class)
                .hasMessageContaining("Expected right table identifier after JOIN");
        assertThatThrownBy(() -> parse("SELECT * FROM users JOIN expenses ON users.id > expenses.user_id"))
                .isInstanceOf(ParserException.class)
                .hasMessageContaining("JOIN conditions only support equality");
    }

    private static SelectStatement parse(String sql) {
        return new Parser(new Lexer(sql).tokenize()).parse();
    }
}
