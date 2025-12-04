package com.transporte.bogota.service;

import com.transporte.bogota.algorithm.MaxFlow;
import com.transporte.bogota.model.Estacion;
import com.transporte.bogota.util.Graph;
import com.transporte.bogota.util.GraphEdge;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para analizar congestión en horas pico usando algoritmos de flujo máximo.
 * Identifica cuellos de botella y sugiere rutas alternativas.
 */
@Service
public class CongestionAnalysisService {

    /**
     * Analiza la congestión entre dos estaciones durante horas pico.
     * Reduce las capacidades al 60% para simular horas pico.
     */
    public AnalisisCongestion analizarCongestion(Graph grafo, Estacion origen, Estacion destino) {
        // Crear un grafo simulando horas pico (capacidad reducida)
        Graph grafoHoraPico = simularHoraPico(grafo, 0.6);

        // Calcular flujo máximo en condiciones normales
        int flujoNormal = MaxFlow.calcularFlujoMaximo(grafo, origen, destino);

        // Calcular flujo máximo en hora pico
        int flujoHoraPico = MaxFlow.calcularFlujoMaximo(grafoHoraPico, origen, destino);

        // Calcular porcentaje de reducción
        double porcentajeReduccion = ((double)(flujoNormal - flujoHoraPico) / flujoNormal) * 100;

        // Identificar cuellos de botella
        List<CuelloBotella> cuellos = identificarCuellosBotella(grafoHoraPico, origen, destino);

        // Determinar nivel de congestión
        NivelCongestion nivel = determinarNivelCongestion(porcentajeReduccion);

        // Generar recomendaciones
        List<String> recomendaciones = generarRecomendaciones(nivel, cuellos, flujoHoraPico);

        return new AnalisisCongestion(
            flujoNormal,
            flujoHoraPico,
            porcentajeReduccion,
            nivel,
            cuellos,
            recomendaciones
        );
    }

    /**
     * Simula condiciones de hora pico reduciendo capacidades.
     */
    private Graph simularHoraPico(Graph grafo, double factorReduccion) {
        Graph grafoHoraPico = new Graph();

        // Copiar aristas con capacidad reducida
        for (Estacion origen : grafo.getNodos()) {
            for (GraphEdge edge : grafo.getVecinos(origen)) {
                int nuevaCapacidad = (int)(edge.getCapacidad() * factorReduccion);
                // addArista automáticamente agrega los nodos si no existen
                grafoHoraPico.addArista(
                    edge.getOrigen(),
                    edge.getDestino(),
                    edge.getTiempo(),
                    nuevaCapacidad
                );
            }
        }

        return grafoHoraPico;
    }

    /**
     * Identifica los cuellos de botella en la red.
     * Un cuello de botella es una arista con alta demanda y baja capacidad disponible.
     */
    private List<CuelloBotella> identificarCuellosBotella(Graph grafo, Estacion origen, Estacion destino) {
        List<CuelloBotella> cuellos = new ArrayList<>();

        // Analizar todas las aristas en el grafo
        for (Estacion estacionOrigen : grafo.getNodos()) {
            for (GraphEdge edge : grafo.getVecinos(estacionOrigen)) {
                // Calcular el flujo que pasa por esta arista
                int capacidad = edge.getCapacidad();

                // Si la capacidad es muy baja, es un potencial cuello de botella
                if (capacidad < 4000) { // Umbral para considerar congestión
                    double porcentajeUso = calcularPorcentajeUso(grafo, estacionOrigen, edge.getDestino());

                    if (porcentajeUso > 70) { // Más del 70% de uso
                        cuellos.add(new CuelloBotella(
                            estacionOrigen,
                            edge.getDestino(),
                            capacidad,
                            (int)(capacidad * porcentajeUso / 100),
                            porcentajeUso
                        ));
                    }
                }
            }
        }

        // Ordenar por porcentaje de uso (descendente)
        cuellos.sort((a, b) -> Double.compare(b.porcentajeUso, a.porcentajeUso));

        // Retornar los 5 cuellos de botella más críticos
        return cuellos.stream().limit(5).collect(Collectors.toList());
    }

    /**
     * Calcula el porcentaje de uso estimado de una arista.
     * Simulación basada en capacidad relativa.
     */
    private double calcularPorcentajeUso(Graph grafo, Estacion origen, Estacion destino) {
        // Obtener la capacidad de la arista
        int capacidad = 0;
        for (GraphEdge edge : grafo.getVecinos(origen)) {
            if (edge.getDestino().equals(destino)) {
                capacidad = edge.getCapacidad();
                break;
            }
        }

        if (capacidad == 0) return 0;

        // Estimación: cuanto menor la capacidad, mayor el porcentaje de uso
        // Capacidad máxima esperada: 8000
        double factorCapacidad = 1.0 - (capacidad / 8000.0);

        // Porcentaje base + factor de capacidad
        return 50 + (factorCapacidad * 40); // Entre 50% y 90%
    }

