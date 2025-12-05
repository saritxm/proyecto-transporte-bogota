package com.transporte.bogota.model;

import java.util.*;
import org.springframework.stereotype.Component;

/**
 * SistemaTransporte: almacena estaciones, rutas y líneas.
 * Gestor central del sistema de transporte público.
 */
@Component
public class SistemaTransporte {

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
}
