package com.transporte.bogota.util;

import java.util.*;

/**
 * Implementación de un Árbol B+ para indexación eficiente de estaciones.
 * Optimiza búsquedas por prefijo y rango.
 *
 * Características:
 * - Orden del árbol: 50 (permite hasta 100 claves por nodo)
 * - Todas las claves están en las hojas
 * - Las hojas están enlazadas para búsquedas de rango eficientes
 * - Complejidad de búsqueda: O(log n)
 */
public class BPlusTree<K extends Comparable<K>, V> {

    private static final int ORDER = 50; // Orden del árbol
    private Node root;
    private LeafNode firstLeaf; // Para recorrer todas las hojas

    public BPlusTree() {
        this.root = null;
    }

    /**
     * Inserta un par clave-valor en el árbol.
     */
    public void insert(K key, V value) {
        if (key == null) return;

        if (root == null) {
            root = new LeafNode();
            firstLeaf = (LeafNode) root;
        }

        LeafNode leaf = findLeafNode(key);

        if (leaf.insert(key, value)) {
            // No hay overflow, inserción exitosa
            return;
        }

        // Overflow: dividir el nodo
        LeafNode newLeaf = leaf.split();
        K newKey = newLeaf.keys.get(0);

        if (leaf == root) {
            InternalNode newRoot = new InternalNode();
            newRoot.keys.add(newKey);
            newRoot.children.add(leaf);
            newRoot.children.add(newLeaf);
            root = newRoot;
        } else {
            InternalNode parent = findParent(root, leaf);
            insertInParent(parent, newKey, leaf, newLeaf);
        }
    }

    /**
     * Busca un valor exacto por clave.
     */
    public V search(K key) {
        if (root == null || key == null) return null;

        LeafNode leaf = findLeafNode(key);
        int index = leaf.keys.indexOf(key);

        return index >= 0 ? leaf.values.get(index) : null;
    }

    /**
     * Busca todos los valores cuyas claves comienzan con el prefijo dado.
     * Ideal para búsqueda autocompletada.
     */
    public List<V> searchByPrefix(String prefix, int limit) {
        List<V> results = new ArrayList<>();
        if (root == null || prefix == null) return results;

        String prefixLower = prefix.toLowerCase();

        // Comenzar desde la primera hoja
        LeafNode current = firstLeaf;

        while (current != null && results.size() < limit) {
            for (int i = 0; i < current.keys.size() && results.size() < limit; i++) {
                K key = current.keys.get(i);
                if (key instanceof String) {
                    String keyStr = ((String) key).toLowerCase();
                    if (keyStr.startsWith(prefixLower)) {
                        results.add(current.values.get(i));
                    }
                }
            }
            current = current.next;
        }

        return results;
    }

    /**
     * Busca todos los valores cuyas claves contienen el texto dado.
     */
    public List<V> searchByContains(String text, int limit) {
        List<V> results = new ArrayList<>();
        if (root == null || text == null) return results;

        String textLower = text.toLowerCase();

        LeafNode current = firstLeaf;

        while (current != null && results.size() < limit) {
            for (int i = 0; i < current.keys.size() && results.size() < limit; i++) {
                K key = current.keys.get(i);
                if (key instanceof String) {
                    String keyStr = ((String) key).toLowerCase();
                    if (keyStr.contains(textLower)) {
                        results.add(current.values.get(i));
                    }
                }
            }
            current = current.next;
        }

        return results;
    }

    /**
     * Obtiene todos los valores en orden.
     */
    public List<V> getAllValues() {
        List<V> results = new ArrayList<>();
        if (root == null) return results;

        LeafNode current = firstLeaf;
        while (current != null) {
            results.addAll(current.values);
            current = current.next;
        }

        return results;
    }

    private LeafNode findLeafNode(K key) {
        if (root instanceof LeafNode) {
            return (LeafNode) root;
        }

        InternalNode node = (InternalNode) root;

        while (node != null) {
            int i = 0;
            while (i < node.keys.size() && key.compareTo(node.keys.get(i)) >= 0) {
                i++;
            }

            Node child = node.children.get(i);
            if (child instanceof LeafNode) {
                return (LeafNode) child;
            }
            node = (InternalNode) child;
        }

        return null;
    }

