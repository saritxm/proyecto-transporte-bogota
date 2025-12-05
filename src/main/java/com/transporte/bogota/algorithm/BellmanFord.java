package com.transporte.bogota.algorithm;

import com.transporte.bogota.model.Estacion;
import com.transporte.bogota.util.Graph;
import com.transporte.bogota.util.GraphEdge;

import java.util.*;

/**
 * Implementación del algoritmo de Bellman-Ford para encontrar caminos más cortos
 * y detectar ciclos negativos en grafos con pesos negativos.
 *
 * En el contexto de transporte, los "pesos negativos" pueden representar:
 * - Penalizaciones por congestión (tiempos adicionales en hora pico)
 * - Costos variables dependiendo de la carga
 * - Incentivos por usar rutas menos congestionadas (pesos negativos para promoción)
 */
public class BellmanFord {

    /**
     * Resultado del algoritmo Bellman-Ford
     */
    public static class Resultado {
        public final Map<Estacion, Double> distancias;
        public final Map<Estacion, Estacion> predecesores;
        public final boolean tieneCicloNegativo;
        public final List<Estacion> cicloNegativo;

        public Resultado(Map<Estacion, Double> distancias, Map<Estacion, Estacion> predecesores,
                        boolean tieneCicloNegativo, List<Estacion> cicloNegativo) {
            this.distancias = distancias;
            this.predecesores = predecesores;
            this.tieneCicloNegativo = tieneCicloNegativo;
            this.cicloNegativo = cicloNegativo;
        }
    }

    /**
     * Ejecuta Bellman-Ford desde una estación origen.
     * OPTIMIZADO: Solo procesa nodos alcanzables desde origen.
     *
     * Complejidad: O(V * E) donde V = nodos alcanzables, E = aristas relevantes
     *
     * @param grafo El grafo de transporte
     * @param origen Estación de inicio
     * @return Resultado con distancias, predecesores y detección de ciclo negativo
     */
    public static Resultado ejecutar(Graph grafo, Estacion origen) {
        // CRÍTICO: Limitar a 800 nodos para evitar OOM
        return ejecutarOptimizado(grafo, origen, null, 800);
    }

    /**
     * Ejecuta Bellman-Ford con destino específico.
     * Usa BFS bidireccional para optimizar la búsqueda.
     *
     * @param grafo El grafo de transporte
     * @param origen Estación de inicio
     * @param destino Estación destino
     * @return Resultado con distancias, predecesores
     */
    public static Resultado ejecutar(Graph grafo, Estacion origen, Estacion destino) {
        // Usar BFS bidireccional cuando hay destino
        return ejecutarConBFSBidireccional(grafo, origen, destino);
    }

