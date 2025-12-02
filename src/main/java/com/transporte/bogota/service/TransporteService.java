package com.transporte.bogota.service;

import com.transporte.bogota.dao.CSVDataLoader;
import com.transporte.bogota.model.*;
import com.transporte.bogota.util.Graph;
import com.transporte.bogota.util.GraphEdge;

// Importación de los algoritmos de optimización
import com.transporte.bogota.algorithm.Dijkstra;
import com.transporte.bogota.algorithm.MaxFlow;
import com.transporte.bogota.algorithm.MinimumSpanningTree;
import com.transporte.bogota.algorithm.GraphColoring; 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio principal para gestión y optimización del sistema de transporte.
 * Incluye la carga de datos, la construcción del grafo y la implementación
 * de algoritmos de optimización (Dijkstra, Max Flow, ARM, Coloreado).
 */
@Service
public class TransporteService {

    private static final Logger logger = LoggerFactory.getLogger(TransporteService.class);

    private final CSVDataLoader dataLoader;
    private SistemaTransporte sistema;
    private Graph grafo;
    private Map<String, Object> analysisResults; // Campo para almacenar resultados de análisis

    public TransporteService(CSVDataLoader dataLoader) {
        this.dataLoader = dataLoader;
        this.analysisResults = new HashMap<>();
    }

    @PostConstruct
    public void init() {
        try {
            logger.info("Inicializando servicio de transporte...");
            this.sistema = dataLoader.cargarDatos();
            this.grafo = construirGrafo();
            calcularResultadosDeAnalisis(); // Calcular análisis al inicio
            logger.info("Servicio inicializado correctamente");
        } catch (IOException e) {
            logger.error("Error al cargar datos del sistema", e);
            throw new RuntimeException("Error al inicializar sistema de transporte", e);
        }
    }
    
    /**
     * Calcula y almacena los resultados de los algoritmos de ARM, Max Flow y Coloreado
     * para ser usados como reporte de análisis.
     */
    private void calcularResultadosDeAnalisis() {
        logger.info("Calculando resultados de análisis (ARM, MaxFlow, GraphColoring)...");
        
        // 1. Árbol de Recubrimiento Mínimo (ARM)
        try {
            List<GraphEdge> arm = MinimumSpanningTree.calcularARM(grafo);
            double costoTotalArm = arm.stream().mapToDouble(GraphEdge::getTiempo).sum();
            analysisResults.put("arm", List.of(
                Map.of("key", "conexiones", "value", arm.size()),
                Map.of("key", "costoTotal", "value", String.format("%.2f min", costoTotalArm))
            ));
        } catch (Exception e) {
            logger.error("Error al calcular ARM", e);
            analysisResults.put("arm", List.of(Map.of("key", "error", "value", "Error de cálculo: " + e.getMessage())));
        }


        // 2. Coloreado de Grafos
        try {
            Map<Estacion, Integer> colores = GraphColoring.colorearGrafo(grafo);
            int coloresUsados = (int) colores.values().stream().distinct().count();
            analysisResults.put("coloring", List.of(
                Map.of("key", "recursosMinimos", "value", coloresUsados),
                Map.of("key", "interpretacion", "value", String.format("Mínimo de %d recursos/franjas para evitar conflictos.", coloresUsados))
            ));
        } catch (Exception e) {
            logger.error("Error al calcular Coloreado de Grafos", e);
            analysisResults.put("coloring", List.of(Map.of("key", "error", "value", "Error de cálculo: " + e.getMessage())));
        }

        // 3. Flujo Máximo (Simulación general)
        // Usaremos dos estaciones de ejemplo para simular un análisis de congestión
        // Nota: Asume que E001 y E002 son IDs válidos. Si no lo son, se debe ajustar la lógica.
        Estacion origenCongestion = sistema.getEstacion("E001"); 
        Estacion destinoCongestion = sistema.getEstacion("E002");
        
        // Si no se encuentran E001 o E002, usa las dos primeras estaciones disponibles para el análisis
        if (origenCongestion == null || destinoCongestion == null) {
            List<Estacion> todasEstaciones = new ArrayList<>(sistema.getAllEstaciones());
            if (todasEstaciones.size() >= 2) {
                origenCongestion = todasEstaciones.get(0);
                destinoCongestion = todasEstaciones.get(1);
            }
        }
        
        if (origenCongestion != null && destinoCongestion != null && !origenCongestion.equals(destinoCongestion)) {
            try {
                int flujoMaximo = MaxFlow.calcularFlujoMaximo(grafo, origenCongestion, destinoCongestion);
                analysisResults.put("maxFlow", List.of(
                    Map.of("key", "flujoMaximo", "value", flujoMaximo),
                    Map.of("key", "rutaAnalizada", "value", origenCongestion.getNombre() + " a " + destinoCongestion.getNombre()),
                    Map.of("key", "recomendacion", "value", flujoMaximo > 5000 ? "Capacidad Alta" : "Posible Cuello de Botella")
                ));
            } catch (Exception e) {
                logger.error("Error al calcular Flujo Máximo", e);
                analysisResults.put("maxFlow", List.of(Map.of("key", "error", "value", "Error de cálculo: " + e.getMessage())));
            }
        } else {
             analysisResults.put("maxFlow", List.of(Map.of("key", "error", "value", "No hay suficientes estaciones (o estaciones válidas) para simular Flujo Máximo.")));
        }

        logger.info("Resultados de análisis calculados y almacenados.");
    }

    private Graph construirGrafo() {
        // ... (código para construirGrafo es el mismo)
        Graph g = new Graph();

        // Agregar todas las estaciones como nodos
        for (Estacion e : sistema.getAllEstaciones()) {
            g.addNodo(e);
        }

        // Agregar todas las rutas como aristas
        for (Ruta r : sistema.getAllRutas()) {
            // Se asume que getTiempoViaje() es el peso (Dijkstra)
            // y getCapacidad() es la capacidad (Max Flow)
            g.addArista(r.getOrigen(), r.getDestino(), r.getTiempoViaje(), r.getCapacidad());
        }

        logger.info("Grafo construido - Nodos: {}, Aristas: {}",
                g.getNodos().size(),
                g.getNodos().stream().mapToInt(n -> g.getVecinos(n).size()).sum());

        return g;
    }

    // =========================================================================
    // 1. ALGORITMO: DIJKSTRA (Ruta Óptima) - Única función interactiva de cálculo
    // =========================================================================

    /**
     * Calcula la ruta más corta (tiempo mínimo) entre dos estaciones usando Dijkstra.
     */
    public Map<String, Object> calcularRutaOptima(String origenId, String destinoId) {
        Estacion origen = sistema.getEstacion(origenId);
        Estacion destino = sistema.getEstacion(destinoId);

        if (origen == null || destino == null) {
            throw new IllegalArgumentException("Estación no encontrada");
        }

        // Llamada al algoritmo Dijkstra
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

    // =========================================================================
    // GETTER PARA RESULTADOS DE ANÁLISIS (Reporte)
    // =========================================================================

    /**
     * Obtiene los resultados de los análisis de optimización precalculados.
     */
    public Map<String, Object> getAnalysisResults() {
        return analysisResults;
    }


    // =========================================================================
    // MÉTODOS AUXILIARES Y GETTERS (se mantienen)
    // =========================================================================

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