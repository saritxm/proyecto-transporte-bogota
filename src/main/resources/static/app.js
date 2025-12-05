// Configuración de la API
const API_URL = 'http://localhost:8080/api/transporte';

// Inicializar mapa centrado en Bogotá
const map = L.map('map').setView([4.6533, -74.0836], 12);

// Agregar capa de OpenStreetMap
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors',
    maxZoom: 19
}).addTo(map);

// Almacenamiento de datos
let estacionesPrincipales = [];
let markers = {};
let markersLayer = L.layerGroup().addTo(map);
let rutaLayer = null;
let cuellosLayer = null;

// Colores por tipo de estación
const COLORES = {
    'metro': '#2196F3',
    'portal': '#9C27B0',
    'tm': '#F44336',
    'sitp': '#4CAF50',
    'intermodal': '#FF9800'
};

// =========================================================================
// FUNCIONES DE INICIALIZACIÓN Y CARGA DE DATOS
// =========================================================================

async function init() {
    try {
        console.log('Inicializando aplicación...');
        await cargarEstadisticas();
        await cargarEstacionesPrincipales();
        configurarBuscador();
        configurarEventos();
        console.log('Aplicación inicializada');
    } catch (error) {
        console.error('Error al inicializar:', error);
        alert('Error al cargar datos: ' + error.message);
    }
}

// Cargar estadísticas
async function cargarEstadisticas() {
    try {
        console.log('Cargando estadísticas...');
        const response = await fetch(`${API_URL}/estadisticas`);

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const stats = await response.json();
        console.log('Estadísticas recibidas:', stats);

        const grid = document.getElementById('stats-grid');
        if (!grid) {
            console.error('Elemento stats-grid no encontrado en el DOM');
            return;
        }

        grid.innerHTML = `
            <div class="p-3 bg-blue-50 rounded-lg">
                <div class="text-2xl font-bold text-blue-700">${stats.totalEstaciones || 0}</div>
                <div class="text-xs text-gray-600">Estaciones</div>
            </div>
            <div class="p-3 bg-green-50 rounded-lg">
                <div class="text-2xl font-bold text-green-700">${stats.totalRutas || 0}</div>
                <div class="text-xs text-gray-600">Rutas</div>
            </div>
            <div class="p-3 bg-purple-50 rounded-lg">
                <div class="text-2xl font-bold text-purple-700">${(stats.capacidadTotal / 1000000).toFixed(1)}M</div>
                <div class="text-xs text-gray-600">Capacidad</div>
            </div>
            <div class="p-3 bg-amber-50 rounded-lg">
                <div class="text-2xl font-bold text-amber-700">${stats.totalLineas || 0}</div>
                <div class="text-xs text-gray-600">Líneas</div>
            </div>
        `;

        console.log('Estadísticas cargadas correctamente');
    } catch (error) {
        console.error('Error al cargar estadísticas:', error);
        const grid = document.getElementById('stats-grid');
        if (grid) {
            grid.innerHTML = '<div class="col-span-2 text-red-500 text-sm p-2">Error al cargar estadísticas</div>';
        }
    }
}

// Cargar solo estaciones principales
async function cargarEstacionesPrincipales() {
    console.log('Cargando estaciones principales...');

    try {
        const response = await fetch(`${API_URL}/estaciones`);

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        estacionesPrincipales = await response.json();
        console.log('Estaciones cargadas:', estacionesPrincipales.length);

        dibujarEstaciones(estacionesPrincipales);
        llenarSelectores(estacionesPrincipales);
    } catch (error) {
        console.error('Error al cargar estaciones:', error);
        alert('Error al cargar estaciones: ' + error.message);
    }
}

function llenarSelectores(estaciones) {
    const selectOrigen = document.getElementById('select-origen');
    const selectDestino = document.getElementById('select-destino');

    selectOrigen.innerHTML = '<option value="" disabled selected>Seleccione Origen</option>';
    selectDestino.innerHTML = '<option value="" disabled selected>Seleccione Destino</option>';

    estaciones.forEach(est => {
        const opt1 = new Option(`${est.nombre} (${est.tipo})`, est.id);
        const opt2 = new Option(`${est.nombre} (${est.tipo})`, est.id);
        selectOrigen.add(opt1);
        selectDestino.add(opt2);
    });
}

// =========================================================================
// DIBUJO DE ESTACIONES Y CAPAS
// =========================================================================

