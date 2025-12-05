#!/usr/bin/env python3
"""
Script para convertir el GeoJSON de SITP a CSV de estaciones.
Procesa paraderos SITP, filtra duplicados y asigna capacidades según tipo.
"""

import json
import csv
import sys
from pathlib import Path
from collections import defaultdict

def determine_sitp_capacity(tipo, modo, transporte):
    """
    Determina la capacidad basada en el tipo de paradero SITP.

    NTRTIPO: 1=Principal, 2=Secundario, 4=Paradero
    NTRMODO: 1=Bus, 2=SITP, 4=Tren
    NTRMTRANSP: Tipo de transporte específico
    """
    # Estaciones principales o intermodales
    if tipo == 1:
        if modo == 4:  # Tren
            return 4000
        return 3000  # Bus principal

    # Estaciones secundarias
    if tipo == 2:
        return 2500

    # Paraderos normales
    return 2000

def clean_name(nombre):
    """Limpia y normaliza el nombre de la estación."""
    if not nombre:
        return None

    # Remover prefijos comunes
    prefixes = ['Br. ', 'Pq. ', 'Pquia. ', 'Urb. ', 'IED ', 'Pl. ']
    for prefix in prefixes:
        if nombre.startswith(prefix):
            nombre = nombre[len(prefix):]

    return nombre.strip()

def main():
    # Rutas de archivos
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    # El archivo tiene caracteres especiales en el nombre
    geojson_file = project_root / 'data' / 'Paraderos_SITP_Bogot%C3%A1_D_C.geojson'
    csv_output = project_root / 'data' / 'estaciones_sitp.csv'

    print(f"📖 Leyendo GeoJSON desde: {geojson_file}")

    # Leer GeoJSON
    try:
        with open(geojson_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except FileNotFoundError:
        print(f"❌ Error: No se encontró el archivo {geojson_file}")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"❌ Error al parsear GeoJSON: {e}")
        sys.exit(1)

    features = data.get('features', [])
    if not features:
        print("❌ Error: No se encontraron paraderos en el GeoJSON")
        sys.exit(1)

    print(f"✅ Encontrados {len(features)} paraderos SITP")

    # Procesar paraderos
    stations = []
    seen_coords = set()  # Para detectar duplicados por coordenadas
    seen_names = defaultdict(int)  # Contador de nombres duplicados
    duplicates = 0
    skipped_no_coords = 0

    for idx, feature in enumerate(features, start=1):
        props = feature.get('properties', {})
        geom = feature.get('geometry', {})

        # Obtener propiedades
        codigo = props.get('NTRCODIGO', '').strip()
        nombre = props.get('NTRNOMBRE', '').strip()
        tipo = props.get('NTRTIPO', 4)  # Default: paradero normal
        modo = props.get('NTRMODO', 2)  # Default: SITP
        transporte = props.get('NTRMTRANSP', 6)  # Default: transporte general

        # Saltar si no tiene nombre
        if not nombre:
            continue

        # Coordenadas
        coords = geom.get('coordinates', [])
        if len(coords) < 2:
            skipped_no_coords += 1
            continue

        lon, lat = coords[0], coords[1]

        # Redondear coordenadas para detectar duplicados (5 decimales ~1m precisión)
        coord_key = (round(lat, 5), round(lon, 5))

        # Saltar duplicados exactos por ubicación
        if coord_key in seen_coords:
            duplicates += 1
            continue

        seen_coords.add(coord_key)

        # Limpiar nombre
        clean_nombre = clean_name(nombre)
        if not clean_nombre:
            clean_nombre = nombre

        # Manejar nombres duplicados agregando sufijo
        if clean_nombre in seen_names:
            seen_names[clean_nombre] += 1
            final_nombre = f"{clean_nombre} {seen_names[clean_nombre]}"
        else:
            seen_names[clean_nombre] = 1
            final_nombre = clean_nombre

        # Determinar capacidad
        capacidad = determine_sitp_capacity(tipo, modo, transporte)

        # Generar ID único para SITP
        if codigo:
            station_id = f"SITP{codigo}"
        else:
            station_id = f"SITP{str(idx).zfill(6)}"

        stations.append({
            'id': station_id,
            'nombre': final_nombre,
            'tipo': 'sitp',
            'latitud': round(lat, 6),
            'longitud': round(lon, 6),
            'capacidad': capacidad
        })

    print(f"📊 Procesados {len(stations)} paraderos únicos")
    print(f"   - {duplicates} duplicados omitidos (misma ubicación)")
    print(f"   - {skipped_no_coords} sin coordenadas válidas")

    # Ordenar por nombre
    stations.sort(key=lambda x: x['nombre'])

    # Escribir CSV
    print(f"💾 Escribiendo CSV en: {csv_output}")

    with open(csv_output, 'w', newline='', encoding='utf-8') as f:
        fieldnames = ['id', 'nombre', 'tipo', 'latitud', 'longitud', 'capacidad']
        writer = csv.DictWriter(f, fieldnames=fieldnames)

        writer.writeheader()
        writer.writerows(stations)

    print(f"✅ CSV generado exitosamente!")

    # Estadísticas por capacidad
    print("\n📈 Estadísticas por capacidad:")
    capacity_counts = defaultdict(int)
    for station in stations:
        capacity_counts[station['capacidad']] += 1

    for capacity, count in sorted(capacity_counts.items(), reverse=True):
        print(f"   - Capacidad {capacity}: {count} estaciones")

    # Mostrar algunas estaciones de ejemplo
    print("\n📝 Primeras 10 estaciones:")
    for station in stations[:10]:
        print(f"   {station['id']}: {station['nombre']} (cap: {station['capacidad']})")

    print(f"\n💡 Total de estaciones SITP en CSV: {len(stations)}")
    print("🎉 Conversión completada!")

if __name__ == '__main__':
    main()
