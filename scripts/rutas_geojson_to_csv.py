#!/usr/bin/env python3
"""
Script para convertir GeoJSON de rutas de transporte a CSV.
Simplifica las geometrías para reducir el tamaño del archivo.
"""

import json
import csv
import sys
from typing import List, Tuple

def simplificar_coordenadas(coords: List[List[float]], factor_simplificacion: int = 5) -> str:
    """
    Simplifica una lista de coordenadas tomando 1 de cada N puntos.
    Siempre incluye el primer y último punto para mantener la ruta completa.

    Args:
        coords: Lista de coordenadas [lon, lat, alt]
        factor_simplificacion: Tomar 1 de cada N puntos (default: 5)

    Returns:
        String con coordenadas simplificadas en formato "lat,lon;lat,lon;..."
    """
    if not coords:
        return ""

    # Siempre incluir primer y último punto
    simplified = [coords[0]]

    # Tomar puntos intermedios cada N pasos
    for i in range(factor_simplificacion, len(coords) - 1, factor_simplificacion):
        simplified.append(coords[i])

    # Asegurar que el último punto esté incluido
    if coords[-1] not in simplified:
        simplified.append(coords[-1])

    # Convertir a formato "lat,lon;lat,lon" (intercambiamos lon,lat a lat,lon)
    # GeoJSON usa [lon, lat], pero nosotros queremos [lat, lon]
    coord_strings = [f"{coord[1]},{coord[0]}" for coord in simplified]

    return ";".join(coord_strings)


def calcular_distancia_aprox(coords: List[List[float]]) -> float:
    """
    Calcula una distancia aproximada sumando las distancias euclidianas.
    Para mejor precisión, se debería usar Haversine, pero esto es suficiente.

    Args:
        coords: Lista de coordenadas [lon, lat, alt]

    Returns:
        Distancia aproximada en kilómetros
    """
    if len(coords) < 2:
        return 0.0

    total_dist = 0.0
    for i in range(len(coords) - 1):
        lon1, lat1 = coords[i][0], coords[i][1]
        lon2, lat2 = coords[i + 1][0], coords[i + 1][1]

        # Aproximación simple (grados a km: ~111 km por grado)
        dlat = (lat2 - lat1) * 111.0
        dlon = (lon2 - lon1) * 111.0 * 0.9  # Factor de corrección para Bogotá
        dist = (dlat**2 + dlon**2)**0.5
        total_dist += dist

    return round(total_dist, 2)


