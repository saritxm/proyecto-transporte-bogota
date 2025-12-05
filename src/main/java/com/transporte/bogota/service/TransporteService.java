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
    private final EstacionIndexService indexService;
    private final RutaIndexService rutaIndexService;
    private final SistemaTransporte sistema;
    private LazyGraphService lazyGraphService;
    private Graph grafoCompleto; // Solo para análisis globales
    private Map<String, Object> analysisResults; // Campo para almacenar resultados de análisis

    public TransporteService(CSVDataLoader dataLoader, EstacionIndexService indexService,
                            RutaIndexService rutaIndexService, SistemaTransporte sistema) {
        this.dataLoader = dataLoader;
        this.indexService = indexService;
        this.rutaIndexService = rutaIndexService;
        this.sistema = sistema;
        this.analysisResults = new HashMap<>();
    }

    @PostConstruct
    public void init() {
        try {
            logger.info("Inicializando servicio de transporte...");
            dataLoader.cargarDatos();

            // Crear LazyGraphService después de cargar el sistema
            this.lazyGraphService = new LazyGraphService(sistema, rutaIndexService);

            // CAMBIO: Ya NO construimos el grafo completo al inicio
            // Solo lo construimos cuando se necesite para análisis globales
            logger.info("Sistema inicializado - Estaciones: {}", sistema.getAllEstaciones().size());
            logger.info("Grafo lazy activado - Se construirá bajo demanda");

            // Calcular análisis solo si se necesita el grafo completo
            // calcularResultadosDeAnalisis(); // Comentado por ahora

            logger.info("Servicio inicializado correctamente (modo lazy)");
        } catch (IOException e) {
            logger.error("Error al cargar datos del sistema", e);
            throw new RuntimeException("Error al inicializar sistema de transporte", e);
        }
    }
    
    /**
     * Calcula y almacena los resultados de los algoritmos de ARM, Max Flow y Coloreado
     * para ser usados como reporte de análisis.
     * NOTA: Este método construye el grafo completo, lo cual es costoso.
     */
    private void calcularResultadosDeAnalisis() {
        logger.info("Calculando resultados de análisis (ARM, MaxFlow, GraphColoring)...");
        logger.warn("Construyendo grafo completo para análisis globales...");

        // Construir grafo completo solo para análisis
        if (grafoCompleto == null) {
            grafoCompleto = lazyGraphService.construirGrafoCompleto();
        }

        // 1. Árbol de Recubrimiento Mínimo (ARM)
        try {
            // Nota: Se usa el atributo tiempoViaje (double) de Ruta como el peso para el ARM
            List<GraphEdge> arm = MinimumSpanningTree.calcularARM(grafoCompleto);
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
            Map<Estacion, Integer> colores = GraphColoring.colorearGrafo(grafoCompleto);
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
                int flujoMaximo = MaxFlow.calcularFlujoMaximo(grafoCompleto, origenCongestion, destinoCongestion);
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

    // NOTA: construirGrafo() ya no se usa.
    // Ahora usamos LazyGraphService para construir grafos bajo demanda.

    // =========================================================================
    // 1. ALGORITMO: DIJKSTRA (Ruta Óptima) - Única función interactiva de cálculo
    // =========================================================================

    /**
     * Calcula la ruta más corta (tiempo mínimo) entre dos estaciones usando Dijkstra.
     * Usa lazy loading: construye un grafo solo con rutas relevantes.
     */
    public Map<String, Object> calcularRutaOptima(String origenId, String destinoId) {
        Estacion origen = sistema.getEstacion(origenId);
        Estacion destino = sistema.getEstacion(destinoId);

        if (origen == null || destino == null) {
            throw new IllegalArgumentException("Estación no encontrada");
        }

        // Construir grafo lazy solo con rutas relevantes
        logger.info("Calculando ruta óptima: {} -> {}", origen.getNombre(), destino.getNombre());
        Graph grafoLazy = lazyGraphService.construirGrafoLazy(origen, destino);

        // Llamada al algoritmo Dijkstra con el grafo lazy
        Dijkstra.ResultadoDijkstra resultado = Dijkstra.calcularCaminoMinimo(grafoLazy, origen, destino);

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

    /**
     * Búsqueda de estaciones por nombre usando índice B+ para búsqueda O(log n).
     * Busca primero en estaciones cargadas (TM, Metro, Portales), luego en índice SITP.
     */
    public List<Map<String, Object>> buscarEstaciones(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String queryLower = query.toLowerCase().trim();

        // 1. Buscar en estaciones principales ya cargadas (TM, Metro, Portales)
        List<Map<String, Object>> resultados = sistema.getAllEstaciones().stream()
                .filter(e -> e.getNombre().toLowerCase().contains(queryLower) ||
                             e.getId().toLowerCase().contains(queryLower))
                .limit(limit)
                .map(this::crearEstacionDTO)
                .collect(Collectors.toList());

        logger.debug("Búsqueda '{}': {} resultados en estaciones principales", query, resultados.size());

        // 2. Si no hay suficientes resultados, buscar en índice SITP (B+ tree)
        if (resultados.size() < limit) {
            try {
                int restantes = limit - resultados.size();
                List<Map<String, Object>> sitpResults = indexService.buscar(queryLower, restantes);
                resultados.addAll(sitpResults);
                logger.debug("Búsqueda '{}': {} resultados adicionales en SITP", query, sitpResults.size());
            } catch (Exception e) {
                logger.warn("Error al buscar en índice SITP: {}", e.getMessage());
            }
        }

        logger.info("Búsqueda '{}': {} resultados totales", query, resultados.size());
        return resultados;
    }

    /**
     * Obtiene estaciones paginadas (evita cargar todas en memoria del cliente).
     */
    public Map<String, Object> getEstacionesPaginadas(int page, int size) {
        List<Estacion> todasEstaciones = new ArrayList<>(sistema.getAllEstaciones());
        int total = todasEstaciones.size();
        int inicio = page * size;
        int fin = Math.min(inicio + size, total);

        List<Map<String, Object>> estaciones = todasEstaciones.subList(inicio, fin).stream()
                .map(this::crearEstacionDTO)
                .collect(Collectors.toList());

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("estaciones", estaciones);
        resultado.put("total", total);
        resultado.put("page", page);
        resultado.put("size", size);
        resultado.put("totalPages", (int) Math.ceil((double) total / size));

        return resultado;
    }

    /**
     * Obtiene solo estaciones principales (portales, intermodales, metro) para carga inicial.
     */
    public List<Map<String, Object>> getEstacionesPrincipales() {
        return sistema.getAllEstaciones().stream()
                .filter(e -> "portal".equals(e.getTipo()) ||
                             "intermodal".equals(e.getTipo()) ||
                             "metro".equals(e.getTipo()))
                .map(this::crearEstacionDTO)
                .collect(Collectors.toList());
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

    public Estacion getEstacionPorId(String id) {
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

    public Graph getGrafoCompleto() {
        // Construir grafo completo bajo demanda si no existe
        if (grafoCompleto == null) {
            grafoCompleto = lazyGraphService.construirGrafoCompleto();
        }
        return grafoCompleto;
    }

    public SistemaTransporte getSistema() {
        return sistema;
    }
}