package model;

public class Ruta {
    private final String id;
    private final Estacion origen;
    private final Estacion destino;
    private int tiempoViaje;    // en minutos
    private int capacidad;      // capacidad del vehículo/tramo (pasajeros por intervalo)
    private double distanciaM;  // distancia en metros (opcional)

    public Ruta(String id, Estacion origen, Estacion destino, int tiempoViaje, int capacidad, double distanciaM) {
        this.id = id;
        this.origen = origen;
        this.destino = destino;
        this.tiempoViaje = tiempoViaje;
        this.capacidad = capacidad;
        this.distanciaM = distanciaM;
    }

    public String getId() { return id; }
    public Estacion getOrigen() { return origen; }
    public Estacion getDestino() { return destino; }
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
                ", origen=" + origen.getId() +
                ", destino=" + destino.getId() +
                ", tiempoViaje=" + tiempoViaje +
                ", capacidad=" + capacidad +
                ", distanciaM=" + distanciaM +
                '}';
    }
}
