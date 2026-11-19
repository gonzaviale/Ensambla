import { useEffect, useRef, useState } from 'react';
import {
  circle,
  circleMarker,
  divIcon,
  latLngBounds,
  layerGroup,
  map,
  marker,
  tileLayer,
  type LayerGroup,
  type Map,
} from 'leaflet';
import { cartografia } from '@/config/ubicacion';
import {
  RADIO_DE_ANALISIS_M,
  formatoPuntaje,
  type Coincidencia,
  type Descartada,
  type Punto,
  type TipoDePunto,
  type ZonaPuntuada,
} from '@/lib/dondeAbrir';

// El color de cada zona dice qué tan bien coincide con lo que la persona busca.
export const COLOR_DE_COINCIDENCIA: Record<Coincidencia, string> = {
  buena: '#00506a',
  intermedia: '#ebce32',
  mala: '#f85f5a',
};
export const COLOR_SIN_PODIO = '#69777c';
/** El lugar que la persona tocó en el mapa: el naranja de la marca, aparte de los colores del ranking. */
const COLOR_DEL_ELEGIDO = '#ee8000';

/** Cinco decimales son ~1 m: alcanza para el mapa y evita pedidos distintos por un pixel. */
const redondear = (valor: number) => Math.round(valor * 1e5) / 1e5;

export const COLOR_DE_PUNTO: Record<TipoDePunto, string> = {
  competencia: '#f85f5a',
  escuela: '#00506a',
  parque: '#6aa84f',
  transporte: '#1f6f8f',
  oficina: '#7a5f00',
  salud: '#a64d9a',
  complementario: '#bd5900',
};

type Parametros = {
  ranking: ZonaPuntuada[];
  descartadas: Descartada[];
  seleccionadaId: string;
  puntos: Punto[];
  capas: Record<TipoDePunto, boolean>;
  onSeleccionar: (id: string) => void;
  /** Se llama con el lugar que la persona toca en el mapa para medirlo en vivo. */
  onElegirPunto?: (latitud: number, longitud: number) => void;
  /** El lugar tocado, para dibujarlo enseguida aunque todavía no haya vuelto la medición. */
  punto?: { latitud: number; longitud: number } | null;
  /** Id de ese lugar dentro del ranking, cuando el servidor ya lo midió. */
  puntoElegidoId?: string | null;
  localActual?: { latitud: number | null; longitud: number | null; nombre: string };
};

type Coordenada = [number, number];
type Local = Parametros['localActual'];
type Vista = { primerAjuste: boolean; ultimaSeleccion: string };

const CENTRO_POR_DEFECTO: Coordenada = [-34.62, -58.47];

const coordenadasDelLocal = (local: Local): Coordenada | null =>
  local?.latitud != null && local.longitud != null ? [local.latitud, local.longitud] : null;

const coordenadasDeLaZona = (item: ZonaPuntuada): Coordenada => [
  item.zona.latitud,
  item.zona.longitud,
];

function agregarMarcadorDelLocal(instancia: Map, local: Local) {
  const coordenadas = coordenadasDelLocal(local);
  if (!coordenadas || !local) return;
  marker(coordenadas, {
    title: local.nombre,
    alt: 'Tu local actual',
    keyboard: false,
    icon: divIcon({
      className: 'donde-local',
      html: '<span>Tu local</span>',
      iconSize: [64, 26],
      iconAnchor: [32, 13],
    }),
  }).addTo(instancia);
}

function agregarCartografia(instancia: Map, alFallar: () => void) {
  tileLayer(cartografia.url, {
    attribution: cartografia.atribucion,
    maxZoom: cartografia.zoomMaximo,
    keepBuffer: 0,
    updateWhenIdle: true,
  })
    .on('tileerror', alFallar)
    .addTo(instancia);
}

function crearMapa(contenedor: HTMLDivElement, local: Local, alFallar: () => void): Map {
  const centro = coordenadasDelLocal(local) ?? CENTRO_POR_DEFECTO;
  const instancia = map(contenedor, { scrollWheelZoom: false, zoomControl: true }).setView(
    centro,
    13,
  );
  agregarCartografia(instancia, alFallar);
  agregarMarcadorDelLocal(instancia, local);
  return instancia;
}

function dibujarDescartada(item: Descartada, grupo: LayerGroup) {
  circle([item.zona.latitud, item.zona.longitud], {
    radius: RADIO_DE_ANALISIS_M,
    color: COLOR_SIN_PODIO,
    weight: 1,
    dashArray: '4 5',
    fillOpacity: 0.02,
  })
    .bindTooltip(`${item.zona.nombre}: descartada. ${item.motivos[0] ?? ''}`)
    .addTo(grupo);
}