def convertir_geojson_a_csv(archivo_geojson: str, archivo_csv: str,
                            factor_simplificacion: int = 5, max_rutas: int = None):
    """
    Convierte archivo GeoJSON de rutas a CSV.

    Args:
        archivo_geojson: Path del archivo GeoJSON de entrada
        archivo_csv: Path del archivo CSV de salida
        factor_simplificacion: Factor de simplificación de coordenadas (1=sin simplificar, 5=muy simplificado)
        max_rutas: Límite máximo de rutas a procesar (None = todas)
    """
    print(f"📂 Leyendo archivo: {archivo_geojson}")

    try:
        with open(archivo_geojson, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except Exception as e:
        print(f"❌ Error al leer archivo GeoJSON: {e}")
        sys.exit(1)

    features = data.get('features', [])
    total_rutas = len(features)

    print(f"📊 Total de rutas encontradas: {total_rutas}")

    if max_rutas:
        features = features[:max_rutas]
        print(f"⚠️  Limitando a {max_rutas} rutas")

    # Abrir archivo CSV para escribir
    print(f"📝 Escribiendo archivo: {archivo_csv}")

    try:
        with open(archivo_csv, 'w', newline='', encoding='utf-8') as csvfile:
            fieldnames = [
                'id_ruta',
                'codigo_ruta',
                'codigo_linea',
                'nombre_ruta',
                'origen',
                'destino',
                'operador',
                'tipo_servicio',
                'tipo_bus',
                'horario_habil',
                'horario_sabado',
                'horario_festivo',
                'longitud_km',
                'puntos_originales',
                'puntos_simplificados',
                'coordenadas'  # lat,lon;lat,lon;...
            ]

            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()

            rutas_procesadas = 0
            rutas_sin_geometria = 0

            for feature in features:
                props = feature.get('properties', {})
                geom = feature.get('geometry', {})

                # Verificar que tenga geometría
                if not geom or geom.get('type') != 'LineString':
                    rutas_sin_geometria += 1
                    continue

                coords = geom.get('coordinates', [])

                if not coords:
                    rutas_sin_geometria += 1
                    continue

                # Extraer propiedades
                id_ruta = props.get('route_id', '')
                codigo_ruta = props.get('cod_ruta', '')
                codigo_linea = props.get('cod_linea', '')
                nombre_ruta = props.get('nom_ruta', '')
                origen = props.get('orig_ruta', '')
                destino = props.get('dest_ruta', '')
                operador = props.get('oper_ruta', '')
                tipo_servicio = props.get('tip_serv', '')
                tipo_bus = props.get('tip_bus', '')
                horario_habil = props.get('hor_habil', '')
                horario_sabado = props.get('hor_sab', '')
                horario_festivo = props.get('hor_fest', '')
                longitud_km = props.get('long_ruta', 0)

                # Simplificar coordenadas
                coords_simplificadas = simplificar_coordenadas(coords, factor_simplificacion)

                # Contar puntos
                puntos_originales = len(coords)
                puntos_simplificados = len(coords_simplificadas.split(';'))

                # Escribir fila
                writer.writerow({
                    'id_ruta': id_ruta,
                    'codigo_ruta': codigo_ruta,
                    'codigo_linea': codigo_linea,
                    'nombre_ruta': nombre_ruta,
                    'origen': origen,
                    'destino': destino,
                    'operador': operador,
                    'tipo_servicio': tipo_servicio,
                    'tipo_bus': tipo_bus,
                    'horario_habil': horario_habil,
                    'horario_sabado': horario_sabado,
                    'horario_festivo': horario_festivo,
                    'longitud_km': longitud_km,
                    'puntos_originales': puntos_originales,
                    'puntos_simplificados': puntos_simplificados,
                    'coordenadas': coords_simplificadas
                })

                rutas_procesadas += 1

                # Mostrar progreso cada 100 rutas
                if rutas_procesadas % 100 == 0:
                    porcentaje = (rutas_procesadas / len(features)) * 100
                    print(f"  ⏳ Procesadas {rutas_procesadas}/{len(features)} rutas ({porcentaje:.1f}%)")

            print(f"\n✅ Conversión completada:")
            print(f"   - Rutas procesadas: {rutas_procesadas}")
            print(f"   - Rutas sin geometría: {rutas_sin_geometria}")
            print(f"   - Archivo generado: {archivo_csv}")

            # Calcular reducción de tamaño
            if rutas_procesadas > 0:
                ejemplo = features[0]
                coords_ejemplo = ejemplo.get('geometry', {}).get('coordinates', [])
                if coords_ejemplo:
                    reduccion = (1 - len(simplificar_coordenadas(coords_ejemplo, factor_simplificacion).split(';')) / len(coords_ejemplo)) * 100
                    print(f"   - Reducción de puntos: ~{reduccion:.1f}%")

    except Exception as e:
        print(f"❌ Error al escribir archivo CSV: {e}")
        sys.exit(1)


def main():
    """Función principal del script."""
    import argparse

    parser = argparse.ArgumentParser(
        description='Convierte GeoJSON de rutas de transporte a CSV con simplificación de geometrías'
    )
    parser.add_argument(
        '--input', '-i',
        default='data/Servicios_(Rutas_Troncales_y_Zonales).geojson',
        help='Archivo GeoJSON de entrada (default: data/Servicios_(Rutas_Troncales_y_Zonales).geojson)'
    )
    parser.add_argument(
        '--output', '-o',
        default='data/rutas_transporte.csv',
        help='Archivo CSV de salida (default: data/rutas_transporte.csv)'
    )
    parser.add_argument(
        '--simplificacion', '-s',
        type=int,
        default=5,
        help='Factor de simplificación: toma 1 de cada N puntos (default: 5, más alto = más simple)'
    )
    parser.add_argument(
        '--max-rutas', '-m',
        type=int,
        default=None,
        help='Número máximo de rutas a procesar (default: todas)'
    )

    args = parser.parse_args()

    print("="*70)
    print("  CONVERSOR DE RUTAS GEOJSON A CSV")
    print("="*70)
    print()

    convertir_geojson_a_csv(
        args.input,
        args.output,
        args.simplificacion,
        args.max_rutas
    )

    print("\n✨ ¡Proceso completado exitosamente!")


if __name__ == '__main__':
    main()
