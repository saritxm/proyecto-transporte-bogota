package com.transporte.bogota.controller;

import com.transporte.bogota.model.Estacion;
import com.transporte.bogota.model.Linea;
import com.transporte.bogota.model.Ruta;
import com.transporte.bogota.service.TransporteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para el sistema de transporte.
 * Expone endpoints para consultar estaciones, rutas y calcular caminos óptimos.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TransporteController {

    private final TransporteService transporteService;

    public TransporteController(TransporteService transporteService) {
        this.transporteService = transporteService;
    }

    /**
     * GET /api/estaciones - Obtiene todas las estaciones
     */
    @GetMapping("/estaciones")
    public ResponseEntity<Collection<Estacion>> getEstaciones() {
        return ResponseEntity.ok(transporteService.getEstaciones());
    }

    /**
     * GET /api/estaciones/{id} - Obtiene una estación por ID
     */
    @GetMapping("/estaciones/{id}")
    public ResponseEntity<Estacion> getEstacion(@PathVariable String id) {
        Estacion estacion = transporteService.getEstacion(id);
        if (estacion == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(estacion);
    }

    /**
     * GET /api/rutas - Obtiene todas las rutas
     */
    @GetMapping("/rutas")
    public ResponseEntity<Collection<Ruta>> getRutas() {
        return ResponseEntity.ok(transporteService.getRutas());
    }

    /**
     * GET /api/lineas - Obtiene todas las líneas
     */
    @GetMapping("/lineas")
    public ResponseEntity<Collection<Linea>> getLineas() {
        return ResponseEntity.ok(transporteService.getLineas());
    }

    /**
     * GET /api/lineas/estacion/{id} - Obtiene líneas que pasan por una estación
     */
    @GetMapping("/lineas/estacion/{id}")
    public ResponseEntity<List<Map<String, Object>>> getLineasPorEstacion(@PathVariable String id) {
        return ResponseEntity.ok(transporteService.getLineasPorEstacion(id));
    }

    /**
     * GET /api/ruta-optima?origen={id}&destino={id} - Calcula la ruta más corta
     */
    @GetMapping("/ruta-optima")
    public ResponseEntity<Map<String, Object>> calcularRutaOptima(
            @RequestParam String origen,
            @RequestParam String destino) {
        try {
            Map<String, Object> resultado = transporteService.calcularRutaOptima(origen, destino);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/estadisticas - Obtiene estadísticas del sistema
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> getEstadisticas() {
        return ResponseEntity.ok(transporteService.getEstadisticas());
    }

    /**
     * GET /api/health - Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Sistema de Transporte Bogotá"
        ));
    }
}
