package com.transporte.bogota.controller;

import com.transporte.bogota.model.Estacion;
import com.transporte.bogota.service.TransporteService;
import com.transporte.bogota.service.CongestionAnalysisService;
import com.transporte.bogota.service.EstacionIndexService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controlador REST para exponer la API de gestión y optimización del sistema de transporte.
 */
@RestController
@RequestMapping("/api/transporte")
public class TransporteController {

    private final TransporteService transporteService;
    private final CongestionAnalysisService congestionService;
    private final EstacionIndexService indexService;

    public TransporteController(TransporteService transporteService,
                                CongestionAnalysisService congestionService,
                                EstacionIndexService indexService) {
        this.transporteService = transporteService;
        this.congestionService = congestionService;
        this.indexService = indexService;
    }

    // =========================================================================
    // ENDPOINTS DE INFORMACIÓN DEL SISTEMA
    // =========================================================================

    /**
     * Obtiene todas las estaciones del sistema.
     * DEPRECATED: Usar /estaciones/principales o /estaciones/buscar para evitar sobrecarga.
     */
    @GetMapping("/estaciones")
    public Collection<Estacion> getEstaciones() {
        return transporteService.getEstaciones();
    }

    /**
     * Obtiene solo estaciones principales (portales, intermodales, metro).
     * Ideal para carga inicial del mapa.
     */
    @GetMapping("/estaciones/principales")
    public ResponseEntity<List<Map<String, Object>>> getEstacionesPrincipales() {
        return ResponseEntity.ok(transporteService.getEstacionesPrincipales());
    }