function dibujarEstaciones(estaciones) {
    markersLayer.clearLayers();
    markers = {};

    estaciones.forEach(estacion => {
        const color = COLORES[estacion.tipo] || '#666';
        const radius = estacion.tipo === 'portal' ? 8 :
                      estacion.tipo === 'intermodal' ? 7 :
                      estacion.tipo === 'metro' ? 6 : 5;

        const marker = L.circleMarker([estacion.latitud, estacion.longitud], {
            radius: radius,
            fillColor: color,
            color: '#fff',
            weight: 2,
            opacity: 1,
            fillOpacity: 0.8
        });

        marker.bindPopup(`
            <div class="font-semibold">${estacion.nombre}</div>
            <div class="text-xs text-gray-600">${estacion.tipo.toUpperCase()} - ${estacion.id}</div>
        `);

        marker.addTo(markersLayer);
        markers[estacion.id] = marker;
    });
}

// Solo mostrar estaciones de la ruta
function showOnlyRouteStations(camino) {
    markersLayer.clearLayers();
    markers = {};

    camino.forEach((est, index) => {
        const color = COLORES[est.tipo] || '#4F46E5';
        const marker = L.circleMarker([est.latitud, est.longitud], {
            radius: 12,
            fillColor: color,
            color: '#fff',
            weight: 3,
            fillOpacity: 0.9
        });

        marker.bindPopup(`
            <div class="font-semibold">${index + 1}. ${est.nombre}</div>
            <div class="text-xs text-gray-600">${est.tipo.toUpperCase()}</div>
        `);

        marker.addTo(markersLayer);
        markers[est.id] = marker;
    });
}

// Resetear todo (botón limpiar)
function resetMapLayers() {
    if (rutaLayer) { rutaLayer.clearLayers(); rutaLayer = null; }
    if (cuellosLayer) { cuellosLayer.clearLayers(); cuellosLayer = null; }
    dibujarEstaciones(estacionesPrincipales);
}

// =========================================================================
// BUSCADOR ÚNICO CON BOTONES ORIGEN/DESTINO
// =========================================================================

function configurarBuscador() {
    const searchInput = document.getElementById('search-stations');
    const searchResults = document.getElementById('search-results');
    const btnBuscar = document.getElementById('btn-buscar');

    let timeoutId = null;

    searchInput.addEventListener('input', (e) => {
        const query = e.target.value.trim();
        clearTimeout(timeoutId);
        if (query.length < 2) {
            searchResults.classList.add('hidden');
            return;
        }
        timeoutId = setTimeout(() => buscarEstaciones(query), 300);
    });

    searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            const query = searchInput.value.trim();
            if (query.length >= 2) buscarEstaciones(query);
        }
    });

    btnBuscar.addEventListener('click', () => {
        const query = searchInput.value.trim();
        if (query.length >= 2) buscarEstaciones(query);
    });

    // Ocultar al clic fuera
    document.addEventListener('click', (e) => {
        if (!searchInput.contains(e.target) && !searchResults.contains(e.target) && e.target !== btnBuscar) {
            searchResults.classList.add('hidden');
        }
    });
}

async function buscarEstaciones(query) {
    const searchResults = document.getElementById('search-results');

    try {
        const response = await fetch(`${API_URL}/estaciones/buscar?q=${encodeURIComponent(query)}&limit=20`);
        const estaciones = await response.json();

        if (estaciones.length === 0) {
            searchResults.innerHTML = '<div class="p-3 text-gray-500 text-sm">No se encontraron resultados</div>';
            searchResults.classList.remove('hidden');
            return;
        }

        searchResults.innerHTML = estaciones.map(est => `
            <div class="p-3 hover:bg-gray-100 border-b flex items-center justify-between text-sm">
                <div>
                    <div class="font-medium">${escapeHtml(est.nombre)}</div>
                    <div class="text-xs text-gray-500">${est.tipo.toUpperCase()} - ${est.id}</div>
                </div>
                <div class="flex gap-2">
                    <button class="bg-indigo-600 text-white px-3 py-1 rounded text-xs"
                            onclick="setAsOrigen('${est.id}', '${escapeHtml(est.nombre)}', '${est.tipo}')">
                        → Origen
                    </button>
                    <button class="bg-green-600 text-white px-3 py-1 rounded text-xs"
                            onclick="setAsDestino('${est.id}', '${escapeHtml(est.nombre)}', '${est.tipo}')">
                        → Destino
                    </button>
                </div>
            </div>
        `).join('');

        searchResults.classList.remove('hidden');
    } catch (error) {
        searchResults.innerHTML = '<div class="p-3 text-red-500 text-sm">Error al buscar</div>';
        searchResults.classList.remove('hidden');
    }
}

