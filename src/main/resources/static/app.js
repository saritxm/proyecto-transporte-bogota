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
let estacionesPrincipales = []; // Solo principales para vista inicial
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
    const response = await fetch(`${API_URL}/estadisticas`);
    const stats = await response.json();

    const grid = document.getElementById('stats-grid');
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
}

// Cargar solo estaciones principales
async function cargarEstacionesPrincipales() {
    console.log('Cargando estaciones principales...');
    const response = await fetch(`${API_URL}/estaciones/principales`);
    estacionesPrincipales = await response.json();

    console.log(`${estacionesPrincipales.length} estaciones principales cargadas`);
    dibujarEstaciones(estacionesPrincipales);
    llenarSelectores(estacionesPrincipales);
}

function llenarSelectores(estaciones) {
    const selectOrigen = document.getElementById('select-origen');
    const selectDestino = document.getElementById('select-destino');

    selectOrigen.innerHTML = '<option value="" disabled selected>Seleccione Origen</option>';
    selectDestino.innerHTML = '<option value="" disabled selected>Seleccione Destino</option>';

    estaciones.forEach(estacion => {
        const optOrigen = document.createElement('option');
        optOrigen.value = estacion.id;
        optOrigen.textContent = `${estacion.nombre} (${estacion.tipo})`;
        selectOrigen.appendChild(optOrigen);

        const optDestino = document.createElement('option');
        optDestino.value = estacion.id;
        optDestino.textContent = `${estacion.nombre} (${estacion.tipo})`;
        selectDestino.appendChild(optDestino);
    });
}

// =========================================================================
// FUNCIONES DE DIBUJO EN MAPA (LEAFLET)
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

