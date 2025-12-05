package com.transporte.bogota.service;

import com.transporte.bogota.util.BPlusTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/**
 * Servicio de indexación de estaciones usando árboles B+.
 * Carga estaciones SITP en memoria indexadas para búsquedas O(log n).
 */
@Service
public class EstacionIndexService {

    private static final Logger logger = LoggerFactory.getLogger(EstacionIndexService.class);
    private static final String SITP_FILE = "data/estaciones_sitp.csv";

    // Índices B+ para búsqueda eficiente
    private BPlusTree<String, Map<String, Object>> indiceNombre;
    private BPlusTree<String, Map<String, Object>> indiceId;

    // Estadísticas
    private int totalEstacionesIndexadas = 0;

    @PostConstruct
    public void init() {
        try {
            logger.info("Iniciando indexación de estaciones SITP...");
            long startTime = System.currentTimeMillis();

            indiceNombre = new BPlusTree<>();
            indiceId = new BPlusTree<>();

            cargarIndices();

            long endTime = System.currentTimeMillis();
            logger.info("Indexación completada en {} ms", endTime - startTime);
            logger.info("Total de estaciones indexadas: {}", totalEstacionesIndexadas);
            logger.info("Índice por nombre: {}", indiceNombre.getStats());
            logger.info("Índice por ID: {}", indiceId.getStats());

        } catch (Exception e) {
            logger.error("Error al indexar estaciones SITP", e);
        }
    }

    private void cargarIndices() {
        try (BufferedReader reader = new BufferedReader(new FileReader(SITP_FILE))) {
            String line;
            reader.readLine(); // Saltar encabezado

            while ((line = reader.readLine()) != null) {
                String[] campos = line.split(",");
                if (campos.length >= 6) {
                    String id = campos[0].trim();
                    String nombre = campos[1].trim();
                    String tipo = campos[2].trim();
                    double latitud = Double.parseDouble(campos[3].trim());
                    double longitud = Double.parseDouble(campos[4].trim());
                    int capacidad = Integer.parseInt(campos[5].trim());

                    Map<String, Object> estacion = new HashMap<>();
                    estacion.put("id", id);
                    estacion.put("nombre", nombre);
                    estacion.put("tipo", tipo);
                    estacion.put("latitud", latitud);
                    estacion.put("longitud", longitud);
                    estacion.put("capacidad", capacidad);

                    // Indexar por nombre (normalizado para búsqueda)
                    String nombreNormalizado = nombre.toLowerCase();
                    indiceNombre.insert(nombreNormalizado, estacion);

                    // Indexar por ID
                    indiceId.insert(id.toLowerCase(), estacion);

                    totalEstacionesIndexadas++;
                }
            }
        } catch (Exception e) {
            logger.error("Error al cargar índices desde archivo SITP", e);
            throw new RuntimeException("No se pudieron cargar los índices de estaciones SITP", e);
        }
    }

    /**
     * Busca estaciones por nombre (búsqueda por contención).
     * Utiliza el índice B+ para búsqueda eficiente.
     *
     * @param query Texto a buscar
     * @param limit Límite de resultados
     * @return Lista de estaciones que coinciden
     */
    public List<Map<String, Object>> buscarPorNombre(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String queryLower = query.toLowerCase().trim();

        // Primero buscar por prefijo (más eficiente)
        List<Map<String, Object>> resultados = indiceNombre.searchByPrefix(queryLower, limit);

        // Si no hay suficientes resultados, buscar por contención
        if (resultados.size() < limit) {
            Set<String> idsYaIncluidos = new HashSet<>();
            resultados.forEach(e -> idsYaIncluidos.add((String) e.get("id")));

            List<Map<String, Object>> adicionales = indiceNombre.searchByContains(queryLower, limit - resultados.size());
            for (Map<String, Object> estacion : adicionales) {
                String id = (String) estacion.get("id");
                if (!idsYaIncluidos.contains(id)) {
                    resultados.add(estacion);
                    idsYaIncluidos.add(id);
                    if (resultados.size() >= limit) break;
                }
            }
        }

        return resultados;
    }

    /**
     * Busca estaciones por ID.
     *
     * @param query ID o parte del ID a buscar
     * @param limit Límite de resultados
     * @return Lista de estaciones que coinciden
     */
    public List<Map<String, Object>> buscarPorId(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String queryLower = query.toLowerCase().trim();
        return indiceId.searchByContains(queryLower, limit);
    }

    /**
     * Busca una estación exacta por ID.
     *
     * @param id ID de la estación
     * @return Mapa con datos de la estación o null
     */
    public Map<String, Object> buscarPorIdExacto(String id) {
        if (id == null) return null;
        return indiceId.search(id.toLowerCase());
    }

    /**
     * Busca estaciones combinando búsqueda por nombre e ID.
     *
     * @param query Texto a buscar
     * @param limit Límite de resultados
     * @return Lista de estaciones ordenadas por relevancia
     */
    public List<Map<String, Object>> buscar(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> idsUnicos = new HashSet<>();
        List<Map<String, Object>> resultados = new ArrayList<>();

        // 1. Buscar por nombre primero (más común)
        List<Map<String, Object>> porNombre = buscarPorNombre(query, limit);
        for (Map<String, Object> estacion : porNombre) {
            String id = (String) estacion.get("id");
            if (!idsUnicos.contains(id)) {
                resultados.add(estacion);
                idsUnicos.add(id);
            }
        }

        // 2. Si no hay suficientes, buscar por ID
        if (resultados.size() < limit) {
            List<Map<String, Object>> porId = buscarPorId(query, limit - resultados.size());
            for (Map<String, Object> estacion : porId) {
                String id = (String) estacion.get("id");
                if (!idsUnicos.contains(id)) {
                    resultados.add(estacion);
                    idsUnicos.add(id);
                    if (resultados.size() >= limit) break;
                }
            }
        }

        return resultados;
    }

    /**
     * Obtiene el número total de estaciones indexadas.
     */
    public int getTotalEstaciones() {
        return totalEstacionesIndexadas;
    }

    /**
     * Obtiene estadísticas de los índices.
     */
    public Map<String, String> getEstadisticas() {
        Map<String, String> stats = new HashMap<>();
        stats.put("totalEstaciones", String.valueOf(totalEstacionesIndexadas));
        stats.put("indiceNombre", indiceNombre.getStats());
        stats.put("indiceId", indiceId.getStats());
        return stats;
    }
}
