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

    // ====== INSERTAR ======
    public void insertar(K key, V value) {
        Nodo r = raiz;
        if (r.llaves.size() == grado - 1) {
            Nodo s = new Nodo(false);
            raiz = s;
            s.hijos.add(r);
            dividirHijo(s, 0, r);
            insertarNoLleno(s, key, value);
        } else {
            insertarNoLleno(r, key, value);
        }
    }

    private void insertarNoLleno(Nodo nodo, K key, V value) {
        int i = nodo.llaves.size() - 1;

        if (nodo.esHoja) {
            nodo.llaves.add(null);
            nodo.valores.add(null);
            while (i >= 0 && key.compareTo(nodo.llaves.get(i)) < 0) {
                nodo.llaves.set(i + 1, nodo.llaves.get(i));
                nodo.valores.set(i + 1, nodo.valores.get(i));
                i--;
            }
            nodo.llaves.set(i + 1, key);
            nodo.valores.set(i + 1, value);
        } else {
            while (i >= 0 && key.compareTo(nodo.llaves.get(i)) < 0) {
                i--;
            }
            i++;
            Nodo hijo = nodo.hijos.get(i);
            if (hijo.llaves.size() == grado - 1) {
                dividirHijo(nodo, i, hijo);
                if (key.compareTo(nodo.llaves.get(i)) > 0) {
                    i++;
                }
            }
            insertarNoLleno(nodo.hijos.get(i), key, value);
        }
    }

    private void dividirHijo(Nodo padre, int indice, Nodo nodoLleno) {
        int t = (grado - 1) / 2;
        Nodo nuevoNodo = new Nodo(nodoLleno.esHoja);

        // Copiar la mitad superior de llaves
        for (int j = 0; j < t; j++) {
            nuevoNodo.llaves.add(nodoLleno.llaves.remove(t + (nodoLleno.esHoja ? 0 : 1)));
        }

        // Si es hoja, copiar valores
        if (nodoLleno.esHoja) {
            for (int j = 0; j < t; j++) {
                nuevoNodo.valores.add(nodoLleno.valores.remove(t));
            }
            nuevoNodo.siguiente = nodoLleno.siguiente;
            nodoLleno.siguiente = nuevoNodo;
        } else {
            // Si no es hoja, copiar hijos
            for (int j = 0; j < t + 1; j++) {
                nuevoNodo.hijos.add(nodoLleno.hijos.remove(t + 1));
            }
        }

        // Promover la llave del medio al padre
        K llavePromovida = nodoLleno.llaves.remove(t);
        padre.llaves.add(indice, llavePromovida);
        padre.hijos.add(indice + 1, nuevoNodo);
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