type EstadoDeLaZona = { color: string; activa: boolean; esElegido: boolean; indice: number };

function dibujarAreaDeLaZona(item: ZonaPuntuada, estado: EstadoDeLaZona, grupo: LayerGroup) {
  const { color, activa } = estado;
  circle(coordenadasDeLaZona(item), {
    radius: RADIO_DE_ANALISIS_M,
    color,
    weight: activa ? 3 : 1.5,
    fillColor: color,
    // Cuanto mejor puntaje, más se marca la zona.
    fillOpacity: (activa ? 0.12 : 0.04) + 0.14 * (item.puntaje / 10),
  }).addTo(grupo);
}

const claseDelPin = ({ activa, esElegido, indice }: EstadoDeLaZona) =>
  ['donde-pin', activa && 'activa', indice > 2 && !esElegido && 'chico', esElegido && 'punto']
    .filter(Boolean)
    .join(' ');

function dibujarPinDeLaZona(
  item: ZonaPuntuada,
  estado: EstadoDeLaZona,
  grupo: LayerGroup,
  alSeleccionar: () => void,
) {
  const { color, esElegido, indice } = estado;
  marker(coordenadasDeLaZona(item), {
    title: `Puesto ${indice + 1}: ${item.zona.nombre} (${formatoPuntaje(item.puntaje)})`,
    alt: `Puesto ${indice + 1}: ${item.zona.nombre}`,
    icon: divIcon({
      className: claseDelPin(estado),
      html: `<span style="--color:${color}">${esElegido ? '★' : indice + 1}</span>`,
      iconSize: [32, 32],
      iconAnchor: [16, 16],
    }),
  })
    .on('click', alSeleccionar)
    .addTo(grupo);
}

type Seleccion = {
  seleccionadaId: string;
  puntoElegidoId?: string | null;
  alSeleccionar: (id: string) => void;
};

function dibujarZonaDelRanking(
  item: ZonaPuntuada,
  indice: number,
  grupo: LayerGroup,
  { seleccionadaId, puntoElegidoId, alSeleccionar }: Seleccion,
) {
  const estado: EstadoDeLaZona = {
    color: COLOR_DE_COINCIDENCIA[item.coincidencia],
    activa: item.zona.id === seleccionadaId,
    esElegido: item.zona.id === puntoElegidoId,
    indice,
  };
  dibujarAreaDeLaZona(item, estado, grupo);
  dibujarPinDeLaZona(item, estado, grupo, () => alSeleccionar(item.zona.id));
}

function dibujarZonas(
  grupo: LayerGroup,
  ranking: ZonaPuntuada[],
  descartadas: Descartada[],
  seleccion: Seleccion,
) {
  grupo.clearLayers();
  descartadas.forEach((item) => dibujarDescartada(item, grupo));
  ranking.forEach((item, indice) => dibujarZonaDelRanking(item, indice, grupo, seleccion));
}

function encuadrarElPodio(
  instancia: Map,
  primera: ZonaPuntuada,
  ranking: ZonaPuntuada[],
  local: Local,
) {
  const limites = latLngBounds([coordenadasDelLocal(local) ?? coordenadasDeLaZona(primera)]);
  ranking.slice(0, 3).forEach((item) => limites.extend(coordenadasDeLaZona(item)));
  instancia.fitBounds(limites, { padding: [40, 40], maxZoom: 15, animate: false });
}

// Solo se mueve el mapa cuando la persona elige otra zona, no en cada cambio de filtro.
function irALaZonaElegida(instancia: Map, ranking: ZonaPuntuada[], seleccionadaId: string) {
  const elegida = ranking.find((item) => item.zona.id === seleccionadaId);
  if (elegida) instancia.panTo(coordenadasDeLaZona(elegida), { animate: true });
}

function moverLaVista(
  instancia: Map,
  ranking: ZonaPuntuada[],
  seleccionadaId: string,
  local: Local,
  vista: Vista,
) {
  const primera = ranking.at(0);
  const esElPrimerAjuste = vista.primerAjuste && primera !== undefined;
  const cambioLaZonaElegida = seleccionadaId && seleccionadaId !== vista.ultimaSeleccion;
  if (esElPrimerAjuste && primera) {
    encuadrarElPodio(instancia, primera, ranking, local);
    vista.primerAjuste = false;
  }
  if (!esElPrimerAjuste && cambioLaZonaElegida) {
    irALaZonaElegida(instancia, ranking, seleccionadaId);
  }
  vista.ultimaSeleccion = seleccionadaId;
}