// Dibujar rutas en el mapa (opaco)
function dibujarRutas() {
    // Las rutas ya dibujadas inicialmente no se eliminan, solo las óptimas
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

function dibujarRutaOptima(resultado) {
    // Resetear capas y marcadores
    resetMapLayers();
    
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

    map.fitBounds(rutaLayer.getBounds(), { padding: [50, 50] });
}

function dibujarARM(armRutas) {
    resetMapLayers();
    
    // Crear una capa de grupo para todas las líneas del ARM
    armLayer = L.layerGroup().addTo(map);

    armRutas.forEach(ruta => {
        const origen = estaciones.find(e => e.id === ruta.origenId);
        const destino = estaciones.find(e => e.id === ruta.destinoId);

        if (origen && destino) {
            L.polyline(
                [[origen.latitud, origen.longitud], [destino.latitud, destino.longitud]],
                {
                    color: '#FFD700', // Dorado para el ARM
                    weight: 3,
                    opacity: 0.7,
                    dashArray: '5, 5' 
                }
            ).addTo(armLayer).bindPopup(`ARM Ruta: ${origen.nombre} a ${destino.nombre}. Tiempo: ${ruta.tiempo} min.`);
            
             // Resaltar nodos (estaciones)
            markers[origen.id].setStyle({fillColor: '#FFD700', radius: 10}).bringToFront();
            markers[destino.id].setStyle({fillColor: '#FFD700', radius: 10}).bringToFront();
        }
    });
    
    alert(`Árbol de Recubrimiento Mínimo (ARM) dibujado. Se seleccionaron ${armRutas.length} rutas esenciales.`);
}

function resetMapLayers() {
    // Quitar la ruta óptima anterior
    if (rutaLayer) {
        map.removeLayer(rutaLayer);
        rutaLayer = null;
    }
    // Quitar el ARM anterior
    if (armLayer) {
        map.removeLayer(armLayer);
        armLayer = null;
    }
    
    // Resetear el estilo de todos los marcadores
    Object.values(markers).forEach(marker => {
        const estacion = estaciones.find(e => e.id === Object.keys(markers).find(key => markers[key] === marker));
        marker.setStyle({
            radius: 8,
            fillColor: COLORES[estacion.tipo] || '#999',
            weight: 2
        });
    });
}


// =========================================================================
// FUNCIONES DE ALGORITMOS DE OPTIMIZACIÓN
// =========================================================================

// 1. Ruta Óptima (Dijkstra)
async function calcularRuta() {
    const origenId = document.getElementById('origen').value;
    const destinoId = document.getElementById('destino').value;

    if (!origenId || !destinoId || origenId === destinoId) {
        alert('Por favor seleccione un origen y un destino diferentes.');
        return;
    }
    
    const btn = document.getElementById('calcular-ruta-btn');
    btn.disabled = true;
    btn.textContent = 'Calculando...';

    try {
        const response = await fetch(`${API_URL}/ruta-optima?origenId=${origenId}&destinoId=${destinoId}`);
        const resultado = await response.json();
        
        let resultadoHTML = `
            <h4>✅ Ruta Óptima (Dijkstra)</h4>
            <p><b>Tiempo total:</b> ${resultado.tiempoTotal} minutos</p>
            <p><b>Estaciones:</b> ${resultado.numeroEstaciones}</p>
            <p><b>Transferencias:</b> ${calcularTransferencias(resultado.camino)}</p>
            <div class="station-list">
                <b>Recorrido:</b>
                ${resultado.camino.map((estacion, index) => `
                    <div class="station-item">
                        ${index + 1}. ${estacion.nombre} <span style="color: ${COLORES[estacion.tipo]}">(${estacion.tipo.toUpperCase()})</span>
                    </div>
                `).join('')}
            </div>
        `;
        
        mostrarResultado('resultado-ruta', resultadoHTML, '#4CAF50');
        dibujarRutaOptima(resultado);
        
    } catch (error) {
        console.error('Error al calcular ruta:', error);
        mostrarResultado('resultado-ruta', 'Error al calcular la ruta.', 'red');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Calcular Ruta Más Corta';
    }
}

// 2. Flujo Máximo (Edmonds-Karp)
async function calcularFlujoMaximo() {
    const origenId = document.getElementById('origen-flujo').value;
    const destinoId = document.getElementById('destino-flujo').value;

    if (!origenId || !destinoId || origenId === destinoId) {
        alert('Por favor seleccione un origen y un destino diferentes para el análisis de flujo.');
        return;
    }
    
    const btn = document.getElementById('calcular-flujo-btn');
    btn.disabled = true;
    btn.textContent = 'Analizando...';

    try {
        const response = await fetch(`${API_URL}/flujo-maximo?origenId=${origenId}&destinoId=${destinoId}`);
        const resultado = await response.json();
        
        const color = resultado.flujoMaximo > 5000 ? '#2196F3' : '#F44336';
        
        let resultadoHTML = `
            <h4>📈 Flujo Máximo (Capacidad)</h4>
            <p><b>Capacidad Máxima:</b> ${resultado.flujoMaximo} pasajeros/hora (simulado)</p>
            <p style="color: ${color}; font-weight: bold;"><b>Análisis:</b> ${resultado.recomendacion}</p>
            <p>Este valor indica el máximo tráfico que puede soportar la red entre estas dos estaciones.</p>
        `;
        
        mostrarResultado('resultado-flujo', resultadoHTML, color);
        
    } catch (error) {
        console.error('Error al calcular flujo:', error);
        mostrarResultado('resultado-flujo', 'Error al calcular el flujo máximo.', 'red');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Analizar Flujo Máximo';
    }
}

// 3. ARM (Kruskal)
async function calcularARM() {
    const btn = document.getElementById('calcular-arm-btn');
    if(btn) {
        btn.disabled = true;
        btn.textContent = 'Calculando...';
    }
    
    try {
        const response = await fetch(`${API_URL}/arm`);
        const resultado = await response.json(); // Lista de rutas del ARM
        
        const tiempoTotal = resultado.reduce((sum, r) => sum + r.tiempo, 0);

        let resultadoHTML = `
            <h4>🌳 Árbol de Recubrimiento Mínimo (ARM)</h4>
            <p><b>Rutas Esenciales:</b> ${resultado.length}</p>
            <p><b>Tiempo Total de ARM:</b> ${tiempoTotal.toFixed(0)} minutos</p>
            <p>El ARM muestra la subred de rutas con el **menor tiempo de viaje acumulado** que mantiene todas las estaciones conectadas.</p>
            <p class="link-mapa" onclick="dibujarARM(${JSON.stringify(resultado).replace(/"/g, '&quot;')})">🌐 Ver ARM en el mapa</p>
        `;
        
        mostrarResultado('resultado-arm', resultadoHTML, '#FF9800');
        
    } catch (error) {
        console.error('Error al calcular ARM:', error);
        mostrarResultado('resultado-arm', 'Error al calcular el ARM.', 'red');
    } finally {
        if(btn) {
            btn.disabled = false;
            btn.textContent = 'Recalcular ARM';
        }
    }
}

// 4. Coloreado de Grafos (Welsh-Powell)
async function calcularColoreado() {
    const btn = document.getElementById('calcular-coloreado-btn');
    if(btn) {
        btn.disabled = true;
        btn.textContent = 'Analizando...';
    }

    try {
        const response = await fetch(`${API_URL}/coloreado`);
        const resultado = await response.json();
        
        const asignacionHTML = Object.entries(resultado.asignacion)
            .map(([nombre, colorId]) => `
                <div class="station-item">
                    ${nombre}: <span style="font-weight: bold; color: hsl(${colorId * 50 % 360}, 70%, 50%);">Recurso ${colorId}</span>
                </div>
            `).join('');
            
        let resultadoHTML = `
            <h4>🎨 Coloreado de Grafos (Asignación de Recursos)</h4>
            <p><b>Mínimo de Recursos:</b> ${resultado.coloresUsados}</p>
            <p><b>Interpretación:</b> ${resultado.interpretacion}</p>
            <div class="station-list">
                <b>Asignación:</b>
                ${asignacionHTML}
            </div>
        `;
        
        mostrarResultado('resultado-coloreado', resultadoHTML, '#764ba2');
        
    } catch (error) {
        console.error('Error al colorear grafo:', error);
        mostrarResultado('resultado-coloreado', 'Error al calcular el Coloreado de Grafos.', 'red');
    } finally {
        if(btn) {
            btn.disabled = false;
            btn.textContent = 'Recalcular Coloreado';
        }
    }
}


// =========================================================================
// FUNCIONES AUXILIARES DE VISTA
// =========================================================================

// Función genérica para mostrar resultados
function mostrarResultado(divId, contentHTML, color) {
    const resultadoDiv = document.getElementById(divId);
    resultadoDiv.innerHTML = `
        <div class="result-box" style="border-left-color: ${color}; background: ${color}1A;">
            ${contentHTML}
        </div>
    `;
    resultadoDiv.style.display = 'block';
}

function calcularTransferencias(camino) {
    let transferencias = 0;
    for (let i = 1; i < camino.length; i++) {
        if (camino[i].tipo !== camino[i - 1].tipo) {
            transferencias++;
        }
    }
    return transferencias;
}

// =========================================================================
// BÚSQUEDA DE ESTACIONES
// =========================================================================

function configurarBuscador() {
    const searchInput = document.getElementById('search-stations');
    const searchResults = document.getElementById('search-results');

    if (!searchInput || !searchResults) {
        console.error('❌ Elementos de búsqueda no encontrados:', { searchInput, searchResults });
        return;
    }

    console.log('✅ Buscador configurado correctamente');

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

    document.addEventListener('click', (e) => {
        if (!searchInput.contains(e.target) && !searchResults.contains(e.target)) {
            searchResults.classList.add('hidden');
        }
    });
}

async function buscarEstaciones(query) {
    const searchResults = document.getElementById('search-results');
    console.log('🔍 Buscando:', query);

    try {
        const url = `${API_URL}/estaciones/buscar?q=${encodeURIComponent(query)}&limit=20`;
        console.log('📡 URL:', url);
        const response = await fetch(url);
        const estaciones = await response.json();
        console.log('📦 Resultados:', estaciones.length, estaciones);

        if (estaciones.length === 0) {
            searchResults.innerHTML = '<div class="p-3 text-gray-500 text-sm">No se encontraron resultados</div>';
            searchResults.classList.remove('hidden');
            return;
        }

        searchResults.innerHTML = estaciones.map(estacion => `
            <div class="p-2 hover:bg-gray-100 cursor-pointer border-b last:border-b-0 flex items-center justify-between"
                 onclick="seleccionarEstacionDesdeBusqueda('${estacion.id}', '${escapeHtml(estacion.nombre)}', '${estacion.tipo}', ${estacion.latitud}, ${estacion.longitud})">
                <div>
                    <div class="font-medium text-sm">${estacion.nombre}</div>
                    <div class="text-xs text-gray-500">${estacion.tipo.toUpperCase()} - ${estacion.id}</div>
                </div>
                <div class="w-3 h-3 rounded-full" style="background-color: ${COLORES[estacion.tipo] || '#666'}"></div>
            </div>
        `).join('');

        searchResults.classList.remove('hidden');
    } catch (error) {
        console.error('Error al buscar:', error);
        searchResults.innerHTML = '<div class="p-3 text-red-500 text-sm">Error en la búsqueda</div>';
        searchResults.classList.remove('hidden');
    }
}

function seleccionarEstacionDesdeBusqueda(id, nombre, tipo, lat, lng) {
    const searchResults = document.getElementById('search-results');
    const searchInput = document.getElementById('search-stations');

    searchResults.classList.add('hidden');
    searchInput.value = nombre;

    agregarEstacionASelectores(id, nombre, tipo);
    map.setView([lat, lng], 15);

    if (!markers[id]) {
        const tempMarker = L.circleMarker([lat, lng], {
            radius: 10,
            fillColor: COLORES[tipo] || '#666',
            color: '#fff',
            weight: 3,
            opacity: 1,
            fillOpacity: 0.9
        }).addTo(markersLayer);

        tempMarker.bindPopup(`
            <div class="font-semibold">${nombre}</div>
            <div class="text-xs text-gray-600">${tipo.toUpperCase()} - ${id}</div>
        `).openPopup();

        markers[id] = tempMarker;
    } else {
        markers[id].openPopup();
    }
}

function agregarEstacionASelectores(id, nombre, tipo) {
    const selectOrigen = document.getElementById('select-origen');
    const selectDestino = document.getElementById('select-destino');

    const existeOrigen = Array.from(selectOrigen.options).some(opt => opt.value === id);
    if (!existeOrigen) {
        const option = document.createElement('option');
        option.value = id;
        option.textContent = `${nombre} (${tipo})`;
        selectOrigen.appendChild(option);
    }

    const existeDestino = Array.from(selectDestino.options).some(opt => opt.value === id);
    if (!existeDestino) {
        const option = document.createElement('option');
        option.value = id;
        option.textContent = `${nombre} (${tipo})`;
        selectDestino.appendChild(option);
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// =========================================================================
// CONFIGURACIÓN DE EVENTOS Y CÁLCULOS
// =========================================================================

function configurarEventos() {
    const btnCalcular = document.getElementById('btn-calcular-ruta');
    const btnCongestion = document.getElementById('btn-analizar-congestion');
    const selectOrigen = document.getElementById('select-origen');
    const selectDestino = document.getElementById('select-destino');

    if (!btnCalcular || !btnCongestion) return;

    function actualizarBotones() {
        const habilitado = selectOrigen.value && selectDestino.value;
        btnCalcular.disabled = !habilitado;
        btnCongestion.disabled = !habilitado;
    }

    selectOrigen.addEventListener('change', actualizarBotones);
    selectDestino.addEventListener('change', actualizarBotones);
    btnCalcular.addEventListener('click', calcularRutaOptima);
    btnCongestion.addEventListener('click', analizarCongestion);
}

async function calcularRutaOptima() {
    const origenId = document.getElementById('select-origen').value;
    const destinoId = document.getElementById('select-destino').value;
    const resultDiv = document.getElementById('dijkstra-result');

    resultDiv.innerHTML = '<div class="text-blue-500">⏳ Calculando...</div>';

    try {
        const response = await fetch(`${API_URL}/ruta-optima?origenId=${origenId}&destinoId=${destinoId}`);
        const resultado = await response.json();

        resultDiv.innerHTML = `
            <div class="bg-green-50 border border-green-200 rounded p-3">
                <div class="font-semibold text-green-800">✅ Ruta encontrada</div>
                <div class="text-sm text-gray-700 mt-2">
                    <div>⏱ Tiempo: <span class="font-bold">${resultado.tiempoTotal} min</span></div>
                    <div>🚏 Estaciones: ${resultado.numeroEstaciones}</div>
                </div>
            </div>
        `;

        await dibujarRutaOptima(resultado.camino);
    } catch (error) {
        console.error('Error:', error);
        resultDiv.innerHTML = `<div class="text-red-500">❌ Error</div>`;
    }
}

async function dibujarRutaOptima(camino) {
    if (rutaLayer) {
        rutaLayer.clearLayers();
    } else {
        rutaLayer = L.layerGroup().addTo(map);
    }

    if (!camino || camino.length < 2) return;

    for (let i = 0; i < camino.length - 1; i++) {
        const origen = camino[i];
        const destino = camino[i + 1];

        try {
            const coords = `${origen.longitud},${origen.latitud};${destino.longitud},${destino.latitud}`;
            const osrmUrl = `https://router.project-osrm.org/route/v1/driving/${coords}?overview=full&geometries=geojson`;

            const response = await fetch(osrmUrl);
            const data = await response.json();

            if (data.routes && data.routes[0]) {
                L.geoJSON(data.routes[0].geometry, {
                    style: { color: '#4F46E5', weight: 4, opacity: 0.7 }
                }).addTo(rutaLayer);
            }
        } catch (error) {
            L.polyline([[origen.latitud, origen.longitud], [destino.latitud, destino.longitud]], {
                color: '#4F46E5', weight: 4, opacity: 0.7
            }).addTo(rutaLayer);
        }
    }

    const bounds = L.latLngBounds(camino.map(e => [e.latitud, e.longitud]));
    map.fitBounds(bounds, { padding: [50, 50] });
}

async function analizarCongestion() {
    const origenId = document.getElementById('select-origen').value;
    const destinoId = document.getElementById('select-destino').value;
    const resultDiv = document.getElementById('congestion-result');

    resultDiv.innerHTML = '<div class="text-blue-500">⏳ Analizando...</div>';

    try {
        const response = await fetch(`${API_URL}/analisis-congestion?origenId=${origenId}&destinoId=${destinoId}`);
        const analisis = await response.json();

        const nivelColor = analisis.nivelCongestion.color;
        const nivel = analisis.nivelCongestion.nivel;

        resultDiv.innerHTML = `
            <div class="border rounded p-3" style="border-color: ${nivelColor}; background-color: ${nivelColor}15">
                <div class="font-semibold mb-2" style="color: ${nivelColor}">🚦 ${nivel}</div>
                <div class="text-sm text-gray-700 space-y-1">
                    <div>📊 Normal: ${analisis.flujoNormal}</div>
                    <div>⚠️ Hora pico: ${analisis.flujoHoraPico}</div>
                    <div>📉 Reducción: ${analisis.porcentajeReduccion}%</div>
                </div>
            </div>
        `;

        if (analisis.cuellosBotella && analisis.cuellosBotella.length > 0) {
            dibujarCuellosBotella(analisis.cuellosBotella);
        }
    } catch (error) {
        console.error('Error:', error);
        resultDiv.innerHTML = `<div class="text-red-500">❌ Error</div>`;
    }
}

function dibujarCuellosBotella(cuellos) {
    if (cuellosLayer) {
        cuellosLayer.clearLayers();
    } else {
        cuellosLayer = L.layerGroup().addTo(map);
    }

    cuellos.forEach(cuello => {
        const color = cuello.porcentajeUso > 85 ? '#991B1B' :
                      cuello.porcentajeUso > 75 ? '#DC2626' : '#F59E0B';

        const line = L.polyline([
            [cuello.latitudOrigen, cuello.longitudOrigen],
            [cuello.latitudDestino, cuello.longitudDestino]
        ], {
            color: color,
            weight: 8,
            opacity: 0.7,
            dashArray: '10, 10'
        }).addTo(cuellosLayer);

        line.bindPopup(`
            <div class="font-semibold text-red-700">🔴 Cuello de Botella</div>
            <div class="text-sm mt-2">
                <div>${cuello.origen.nombre} → ${cuello.destino.nombre}</div>
                <div class="mt-1">Uso: ${cuello.porcentajeUso}%</div>
            </div>
        `);
    });
}

// Iniciar aplicación
document.addEventListener('DOMContentLoaded', init);