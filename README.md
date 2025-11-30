# 🚇 Sistema de Optimización de Transporte Público de Bogotá

Sistema para modelar, analizar y optimizar rutas del Metro, TransMilenio y SITP de Bogotá.

## 📋 Descripción

Herramienta completa que permite a planificadores de transporte público simular y optimizar las rutas y el flujo de pasajeros a través del nuevo metro, TransMilenio y sistema integrado de transporte público (SITP) en Bogotá.

## ✨ Características

### ✅ Implementadas

- **Base de Datos con Árboles B+**: Gestión eficiente de estaciones, rutas y líneas usando estructuras de datos avanzadas
- **Algoritmos de Optimización**:
  - ✅ **Dijkstra**: Cálculo de caminos mínimos entre estaciones
  - ✅ **Edmonds-Karp**: Análisis de flujos máximos (capacidad de red)
  - ✅ **Kruskal**: Árbol de recubrimiento mínimo
  - ✅ **Graph Coloring**: Asignación de recursos
- **Persistencia CSV**: Carga y almacenamiento de datos del sistema
- **API REST**: Endpoints para consultar estaciones, rutas y calcular caminos óptimos
- **Interfaz Web Interactiva**: Visualización en mapa con Leaflet/OpenStreetMap
- **Datos Reales**: Estaciones y rutas del Metro, TransMilenio y SITP

## 🏗️ Arquitectura

```
proyecto-transporte-bogota/
├── src/main/java/com/transporte/bogota/
│   ├── algorithm/          # Algoritmos (Dijkstra, MaxFlow, etc.)
│   ├── controller/         # Controladores REST
│   ├── dao/                # Acceso a datos (CSV)
│   ├── model/              # Modelos de dominio
│   ├── service/            # Lógica de negocio
│   ├── util/               # Utilidades (Graph, GraphEdge)
│   └── TransporteBogotaApplication.java
├── src/main/resources/
│   ├── static/             # Frontend web
│   │   ├── index.html
│   │   └── app.js
│   └── application.properties
├── data/                   # Datos CSV
│   ├── estaciones.csv
│   ├── rutas.csv
│   └── lineas.csv
└── pom.xml                 # Configuración Maven
```

## 🛠️ Tecnologías

- **Backend**: Java 17, Spring Boot 3.2.1
- **Frontend**: HTML5, CSS3, JavaScript, Leaflet.js
- **Datos**: Apache Commons CSV
- **Build**: Maven
- **Algoritmos**: Dijkstra, Edmonds-Karp, Kruskal, Graph Coloring

## 📦 Instalación

### Prerrequisitos

- Java 17 o superior
- Maven 3.6+
- Puerto 8080 disponible

### Pasos

1. **Clonar el repositorio**
```bash
git clone <repository-url>
cd proyecto-transporte-bogota
```

2. **Compilar el proyecto**
```bash
mvn clean install
```

3. **Ejecutar la aplicación**
```bash
mvn spring-boot:run
```

4. **Abrir en el navegador**
```
http://localhost:8080
```

## 🚀 Uso

### Interfaz Web

1. Abre `http://localhost:8080` en tu navegador
2. Verás un mapa interactivo de Bogotá con todas las estaciones
3. Selecciona una estación de **origen** y una de **destino**
4. Haz clic en **"Calcular Ruta Más Corta"**
5. La ruta óptima se mostrará en el mapa y en el panel lateral

### API REST

#### Obtener todas las estaciones
```bash
GET http://localhost:8080/api/estaciones
```

#### Obtener una estación específica
```bash
GET http://localhost:8080/api/estaciones/E001
```

#### Calcular ruta óptima
```bash
GET http://localhost:8080/api/ruta-optima?origen=E001&destino=E015
```

Respuesta:
```json
{
  "origen": {
    "id": "E001",
    "nombre": "Autopista Sur",
    "tipo": "metro",
    "latitud": 4.5708,
    "longitud": -74.1374
  },
  "destino": {
    "id": "E015",
    "nombre": "Calle 10",
    "tipo": "metro",
    "latitud": 4.6791,
    "longitud": -74.1040
  },
  "tiempoTotal": 42,
  "numeroEstaciones": 15,
  "camino": [...]
}
```

#### Obtener estadísticas
```bash
GET http://localhost:8080/api/estadisticas
```

#### Obtener todas las líneas
```bash
GET http://localhost:8080/api/lineas
```

#### Health check
```bash
GET http://localhost:8080/api/health
```

## 📊 Datos del Sistema

### Estaciones

- **Metro**: 15 estaciones (Línea 1)
- **TransMilenio**: 15 portales y estaciones principales
- **SITP**: 10 puntos de conexión
- **Intermodales**: 3 estaciones de transferencia

### Tipos de Estaciones

| Tipo | Color en Mapa | Descripción |
|------|---------------|-------------|
| Metro | 🔵 Azul | Estaciones del Metro de Bogotá |
| TransMilenio | 🔴 Rojo | Portales y estaciones TM |
| SITP | 🟢 Verde | Puntos de conexión SITP |
| Intermodal | 🟠 Naranja | Estaciones de transferencia |

## 🧪 Testing

```bash
mvn test
```

## 📈 Algoritmos Implementados

### 1. Dijkstra (Camino Más Corto)
- **Uso**: Calcular la ruta óptima entre dos estaciones
- **Complejidad**: O((V+E) log V)
- **Peso**: Tiempo de viaje en minutos

### 2. Edmonds-Karp (Flujo Máximo)
- **Uso**: Analizar capacidad de la red y congestión
- **Complejidad**: O(VE²)
- **Aplicación**: Identificar cuellos de botella

### 3. Kruskal (Árbol de Recubrimiento Mínimo)
- **Uso**: Optimizar conexiones entre sistemas
- **Complejidad**: O(E log E)
- **Aplicación**: Reducir tiempos de transferencia

### 4. Graph Coloring (Coloreado de Grafos)
- **Uso**: Asignación de frecuencias de servicio
- **Complejidad**: O(V²)
- **Aplicación**: Optimizar horarios

## 🔧 Configuración

Editar `src/main/resources/application.properties`:

```properties
# Puerto del servidor
server.port=8080

# Nivel de logging
logging.level.com.transporte.bogota=DEBUG
```

## 📁 Formato de Datos CSV

### estaciones.csv
```csv
id,nombre,tipo,latitud,longitud,capacidad
E001,Autopista Sur,metro,4.5708,-74.1374,3000
```

### rutas.csv
```csv
id,origen,destino,tiempoViaje,capacidad,distancia
R001,E001,E002,3,3000,840
```

### lineas.csv
```csv
id,nombre,tipo,estaciones
L001,Línea 1 Metro,metro,"E001,E002,E003"
```

## 🎯 Próximas Características

- [ ] Simulación de flujo de pasajeros con demanda variable
- [ ] Análisis de congestión en horas pico
- [ ] Algoritmos genéticos para optimización global
- [ ] Integración con datos en tiempo real
- [ ] Dashboard de analíticas avanzadas
- [ ] Exportación de reportes PDF
- [ ] API GraphQL
- [ ] Tests unitarios y de integración

## 👥 Contribuir

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto es de código abierto y está disponible bajo la licencia MIT.

## 📞 Contacto

Para preguntas o sugerencias, por favor abre un issue en el repositorio.

---

**Desarrollado para la optimización del sistema de transporte público de Bogotá** 🚇🚌🚎