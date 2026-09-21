package com.queryscope.backend.engine.index;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BPlusTreeTest {

    @Test
    void supportsEmptyAndSingleLookup() {
        BPlusTree<Integer, String> tree = new BPlusTree<>(4, Comparator.naturalOrder());
        assertThat(tree.exact(10).values()).isEmpty();

        tree.insert(10, "ten");
        assertThat(tree.exact(10).values()).containsExactly("ten");
        assertThat(tree.keysInOrder()).containsExactly(10);
        tree.validateInvariants();
    }

    @Test
    void keepsSortedKeysAcrossReverseRandomAndDuplicateInsertions() {
        BPlusTree<Integer, Integer> tree = new BPlusTree<>(4, Comparator.naturalOrder());
        for (int key = 100; key >= 0; key--) {
            tree.insert(key, key);
        }
        for (int key = 0; key <= 100; key += 3) {
            tree.insert(key, key * 10);
        }

        assertThat(tree.keysInOrder()).containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(0, 100).boxed().toList());
        assertThat(tree.exact(3).values()).containsExactly(3, 30);
        assertThat(tree.exact(1000).values()).isEmpty();
        tree.validateInvariants();
    }

    @Test
    void walksLinkedLeavesForInclusiveAndExclusiveRanges() {
        BPlusTree<Integer, Integer> tree = new BPlusTree<>(3, Comparator.naturalOrder());
        for (int key = 1; key <= 20; key++) {
            tree.insert(key, key);
        }

        assertThat(tree.range(5, true, 10, true).values()).containsExactly(5, 6, 7, 8, 9, 10);
        assertThat(tree.range(5, false, 10, false).values()).containsExactly(6, 7, 8, 9);
        assertThat(tree.range(null, false, 4, true).values()).containsExactly(1, 2, 3, 4);
        assertThat(tree.range(18, false, null, false).values()).containsExactly(19, 20);
        assertThat(tree.range(30, true, 40, true).values()).isEmpty();
        tree.validateInvariants();
    }

    @Test
    void preservesTextualKeySemantics() {
        BPlusTree<String, String> tree = new BPlusTree<>(4, Comparator.naturalOrder());
        tree.insert("10", "text-ten");
        tree.insert("2", "text-two");
        tree.insert("10", "text-ten-duplicate");

        assertThat(tree.keysInOrder()).containsExactly("10", "2");
        assertThat(tree.exact("10").values()).containsExactly("text-ten", "text-ten-duplicate");
        assertThat(tree.range("10", true, "2", true).values())
                .containsExactly("text-ten", "text-ten-duplicate", "text-two");
        tree.validateInvariants();
    }
}