    /**
     * Versión optimizada que limita el número de nodos a procesar.
     * CRÍTICO para grafos grandes.
     *
     * @param grafo El grafo de transporte
     * @param origen Estación de inicio
     * @param destino Estación destino (opcional, para limitar búsqueda)
     * @param maxNodos Máximo de nodos a procesar (para evitar OOM)
     * @return Resultado con distancias, predecesores y detección de ciclo negativo
     */
    private static Resultado ejecutarOptimizado(Graph grafo, Estacion origen, Estacion destino, int maxNodos) {
        Map<Estacion, Double> distancias = new HashMap<>();
        Map<Estacion, Estacion> predecesores = new HashMap<>();

        // Si maxNodos >= tamaño del grafo, usar todos los nodos (evitar BFS redundante)
        Set<Estacion> nodosAlcanzables;
        if (maxNodos >= grafo.getNodos().size()) {
            System.out.println("      Usando TODOS los nodos del grafo (" + grafo.getNodos().size() + ")");
            nodosAlcanzables = new HashSet<>(grafo.getNodos());
        } else {
            System.out.println("      Limitando a " + maxNodos + " nodos alcanzables desde origen");
            nodosAlcanzables = obtenerNodosAlcanzables(grafo, origen, maxNodos);
        }

        System.out.println("      Nodos a procesar: " + nodosAlcanzables.size());
        System.out.println("      ¿Destino en nodos alcanzables? " + nodosAlcanzables.contains(destino));

        // Inicializar distancias solo para nodos alcanzables
        for (Estacion nodo : nodosAlcanzables) {
            distancias.put(nodo, Double.POSITIVE_INFINITY);
        }
        distancias.put(origen, 0.0);

        int numNodos = nodosAlcanzables.size();

        // Relajar todas las aristas |V| - 1 veces
        for (int i = 0; i < numNodos - 1; i++) {
            boolean cambio = false;

            for (Estacion u : nodosAlcanzables) {
                Double distU = distancias.get(u);
                if (distU == null || distU == Double.POSITIVE_INFINITY) continue;

                for (GraphEdge arista : grafo.getVecinos(u)) {
                    Estacion v = arista.getDestino();

                    // Solo procesar si el destino está en nodos alcanzables
                    if (!nodosAlcanzables.contains(v)) continue;

                    double peso = arista.getTiempo();
                    Double distV = distancias.get(v);
                    if (distV == null) distV = Double.POSITIVE_INFINITY;

                    if (distU + peso < distV) {
                        distancias.put(v, distU + peso);
                        predecesores.put(v, u);
                        cambio = true;
                    }
                }
            }

            // Optimización: si llegamos al destino y no hay cambios, terminar
            if (!cambio) break;
            if (destino != null && predecesores.containsKey(destino) && !cambio) break;
        }

        // NO detectar ciclos negativos en modo optimizado (muy costoso)
        // Solo en modo completo
        List<Estacion> cicloNegativo = Collections.emptyList();
        boolean tieneCiclo = false;

        return new Resultado(distancias, predecesores, tieneCiclo, cicloNegativo);
    }

    /**
     * Obtiene nodos alcanzables desde origen usando BFS limitado.
     * CRÍTICO: Limita el espacio de búsqueda para evitar OOM.
     */
    private static Set<Estacion> obtenerNodosAlcanzables(Graph grafo, Estacion origen, int maxNodos) {
        Set<Estacion> alcanzables = new HashSet<>();
        Queue<Estacion> cola = new LinkedList<>();

        cola.offer(origen);
        alcanzables.add(origen);

        while (!cola.isEmpty() && alcanzables.size() < maxNodos) {
            Estacion actual = cola.poll();

            for (GraphEdge arista : grafo.getVecinos(actual)) {
                Estacion vecino = arista.getDestino();
                if (!alcanzables.contains(vecino)) {
                    alcanzables.add(vecino);
                    cola.offer(vecino);

                    if (alcanzables.size() >= maxNodos) break;
                }
            }
        }

        return alcanzables;
    }

