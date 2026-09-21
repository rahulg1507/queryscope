package com.queryscope.backend.engine.ast;

public sealed interface AggregateArgument permits AggregateColumnArgument, AggregateWildcardArgument {

    String display();
}
