package model;

public class Estacion {
    private final String id;
    private String nombre;
    private String tipo; // "metro", "tm", "sitp", "intermodal"
    private double lat;
    private double lon;
    private int capacidad; // capacidad estimada de la estación

    public Estacion(String id, String nombre, String tipo, double lat, double lon, int capacidad) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.lat = lat;
        this.lon = lon;
        this.capacidad = capacidad;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }
    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }

    @Override
    public String toString() {
        return "Estacion{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", tipo='" + tipo + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
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
