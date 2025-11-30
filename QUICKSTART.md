# 🚀 Guía Rápida de Inicio

## Paso 1: Verificar Prerrequisitos

```bash
# Verificar Java (mínimo 17)
java --version

# Verificar Maven
mvn --version
```

## Paso 2: Compilar el Proyecto

```bash
cd proyecto-transporte-bogota
mvn clean install
```

## Paso 3: Ejecutar la Aplicación

```bash
mvn spring-boot:run
```

Verás algo como:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.1)

...
INFO  Tomcat started on port(s): 8080 (http)
INFO  Started TransporteBogotaApplication in X.XXX seconds
```

## Paso 4: Abrir en el Navegador

Abre tu navegador en:
```
http://localhost:8080
```

## Paso 5: Probar la Aplicación

### Interfaz Web

1. Verás un mapa interactivo de Bogotá
2. En el panel izquierdo hay selectores de origen y destino
3. Selecciona dos estaciones diferentes
4. Haz clic en "Calcular Ruta Más Corta"
5. La ruta se mostrará en el mapa con color morado

### API REST

Prueba los endpoints con curl o Postman:

```bash
# Obtener todas las estaciones
curl http://localhost:8080/api/estaciones

# Obtener estadísticas
curl http://localhost:8080/api/estadisticas

# Calcular ruta óptima
curl "http://localhost:8080/api/ruta-optima?origen=E001&destino=E015"
```

## Personalizar Datos

Los archivos CSV están en `data/`:
- `estaciones.csv` - Estaciones del sistema
- `rutas.csv` - Conexiones entre estaciones
- `lineas.csv` - Líneas de transporte

Edita estos archivos y reinicia la aplicación para ver los cambios.

## Detener la Aplicación

Presiona `Ctrl+C` en la terminal donde está corriendo.

## Solución de Problemas

### Puerto 8080 ocupado
Edita `src/main/resources/application.properties`:
```properties
server.port=9090
```

### Error al cargar datos CSV
Verifica que la carpeta `data/` existe y contiene los 3 archivos CSV.

### Página en blanco
Verifica la consola del navegador (F12) y los logs de la aplicación.

---

¡Listo! 🎉 Tu sistema de transporte está funcionando.
