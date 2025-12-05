#!/usr/bin/env python3
"""
Script para convertir el JSON de TransMilenio a CSV de estaciones.
Genera automáticamente IDs, asigna capacidades según tipo de estación,
y mantiene compatibilidad con el sistema existente.
"""

import json
import csv
import sys
from pathlib import Path

def determine_station_type(nombre, num_est):
    """Determina el tipo de estación basado en el nombre y número."""
    nombre_lower = nombre.lower()

    # Portales (capacidad más alta)
    if 'portal' in nombre_lower:
        return 'portal', 8000

    # Estaciones intermodales (alta capacidad)
    if any(keyword in nombre_lower for keyword in ['intermodal', 'terminal']):
        return 'intermodal', 7000

    # Estaciones troncales normales
    return 'tm', 5000

def generate_station_id(num_est, index):
    """Genera un ID único para la estación."""
    # Usar el número de estación si está disponible, sino usar índice
    if num_est and num_est.strip():
        return f"TM{num_est}"
    return f"TM{str(index).zfill(3)}"

def main():
    # Rutas de archivos
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    json_file = project_root / 'data' / 'transmilenio.json'
    csv_file = project_root / 'data' / 'estaciones.csv'

    print(f"📖 Leyendo JSON desde: {json_file}")

    # Leer JSON
    try:
        with open(json_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except FileNotFoundError:
        print(f"❌ Error: No se encontró el archivo {json_file}")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"❌ Error al parsear JSON: {e}")
        sys.exit(1)

    features = data.get('features', [])
    if not features:
        print("❌ Error: No se encontraron estaciones en el JSON")
        sys.exit(1)

    print(f"✅ Encontradas {len(features)} estaciones")

    # Procesar estaciones
    stations = []
    seen_names = set()
    duplicates = 0

    for idx, feature in enumerate(features, start=1):
        attrs = feature.get('attributes', {})
        geom = feature.get('geometry', {})

        num_est = attrs.get('num_est', '').strip()
        nombre = attrs.get('nom_est', '').strip()

        # Saltar estaciones sin nombre
        if not nombre:
            continue

        # Detectar y manejar duplicados
        if nombre in seen_names:
            duplicates += 1
            print(f"⚠️  Duplicado detectado: {nombre} - Saltando")
            continue

        seen_names.add(nombre)

        # Coordenadas
        lat = geom.get('y', 0)
        lon = geom.get('x', 0)

        # Determinar tipo y capacidad
        tipo, capacidad = determine_station_type(nombre, num_est)

        # Generar ID
        station_id = generate_station_id(num_est, idx)

        stations.append({
            'id': station_id,
            'nombre': nombre,
            'tipo': tipo,
            'latitud': round(lat, 6),
            'longitud': round(lon, 6),
            'capacidad': capacidad
        })

    print(f"📊 Procesadas {len(stations)} estaciones únicas ({duplicates} duplicados omitidos)")

    # Ordenar por nombre para mejor legibilidad
    stations.sort(key=lambda x: x['nombre'])

    # Escribir CSV
    print(f"💾 Escribiendo CSV en: {csv_file}")

    with open(csv_file, 'w', newline='', encoding='utf-8') as f:
        fieldnames = ['id', 'nombre', 'tipo', 'latitud', 'longitud', 'capacidad']
        writer = csv.DictWriter(f, fieldnames=fieldnames)

        writer.writeheader()
        writer.writerows(stations)

    print(f"✅ CSV generado exitosamente!")

    # Estadísticas
    print("\n📈 Estadísticas:")
    print(f"   Total estaciones: {len(stations)}")

    # Contar por tipo
    type_counts = {}
    for station in stations:
        tipo = station['tipo']
        type_counts[tipo] = type_counts.get(tipo, 0) + 1

    for tipo, count in sorted(type_counts.items()):
        print(f"   - {tipo}: {count}")

    # Mostrar algunas estaciones de ejemplo
    print("\n📝 Primeras 5 estaciones:")
    for station in stations[:5]:
        print(f"   {station['id']}: {station['nombre']} ({station['tipo']}) - Cap: {station['capacidad']}")

    print("\n🎉 Conversión completada!")

if __name__ == '__main__':
    main()
