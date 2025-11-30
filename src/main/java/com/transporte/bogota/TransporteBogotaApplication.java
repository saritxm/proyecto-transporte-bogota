package com.transporte.bogota;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicación principal del Sistema de Optimización de Transporte Público de Bogotá.
 *
 * Sistema para modelar, analizar y optimizar rutas del Metro, TransMilenio y SITP.
 */
@SpringBootApplication
public class TransporteBogotaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransporteBogotaApplication.class, args);
    }
}
