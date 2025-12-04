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
let estaciones = [];
let rutas = [];
let markers = {};
let rutaLayer = null;
let armLayer = null; // Capa para dibujar el ARM

// Colores por tipo de estación
const COLORES = {
    'metro': '#2196F3',
    'tm': '#F44336',
    'sitp': '#4CAF50',
    'intermodal': '#FF9800'
};

// =========================================================================
// FUNCIONES DE INICIALIZACIÓN Y CARGA DE DATOS
// =========================================================================

async function init() {
    showLoading(true);
    try {
        await cargarEstadisticas();
        await cargarEstaciones();
        await cargarRutas();
        dibujarEstaciones();
        dibujarRutas();
        
        // Ejecutar los algoritmos globales al inicio (ARM y Coloreado)
        await calcularARM();
        await calcularColoreado();
        
    } catch (error) {
        console.error('Error al inicializar:', error);
        alert('Error al cargar datos del sistema: ' + error.message);
    } finally {
        showLoading(false);
    }
}

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

// Cargar estaciones (y llenar selectores)
async function cargarEstaciones() {
    const response = await fetch(`${API_URL}/estaciones`);
    estaciones = await response.json();

    // Llenar selectores
    const selectores = ['origen', 'destino', 'origen-flujo', 'destino-flujo'];
    
    selectores.forEach(id => {
        const select = document.getElementById(id);
        if (select) {
            select.innerHTML = `<option value="">Seleccione...</option>`; // Reset
            estaciones.forEach(estacion => {
                const option = new Option(`${estacion.nombre} (${estacion.tipo.toUpperCase()})`, estacion.id);
                select.add(option);
            });
        }
    });
}

// Cargar rutas
async function cargarRutas() {
    const response = await fetch(`${API_URL}/rutas`);
    rutas = await response.json();
}

// =========================================================================
// FUNCIONES DE DIBUJO EN MAPA (LEAFLET)
// =========================================================================

function dibujarEstaciones() {
    // Reset markers para evitar duplicados si se llama de nuevo
    Object.values(markers).forEach(marker => map.removeLayer(marker));
    markers = {};
    
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
            <b>Capacidad Estación:</b> ${estacion.capacidad} pasajeros<br>
            <b>ID:</b> ${estacion.id}
        `);

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

// Iniciar aplicación cuando cargue la página
document.addEventListener('DOMContentLoaded', init);