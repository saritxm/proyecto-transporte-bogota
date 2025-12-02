package com.transporte.bogota.model;

/**
 * Representa una conexión directa entre dos estaciones.
 * Incluye tiempo de viaje, capacidad y distancia.
 */
public class Ruta {
    private String id;
    private Estacion origen;
    private Estacion destino;
    private int tiempoViaje;    // en minutos
    private int capacidad;      // capacidad del vehículo/tramo (pasajeros por intervalo)
    private double distanciaM;  // distancia en metros
    private List<Map<String, Double>> polyline;
    public Ruta() {
    }

    public Ruta(String id, Estacion origen, Estacion destino, int tiempoViaje, int capacidad, double distanciaM) {
        this.id = id;
        this.origen = origen;
        this.destino = destino;
        this.tiempoViaje = tiempoViaje;
        this.capacidad = capacidad;
        this.distanciaM = distanciaM;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Estacion getOrigen() { return origen; }
    public void setOrigen(Estacion origen) { this.origen = origen; }

    public Estacion getDestino() { return destino; }
    public void setDestino(Estacion destino) { this.destino = destino; }

    public int getTiempoViaje() { return tiempoViaje; }
    public void setTiempoViaje(int tiempoViaje) { this.tiempoViaje = tiempoViaje; }

    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }

    public double getDistanciaM() { return distanciaM; }
    public void setDistanciaM(double distanciaM) { this.distanciaM = distanciaM; }

    @Override
    public String toString() {
        return "Ruta{" +
                "id='" + id + '\'' +
                ", origen=" + (origen != null ? origen.getId() : "null") +
                ", destino=" + (destino != null ? destino.getId() : "null") +
                ", tiempoViaje=" + tiempoViaje +
                ", capacidad=" + capacidad +
                ", distanciaM=" + distanciaM +
                '}';
    }
}