function setAsOrigen(id, nombre, tipo) {
    const select = document.getElementById('select-origen');
    if (!Array.from(select.options).some(o => o.value === id)) {
        select.add(new Option(`${nombre} (${tipo})`, id));
    }
    select.value = id;
    select.dispatchEvent(new Event('change'));
    document.getElementById('search-results').classList.add('hidden');
    if (markers[id]) markers[id].openPopup();
}

function setAsDestino(id, nombre, tipo) {
    const select = document.getElementById('select-destino');
    if (!Array.from(select.options).some(o => o.value === id)) {
        select.add(new Option(`${nombre} (${tipo})`, id));
    }
    select.value = id;
    select.dispatchEvent(new Event('change'));
    document.getElementById('search-results').classList.add('hidden');
    if (markers[id]) markers[id].openPopup();
}

// =========================================================================
// EVENTOS Y BOTONES
// =========================================================================

function configurarEventos() {
    const selectOrigen = document.getElementById('select-origen');
    const selectDestino = document.getElementById('select-destino');
    const btnCalcular = document.getElementById('btn-calcular-ruta');
    const btnCongestion = document.getElementById('btn-analizar-congestion');
    const btnLimpiar = document.getElementById('btn-limpiar-mapa');

    function actualizarBotones() {
        const ok = selectOrigen.value && selectDestino.value && selectOrigen.value !== selectDestino.value;
        btnCalcular.disabled = !ok;
        btnCongestion.disabled = !ok;
    }

    selectOrigen.addEventListener('change', actualizarBotones);
    selectDestino.addEventListener('change', actualizarBotones);
    btnCalcular.addEventListener('click', calcularRutaOptima);
    btnCongestion.addEventListener('click', analizarCongestion);
    btnLimpiar.addEventListener('click', () => {
        resetMapLayers();
        document.getElementById('dijkstra-result').innerHTML = '';
        document.getElementById('congestion-result').innerHTML = '';
    });

    actualizarBotones();
}

// =========================================================================
// RUTA ÓPTIMA
// =========================================================================

async function calcularRutaOptima() {
    const origenId = document.getElementById('select-origen').value;
    const destinoId = document.getElementById('select-destino').value;

    const resultDiv = document.getElementById('dijkstra-result');
    resultDiv.innerHTML = '<div class="text-blue-500 animate-pulse">⏳ Calculando ruta...</div>';

    try {
        console.log(`Calculando ruta: ${origenId} → ${destinoId}`);
        const response = await fetch(`${API_URL}/ruta-optima?origenId=${origenId}&destinoId=${destinoId}`);

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`HTTP ${response.status}: ${errorText}`);
        }

        const resultado = await response.json();
        console.log('Resultado de ruta:', resultado);

        if (!resultado.camino || resultado.camino.length === 0) {
            resultDiv.innerHTML = `
                <div class="bg-yellow-50 border border-yellow-300 rounded p-3">
                    <div class="font-semibold text-yellow-800">⚠️ No se encontró camino</div>
                    <div class="text-sm mt-2 text-yellow-700">
                        No existe una ruta conectada entre estas estaciones.
                    </div>
                </div>
            `;
            return;
        }

        resultDiv.innerHTML = `
            <div class="bg-green-50 border border-green-200 rounded p-3">
                <div class="font-semibold text-green-800">✅ Ruta óptima encontrada</div>
                <div class="text-sm mt-2 space-y-1">
                    <div>⏱️ Tiempo: <strong>${resultado.tiempoTotal.toFixed(1)} min</strong></div>
                    <div>📍 Estaciones: <strong>${resultado.numeroEstaciones}</strong></div>
                    <div>🔄 Transferencias: <strong>${calcularTransferencias(resultado.camino)}</strong></div>
                </div>
            </div>
        `;

        await dibujarRutaOptima(resultado.camino);
        showOnlyRouteStations(resultado.camino);

    } catch (error) {
        console.error('Error al calcular ruta:', error);
        resultDiv.innerHTML = `
            <div class="bg-red-50 border border-red-200 rounded p-3">
                <div class="font-semibold text-red-800">❌ Error</div>
                <div class="text-sm mt-1 text-red-600">${error.message}</div>
            </div>
        `;
    }
}

