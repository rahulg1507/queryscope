package com.queryscope.backend.engine.index;

import java.util.List;

public record TreeLookupResult<V>(List<V> values, int leafEntriesVisited) {
    public TreeLookupResult {
        values = List.copyOf(values);
    }
}