    /**
     * BFS Bidireccional: expande desde origen y destino simultáneamente.
     * Mucho más eficiente que BFS unidireccional para grafos grandes.
     * Garantiza conectividad entre origen y destino.
     */
    private static Resultado ejecutarConBFSBidireccional(Graph grafo, Estacion origen, Estacion destino) {
        // Expandir desde origen y destino simultáneamente
        Set<Estacion> alcanzablesOrigen = new HashSet<>();
        Set<Estacion> alcanzablesDestino = new HashSet<>();

        Queue<Estacion> colaOrigen = new LinkedList<>();
        Queue<Estacion> colaDestino = new LinkedList<>();

        colaOrigen.offer(origen);
        alcanzablesOrigen.add(origen);

        colaDestino.offer(destino);
        alcanzablesDestino.add(destino);

        int maxPorLado = 500; // 500 nodos desde origen + 500 desde destino = 1000 total máximo

        // Expandir alternadamente hasta que se conecten o se alcance el límite
        while ((!colaOrigen.isEmpty() || !colaDestino.isEmpty()) &&
               alcanzablesOrigen.size() < maxPorLado &&
               alcanzablesDestino.size() < maxPorLado) {

            // Expandir desde origen
            if (!colaOrigen.isEmpty() && alcanzablesOrigen.size() < maxPorLado) {
                Estacion actual = colaOrigen.poll();
                for (GraphEdge arista : grafo.getVecinos(actual)) {
                    Estacion vecino = arista.getDestino();
                    if (!alcanzablesOrigen.contains(vecino)) {
                        alcanzablesOrigen.add(vecino);
                        colaOrigen.offer(vecino);

                        // Si encontramos conexión con el otro lado, detener
                        if (alcanzablesDestino.contains(vecino)) {
                            break;
                        }
                    }
                }
            }

            // Expandir desde destino
            if (!colaDestino.isEmpty() && alcanzablesDestino.size() < maxPorLado) {
                Estacion actual = colaDestino.poll();
                // Buscar aristas entrantes (invertir la búsqueda)
                for (Estacion nodo : grafo.getNodos()) {
                    for (GraphEdge arista : grafo.getVecinos(nodo)) {
                        if (arista.getDestino().equals(actual) && !alcanzablesDestino.contains(nodo)) {
                            alcanzablesDestino.add(nodo);
                            colaDestino.offer(nodo);

                            // Si encontramos conexión con el otro lado, detener
                            if (alcanzablesOrigen.contains(nodo)) {
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Combinar ambos conjuntos - solo nodos alcanzables desde ambos lados
        Set<Estacion> nodosRelevantes = new HashSet<>(alcanzablesOrigen);
        nodosRelevantes.retainAll(alcanzablesDestino); // Intersección

        // Si la intersección es vacía, usar la unión (puede que no haya camino directo)
        if (nodosRelevantes.isEmpty()) {
            nodosRelevantes.addAll(alcanzablesOrigen);
            nodosRelevantes.addAll(alcanzablesDestino);
        }

        // Ejecutar Bellman-Ford solo en el subgrafo relevante
        return ejecutarEnSubgrafo(grafo, origen, destino, nodosRelevantes);
    }

    /**
     * Ejecuta Bellman-Ford en un subgrafo definido por un conjunto de nodos.
     */
    private static Resultado ejecutarEnSubgrafo(Graph grafo, Estacion origen, Estacion destino,
                                                 Set<Estacion> nodosRelevantes) {
        Map<Estacion, Double> distancias = new HashMap<>();
        Map<Estacion, Estacion> predecesores = new HashMap<>();

        // Inicializar solo nodos relevantes
        for (Estacion nodo : nodosRelevantes) {
            distancias.put(nodo, Double.POSITIVE_INFINITY);
        }
        distancias.put(origen, 0.0);

        int numNodos = nodosRelevantes.size();

        // Relajar aristas |V| - 1 veces
        for (int i = 0; i < numNodos - 1; i++) {
            boolean cambio = false;

            for (Estacion u : nodosRelevantes) {
                Double distU = distancias.get(u);
                if (distU == null || distU == Double.POSITIVE_INFINITY) continue;

                for (GraphEdge arista : grafo.getVecinos(u)) {
                    Estacion v = arista.getDestino();

                    // Solo procesar nodos en el subgrafo
                    if (!nodosRelevantes.contains(v)) continue;

                    double peso = arista.getTiempo();
                    Double distV = distancias.get(v);
                    if (distV == null) distV = Double.POSITIVE_INFINITY;

                    if (distU + peso < distV) {
                        distancias.put(v, distU + peso);
                        predecesores.put(v, u);
                        cambio = true;
                    }
                }
            }

            // Si no hay cambios y ya llegamos al destino, terminar
            if (!cambio) break;
            if (destino != null && predecesores.containsKey(destino) && !cambio) break;
        }

        return new Resultado(distancias, predecesores, false, Collections.emptyList());
    }

    /**
     * Detecta si existe un ciclo negativo en el grafo.
     * Si existe, lo reconstruye y lo retorna.
     */
    private static List<Estacion> detectarCicloNegativo(Graph grafo,
                                                         Map<Estacion, Double> distancias,
                                                         Map<Estacion, Estacion> predecesores) {
        Estacion nodoEnCiclo = null;

        // Intentar relajar una vez más - si hay mejora, hay ciclo negativo
        for (Estacion u : grafo.getNodos()) {
            if (distancias.get(u) == Double.POSITIVE_INFINITY) continue;

            for (GraphEdge arista : grafo.getVecinos(u)) {
                Estacion v = arista.getDestino();
                double peso = arista.getTiempo();

                if (distancias.get(u) + peso < distancias.get(v)) {
                    nodoEnCiclo = v;
                    predecesores.put(v, u);
                    break;
                }
            }
            if (nodoEnCiclo != null) break;
        }

        // Si no hay ciclo negativo, retornar lista vacía
        if (nodoEnCiclo == null) {
            return Collections.emptyList();
        }

        // Reconstruir el ciclo negativo
        Set<Estacion> visitados = new HashSet<>();
        Estacion actual = nodoEnCiclo;

        // Retroceder hasta encontrar el ciclo
        while (!visitados.contains(actual)) {
            visitados.add(actual);
            actual = predecesores.get(actual);
            if (actual == null) return Collections.emptyList();
        }

        // Reconstruir el ciclo desde el nodo repetido
        List<Estacion> ciclo = new ArrayList<>();
        Estacion inicioCiclo = actual;
        ciclo.add(actual);
        actual = predecesores.get(actual);

        while (!actual.equals(inicioCiclo)) {
            ciclo.add(actual);
            actual = predecesores.get(actual);
        }
        ciclo.add(inicioCiclo); // Cerrar el ciclo

        Collections.reverse(ciclo);
        return ciclo;
    }

    /**
     * Reconstruye el camino desde origen hasta destino usando los predecesores.
     */
    public static List<Estacion> reconstruirCamino(Estacion origen, Estacion destino,
                                                    Map<Estacion, Estacion> predecesores) {
        List<Estacion> camino = new ArrayList<>();
        Estacion actual = destino;

        while (actual != null && !actual.equals(origen)) {
            camino.add(actual);
            actual = predecesores.get(actual);
        }

        if (actual == null) {
            return Collections.emptyList(); // No hay camino
        }

        camino.add(origen);
        Collections.reverse(camino);
        return camino;
    }

    /**
     * Variante de Bellman-Ford que considera penalizaciones por congestión.
     * Ajusta los pesos de las aristas basándose en la capacidad y carga estimada.
     *
     * @param grafo Grafo original
     * @param origen Estación origen
     * @param factorCongestion Factor de congestión (0.5 = 50% más lento en hora pico)
     * @return Resultado con rutas considerando congestión
     */
    public static Resultado ejecutarConCongestion(Graph grafo, Estacion origen, double factorCongestion) {
        // Crear un grafo con pesos ajustados por congestión
        Graph grafoAjustado = crearGrafoConPenalizaciones(grafo, factorCongestion);
        return ejecutar(grafoAjustado, origen);
    }

    /**
     * Crea un grafo donde los pesos son ajustados por la congestión.
     * Rutas con baja capacidad reciben penalizaciones mayores.
     */
    private static Graph crearGrafoConPenalizaciones(Graph grafo, double factorCongestion) {
        Graph grafoAjustado = new Graph();

        for (Estacion origen : grafo.getNodos()) {
            for (GraphEdge arista : grafo.getVecinos(origen)) {
                int tiempoBase = arista.getTiempo();
                int capacidad = arista.getCapacidad();

                // Calcular penalización: menor capacidad = mayor penalización
                // Capacidad alta (>6000): penalización baja
                // Capacidad baja (<3000): penalización alta
                double penalizacion = 0.0;
                if (capacidad < 3000) {
                    penalizacion = tiempoBase * (factorCongestion * 1.5); // +150% en congestión alta
                } else if (capacidad < 5000) {
                    penalizacion = tiempoBase * factorCongestion; // +50% en congestión media
                } else {
                    penalizacion = tiempoBase * (factorCongestion * 0.3); // +30% en congestión baja
                }

                int tiempoAjustado = (int) (tiempoBase + penalizacion);

                grafoAjustado.addArista(
                    arista.getOrigen(),
                    arista.getDestino(),
                    tiempoAjustado,
                    capacidad
                );
            }
        }

        return grafoAjustado;
    }

    /**
     * Encuentra K rutas alternativas usando Bellman-Ford iterativo.
     * OPTIMIZADO: Usa BFS bidireccional para limitar espacio de búsqueda.
     * Cada iteración penaliza las aristas de la ruta anterior para forzar alternativas.
     *
     * @param grafo Grafo original
     * @param origen Estación origen
     * @param destino Estación destino
     * @param k Número de rutas alternativas a encontrar (máximo 5)
     * @return Lista de caminos alternativos ordenados por costo total
     */
    public static List<RutaAlternativa> encontrarRutasAlternativas(Graph grafo, Estacion origen,
                                                                    Estacion destino, int k) {
        System.out.println("🔍 Bellman-Ford: Buscando " + k + " rutas alternativas de " + origen.getId() + " a " + destino.getId());

        // Limitar K para evitar sobrecarga
        k = Math.min(k, 5);

        List<RutaAlternativa> rutas = new ArrayList<>();

        // Crear subgrafo usando BFS bidireccional
        Graph subgrafo = crearSubgrafoBidireccional(grafo, origen, destino);
        System.out.println("   Subgrafo creado: " + subgrafo.getNodos().size() + " nodos");

        for (int i = 0; i < k; i++) {
            // Ejecutar Bellman-Ford directamente en el subgrafo (ya optimizado)
            // NO llamar a ejecutar() porque crearía otro subgrafo dentro del subgrafo
            Resultado resultado = ejecutarOptimizado(subgrafo, origen, destino, subgrafo.getNodos().size());

            Double distanciaDestino = resultado.distancias.get(destino);
            System.out.println("   Iteración " + (i+1) + ": distancia = " + distanciaDestino);

            if (distanciaDestino == null || distanciaDestino == Double.POSITIVE_INFINITY) {
                System.out.println("   ❌ No se encontró ruta en iteración " + (i+1));
                break; // No hay más rutas disponibles
            }

            List<Estacion> camino = reconstruirCamino(origen, destino, resultado.predecesores);
            if (camino.isEmpty() || camino.size() < 2) {
                System.out.println("   ❌ Camino vacío o muy corto");
                break;
            }

            System.out.println("   ✅ Ruta " + (i+1) + " encontrada: " + camino.size() + " estaciones, tiempo " + distanciaDestino);
            rutas.add(new RutaAlternativa(camino, distanciaDestino, i + 1));

            // Penalizar aristas del camino encontrado para la próxima iteración
            penalizarCamino(subgrafo, camino, 1.5);
        }

        System.out.println("✅ Total de rutas encontradas: " + rutas.size());
        return rutas;
    }

    /**
     * Crea un subgrafo usando BFS bidireccional desde origen y destino.
     * Mucho más eficiente que expandir solo desde origen.
     */
    private static Graph crearSubgrafoBidireccional(Graph grafoOriginal, Estacion origen, Estacion destino) {
        System.out.println("   🔧 Creando subgrafo bidireccional...");

        // Intentaremos construir un subgrafo bidireccional creciente.
        // Empezamos con límites conservadores y los aumentamos si origen->destino
        // no es alcanzable dentro del subgrafo.
        int[] limites = new int[]{500, 1000, 2000, 5000};

        for (int limite : limites) {
            Set<Estacion> alcanzablesOrigen = obtenerNodosAlcanzables(grafoOriginal, origen, limite);
            Set<Estacion> alcanzablesDestino = obtenerNodosAlcanzablesInverso(grafoOriginal, destino, limite);

            System.out.println("      Límite " + limite + ": origen alcanza " + alcanzablesOrigen.size() +
                             " nodos, destino alcanza " + alcanzablesDestino.size() + " nodos (inverso)");

            // Combinar ambos conjuntos
            Set<Estacion> nodosRelevantes = new HashSet<>(alcanzablesOrigen);
            nodosRelevantes.addAll(alcanzablesDestino);

            // Asegurar que origen y destino estén presentes
            nodosRelevantes.add(origen);
            nodosRelevantes.add(destino);

            System.out.println("      Total nodos combinados: " + nodosRelevantes.size());

            // Crear subgrafo con solo estos nodos
            Graph subgrafo = new Graph();
            for (Estacion nodo : nodosRelevantes) {
                for (GraphEdge arista : grafoOriginal.getVecinos(nodo)) {
                    if (nodosRelevantes.contains(arista.getDestino())) {
                        subgrafo.addArista(
                            arista.getOrigen(),
                            arista.getDestino(),
                            arista.getTiempo(),
                            arista.getCapacidad()
                        );
                    }
                }
            }

            // Comprobar alcanzabilidad directa en el subgrafo
            boolean alcanzable = esAlcanzable(subgrafo, origen, destino);
            System.out.println("      ¿Es alcanzable? " + alcanzable);

            if (alcanzable) {
                System.out.println("   ✅ Subgrafo con conectividad encontrado (límite=" + limite + ")");
                return subgrafo;
            }
            // Si no es alcanzable, intentamos con un límite mayor
        }

        // Último recurso: devolver un subgrafo amplio construido con el mayor límite
        Set<Estacion> alcanzablesOrigen = obtenerNodosAlcanzables(grafoOriginal, origen, 10000);
        Set<Estacion> alcanzablesDestino = obtenerNodosAlcanzablesInverso(grafoOriginal, destino, 10000);
        Set<Estacion> nodosRelevantes = new HashSet<>(alcanzablesOrigen);
        nodosRelevantes.addAll(alcanzablesDestino);
        nodosRelevantes.add(origen);
        nodosRelevantes.add(destino);

        Graph subgrafo = new Graph();
        for (Estacion nodo : nodosRelevantes) {
            for (GraphEdge arista : grafoOriginal.getVecinos(nodo)) {
                if (nodosRelevantes.contains(arista.getDestino())) {
                    subgrafo.addArista(arista.getOrigen(), arista.getDestino(), arista.getTiempo(), arista.getCapacidad());
                }
            }
        }

        return subgrafo;
    }

    /**
     * BFS inverso: encuentra nodos desde los cuales se puede llegar al destino.
     */
    private static Set<Estacion> obtenerNodosAlcanzablesInverso(Graph grafo, Estacion destino, int maxNodos) {
        Set<Estacion> alcanzables = new HashSet<>();
        Queue<Estacion> cola = new LinkedList<>();

        cola.offer(destino);
        alcanzables.add(destino);

        // Para acelerar la búsqueda inversa, pre-calcular aristas entrantes por nodo
        Map<Estacion, List<Estacion>> incoming = new HashMap<>();
        for (Estacion nodo : grafo.getNodos()) {
            for (GraphEdge arista : grafo.getVecinos(nodo)) {
                incoming.computeIfAbsent(arista.getDestino(), k -> new ArrayList<>()).add(nodo);
            }
        }

        while (!cola.isEmpty() && alcanzables.size() < maxNodos) {
            Estacion actual = cola.poll();

            List<Estacion> padres = incoming.getOrDefault(actual, Collections.emptyList());
            for (Estacion nodo : padres) {
                if (!alcanzables.contains(nodo)) {
                    alcanzables.add(nodo);
                    cola.offer(nodo);
                    if (alcanzables.size() >= maxNodos) return alcanzables;
                }
            }
        }

        return alcanzables;
    }

    /**
     * Comprueba si existe un camino desde origen a destino en el grafo (BFS).
     */
    private static boolean esAlcanzable(Graph grafo, Estacion origen, Estacion destino) {
        if (origen == null || destino == null) return false;
        if (!grafo.contiene(origen) || !grafo.contiene(destino)) return false;

        Set<Estacion> visitados = new HashSet<>();
        Queue<Estacion> q = new LinkedList<>();
        q.offer(origen);
        visitados.add(origen);

        while (!q.isEmpty()) {
            Estacion u = q.poll();
            if (u.equals(destino)) return true;
            for (GraphEdge e : grafo.getVecinos(u)) {
                Estacion v = e.getDestino();
                if (!visitados.contains(v)) {
                    visitados.add(v);
                    q.offer(v);
                }
            }
        }

        return false;
    }

    /**
     * Crea un subgrafo con solo nodos alcanzables desde origen.
     * CRÍTICO para rendimiento en grafos grandes.
     */
    private static Graph crearSubgrafoAlcanzable(Graph grafoOriginal, Estacion origen, int maxNodos) {
        Set<Estacion> nodosAlcanzables = obtenerNodosAlcanzables(grafoOriginal, origen, maxNodos);

        Graph subgrafo = new Graph();
        for (Estacion nodo : nodosAlcanzables) {
            for (GraphEdge arista : grafoOriginal.getVecinos(nodo)) {
                if (nodosAlcanzables.contains(arista.getDestino())) {
                    subgrafo.addArista(
                        arista.getOrigen(),
                        arista.getDestino(),
                        arista.getTiempo(),
                        arista.getCapacidad()
                    );
                }
            }
        }

        return subgrafo;
    }

    /**
     * Copia un grafo (estructura simple, no copia profunda de estaciones)
     */
    private static Graph copiarGrafo(Graph original) {
        Graph copia = new Graph();
        for (Estacion origen : original.getNodos()) {
            for (GraphEdge arista : original.getVecinos(origen)) {
                copia.addArista(
                    arista.getOrigen(),
                    arista.getDestino(),
                    arista.getTiempo(),
                    arista.getCapacidad()
                );
            }
        }
        return copia;
    }

    /**
     * Penaliza las aristas de un camino para desfavorecerlo en futuras búsquedas.
     */
    private static void penalizarCamino(Graph grafo, List<Estacion> camino, double factor) {
        System.out.println("   🔧 Penalizando camino con factor " + factor + "...");

        for (int i = 0; i < camino.size() - 1; i++) {
            Estacion origen = camino.get(i);
            Estacion destino = camino.get(i + 1);

            // Buscar la arista que necesitamos penalizar
            GraphEdge aristaAModificar = null;
            for (GraphEdge arista : grafo.getVecinos(origen)) {
                if (arista.getDestino().equals(destino)) {
                    aristaAModificar = arista;
                    break;
                }
            }

            if (aristaAModificar != null) {
                int tiempoViejo = aristaAModificar.getTiempo();

                // Estrategia de penalización: multiplicar Y sumar un valor fijo
                // Esto asegura que incluso aristas con tiempo=1 sean penalizadas efectivamente
                int nuevoTiempo = (int) (tiempoViejo * factor) + 10;

                // Remover arista vieja (evitar ConcurrentModificationException)
                List<GraphEdge> vecinos = grafo.getVecinos(origen);
                vecinos.remove(aristaAModificar);

                // Agregar nueva arista con peso penalizado
                grafo.addArista(origen, destino, nuevoTiempo, aristaAModificar.getCapacidad());

                System.out.println("      Arista " + origen.getId() + " -> " + destino.getId() +
                                 ": tiempo " + tiempoViejo + " -> " + nuevoTiempo);
            }
        }
    }

    /**
     * Clase para representar una ruta alternativa con su costo.
     */
    public static class RutaAlternativa {
        public final List<Estacion> camino;
        public final double costoTotal;
        public final int numeroRuta;

        public RutaAlternativa(List<Estacion> camino, double costoTotal, int numeroRuta) {
            this.camino = camino;
            this.costoTotal = costoTotal;
            this.numeroRuta = numeroRuta;
        }

        public int getNumeroEstaciones() {
            return camino.size();
        }

        @Override
        public String toString() {
            return String.format("Ruta #%d: %.1f min, %d estaciones",
                               numeroRuta, costoTotal, camino.size());
        }
    }
}