    /**
     * Búsqueda de estaciones por nombre o ID.
     * http://localhost:8080/api/transporte/estaciones/buscar?q=portal&limit=20
     */
    @GetMapping("/estaciones/buscar")
    public ResponseEntity<List<Map<String, Object>>> buscarEstaciones(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "20") int limit) {

        if (limit > 100) limit = 100; // Máximo 100 resultados

        List<Map<String, Object>> resultados = transporteService.buscarEstaciones(q, limit);
        return ResponseEntity.ok(resultados);
    }

    /**
     * Obtiene estaciones paginadas.
     * http://localhost:8080/api/transporte/estaciones/pagina?page=0&size=50
     */
    @GetMapping("/estaciones/pagina")
    public ResponseEntity<Map<String, Object>> getEstacionesPaginadas(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size) {

        if (size > 200) size = 200; // Máximo 200 por página

        Map<String, Object> resultado = transporteService.getEstacionesPaginadas(page, size);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Obtiene las rutas (aristas) del sistema.
     */
    @GetMapping("/rutas")
    public Collection<Map<String, Object>> getRutas() {
        // Se asume que el servicio devuelve objetos serializables directamente o Map<String, Object>
        return (Collection) transporteService.getRutas();
    }

    /**
     * Obtiene las líneas del sistema.
     */
    @GetMapping("/lineas")
    public Collection<Map<String, Object>> getLineas() {
        return (Collection) transporteService.getLineas();
    }
    
    /**
     * Obtiene estadísticas generales del sistema de transporte.
     */
    @GetMapping("/estadisticas")
    public Map<String, Object> getEstadisticas() {
        return transporteService.getEstadisticas();
    }

    /**
     * Obtiene estadísticas del índice B+ de estaciones SITP.
     * Útil para debugging y verificación del sistema de indexación.
     */
    @GetMapping("/estadisticas/indice")
    public Map<String, Object> getEstadisticasIndice() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEstacionesSITP", indexService.getTotalEstaciones());
        stats.putAll(indexService.getEstadisticas());
        return stats;
    }

    // =========================================================================
    // ENDPOINT DE DIJKSTRA (Ruta Óptima)
    // =========================================================================

    /**
     * Calcula la ruta óptima (tiempo mínimo) entre dos estaciones usando Dijkstra.
     * http://localhost:8080/api/transporte/ruta-optima?origenId=E001&destinoId=E005
     */
    @GetMapping("/ruta-optima")
    public ResponseEntity<?> calcularRutaOptima(
            @RequestParam String origenId, 
            @RequestParam String destinoId) {
        try {
            Map<String, Object> resultado = transporteService.calcularRutaOptima(origenId, destinoId);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error interno al calcular la ruta: " + e.getMessage());
        }
    }

    // =========================================================================
    // ENDPOINT DE RESULTADOS DE ANÁLISIS (Reporte de Optimización)
    // =========================================================================

    /**
     * Obtiene los resultados precalculados de los algoritmos de optimización (ARM, Max Flow, Coloreado).
     * Reemplaza las llamadas obsoletas.
     * http://localhost:8080/api/transporte/analysis
     */
    @GetMapping("/analysis")
    public Map<String, Object> getAnalysisReport() {
        return transporteService.getAnalysisResults();
    }

    // =========================================================================
    // ENDPOINT AUXILIAR
    // =========================================================================

    /**
     * Obtiene las líneas que pasan por una estación.
     */
    @GetMapping("/estaciones/{id}/lineas")
    public ResponseEntity<?> getLineasPorEstacion(@PathVariable String id) {
        try {
            return ResponseEntity.ok(transporteService.getLineasPorEstacion(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // ENDPOINT DE ANÁLISIS DE CONGESTIÓN (HORA PICO)
    // =========================================================================

    /**
     * Analiza la congestión entre dos estaciones durante horas pico.
     * Usa algoritmos de flujo máximo para identificar cuellos de botella.
     * http://localhost:8080/api/transporte/analisis-congestion?origenId=E013&destinoId=TM002
     */
    @GetMapping("/analisis-congestion")
    public ResponseEntity<?> analizarCongestion(
            @RequestParam String origenId,
            @RequestParam String destinoId) {
        try {
            // Obtener estaciones
            Estacion origen = transporteService.getEstacionPorId(origenId);
            Estacion destino = transporteService.getEstacionPorId(destinoId);

            if (origen == null || destino == null) {
                return ResponseEntity.badRequest().body("Estación no encontrada");
            }

            // Realizar análisis de congestión
            // IMPORTANTE: NO usar grafo completo (7849 nodos causa OOM)
            // Usar grafo lazy que solo carga nodos relevantes
            CongestionAnalysisService.AnalisisCongestion analisis =
                congestionService.analizarCongestion(
                    transporteService.construirGrafoLazy(origen, destino),
                    origen,
                    destino
                );

            // Construir respuesta
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("origen", Map.of(
                "id", origen.getId(),
                "nombre", origen.getNombre(),
                "tipo", origen.getTipo()
            ));
            respuesta.put("destino", Map.of(
                "id", destino.getId(),
                "nombre", destino.getNombre(),
                "tipo", destino.getTipo()
            ));
            respuesta.put("flujoNormal", analisis.flujoNormal);
            respuesta.put("flujoHoraPico", analisis.flujoHoraPico);
            respuesta.put("porcentajeReduccion", Math.round(analisis.porcentajeReduccion * 100) / 100.0);
            respuesta.put("nivelCongestion", Map.of(
                "nivel", analisis.nivelCongestion.nombre,
                "color", analisis.nivelCongestion.color
            ));

            // Cuellos de botella
            respuesta.put("cuellosBotella", analisis.cuellosBotella.stream()
                .map(cuello -> Map.of(
                    "origen", Map.of(
                        "id", cuello.origen.getId(),
                        "nombre", cuello.origen.getNombre()
                    ),
                    "destino", Map.of(
                        "id", cuello.destino.getId(),
                        "nombre", cuello.destino.getNombre()
                    ),
                    "capacidadTotal", cuello.capacidadTotal,
                    "flujoActual", cuello.flujoActual,
                    "porcentajeUso", Math.round(cuello.porcentajeUso * 100) / 100.0,
                    "latitudOrigen", cuello.origen.getLatitud(),
                    "longitudOrigen", cuello.origen.getLongitud(),
                    "latitudDestino", cuello.destino.getLatitud(),
                    "longitudDestino", cuello.destino.getLongitud()
                ))
                .toList()
            );

            respuesta.put("recomendaciones", analisis.recomendaciones);

            // Incluir rutas alternativas si están disponibles
            if (analisis.rutasAlternativas != null && !analisis.rutasAlternativas.isEmpty()) {
                respuesta.put("rutasAlternativas", analisis.rutasAlternativas.stream()
                    .limit(3)
                    .map(ruta -> Map.of(
                        "numeroRuta", ruta.numeroRuta,
                        "tiempoTotal", Math.round(ruta.costoTotal * 10) / 10.0,
                        "numeroEstaciones", ruta.getNumeroEstaciones(),
                        "camino", ruta.camino.stream()
                            .map(est -> Map.of(
                                "id", est.getId(),
                                "nombre", est.getNombre(),
                                "tipo", est.getTipo(),
                                "latitud", est.getLatitud(),
                                "longitud", est.getLongitud()
                            ))
                            .toList()
                    ))
                    .toList()
                );
            }

            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error al analizar congestión: " + e.getMessage());
        }
    }

    // =========================================================================
    // ENDPOINT DE RUTAS ALTERNATIVAS CON BELLMAN-FORD
    // =========================================================================

    /**
     * Encuentra rutas alternativas considerando congestión usando Bellman-Ford.
     * Este algoritmo penaliza rutas con baja capacidad y encuentra alternativas óptimas.
     * http://localhost:8080/api/transporte/rutas-alternativas?origenId=E013&destinoId=TM002&numRutas=3
     */
    @GetMapping("/rutas-alternativas")
    public ResponseEntity<?> encontrarRutasAlternativas(
            @RequestParam String origenId,
            @RequestParam String destinoId,
            @RequestParam(required = false, defaultValue = "3") int numRutas) {
        try {
            // Validar número de rutas
            if (numRutas < 1) numRutas = 1;
            if (numRutas > 5) numRutas = 5; // Máximo 5 rutas

            // Obtener estaciones
            Estacion origen = transporteService.getEstacionPorId(origenId);
            Estacion destino = transporteService.getEstacionPorId(destinoId);

            if (origen == null || destino == null) {
                return ResponseEntity.badRequest().body("Estación no encontrada");
            }

            // Analizar rutas alternativas con Bellman-Ford
            // IMPORTANTE: NO usar grafo completo (7849 nodos causa OOM)
            // Usar grafo lazy que solo carga nodos relevantes
            CongestionAnalysisService.AnalisisRutasAlternativas analisis =
                congestionService.analizarRutasAlternativas(
                    transporteService.construirGrafoLazy(origen, destino),
                    origen,
                    destino,
                    numRutas
                );

            // Construir respuesta
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("origen", Map.of(
                "id", origen.getId(),
                "nombre", origen.getNombre(),
                "tipo", origen.getTipo()
            ));
            respuesta.put("destino", Map.of(
                "id", destino.getId(),
                "nombre", destino.getNombre(),
                "tipo", destino.getTipo()
            ));
            respuesta.put("mensaje", analisis.mensaje);
            respuesta.put("tieneCicloNegativo", analisis.tieneCicloNegativo);

            // Si hay ciclo negativo, informarlo
            if (analisis.tieneCicloNegativo && analisis.cicloNegativo != null) {
                respuesta.put("cicloNegativo", analisis.cicloNegativo.stream()
                    .map(est -> Map.of(
                        "id", est.getId(),
                        "nombre", est.getNombre()
                    ))
                    .toList()
                );
            }

            // Rutas encontradas
            respuesta.put("totalRutas", analisis.rutas.size());
            respuesta.put("rutas", analisis.rutas.stream()
                .map(analisisRuta -> {
                    Map<String, Object> rutaMap = new HashMap<>();
                    rutaMap.put("numero", analisisRuta.ruta.numeroRuta);
                    rutaMap.put("tiempoTotal", Math.round(analisisRuta.ruta.costoTotal * 10) / 10.0);
                    rutaMap.put("numeroEstaciones", analisisRuta.ruta.getNumeroEstaciones());
                    rutaMap.put("nivelCongestion", Math.round(analisisRuta.nivelCongestion * 100));
                    rutaMap.put("transferencias", analisisRuta.transferencias);
                    rutaMap.put("puntuacion", Math.round(analisisRuta.puntuacion * 100));
                    rutaMap.put("descripcion", analisisRuta.getDescripcion());
                    rutaMap.put("camino", analisisRuta.ruta.camino.stream()
                        .map(est -> Map.of(
                            "id", est.getId(),
                            "nombre", est.getNombre(),
                            "tipo", est.getTipo(),
                            "latitud", est.getLatitud(),
                            "longitud", est.getLongitud()
                        ))
                        .toList()
                    );
                    return rutaMap;
                })
                .toList()
            );

            // Recomendación de mejor ruta
            if (!analisis.rutas.isEmpty()) {
                CongestionAnalysisService.AnalisisRuta mejorRuta = analisis.rutas.get(0);
                respuesta.put("mejorRuta", Map.of(
                    "numero", mejorRuta.ruta.numeroRuta,
                    "razon", "Mejor balance entre tiempo, congestión y transferencias",
                    "puntuacion", Math.round(mejorRuta.puntuacion * 100)
                ));
            }

            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body("Error al encontrar rutas alternativas: " + e.getMessage());
        }
    }
}