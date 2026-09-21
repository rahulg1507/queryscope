package com.queryscope.backend.engine.ast;

/** Marker interface for parsed SQL statements. */
public sealed interface SqlStatement permits CreateIndexStatement, SelectStatement {
}
