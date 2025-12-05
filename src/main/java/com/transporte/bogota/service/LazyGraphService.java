package com.transporte.bogota.service;

import com.transporte.bogota.model.Estacion;
import com.transporte.bogota.model.SistemaTransporte;
import com.transporte.bogota.util.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Servicio para construcción lazy (bajo demanda) de grafos.
 * En lugar de cargar todas las 14,687 rutas al inicio, construye
 * un grafo solo con las estaciones relevantes para la búsqueda.
 * Usa B+ tree para búsqueda eficiente O(log n) de rutas.
 */
@Service
public class LazyGraphService {

    private static final Logger logger = LoggerFactory.getLogger(LazyGraphService.class);

    private final SistemaTransporte sistema;
    private final RutaIndexService rutaIndexService;

    public LazyGraphService(SistemaTransporte sistema, RutaIndexService rutaIndexService) {
        this.sistema = sistema;
        this.rutaIndexService = rutaIndexService;
    }

    /**
     * Construye un grafo lazy que solo incluye rutas relevantes para ir de origen a destino.
     *
     * Estrategia:
     * 1. Calcula la distancia entre origen y destino
     * 2. Define un radio dinámico basado en esa distancia
     * 3. Carga estaciones en ese radio alrededor del origen, destino y ruta directa
     *
     * @param origen Estación de origen
     * @param destino Estación de destino
     * @return Grafo con rutas relevantes
     */
    public Graph construirGrafoLazy(Estacion origen, Estacion destino) {
        long startTime = System.currentTimeMillis();

        Graph grafo = new Graph();

        // 1. Calcular distancia entre origen y destino
        double distanciaDirecta = calcularDistancia(
            origen.getLatitud(), origen.getLongitud(),
            destino.getLatitud(), destino.getLongitud()
        );

        // 2. Estrategia de expansión bidireccional
        // Expandir desde origen y destino hasta que se conecten
        Set<String> estacionesRelevantes = new HashSet<>();
        estacionesRelevantes.add(origen.getId());
        estacionesRelevantes.add(destino.getId());

        // Radio base: mínimo 3 km
        double radioBase = Math.max(3.0, distanciaDirecta / 4.0);

        logger.info("Distancia origen-destino: {:.2f} km, Radio base: {:.2f} km",
                    distanciaDirecta, radioBase);

        // Expandir en múltiples capas desde origen y destino
        int maxCapas = 3; // Máximo 3 saltos de expansión
        expandirDesdePunto(estacionesRelevantes, origen, radioBase, maxCapas);
        expandirDesdePunto(estacionesRelevantes, destino, radioBase, maxCapas);

        // Agregar estaciones en el "corredor" entre origen y destino para asegurar conectividad
        estacionesRelevantes.addAll(getEstacionesEnCorredor(origen, destino, radioBase));

        logger.info("Estaciones relevantes: {} (de {} totales)",
                    estacionesRelevantes.size(),
                    sistema.getAllEstaciones().size());

        // 2. Agregar nodos al grafo
        for (String estId : estacionesRelevantes) {
            Estacion est = sistema.getEstacion(estId);
            if (est != null) {
                grafo.addNodo(est);
            }
        }

        // 3. Cargar rutas que conectan estas estaciones usando índice
        int rutasCargadas = cargarRutasRelevantesBTree(grafo, estacionesRelevantes);

        long endTime = System.currentTimeMillis();
        logger.info("Grafo lazy construido en {} ms - Nodos: {}, Rutas: {}",
                   endTime - startTime,
                   grafo.getNodos().size(),
                   rutasCargadas);

        // Verificar que origen y destino estén en el grafo
        boolean origenEnGrafo = grafo.getNodos().contains(origen);
        boolean destinoEnGrafo = grafo.getNodos().contains(destino);
        logger.info("Origen '{}' en grafo: {}, Destino '{}' en grafo: {}",
                   origen.getId(), origenEnGrafo, destino.getId(), destinoEnGrafo);

        if (!origenEnGrafo || !destinoEnGrafo) {
            logger.error("ERROR: Origen o destino no están en el grafo construido!");
        }

        return grafo;
    }

    /**
     * Expande desde un punto en múltiples capas, agregando vecinos de vecinos.
     * Esto asegura conectividad incluso con radio pequeño.
     */
    private void expandirDesdePunto(Set<String> estacionesRelevantes, Estacion puntoInicial,
                                    double radio, int maxCapas) {
        Set<String> capaActual = new HashSet<>();
        capaActual.add(puntoInicial.getId());

        for (int capa = 0; capa < maxCapas; capa++) {
            Set<String> siguienteCapa = new HashSet<>();

            for (String estId : capaActual) {
                Estacion est = sistema.getEstacion(estId);
                if (est == null) continue;

                // Agregar estaciones cercanas a esta estación
                Set<String> cercanas = getEstacionesCercanas(est, radio);
                siguienteCapa.addAll(cercanas);
            }

            // Agregar todas las estaciones de esta capa
            estacionesRelevantes.addAll(siguienteCapa);

            // Si no encontramos nuevas estaciones, terminar
            if (siguienteCapa.isEmpty() || capaActual.equals(siguienteCapa)) {
                break;
            }

            capaActual = siguienteCapa;
        }
    }

    /**
     * Obtiene estaciones cercanas a una estación base en un radio dado.
     */
    private Set<String> getEstacionesCercanas(Estacion base, double radioKm) {
        Set<String> cercanas = new HashSet<>();

        for (Estacion est : sistema.getAllEstaciones()) {
            double dist = calcularDistancia(
                base.getLatitud(), base.getLongitud(),
                est.getLatitud(), est.getLongitud()
            );

            if (dist <= radioKm) {
                cercanas.add(est.getId());
            }
        }

        return cercanas;
    }

