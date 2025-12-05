package com.transporte.bogota.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/**
 * Servicio para indexar rutas usando HashMap.
 * Permite búsqueda O(1) de rutas por estación origen.
 * Cada estación puede tener múltiples rutas asociadas.
 */
@Service
public class RutaIndexService {

    private static final Logger logger = LoggerFactory.getLogger(RutaIndexService.class);
    private static final String RUTAS_FILE = "data/rutas_generadas.csv";

    // Índice: estación_origen -> lista de rutas que salen de esa estación
    private Map<String, List<Map<String, Object>>> indicePorOrigen;

    // Índice: estación_destino -> lista de rutas que llegan a esa estación
    private Map<String, List<Map<String, Object>>> indicePorDestino;

    @PostConstruct
    public void init() {
        logger.info("Inicializando índice de rutas con HashMap...");
        long startTime = System.currentTimeMillis();

        indicePorOrigen = new HashMap<>();
        indicePorDestino = new HashMap<>();

        cargarIndices();

        long endTime = System.currentTimeMillis();
        logger.info("Índice de rutas inicializado en {} ms - Orígenes: {}, Destinos: {}",
                   endTime - startTime, indicePorOrigen.size(), indicePorDestino.size());
    }

    private void cargarIndices() {
        int rutasCargadas = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(RUTAS_FILE))) {
            String line = br.readLine(); // Skip header

            while ((line = br.readLine()) != null) {
                String[] campos = line.split(",");

                if (campos.length >= 6) {
                    String id = campos[0];
                    String origenId = campos[1];
                    String destinoId = campos[2];
                    int tiempoViaje = Integer.parseInt(campos[3]);
                    int capacidad = Integer.parseInt(campos[4]);
                    double distancia = Double.parseDouble(campos[5]);

                    Map<String, Object> rutaData = new HashMap<>();
                    rutaData.put("id", id);
                    rutaData.put("origen", origenId);
                    rutaData.put("destino", destinoId);
                    rutaData.put("tiempoViaje", tiempoViaje);
                    rutaData.put("capacidad", capacidad);
                    rutaData.put("distancia", distancia);

                    // Indexar por origen (permitir múltiples rutas)
                    indicePorOrigen.computeIfAbsent(origenId, k -> new ArrayList<>()).add(rutaData);

                    // Indexar por destino (permitir múltiples rutas)
                    indicePorDestino.computeIfAbsent(destinoId, k -> new ArrayList<>()).add(rutaData);

                    rutasCargadas++;
                }
            }

            logger.info("Rutas indexadas: {}", rutasCargadas);

        } catch (Exception e) {
            logger.error("Error al cargar índice de rutas: {}", e.getMessage());
        }
    }

    /**
     * Obtiene todas las rutas que salen de una estación.
     */
    public List<Map<String, Object>> getRutasPorOrigen(String estacionId) {
        return indicePorOrigen.getOrDefault(estacionId, Collections.emptyList());
    }

    /**
     * Obtiene todas las rutas que llegan a una estación.
     */
    public List<Map<String, Object>> getRutasPorDestino(String estacionId) {
        return indicePorDestino.getOrDefault(estacionId, Collections.emptyList());
    }

    /**
     * Obtiene todas las rutas relacionadas con una estación (origen o destino).
     */
    public List<Map<String, Object>> getRutasPorEstacion(String estacionId) {
        List<Map<String, Object>> rutas = new ArrayList<>();
        rutas.addAll(getRutasPorOrigen(estacionId));
        rutas.addAll(getRutasPorDestino(estacionId));
        return rutas;
    }

    /**
     * Obtiene rutas para un conjunto de estaciones.
     * Útil para el grafo lazy: solo carga rutas entre estaciones relevantes.
     */
    public List<Map<String, Object>> getRutasParaEstaciones(Set<String> estacionIds) {
        Set<String> rutasUnicas = new HashSet<>();
        List<Map<String, Object>> rutas = new ArrayList<>();

        for (String estacionId : estacionIds) {
            List<Map<String, Object>> rutasOrigen = getRutasPorOrigen(estacionId);

            for (Map<String, Object> ruta : rutasOrigen) {
                String rutaId = (String) ruta.get("id");
                String destinoId = (String) ruta.get("destino");

                // Solo incluir si el destino también está en el conjunto de estaciones relevantes
                if (estacionIds.contains(destinoId) && !rutasUnicas.contains(rutaId)) {
                    rutas.add(ruta);
                    rutasUnicas.add(rutaId);
                }
            }
        }

        return rutas;
    }
}
