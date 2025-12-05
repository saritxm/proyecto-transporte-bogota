#!/usr/bin/env python3
"""
Script para extraer conexiones entre estaciones desde rutas GeoJSON.
Genera un CSV de aristas (conexiones) entre estaciones para usar en Dijkstra.

La estrategia es:
1. Leer todas las estaciones (TM, Metro, SITP) con sus coordenadas
2. Para cada ruta, encontrar qué estaciones están cerca de sus puntos
3. Generar conexiones entre estaciones consecutivas en cada ruta
"""

import json
import csv
import sys
import math
from typing import List, Dict, Tuple, Set

# Umbral de distancia en km para considerar que una estación está en la ruta
DISTANCIA_UMBRAL_KM = 0.3  # 300 metros

def cargar_estaciones(archivo_csv: str) -> Dict[str, Dict]:
    """
    Carga todas las estaciones desde CSV.

    Returns:
        Dict con id_estacion -> {id, nombre, tipo, lat, lon}
    """
    estaciones = {}

    try:
        with open(archivo_csv, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                estaciones[row['id']] = {
                    'id': row['id'],
                    'nombre': row['nombre'],
                    'tipo': row['tipo'],
                    'lat': float(row['latitud']),
                    'lon': float(row['longitud'])
                }

        print(f"✅ Cargadas {len(estaciones)} estaciones")
        return estaciones

    except Exception as e:
        print(f"❌ Error al cargar estaciones: {e}")
        sys.exit(1)


def distancia_haversine(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """
    Calcula la distancia entre dos puntos usando la fórmula de Haversine.

    Returns:
        Distancia en kilómetros
    """
    R = 6371.0  # Radio de la Tierra en km

    lat1_rad = math.radians(lat1)
    lat2_rad = math.radians(lat2)
    dlon = math.radians(lon2 - lon1)
    dlat = math.radians(lat2 - lat1)

    a = math.sin(dlat/2)**2 + math.cos(lat1_rad) * math.cos(lat2_rad) * math.sin(dlon/2)**2
    c = 2 * math.asin(math.sqrt(a))

    return R * c


def encontrar_estacion_cercana(lat: float, lon: float, estaciones: Dict[str, Dict],
                               umbral_km: float = DISTANCIA_UMBRAL_KM) -> str:
    """
    Encuentra la estación más cercana a un punto dado.

    Returns:
        ID de la estación más cercana, o None si no hay ninguna en el umbral
    """
    min_dist = float('inf')
    estacion_cercana = None

    for est_id, est_data in estaciones.items():
        dist = distancia_haversine(lat, lon, est_data['lat'], est_data['lon'])

        if dist < min_dist and dist <= umbral_km:
            min_dist = dist
            estacion_cercana = est_id

    return estacion_cercana


def procesar_rutas(archivo_geojson: str, estaciones: Dict[str, Dict],
                   factor_muestreo: int = 10) -> List[Dict]:
    """
    Procesa rutas del GeoJSON y extrae conexiones entre estaciones.

    Args:
        archivo_geojson: Path al archivo GeoJSON
        estaciones: Diccionario de estaciones
        factor_muestreo: Revisar 1 de cada N puntos de la ruta (para optimizar)

    Returns:
        Lista de conexiones (aristas) entre estaciones con geometría
    """
    print(f"📂 Leyendo rutas: {archivo_geojson}")

    try:
        with open(archivo_geojson, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except Exception as e:
        print(f"❌ Error al leer GeoJSON: {e}")
        sys.exit(1)

    features = data.get('features', [])
    print(f"📊 Total de rutas encontradas: {len(features)}")

    conexiones = []
    conexiones_set = set()  # Para evitar duplicados

    for idx, feature in enumerate(features):
        props = feature.get('properties', {})
        geom = feature.get('geometry', {})

        if geom.get('type') != 'LineString':
            continue

        coords = geom.get('coordinates', [])
        if not coords:
            continue

        # Información de la ruta
        codigo_ruta = props.get('cod_ruta', '')
        nombre_ruta = props.get('nom_ruta', '')
        tipo_servicio = props.get('tip_serv', '')

        # Encontrar estaciones a lo largo de la ruta
        estaciones_en_ruta = []  # [(est_id, indice_en_coords)]

        # Muestrear puntos de la ruta (no revisar todos para optimizar)
        for i in range(0, len(coords), factor_muestreo):
            lon, lat = coords[i][0], coords[i][1]
            est_id = encontrar_estacion_cercana(lat, lon, estaciones)

            if est_id and (not estaciones_en_ruta or est_id != estaciones_en_ruta[-1][0]):
                estaciones_en_ruta.append((est_id, i))

        # Revisar también el último punto si no fue incluido
        if len(coords) % factor_muestreo != 0:
            lon, lat = coords[-1][0], coords[-1][1]
            est_id = encontrar_estacion_cercana(lat, lon, estaciones)
            if est_id and (not estaciones_en_ruta or est_id != estaciones_en_ruta[-1][0]):
                estaciones_en_ruta.append((est_id, len(coords) - 1))

        # Generar conexiones entre estaciones consecutivas
        for i in range(len(estaciones_en_ruta) - 1):
            origen_id, origen_idx = estaciones_en_ruta[i]
            destino_id, destino_idx = estaciones_en_ruta[i + 1]

            # Crear clave única para evitar duplicados
            clave = f"{origen_id}_{destino_id}"
            clave_inversa = f"{destino_id}_{origen_id}"

            # Calcular distancia real entre estaciones (en km)
            dist_km = distancia_haversine(
                estaciones[origen_id]['lat'], estaciones[origen_id]['lon'],
                estaciones[destino_id]['lat'], estaciones[destino_id]['lon']
            )

            # Convertir distancia a metros
            distancia_m = int(dist_km * 1000)

            # Calcular tiempo de viaje estimado (asumiendo 40 km/h promedio en transporte público)
            tiempo_viaje_minutos = max(1, int((dist_km / 40.0) * 60))

            # Capacidad basada en tipo de servicio
            # 1=Troncal, 2=Alimentador, 3=Urbano
            capacidad = 3500 if tipo_servicio == '1' else (2500 if tipo_servicio == '2' else 2000)

            if clave not in conexiones_set and clave_inversa not in conexiones_set:
                # Generar ID único para la ruta
                ruta_id = f"R{len(conexiones) + 1:04d}"

                conexiones.append({
                    'id': ruta_id,
                    'origen': origen_id,
                    'destino': destino_id,
                    'tiempoViaje': tiempo_viaje_minutos,
                    'capacidad': capacidad,
                    'distancia': distancia_m
                })
                conexiones_set.add(clave)

        # Mostrar progreso
        if (idx + 1) % 100 == 0:
            print(f"  ⏳ Procesadas {idx + 1}/{len(features)} rutas - {len(conexiones)} conexiones encontradas")

    print(f"\n✅ Total de conexiones encontradas: {len(conexiones)}")
    return conexiones


def guardar_conexiones(conexiones: List[Dict], archivo_csv: str):
    """Guarda las conexiones en un archivo CSV compatible con CSVDataLoader."""
    print(f"📝 Guardando conexiones: {archivo_csv}")

    try:
        with open(archivo_csv, 'w', newline='', encoding='utf-8') as f:
            # Formato exacto que espera CSVDataLoader
            fieldnames = ['id', 'origen', 'destino', 'tiempoViaje', 'capacidad', 'distancia']
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(conexiones)

        print(f"✅ Archivo guardado exitosamente")
        print(f"   Formato: id,origen,destino,tiempoViaje,capacidad,distancia")
    except Exception as e:
        print(f"❌ Error al guardar CSV: {e}")
        sys.exit(1)


def main():
    import argparse

    parser = argparse.ArgumentParser(
        description='Extrae conexiones entre estaciones desde rutas GeoJSON'
    )
    parser.add_argument(
        '--rutas',
        default='data/Servicios_(Rutas_Troncales_y_Zonales).geojson',
        help='Archivo GeoJSON de rutas'
    )
    parser.add_argument(
        '--estaciones',
        default='data/estaciones_completo.csv.backup',
        help='Archivo CSV con todas las estaciones (TM + SITP)'
    )
    parser.add_argument(
        '--output',
        default='data/rutas_generadas.csv',
        help='Archivo CSV de salida con rutas (formato: id,origen,destino,tiempoViaje,capacidad,distancia)'
    )
    parser.add_argument(
        '--umbral',
        type=float,
        default=0.3,
        help='Umbral de distancia en km para considerar estación en ruta (default: 0.3)'
    )
    parser.add_argument(
        '--muestreo',
        type=int,
        default=10,
        help='Revisar 1 de cada N puntos de cada ruta (default: 10)'
    )

    args = parser.parse_args()

    global DISTANCIA_UMBRAL_KM
    DISTANCIA_UMBRAL_KM = args.umbral

    print("="*70)
    print("  EXTRACTOR DE CONEXIONES ENTRE ESTACIONES")
    print("="*70)
    print(f"  Umbral de distancia: {DISTANCIA_UMBRAL_KM} km")
    print(f"  Factor de muestreo: {args.muestreo}")
    print("="*70)
    print()

    # 1. Cargar estaciones
    estaciones = cargar_estaciones(args.estaciones)

    # 2. Procesar rutas y encontrar conexiones
    conexiones = procesar_rutas(args.rutas, estaciones, args.muestreo)

    # 3. Guardar conexiones
    guardar_conexiones(conexiones, args.output)

    print("\n✨ ¡Proceso completado exitosamente!")
    print(f"\n💡 Ahora puedes cargar {args.output} en tu sistema de transporte")
    print(f"   para agregar las conexiones al grafo de Dijkstra.")


if __name__ == '__main__':
    main()
