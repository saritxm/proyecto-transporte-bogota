package com.transporte.bogota.util;

import com.transporte.bogota.model.Estacion;

/**
 * Representa una arista del grafo (conexión entre dos estaciones).
 */
public class GraphEdge {
    private final Estacion origen;
    private final Estacion destino;
    private final int tiempo;    // peso principal (minutos)
    private final int capacidad; // pasajeros posibles en ese tramo

    public GraphEdge(Estacion origen, Estacion destino, int tiempo, int capacidad) {
        this.origen = origen;
        this.destino = destino;
        this.tiempo = tiempo;
        this.capacidad = capacidad;
    }

    public Estacion getOrigen() { return origen; }
    public Estacion getDestino() { return destino; }
    public int getTiempo() { return tiempo; }
    public int getCapacidad() { return capacidad; }

    @Override
    public String toString() {
        return origen.getId() + " -> " + destino.getId() +
                " (tiempo=" + tiempo + ", cap=" + capacidad + ")";
    }
}
