package com.transporte.bogota.controller;

import com.transporte.bogota.model.Estacion;
import com.transporte.bogota.service.TransporteService;
import com.transporte.bogota.service.CongestionAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

/**
 * Controlador REST para exponer la API de gestión y optimización del sistema de transporte.
 */
@RestController
@RequestMapping("/api/transporte")
public class TransporteController {

    private final TransporteService transporteService;
    private final CongestionAnalysisService congestionService;

    public TransporteController(TransporteService transporteService, CongestionAnalysisService congestionService) {
        this.transporteService = transporteService;
        this.congestionService = congestionService;
    }

    // =========================================================================
    // ENDPOINTS DE INFORMACIÓN DEL SISTEMA
    // =========================================================================

    /**
     * Obtiene todas las estaciones del sistema.
     */
    @GetMapping("/estaciones")
    public Collection<Estacion> getEstaciones() {
        return transporteService.getEstaciones();
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
            CongestionAnalysisService.AnalisisCongestion analisis =
                congestionService.analizarCongestion(
                    transporteService.getGrafo(),
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
                "nivel", analisis.nivel.nombre,
                "color", analisis.nivel.color
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

            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error al analizar congestión: " + e.getMessage());
        }
    }
}