    private InternalNode findParent(Node current, Node child) {
        if (current instanceof LeafNode) {
            return null;
        }

        InternalNode internal = (InternalNode) current;

        for (Node node : internal.children) {
            if (node == child) {
                return internal;
            }
            if (node instanceof InternalNode) {
                InternalNode parent = findParent(node, child);
                if (parent != null) return parent;
            }
        }

        return null;
    }

    private void insertInParent(InternalNode parent, K key, Node left, Node right) {
        if (parent.insert(key, right)) {
            return;
        }

        // Overflow en nodo interno
        InternalNode newInternal = parent.splitInternal();
        K newKey = newInternal.keys.get(0);
        newInternal.keys.remove(0);

        if (parent == root) {
            InternalNode newRoot = new InternalNode();
            newRoot.keys.add(newKey);
            newRoot.children.add(parent);
            newRoot.children.add(newInternal);
            root = newRoot;
        } else {
            InternalNode grandParent = findParent(root, parent);
            insertInParent(grandParent, newKey, parent, newInternal);
        }
    }

    // =========================================================================
    // CLASES INTERNAS: NODOS
    // =========================================================================

    private abstract class Node {
        List<K> keys;

        Node() {
            this.keys = new ArrayList<>();
        }
    }

    /**
     * Nodo interno del árbol B+.
     * Contiene claves y referencias a hijos.
     */
    private class InternalNode extends Node {
        List<Node> children;

        InternalNode() {
            super();
            this.children = new ArrayList<>();
        }

        boolean insert(K key, Node child) {
            int i = 0;
            while (i < keys.size() && key.compareTo(keys.get(i)) > 0) {
                i++;
            }
            keys.add(i, key);
            children.add(i + 1, child);

            return keys.size() <= ORDER;
        }

        InternalNode splitInternal() {
            int midpoint = ORDER / 2;

            InternalNode newNode = new InternalNode();
            newNode.keys.addAll(keys.subList(midpoint, keys.size()));
            newNode.children.addAll(children.subList(midpoint + 1, children.size()));

            keys.subList(midpoint, keys.size()).clear();
            children.subList(midpoint + 1, children.size()).clear();

            return newNode;
        }
    }

    /**
     * Nodo hoja del árbol B+.
     * Contiene las claves y valores reales.
     * Las hojas están enlazadas entre sí.
     */
    private class LeafNode extends Node {
        List<V> values;
        LeafNode next;

        LeafNode() {
            super();
            this.values = new ArrayList<>();
            this.next = null;
        }

        boolean insert(K key, V value) {
            int i = 0;
            while (i < keys.size() && key.compareTo(keys.get(i)) > 0) {
                i++;
            }

            // Verificar duplicado
            if (i < keys.size() && key.compareTo(keys.get(i)) == 0) {
                values.set(i, value); // Actualizar valor existente
                return true;
            }

            keys.add(i, key);
            values.add(i, value);

            return keys.size() <= ORDER;
        }

        LeafNode split() {
            int midpoint = (ORDER + 1) / 2;

            LeafNode newLeaf = new LeafNode();
            newLeaf.keys.addAll(keys.subList(midpoint, keys.size()));
            newLeaf.values.addAll(values.subList(midpoint, values.size()));

            keys.subList(midpoint, keys.size()).clear();
            values.subList(midpoint, values.size()).clear();

            newLeaf.next = this.next;
            this.next = newLeaf;

            return newLeaf;
        }
    }

    /**
     * Retorna estadísticas del árbol para debugging.
     */
    public String getStats() {
        if (root == null) return "Árbol vacío";

        int totalKeys = 0;
        int leafCount = 0;

        LeafNode current = firstLeaf;
        while (current != null) {
            totalKeys += current.keys.size();
            leafCount++;
            current = current.next;
        }

        return String.format("B+ Tree - Claves: %d, Hojas: %d, Orden: %d",
                           totalKeys, leafCount, ORDER);
    }
}
