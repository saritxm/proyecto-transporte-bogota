package com.transporte.bogota.model;

import java.util.Objects;

/**
 * Representa una estación del sistema de transporte público.
 * Puede ser de tipo: metro, TransMilenio (tm), SITP o intermodal.
 */
public class Estacion {
    private String id;
    private String nombre;
    private String tipo; // "metro", "tm", "sitp", "intermodal"
    private double latitud;
    private double longitud;
    private int capacidad; // capacidad estimada de pasajeros

    public Estacion() {
    }

    public Estacion(String id, String nombre, String tipo, double latitud, double longitud, int capacidad) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.latitud = latitud;
        this.longitud = longitud;
        this.capacidad = capacidad;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }

    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }

    @Override
    public String toString() {
        return "Estacion{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", tipo='" + tipo + '\'' +
                ", latitud=" + latitud +
                ", longitud=" + longitud +
                ", capacidad=" + capacidad +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Estacion)) return false;
        Estacion estacion = (Estacion) o;
        return Objects.equals(id, estacion.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
