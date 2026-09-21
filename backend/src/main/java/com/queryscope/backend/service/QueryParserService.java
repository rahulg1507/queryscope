package com.queryscope.backend.service;

import com.queryscope.backend.engine.ast.SelectStatement;
import com.queryscope.backend.engine.parser.Lexer;
import com.queryscope.backend.engine.parser.Parser;
import com.queryscope.backend.engine.parser.ParserException;
import org.springframework.stereotype.Service;

@Service
public class QueryParserService {

    public SelectStatement parse(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new ParserException("SQL query must not be blank");
        }
        return new Parser(new Lexer(sql).tokenize()).parse();
    }
}
