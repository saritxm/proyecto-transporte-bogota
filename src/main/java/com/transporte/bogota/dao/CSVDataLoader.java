package com.transporte.bogota.dao;

import com.transporte.bogota.model.Estacion;
import com.transporte.bogota.model.Linea;
import com.transporte.bogota.model.Ruta;
import com.transporte.bogota.model.SistemaTransporte;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;

/**
 * Cargador de datos desde archivos CSV para el sistema de transporte.
 */
@Repository
public class CSVDataLoader {

    private static final Logger logger = LoggerFactory.getLogger(CSVDataLoader.class);

    private final String dataPath;

    public CSVDataLoader() {
        this.dataPath = "data";
    }

    public CSVDataLoader(String dataPath) {
        this.dataPath = dataPath;
    }

    public SistemaTransporte cargarDatos() throws IOException {
        SistemaTransporte sistema = new SistemaTransporte();
        logger.info("Iniciando carga de datos desde: {}", dataPath);

        cargarEstaciones(sistema);
        cargarRutas(sistema);
        cargarLineas(sistema);

        logger.info("Carga completa - Estaciones: {}, Rutas: {}, Líneas: {}",
            sistema.getAllEstaciones().size(),
            sistema.getAllRutas().size(),
            sistema.getAllLineas().size());

        return sistema;
    }

    private void cargarEstaciones(SistemaTransporte sistema) throws IOException {
        String archivo = dataPath + "/estaciones.csv";
        logger.info("Cargando estaciones desde: {}", archivo);

        try (Reader reader = new FileReader(archivo);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            for (CSVRecord record : csvParser) {
                Estacion estacion = new Estacion();
                estacion.setId(record.get("id"));
                estacion.setNombre(record.get("nombre"));
                estacion.setTipo(record.get("tipo"));
                estacion.setLatitud(Double.parseDouble(record.get("latitud")));
                estacion.setLongitud(Double.parseDouble(record.get("longitud")));
                estacion.setCapacidad(Integer.parseInt(record.get("capacidad")));

                sistema.addEstacion(estacion);
            }

            logger.info("Estaciones cargadas: {}", sistema.getAllEstaciones().size());
        }
    }

    private void cargarRutas(SistemaTransporte sistema) throws IOException {
        String archivo = dataPath + "/rutas.csv";
        logger.info("Cargando rutas desde: {}", archivo);

        try (Reader reader = new FileReader(archivo);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            for (CSVRecord record : csvParser) {
                String id = record.get("id");
                String origenId = record.get("origen");
                String destinoId = record.get("destino");

                Estacion origen = sistema.getEstacion(origenId);
                Estacion destino = sistema.getEstacion(destinoId);

                if (origen != null && destino != null) {
                    Ruta ruta = new Ruta();
                    ruta.setId(id);
                    ruta.setOrigen(origen);
                    ruta.setDestino(destino);
                    ruta.setTiempoViaje(Integer.parseInt(record.get("tiempoViaje")));
                    ruta.setCapacidad(Integer.parseInt(record.get("capacidad")));
                    ruta.setDistanciaM(Double.parseDouble(record.get("distancia")));

                    sistema.addRuta(ruta);
                }
            }

            logger.info("Rutas cargadas: {}", sistema.getAllRutas().size());
        }
    }

    private void cargarLineas(SistemaTransporte sistema) throws IOException {
        String archivo = dataPath + "/lineas.csv";
        logger.info("Cargando líneas desde: {}", archivo);

        try (Reader reader = new FileReader(archivo);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            for (CSVRecord record : csvParser) {
                Linea linea = new Linea();
                linea.setId(record.get("id"));
                linea.setNombre(record.get("nombre"));
                linea.setTipo(record.get("tipo"));

                List<String> estacionIds = Arrays.asList(record.get("estaciones").split(","));
                for (String estacionId : estacionIds) {
                    Estacion estacion = sistema.getEstacion(estacionId.trim());
                    if (estacion != null) {
                        linea.addEstacion(estacion);
                    }
                }

                sistema.addLinea(linea);
            }

            logger.info("Líneas cargadas: {}", sistema.getAllLineas().size());
        }
    }
}
