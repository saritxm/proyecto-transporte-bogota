package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Linea {
    private final String id;
    private final String nombre;
    private final String tipo;
    private final List<Estacion> estaciones = new ArrayList<>();

    public Linea(String id, String nombre, String tipo) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }

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
