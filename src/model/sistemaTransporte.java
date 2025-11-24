package model;
import java.util.*;

/**
 * SistemaTransporte: almacena estaciones, rutas y líneas.
 * Actualmente usa colecciones en memoria; más adelante se conecta a structures.BPlusTree y model.structures.Graph.
 */
public class SistemaTransporte {

    // Almacenamiento principal: puedes reemplazar Map por BPlusTree en structures cuando lo tengas.
    private final Map<String, Estacion> estacionesById = new HashMap<>();
    private final Map<String, Ruta> rutasById = new HashMap<>();
    private final Map<String, Linea> lineasById = new HashMap<>();

    public SistemaTransporte() {}

    // ------ Estaciones ------
    public void addEstacion(Estacion e) {
        estacionesById.put(e.getId(), e);
    }

    public Estacion getEstacion(String id) {
        return estacionesById.get(id);
    }

    public Collection<Estacion> getAllEstaciones() {
        return Collections.unmodifiableCollection(estacionesById.values());
    }

    public boolean removeEstacion(String id) {
        return estacionesById.remove(id) != null;
    }

    // ------ Rutas ------
    public void addRuta(Ruta r) {
        rutasById.put(r.getId(), r);
    }

    public Ruta getRuta(String id) {
        return rutasById.get(id);
    }

    public Collection<Ruta> getAllRutas() {
        return Collections.unmodifiableCollection(rutasById.values());
    }

    public boolean removeRuta(String id) {
        return rutasById.remove(id) != null;
    }

    // ------ Lineas ------
    public void addLinea(Linea l) {
        lineasById.put(l.getId(), l);
    }

    public Linea getLinea(String id) {
        return lineasById.get(id);
    }

    public Collection<Linea> getAllLineas() {
        return Collections.unmodifiableCollection(lineasById.values());
    }

    // ------ Utilitarios ------
    public List<Ruta> rutasDesdeEstacion(String estacionId) {
        List<Ruta> res = new ArrayList<>();
        for (Ruta r : rutasById.values()) {
            if (r.getOrigen().getId().equals(estacionId)) res.add(r);
        }
        return res;
    }

    public List<Ruta> rutasHaciaEstacion(String estacionId) {
        List<Ruta> res = new ArrayList<>();
        for (Ruta r : rutasById.values()) {
            if (r.getDestino().getId().equals(estacionId)) res.add(r);
        }
        return res;
    }

    // método de ayuda: crea grafo en structures.Graph (stub para cuando lo implementes)
    public void exportToGraph(/*Graph graph*/) {
        // TODO: implementar conversión del modelo a la estructura Graph en model.structures
        // ejemplo: recorrer rutasById y añadir aristas al graph
    }
}