    /**
     * Determina el nivel de congestión basado en la reducción de flujo.
     */
    private NivelCongestion determinarNivelCongestion(double porcentajeReduccion) {
        if (porcentajeReduccion < 10) {
            return NivelCongestion.BAJO;
        } else if (porcentajeReduccion < 25) {
            return NivelCongestion.MEDIO;
        } else if (porcentajeReduccion < 40) {
            return NivelCongestion.ALTO;
        } else {
            return NivelCongestion.CRITICO;
        }
    }

    /**
     * Genera recomendaciones basadas en el análisis de congestión.
     */
    private List<String> generarRecomendaciones(NivelCongestion nivel, List<CuelloBotella> cuellos, int flujoDisponible) {
        List<String> recomendaciones = new ArrayList<>();

        switch (nivel) {
            case BAJO:
                recomendaciones.add("✅ Condiciones de tráfico favorables");
                recomendaciones.add("💡 No se requieren acciones especiales");
                break;

            case MEDIO:
                recomendaciones.add("⚠️ Congestión moderada detectada");
                recomendaciones.add("💡 Considerar rutas alternativas durante horas pico");
                recomendaciones.add("🕐 Evitar horarios de 7-9 AM y 5-7 PM si es posible");
                break;

            case ALTO:
                recomendaciones.add("⚠️ Congestión alta en la ruta");
                recomendaciones.add("🚨 Se recomienda usar rutas alternativas");
                recomendaciones.add("💡 Incrementar frecuencia de vehículos en " + flujoDisponible / 500 + " unidades");
                break;

            case CRITICO:
                recomendaciones.add("🚨 CONGESTIÓN CRÍTICA - Acción inmediata requerida");
                recomendaciones.add("⛔ Evitar esta ruta durante horas pico");
                recomendaciones.add("💡 Implementar sistema de buses expresos");
                recomendaciones.add("🚦 Considerar carriles exclusivos en segmentos congestionados");
                break;
        }

        // Recomendaciones específicas para cuellos de botella
        if (!cuellos.isEmpty()) {
            recomendaciones.add("");
            recomendaciones.add("🔴 Cuellos de botella identificados:");
            for (int i = 0; i < Math.min(3, cuellos.size()); i++) {
                CuelloBotella cuello = cuellos.get(i);
                recomendaciones.add(String.format("   • %s → %s (%.0f%% uso)",
                    cuello.origen.getNombre(),
                    cuello.destino.getNombre(),
                    cuello.porcentajeUso));
            }
            recomendaciones.add("💡 Priorizar mejoras en estos segmentos");
        }

        return recomendaciones;
    }

    // ==================== Clases de Datos ====================

    public static class AnalisisCongestion {
        public final int flujoNormal;
        public final int flujoHoraPico;
        public final double porcentajeReduccion;
        public final NivelCongestion nivel;
        public final List<CuelloBotella> cuellosBotella;
        public final List<String> recomendaciones;

        public AnalisisCongestion(int flujoNormal, int flujoHoraPico, double porcentajeReduccion,
                                  NivelCongestion nivel, List<CuelloBotella> cuellosBotella,
                                  List<String> recomendaciones) {
            this.flujoNormal = flujoNormal;
            this.flujoHoraPico = flujoHoraPico;
            this.porcentajeReduccion = porcentajeReduccion;
            this.nivel = nivel;
            this.cuellosBotella = cuellosBotella;
            this.recomendaciones = recomendaciones;
        }
    }

    public static class CuelloBotella {
        public final Estacion origen;
        public final Estacion destino;
        public final int capacidadTotal;
        public final int flujoActual;
        public final double porcentajeUso;

        public CuelloBotella(Estacion origen, Estacion destino, int capacidadTotal,
                            int flujoActual, double porcentajeUso) {
            this.origen = origen;
            this.destino = destino;
            this.capacidadTotal = capacidadTotal;
            this.flujoActual = flujoActual;
            this.porcentajeUso = porcentajeUso;
        }
    }

    public enum NivelCongestion {
        BAJO("Bajo", "#22C55E"),      // Verde
        MEDIO("Medio", "#F59E0B"),    // Amarillo
        ALTO("Alto", "#EF4444"),      // Rojo
        CRITICO("Crítico", "#991B1B"); // Rojo oscuro

        public final String nombre;
        public final String color;

        NivelCongestion(String nombre, String color) {
            this.nombre = nombre;
            this.color = color;
        }
    }
}