async function dibujarRutaOptima(camino) {
    if (rutaLayer) rutaLayer.clearLayers();
    else rutaLayer = L.layerGroup().addTo(map);

    for (let i = 0; i < camino.length - 1; i++) {
        const o = camino[i];
        const d = camino[i + 1];
        const coords = `${o.longitud},${o.latitud};${d.longitud},${d.latitud}`;

        try {
            const osrm = await fetch(`https://router.project-osrm.org/route/v1/driving/${coords}?overview=full&geometries=geojson`);
            const data = await osrm.json();
            if (data.routes?.[0]) {
                L.geoJSON(data.routes[0].geometry, {
                    style: { color: '#6366F1', weight: 6, opacity: 0.8 }
                }).addTo(rutaLayer);
            }
        } catch {
            L.polyline([[o.latitud, o.longitud], [d.latitud, d.longitud]], {
                color: '#6366F1', weight: 6, opacity: 0.8
            }).addTo(rutaLayer);
        }
    }

    const bounds = L.latLngBounds(camino.map(e => [e.latitud, e.longitud]));
    map.fitBounds(bounds, { padding: [50, 50] });
}

function calcularTransferencias(camino) {
    let t = 0;
    for (let i = 1; i < camino.length; i++) {
        if (camino[i].tipo !== camino[i-1].tipo) t++;
    }
    return t;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// =========================================================================
// ANÁLISIS DE CONGESTIÓN
// =========================================================================

async function analizarCongestion() {
    const origenId = document.getElementById('select-origen').value;
    const destinoId = document.getElementById('select-destino').value;
    const resultDiv = document.getElementById('congestion-result');

    resultDiv.innerHTML = '<div class="text-blue-500 animate-pulse">⏳ Analizando congestión...</div>';

    try {
        console.log(`Analizando congestión: ${origenId} → ${destinoId}`);
        const response = await fetch(`${API_URL}/analisis-congestion?origenId=${origenId}&destinoId=${destinoId}`);

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`HTTP ${response.status}: ${errorText}`);
        }

        const analisis = await response.json();
        console.log('Análisis de congestión:', analisis);

        const colorNivel = analisis.nivelCongestion?.color || '#666';
        const nombreNivel = analisis.nivelCongestion?.nivel || 'Desconocido';

        resultDiv.innerHTML = `
            <div class="bg-white border-2 rounded p-3" style="border-color: ${colorNivel}">
                <div class="font-semibold" style="color: ${colorNivel}">🚦 Nivel: ${nombreNivel}</div>
                <div class="text-sm mt-2 space-y-1">
                    <div>📊 Flujo normal: <strong>${analisis.flujoNormal || 0}</strong></div>
                    <div>⚠️ Flujo hora pico: <strong>${analisis.flujoHoraPico || 0}</strong></div>
                    <div>📉 Reducción: <strong>${analisis.porcentajeReduccion?.toFixed(1) || 0}%</strong></div>
                </div>
                ${analisis.recomendaciones?.length > 0 ? `
                    <div class="mt-3 text-xs">
                        <div class="font-semibold mb-1">Recomendaciones:</div>
                        ${analisis.recomendaciones.slice(0, 3).map(r => `<div>• ${r}</div>`).join('')}
                    </div>
                ` : ''}
            </div>
        `;

        if (analisis.cuellosBotella?.length > 0) {
            dibujarCuellosBotella(analisis.cuellosBotella);
        }

        dibujarEstaciones(estacionesPrincipales);

    } catch (error) {
        console.error('Error al analizar congestión:', error);
        resultDiv.innerHTML = `
            <div class="bg-red-50 border border-red-200 rounded p-3">
                <div class="font-semibold text-red-800">❌ Error</div>
                <div class="text-sm mt-1 text-red-600">${error.message}</div>
            </div>
        `;
    }
}

function dibujarCuellosBotella(cuellos) {
    if (cuellosLayer) cuellosLayer.clearLayers();
    else cuellosLayer = L.layerGroup().addTo(map);

    cuellos.forEach(cuello => {
        const coords = [
            [cuello.latitudOrigen, cuello.longitudOrigen],
            [cuello.latitudDestino, cuello.longitudDestino]
        ];

        L.polyline(coords, {
            color: '#EF4444',
            weight: 5,
            opacity: 0.7,
            dashArray: '10, 10'
        }).bindPopup(`
            <div class="text-sm">
                <div class="font-semibold text-red-600">🔴 Cuello de Botella</div>
                <div class="mt-1">${cuello.origen.nombre} → ${cuello.destino.nombre}</div>
                <div class="text-xs mt-1">Uso: ${cuello.porcentajeUso?.toFixed(0)}%</div>
            </div>
        `).addTo(cuellosLayer);
    });
}

// =========================================================================
// INICIO
// =========================================================================

document.addEventListener('DOMContentLoaded', init);
