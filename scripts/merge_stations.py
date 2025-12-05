#!/usr/bin/env python3
"""
Script para combinar todas las estaciones (TransMilenio, Metro, SITP) en un solo CSV.
Lee estaciones.csv (TM + Metro) y estaciones_sitp.csv y los combina.
"""

import csv
from pathlib import Path

def main():
    script_dir = Path(__file__).parent
    project_root = script_dir.parent

    # Archivos de entrada
    tm_metro_file = project_root / 'data' / 'estaciones.csv'
    sitp_file = project_root / 'data' / 'estaciones_sitp.csv'

    # Archivo de salida consolidado
    output_file = project_root / 'data' / 'estaciones_todas.csv'

    print("🔄 Combinando estaciones...")

    all_stations = []
    stats = {'tm': 0, 'portal': 0, 'intermodal': 0, 'metro': 0, 'sitp': 0}

    # Leer estaciones TM + Metro
    print(f"📖 Leyendo TransMilenio y Metro desde: {tm_metro_file}")
    with open(tm_metro_file, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            all_stations.append(row)
            tipo = row['tipo']
            stats[tipo] = stats.get(tipo, 0) + 1

    tm_metro_count = len(all_stations)
    print(f"   ✅ {tm_metro_count} estaciones TM/Metro cargadas")

    # Leer estaciones SITP
    print(f"📖 Leyendo SITP desde: {sitp_file}")
    with open(sitp_file, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            all_stations.append(row)
            stats['sitp'] += 1

    sitp_count = stats['sitp']
    print(f"   ✅ {sitp_count} estaciones SITP cargadas")

    # Escribir archivo consolidado
    print(f"💾 Escribiendo archivo consolidado: {output_file}")
    with open(output_file, 'w', newline='', encoding='utf-8') as f:
        fieldnames = ['id', 'nombre', 'tipo', 'latitud', 'longitud', 'capacidad']
        writer = csv.DictWriter(f, fieldnames=fieldnames)

        writer.writeheader()
        writer.writerows(all_stations)

    print(f"✅ Archivo consolidado generado!")
    print(f"\n📊 Estadísticas finales:")
    print(f"   - TransMilenio (tm): {stats.get('tm', 0)}")
    print(f"   - Portales: {stats.get('portal', 0)}")
    print(f"   - Intermodal: {stats.get('intermodal', 0)}")
    print(f"   - Metro: {stats.get('metro', 0)}")
    print(f"   - SITP: {stats.get('sitp', 0)}")
    print(f"\n   🎯 TOTAL: {len(all_stations)} estaciones")

    print(f"\n💡 Archivos generados:")
    print(f"   - {tm_metro_file.name} ({tm_metro_count} estaciones)")
    print(f"   - {sitp_file.name} ({sitp_count} estaciones)")
    print(f"   - {output_file.name} ({len(all_stations)} estaciones) ← CONSOLIDADO")

    print("\n🎉 ¡Proceso completado!")

if __name__ == '__main__':
    main()
