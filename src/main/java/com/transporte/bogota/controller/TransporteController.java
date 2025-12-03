package com.transporte.bogota.controller;

import com.transporte.bogota.model.Estacion;
import com.transporte.bogota.service.TransporteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * Controlador REST para exponer la API de gestión y optimización del sistema de transporte.
 */
@RestController
@RequestMapping("/api/transporte")
public class TransporteController {

    private final TransporteService transporteService;

    public TransporteController(TransporteService transporteService) {
        this.transporteService = transporteService;
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
}