function dibujarElPuntoElegido(grupo: LayerGroup, punto: { latitud: number; longitud: number }) {
  const centro: Coordenada = [punto.latitud, punto.longitud];
  circle(centro, {
    radius: RADIO_DE_ANALISIS_M,
    color: COLOR_DEL_ELEGIDO,
    weight: 2,
    dashArray: '2 6',
    fillOpacity: 0.04,
    interactive: false,
  }).addTo(grupo);
  circleMarker(centro, {
    radius: 5,
    color: '#ffffff',
    weight: 2,
    fillColor: COLOR_DEL_ELEGIDO,
    fillOpacity: 1,
    interactive: false,
  }).addTo(grupo);
}

function dibujarPuntoCercano(grupo: LayerGroup, punto: Punto) {
  circleMarker([punto.latitud, punto.longitud], {
    radius: 6,
    color: '#ffffff',
    weight: 1.5,
    fillColor: COLOR_DE_PUNTO[punto.tipo],
    fillOpacity: 0.95,
  })
    .bindTooltip(punto.nombre)
    .addTo(grupo);
}

function dibujarPuntosCercanos(
  grupo: LayerGroup,
  puntos: Punto[],
  capas: Record<TipoDePunto, boolean>,
) {
  grupo.clearLayers();
  puntos.filter((punto) => capas[punto.tipo]).forEach((punto) => dibujarPuntoCercano(grupo, punto));
}

/** El mapa: todas las zonas (las del podio numeradas, las descartadas en gris) y lo que hay alrededor. */
export function useMapaDeZonas({
  ranking,
  descartadas,
  seleccionadaId,
  puntos,
  capas,
  onSeleccionar,
  onElegirPunto,
  punto,
  puntoElegidoId,
  localActual,
}: Parametros) {
  const contenedor = useRef<HTMLDivElement>(null);
  const mapa = useRef<Map | null>(null);
  const grupoDeZonas = useRef<LayerGroup | null>(null);
  const grupoDePuntos = useRef<LayerGroup | null>(null);
  const grupoDelElegido = useRef<LayerGroup | null>(null);
  const seleccionar = useRef(onSeleccionar);
  const elegirPunto = useRef(onElegirPunto);
  const vista = useRef<Vista>({ primerAjuste: true, ultimaSeleccion: '' });
  const [error, setError] = useState(false);
  const local = useRef(localActual);

  useEffect(() => {
    seleccionar.current = onSeleccionar;
    elegirPunto.current = onElegirPunto;
  }, [onSeleccionar, onElegirPunto]);

  useEffect(() => {
    if (!contenedor.current) return;
    try {
      const instancia = crearMapa(contenedor.current, local.current, () => setError(true));
      grupoDeZonas.current = layerGroup().addTo(instancia);
      grupoDePuntos.current = layerGroup().addTo(instancia);
      grupoDelElegido.current = layerGroup().addTo(instancia);
      instancia.on('click', (evento) =>
        elegirPunto.current?.(redondear(evento.latlng.lat), redondear(evento.latlng.lng)),
      );
      mapa.current = instancia;
    } catch {
      setError(true);
    }
    return () => {
      mapa.current?.remove();
      mapa.current = null;
      grupoDeZonas.current = null;
      grupoDePuntos.current = null;
      grupoDelElegido.current = null;
    };
  }, []);

  useEffect(() => {
    const instancia = mapa.current;
    const grupo = grupoDeZonas.current;
    if (!instancia || !grupo) return;
    const alSeleccionar = (id: string) => seleccionar.current(id);
    dibujarZonas(grupo, ranking, descartadas, { seleccionadaId, puntoElegidoId, alSeleccionar });
    try {
      moverLaVista(instancia, ranking, seleccionadaId, local.current, vista.current);
    } catch {
      // Sin tamaño (por ejemplo, en pruebas): el mapa queda donde está.
    }
  }, [ranking, descartadas, seleccionadaId, puntoElegidoId]);

  // El lugar tocado se ve al instante; cuando llega la medición, entra al ranking como una zona más.
  useEffect(() => {
    const grupo = grupoDelElegido.current;
    if (!grupo) return;
    grupo.clearLayers();
    if (punto) dibujarElPuntoElegido(grupo, punto);
  }, [punto]);

  useEffect(() => {
    if (grupoDePuntos.current) dibujarPuntosCercanos(grupoDePuntos.current, puntos, capas);
  }, [puntos, capas]);

  return { contenedor, error };
}
