package com.transporte.bogota.service;

import com.transporte.bogota.dao.CSVDataLoader;
import com.transporte.bogota.model.*;
import com.transporte.bogota.util.Graph;
import com.transporte.bogota.util.GraphEdge;
import com.transporte.bogota.algorithm.Dijkstra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio principal para gestión y optimización del sistema de transporte.
 */
@Service
public class TransporteService {

    private static final Logger logger = LoggerFactory.getLogger(TransporteService.class);

    private final CSVDataLoader dataLoader;
    private SistemaTransporte sistema;
    private Graph grafo;

    public TransporteService(CSVDataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    @PostConstruct
    public void init() {
        try {
            logger.info("Inicializando servicio de transporte...");
            this.sistema = dataLoader.cargarDatos();
            this.grafo = construirGrafo();
            logger.info("Servicio inicializado correctamente");
        } catch (IOException e) {
            logger.error("Error al cargar datos del sistema", e);
            throw new RuntimeException("Error al inicializar sistema de transporte", e);
        }
    }

    private Graph construirGrafo() {
        Graph g = new Graph();

        // Agregar todas las estaciones como nodos
        for (Estacion e : sistema.getAllEstaciones()) {
            g.addNodo(e);
        }

        // Agregar todas las rutas como aristas
        for (Ruta r : sistema.getAllRutas()) {
            g.addArista(r.getOrigen(), r.getDestino(), r.getTiempoViaje(), r.getCapacidad());
        }

        logger.info("Grafo construido - Nodos: {}, Aristas: {}",
                g.getNodos().size(),
                g.getNodos().stream().mapToInt(n -> g.getVecinos(n).size()).sum());

        return g;
    }

    public Collection<Estacion> getEstaciones() {
        return sistema.getAllEstaciones();
    }

    public Collection<Ruta> getRutas() {
        return sistema.getAllRutas();
    }

    public Collection<Linea> getLineas() {
        return sistema.getAllLineas();
    }

    public Estacion getEstacion(String id) {
        return sistema.getEstacion(id);
    }

    /**
     * Calcula la ruta más corta entre dos estaciones usando Dijkstra
     */
    public Map<String, Object> calcularRutaOptima(String origenId, String destinoId) {
        Estacion origen = sistema.getEstacion(origenId);
        Estacion destino = sistema.getEstacion(destinoId);

        if (origen == null || destino == null) {
            throw new IllegalArgumentException("Estación no encontrada");
        }

        Dijkstra.ResultadoDijkstra resultado = Dijkstra.calcularCaminoMinimo(grafo, origen, destino);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("origen", crearEstacionDTO(origen));
        respuesta.put("destino", crearEstacionDTO(destino));
        respuesta.put("tiempoTotal", resultado.distancia);
        respuesta.put("camino", resultado.camino.stream()
                .map(this::crearEstacionDTO)
                .collect(Collectors.toList()));
        respuesta.put("numeroEstaciones", resultado.camino.size());

        return respuesta;
    }

    /**
     * Obtiene estadísticas generales del sistema
     */
    public Map<String, Object> getEstadisticas() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalEstaciones", sistema.getAllEstaciones().size());
        stats.put("totalRutas", sistema.getAllRutas().size());
        stats.put("totalLineas", sistema.getAllLineas().size());

        // Estaciones por tipo
        Map<String, Long> porTipo = sistema.getAllEstaciones().stream()
                .collect(Collectors.groupingBy(Estacion::getTipo, Collectors.counting()));
        stats.put("estacionesPorTipo", porTipo);

        // Capacidad total
        int capacidadTotal = sistema.getAllEstaciones().stream()
                .mapToInt(Estacion::getCapacidad)
                .sum();
        stats.put("capacidadTotal", capacidadTotal);

        return stats;
    }

    /**
     * Obtiene las líneas que conectan con una estación específica
     */
    public List<Map<String, Object>> getLineasPorEstacion(String estacionId) {
        Estacion estacion = sistema.getEstacion(estacionId);
        if (estacion == null) {
            return Collections.emptyList();
        }

        return sistema.getAllLineas().stream()
                .filter(linea -> linea.getEstaciones().contains(estacion))
                .map(this::crearLineaDTO)
                .collect(Collectors.toList());
    }

    private Map<String, Object> crearEstacionDTO(Estacion e) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", e.getId());
        dto.put("nombre", e.getNombre());
        dto.put("tipo", e.getTipo());
        dto.put("latitud", e.getLatitud());
        dto.put("longitud", e.getLongitud());
        dto.put("capacidad", e.getCapacidad());
        return dto;
    }

    private Map<String, Object> crearLineaDTO(Linea l) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", l.getId());
        dto.put("nombre", l.getNombre());
        dto.put("tipo", l.getTipo());
        dto.put("numeroEstaciones", l.getEstaciones().size());
        return dto;
    }

    public Graph getGrafo() {
        return grafo;
    }

    public SistemaTransporte getSistema() {
        return sistema;
    }
}
