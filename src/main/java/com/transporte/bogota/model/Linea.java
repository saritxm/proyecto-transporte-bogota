package com.transporte.bogota.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa una línea de transporte que agrupa múltiples estaciones.
 * Puede ser Metro, TransMilenio o SITP.
 */
public class Linea {
    private String id;
    private String nombre;
    private String tipo;
    private List<Estacion> estaciones = new ArrayList<>();

    public Linea() {
    }

    public Linea(String id, String nombre, String tipo) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public void addEstacion(Estacion e) { estaciones.add(e); }
    public void removeEstacion(Estacion e) { estaciones.remove(e); }
    public List<Estacion> getEstaciones() { return Collections.unmodifiableList(estaciones); }

    @Override
    public String toString() {
        return "Linea{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", tipo='" + tipo + '\'' +
                ", estaciones=" + estaciones.size() +
                '}';
    }
}
