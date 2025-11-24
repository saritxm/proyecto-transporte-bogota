package model;
import java.util.*;

public class BPlusTree<K extends Comparable<K>, V> {

    private final int grado;
    private Nodo raiz;

    public BPlusTree(int grado) {
        this.grado = grado;
        this.raiz = new Nodo(true);
    }

    // ====== NODO ======
    private class Nodo {
        boolean esHoja;
        List<K> llaves;
        List<V> valores;        // solo hojas
        List<Nodo> hijos;       // solo nodos internos
        Nodo siguiente;         // para recorrido secuencial

        Nodo(boolean esHoja) {
            this.esHoja = esHoja;
            this.llaves = new ArrayList<>();
            this.valores = new ArrayList<>();
            this.hijos = new ArrayList<>();
        }
    }

    // ====== BUSCAR ======
    public V buscar(K key) {
        return buscarRec(raiz, key);
    }

    private V buscarRec(Nodo nodo, K key) {
        int i = 0;
        while (i < nodo.llaves.size() && key.compareTo(nodo.llaves.get(i)) > 0) i++;

        if (nodo.esHoja) {
            if (i < nodo.llaves.size() && nodo.llaves.get(i).equals(key)) {
                return nodo.valores.get(i);
            }
            return null;
        } else {
            return buscarRec(nodo.hijos.get(i), key);
        }
    }

    // ====== INSERTAR (skeleton – simple) ======
    public void insertar(K key, V value) {
        // Inserción básica (todavía no divide nodos)
        insertarRec(raiz, key, value);

        // TODO: agregar división de nodos cuando se exceda el grado
    }

    private void insertarRec(Nodo nodo, K key, V value) {
        int i = 0;
        while (i < nodo.llaves.size() && key.compareTo(nodo.llaves.get(i)) > 0) i++;

        if (nodo.esHoja) {
            nodo.llaves.add(i, key);
            nodo.valores.add(i, value);
        } else {
            insertarRec(nodo.hijos.get(i), key, value);
        }
    }

    // ====== Recorrido InOrden (debug) ======
    public void imprimir() {
        Nodo n = raiz;
        while (!n.esHoja) n = n.hijos.get(0);

        while (n != null) {
            System.out.print(n.llaves + " ");
            n = n.siguiente;
        }
        System.out.println();
    }
}