    /**
     * Obtiene estaciones que están en el "corredor" entre origen y destino.
     * Esto asegura que haya conectividad incluso para rutas largas.
     */
    private Set<String> getEstacionesEnCorredor(Estacion origen, Estacion destino, double anchoKm) {
        Set<String> enCorredor = new HashSet<>();

        double origenLat = origen.getLatitud();
        double origenLon = origen.getLongitud();
        double destinoLat = destino.getLatitud();
        double destinoLon = destino.getLongitud();

        for (Estacion est : sistema.getAllEstaciones()) {
            // Calcular si la estación está cerca de la línea directa origen-destino
            double distanciaAlCorredor = calcularDistanciaPuntoALinea(
                est.getLatitud(), est.getLongitud(),
                origenLat, origenLon,
                destinoLat, destinoLon
            );

            if (distanciaAlCorredor <= anchoKm) {
                // Verificar que esté entre origen y destino (no más allá)
                double distAOrigen = calcularDistancia(origenLat, origenLon, est.getLatitud(), est.getLongitud());
                double distADestino = calcularDistancia(destinoLat, destinoLon, est.getLatitud(), est.getLongitud());
                double distOrigenDestino = calcularDistancia(origenLat, origenLon, destinoLat, destinoLon);

                // Solo incluir si está "entre" origen y destino (con margen de 10%)
                if (distAOrigen + distADestino <= distOrigenDestino * 1.1) {
                    enCorredor.add(est.getId());
                }
            }
        }

        return enCorredor;
    }

    /**
     * Calcula la distancia de un punto a una línea definida por dos puntos.
     * Usa la fórmula de distancia perpendicular.
     */
    private double calcularDistanciaPuntoALinea(double pLat, double pLon,
                                                double lat1, double lon1,
                                                double lat2, double lon2) {
        // Convertir a coordenadas cartesianas aproximadas (válido para distancias cortas)
        double x0 = pLon;
        double y0 = pLat;
        double x1 = lon1;
        double y1 = lat1;
        double x2 = lon2;
        double y2 = lat2;

        // Distancia perpendicular
        double numerador = Math.abs((y2 - y1) * x0 - (x2 - x1) * y0 + x2 * y1 - y2 * x1);
        double denominador = Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));

        if (denominador == 0) {
            return calcularDistancia(pLat, pLon, lat1, lon1);
        }

        // Convertir de grados a km (aproximado: 111 km por grado)
        return (numerador / denominador) * 111.0;
    }

    /**
     * Calcula distancia haversine entre dos puntos.
     */
    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0; // Radio de la Tierra en km

        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double dlon = Math.toRadians(lon2 - lon1);
        double dlat = Math.toRadians(lat2 - lat1);

        double a = Math.sin(dlat/2) * Math.sin(dlat/2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(dlon/2) * Math.sin(dlon/2);

        double c = 2 * Math.asin(Math.sqrt(a));

        return R * c;
    }

    /**
     * Carga rutas usando índice HashMap - búsqueda O(1).
     * Solo carga rutas que conectan estaciones relevantes.
     */
    private int cargarRutasRelevantesBTree(Graph grafo, Set<String> estacionesRelevantes) {
        int rutasCargadas = 0;

        // Usar el índice para obtener solo las rutas entre estaciones relevantes
        List<Map<String, Object>> rutas = rutaIndexService.getRutasParaEstaciones(estacionesRelevantes);

        logger.debug("Rutas encontradas en índice: {}", rutas.size());

        for (Map<String, Object> rutaData : rutas) {
            String origenId = (String) rutaData.get("origen");
            String destinoId = (String) rutaData.get("destino");
            int tiempoViaje = (Integer) rutaData.get("tiempoViaje");
            int capacidad = (Integer) rutaData.get("capacidad");

            Estacion origen = sistema.getEstacion(origenId);
            Estacion destino = sistema.getEstacion(destinoId);

            if (origen != null && destino != null) {
                grafo.addArista(origen, destino, tiempoViaje, capacidad);
                rutasCargadas++;
            }
        }

        logger.info("Rutas cargadas desde índice: {}", rutasCargadas);
        return rutasCargadas;
    }

    /**
     * Construye un grafo completo (para casos especiales o análisis globales).
     * ADVERTENCIA: Carga todas las 14,687 rutas. Usar solo cuando sea necesario.
     */
    public Graph construirGrafoCompleto() {
        long startTime = System.currentTimeMillis();
        logger.warn("Construyendo grafo COMPLETO - Esto puede tomar tiempo...");

        Graph grafo = new Graph();

        // Agregar todas las estaciones
        for (Estacion e : sistema.getAllEstaciones()) {
            grafo.addNodo(e);
        }

        // Cargar TODAS las rutas usando B+ tree
        Set<String> todasEstaciones = new HashSet<>();
        for (Estacion e : sistema.getAllEstaciones()) {
            todasEstaciones.add(e.getId());
        }
        int rutasCargadas = cargarRutasRelevantesBTree(grafo, todasEstaciones);

        long endTime = System.currentTimeMillis();
        logger.info("Grafo completo construido en {} ms - Nodos: {}, Rutas: {}",
                   endTime - startTime,
                   grafo.getNodos().size(),
                   rutasCargadas);

        return grafo;
    }
}
