// Configuración de la API
const API_URL = 'http://localhost:8080/api';

// Inicializar mapa centrado en Bogotá
const map = L.map('map').setView([4.6533, -74.0836], 12);

// Agregar capa de OpenStreetMap
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors',
    maxZoom: 19
}).addTo(map);

// Almacenamiento de datos
let estaciones = [];
let rutas = [];
let markers = {};
let rutaLayer = null;

// Colores por tipo de estación
const COLORES = {
    'metro': '#2196F3',
    'tm': '#F44336',
    'sitp': '#4CAF50',
    'intermodal': '#FF9800'
};

// Inicializar aplicación
async function init() {
    showLoading(true);
    try {
        await cargarEstadisticas();
        await cargarEstaciones();
        await cargarRutas();
        dibujarEstaciones();
        dibujarRutas();
    } catch (error) {
        console.error('Error al inicializar:', error);
        alert('Error al cargar datos del sistema');
    } finally {
        showLoading(false);
    }
}

// Mostrar/ocultar loading
function showLoading(show) {
    const loading = document.getElementById('loading');
    if (show) {
        loading.classList.add('active');
    } else {
        loading.classList.remove('active');
    }
}

// Cargar estadísticas
async function cargarEstadisticas() {
    const response = await fetch(`${API_URL}/estadisticas`);
    const stats = await response.json();

    const grid = document.getElementById('stats-grid');
    grid.innerHTML = `
        <div class="stat-card">
            <div class="stat-value">${stats.totalEstaciones}</div>
            <div class="stat-label">Estaciones</div>
        </div>
        <div class="stat-card">
            <div class="stat-value">${stats.totalRutas}</div>
            <div class="stat-label">Rutas</div>
        </div>
        <div class="stat-card">
            <div class="stat-value">${stats.totalLineas}</div>
            <div class="stat-label">Líneas</div>
        </div>
        <div class="stat-card">
            <div class="stat-value">${(stats.capacidadTotal / 1000).toFixed(0)}K</div>
            <div class="stat-label">Capacidad Total</div>
        </div>
    `;
}

// Cargar estaciones
async function cargarEstaciones() {
    const response = await fetch(`${API_URL}/estaciones`);
    estaciones = await response.json();

    // Llenar selectores
    const origenSelect = document.getElementById('origen');
    const destinoSelect = document.getElementById('destino');

    estaciones.forEach(estacion => {
        const option1 = new Option(`${estacion.nombre} (${estacion.tipo.toUpperCase()})`, estacion.id);
        const option2 = new Option(`${estacion.nombre} (${estacion.tipo.toUpperCase()})`, estacion.id);

        origenSelect.add(option1);
        destinoSelect.add(option2);
    });
}

// Cargar rutas
async function cargarRutas() {
    const response = await fetch(`${API_URL}/rutas`);
    rutas = await response.json();
}

// Dibujar estaciones en el mapa
function dibujarEstaciones() {
    estaciones.forEach(estacion => {
        const marker = L.circleMarker([estacion.latitud, estacion.longitud], {
            radius: 8,
            fillColor: COLORES[estacion.tipo] || '#999',
            color: '#fff',
            weight: 2,
            opacity: 1,
            fillOpacity: 0.8
        }).addTo(map);

        marker.bindPopup(`
            <b>${estacion.nombre}</b><br>
            <b>Tipo:</b> ${estacion.tipo.toUpperCase()}<br>
            <b>Capacidad:</b> ${estacion.capacidad} pasajeros<br>
            <b>ID:</b> ${estacion.id}
        `);

        markers[estacion.id] = marker;
    });
}

// Dibujar rutas en el mapa (solo las principales para no saturar)
function dibujarRutas() {
    rutas.forEach(ruta => {
        const origen = estaciones.find(e => e.id === ruta.origen.id);
        const destino = estaciones.find(e => e.id === ruta.destino.id);

        if (origen && destino) {
            L.polyline(
                [[origen.latitud, origen.longitud], [destino.latitud, destino.longitud]],
                {
                    color: COLORES[origen.tipo] || '#999',
                    weight: 2,
                    opacity: 0.3
                }
            ).addTo(map);
        }
    });
}

// Calcular ruta óptima
async function calcularRuta() {
    const origenId = document.getElementById('origen').value;
    const destinoId = document.getElementById('destino').value;

    if (!origenId || !destinoId) {
        alert('Por favor seleccione origen y destino');
        return;
    }

    if (origenId === destinoId) {
        alert('Origen y destino deben ser diferentes');
        return;
    }

    const btn = document.getElementById('calcular-btn');
    btn.disabled = true;
    btn.textContent = 'Calculando...';

    try {
        const response = await fetch(`${API_URL}/ruta-optima?origen=${origenId}&destino=${destinoId}`);
        const resultado = await response.json();

        mostrarResultado(resultado);
        dibujarRutaOptima(resultado);
    } catch (error) {
        console.error('Error al calcular ruta:', error);
        alert('Error al calcular la ruta');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Calcular Ruta Más Corta';
    }
}

// Mostrar resultado de ruta
function mostrarResultado(resultado) {
    const resultadoDiv = document.getElementById('resultado');

    const caminoHTML = resultado.camino.map((estacion, index) => `
        <div class="station-item">
            ${index + 1}. ${estacion.nombre} <span style="color: ${COLORES[estacion.tipo]}">(${estacion.tipo.toUpperCase()})</span>
        </div>
    `).join('');

    resultadoDiv.innerHTML = `
        <div class="result-box">
            <h4>✅ Ruta Calculada</h4>
            <p><b>Tiempo total:</b> ${resultado.tiempoTotal} minutos</p>
            <p><b>Estaciones:</b> ${resultado.numeroEstaciones}</p>
            <p><b>Transferencias:</b> ${calcularTransferencias(resultado.camino)}</p>

            <div class="station-list">
                <b>Recorrido:</b>
                ${caminoHTML}
            </div>
        </div>
    `;

    resultadoDiv.style.display = 'block';
}

// Calcular número de transferencias
function calcularTransferencias(camino) {
    let transferencias = 0;
    for (let i = 1; i < camino.length; i++) {
        if (camino[i].tipo !== camino[i - 1].tipo) {
            transferencias++;
        }
    }
    return transferencias;
}

// Dibujar ruta óptima en el mapa
function dibujarRutaOptima(resultado) {
    // Eliminar ruta anterior si existe
    if (rutaLayer) {
        map.removeLayer(rutaLayer);
    }

    const coordinates = resultado.camino.map(est => [est.latitud, est.longitud]);

    rutaLayer = L.polyline(coordinates, {
        color: '#9C27B0',
        weight: 5,
        opacity: 0.8
    }).addTo(map);

    // Resaltar estaciones de la ruta
    resultado.camino.forEach(estacion => {
        const marker = markers[estacion.id];
        if (marker) {
            marker.setStyle({
                radius: 12,
                fillColor: '#9C27B0',
                weight: 3
            });
            marker.bringToFront();
        }
    });

    // Ajustar vista del mapa a la ruta
    map.fitBounds(rutaLayer.getBounds(), { padding: [50, 50] });
}

// Iniciar aplicación cuando cargue la página
document.addEventListener('DOMContentLoaded', init);
