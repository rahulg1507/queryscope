package com.queryscope.backend.engine.index;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A small in-memory B+ tree. The order is the maximum number of keys per node. */
public final class BPlusTree<K, V> {

    private final int order;
    private final Comparator<? super K> comparator;
    private Node<K, V> root;
    private LeafNode<K, V> firstLeaf;
    private int distinctKeys;
    private int entryCount;

    public BPlusTree(int order, Comparator<? super K> comparator) {
        if (order < 3 || comparator == null) {
            throw new IllegalArgumentException("A B+ tree requires order >= 3 and a comparator.");
        }
        this.order = order;
        this.comparator = comparator;
        this.firstLeaf = new LeafNode<>();
        this.root = firstLeaf;
    }

    public void insert(K key, V value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("B+ tree keys and values must not be null.");
        }
        LeafNode<K, V> leaf = findLeaf(key);
        int position = lowerBound(leaf.keys, key);
        if (position < leaf.keys.size() && comparator.compare(leaf.keys.get(position), key) == 0) {
            leaf.values.get(position).add(value);
            entryCount++;
            return;
        }
        leaf.keys.add(position, key);
        List<V> values = new ArrayList<>();
        values.add(value);
        leaf.values.add(position, values);
        distinctKeys++;
        entryCount++;
        if (leaf.keys.size() > order) {
            splitLeaf(leaf);
        }
    }

    public TreeLookupResult<V> exact(K key) {
        LeafNode<K, V> leaf = findLeaf(key);
        int position = lowerBound(leaf.keys, key);
        int visited = Math.min(position + 1, leaf.keys.size());
        if (position >= leaf.keys.size() || comparator.compare(leaf.keys.get(position), key) != 0) {
            return new TreeLookupResult<>(List.of(), visited);
        }
        return new TreeLookupResult<>(leaf.values.get(position), visited);
    }

    public TreeLookupResult<V> range(K lower, boolean includeLower, K upper, boolean includeUpper) {
        if (lower != null && upper != null) {
            int comparison = comparator.compare(lower, upper);
            if (comparison > 0 || (comparison == 0 && (!includeLower || !includeUpper))) {
                return new TreeLookupResult<>(List.of(), 0);
            }
        }
        LeafNode<K, V> leaf = lower == null ? firstLeaf : findLeaf(lower);
        int position = lower == null ? 0 : lowerBound(leaf.keys, lower);
        List<V> matches = new ArrayList<>();
        int visited = 0;
        while (leaf != null) {
            while (position < leaf.keys.size()) {
                K key = leaf.keys.get(position);
                visited++;
                boolean afterLower = lower == null || comparator.compare(key, lower) > 0
                        || (includeLower && comparator.compare(key, lower) == 0);
                boolean beforeUpper = upper == null || comparator.compare(key, upper) < 0
                        || (includeUpper && comparator.compare(key, upper) == 0);
                if (afterLower && beforeUpper) {
                    matches.addAll(leaf.values.get(position));
                }
                if (upper != null && comparator.compare(key, upper) > 0) {
                    return new TreeLookupResult<>(matches, visited);
                }
                if (upper != null && comparator.compare(key, upper) == 0) {
                    return new TreeLookupResult<>(matches, visited);
                }
                position++;
            }
            leaf = leaf.next;
            position = 0;
        }
        return new TreeLookupResult<>(matches, visited);
    }

    public int distinctKeyCount() {
        return distinctKeys;
    }

    public int entryCount() {
        return entryCount;
    }

    public List<K> keysInOrder() {
        List<K> keys = new ArrayList<>();
        LeafNode<K, V> leaf = firstLeaf;
        while (leaf != null) {
            keys.addAll(leaf.keys);
            leaf = leaf.next;
        }
        return List.copyOf(keys);
    }

    public void validateInvariants() {
        List<LeafNode<K, V>> leaves = new ArrayList<>();
        int leafDepth = validateNode(root, 0, leaves);
        if (leafDepth < 0 || leaves.isEmpty() || leaves.get(0) != firstLeaf) {
            throw new IllegalStateException("Invalid B+ tree leaf chain.");
        }
        for (int index = 0; index + 1 < leaves.size(); index++) {
            if (leaves.get(index).next != leaves.get(index + 1)) {
                throw new IllegalStateException("Invalid B+ tree leaf link.");
            }
        }
        if (keysInOrder().size() != distinctKeys) {
            throw new IllegalStateException("Invalid B+ tree key count.");
        }
    }

    private int validateNode(Node<K, V> node, int depth, List<LeafNode<K, V>> leaves) {
        if (node instanceof LeafNode<K, V> leaf) {
            for (int index = 1; index < leaf.keys.size(); index++) {
                if (comparator.compare(leaf.keys.get(index - 1), leaf.keys.get(index)) >= 0) {
                    throw new IllegalStateException("Leaf keys are not strictly sorted.");
                }
            }
            leaves.add(leaf);
            return depth;
        }
        InternalNode<K, V> internal = (InternalNode<K, V>) node;
        if (internal.children.size() != internal.keys.size() + 1) {
            throw new IllegalStateException("Internal separator count is invalid.");
        }
        for (int index = 0; index < internal.children.size(); index++) {
            if (index > 0 && !firstKey(internal.children.get(index)).equals(internal.keys.get(index - 1))) {
                throw new IllegalStateException("Internal separator key is invalid.");
            }
            int childDepth = validateNode(internal.children.get(index), depth + 1, leaves);
            if (childDepth != depth + 1) {
                throw new IllegalStateException("Leaf depths are inconsistent.");
            }
        }
        return depth;
    }

    private LeafNode<K, V> findLeaf(K key) {
        Node<K, V> node = root;
        while (node instanceof InternalNode<K, V> internal) {
            int child = 0;
            while (child < internal.keys.size() && comparator.compare(key, internal.keys.get(child)) >= 0) {
                child++;
            }
            node = internal.children.get(child);
        }
        return (LeafNode<K, V>) node;
    }

    private void splitLeaf(LeafNode<K, V> leaf) {
        int split = (leaf.keys.size() + 1) / 2;
        LeafNode<K, V> right = new LeafNode<>();
        right.keys.addAll(new ArrayList<>(leaf.keys.subList(split, leaf.keys.size())));
        right.values.addAll(new ArrayList<>(leaf.values.subList(split, leaf.values.size())));
        leaf.keys.subList(split, leaf.keys.size()).clear();
        leaf.values.subList(split, leaf.values.size()).clear();
        right.next = leaf.next;
        leaf.next = right;
        if (leaf == firstLeaf && comparator.compare(right.keys.get(0), firstLeaf.keys.get(0)) < 0) {
            firstLeaf = right;
        }
        insertAfter(leaf, right);
    }

    private void insertAfter(Node<K, V> left, Node<K, V> right) {
        if (left.parent == null) {
            InternalNode<K, V> newRoot = new InternalNode<>();
            newRoot.children.add(left);
            newRoot.children.add(right);
            left.parent = newRoot;
            right.parent = newRoot;
            rebuildKeys(newRoot);
            root = newRoot;
            return;
        }
        InternalNode<K, V> parent = left.parent;
        int index = parent.children.indexOf(left);
        parent.children.add(index + 1, right);
        right.parent = parent;
        rebuildKeys(parent);
        if (parent.children.size() > order + 1) {
            splitInternal(parent);
        }
    }

    private void splitInternal(InternalNode<K, V> node) {
        int split = node.children.size() / 2;
        InternalNode<K, V> right = new InternalNode<>();
        right.children.addAll(new ArrayList<>(node.children.subList(split, node.children.size())));
        node.children.subList(split, node.children.size()).clear();
        right.children.forEach(child -> child.parent = right);
        rebuildKeys(node);
        rebuildKeys(right);
        insertAfter(node, right);
    }

    private void rebuildKeys(InternalNode<K, V> node) {
        node.keys.clear();
        for (int index = 1; index < node.children.size(); index++) {
            node.keys.add(firstKey(node.children.get(index)));
        }
    }

    private K firstKey(Node<K, V> node) {
        if (node instanceof LeafNode<K, V> leaf) {
            return leaf.keys.get(0);
        }
        return firstKey(((InternalNode<K, V>) node).children.get(0));
    }

    private int lowerBound(List<K> keys, K key) {
        int low = 0;
        int high = keys.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (comparator.compare(keys.get(middle), key) < 0) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    private abstract static class Node<K, V> {
        private InternalNode<K, V> parent;
    }

    private static final class InternalNode<K, V> extends Node<K, V> {
        private final List<K> keys = new ArrayList<>();
        private final List<Node<K, V>> children = new ArrayList<>();
    }

    private static final class LeafNode<K, V> extends Node<K, V> {
        private final List<K> keys = new ArrayList<>();
        private final List<List<V>> values = new ArrayList<>();
        private LeafNode<K, V> next;
    }
}
