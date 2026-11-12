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
  buena: '#2f7a55',
  intermedia: '#c99a00',
  mala: '#c42b2b',
};
export const COLOR_SIN_PODIO = '#69777c';
/** El lugar que la persona tocó en el mapa: el naranja de la marca, aparte de los colores del ranking. */
const COLOR_DEL_ELEGIDO = '#ee8000';

/** Cinco decimales son ~1 m: alcanza para el mapa y evita pedidos distintos por un pixel. */
const redondear = (valor: number) => Math.round(valor * 1e5) / 1e5;

export const COLOR_DE_PUNTO: Record<TipoDePunto, string> = {
  competencia: '#c42b2b',
  escuela: '#2f7a55',
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
  const primerAjuste = useRef(true);
  const ultimaSeleccion = useRef('');
  const [error, setError] = useState(false);
  const local = useRef(localActual);

  useEffect(() => {
    seleccionar.current = onSeleccionar;
    elegirPunto.current = onElegirPunto;
  }, [onSeleccionar, onElegirPunto]);

  useEffect(() => {
    if (!contenedor.current) return;
    try {
      const centro =
        local.current?.latitud != null && local.current.longitud != null
          ? ([local.current.latitud, local.current.longitud] as [number, number])
          : ([-34.62, -58.47] as [number, number]);
      const instancia = map(contenedor.current, {
        scrollWheelZoom: false,
        zoomControl: true,
      }).setView(centro, 13);
      tileLayer(cartografia.url, {
        attribution: cartografia.atribucion,
        maxZoom: cartografia.zoomMaximo,
        keepBuffer: 0,
        updateWhenIdle: true,
      })
        .on('tileerror', () => setError(true))
        .addTo(instancia);
      if (local.current?.latitud != null && local.current.longitud != null) {
        marker([local.current.latitud, local.current.longitud], {
          title: local.current.nombre,
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
    grupo.clearLayers();

    descartadas.forEach((item) => {
      const centro: [number, number] = [item.zona.latitud, item.zona.longitud];
      circle(centro, {
        radius: RADIO_DE_ANALISIS_M,
        color: COLOR_SIN_PODIO,
        weight: 1,
        dashArray: '4 5',
        fillOpacity: 0.02,
      })
        .bindTooltip(`${item.zona.nombre}: descartada. ${item.motivos[0] ?? ''}`)
        .addTo(grupo);
    });

    ranking.forEach((item, indice) => {
      const color = COLOR_DE_COINCIDENCIA[item.coincidencia];
      const activa = item.zona.id === seleccionadaId;
      const esElegido = item.zona.id === puntoElegidoId;
      const centro: [number, number] = [item.zona.latitud, item.zona.longitud];
      circle(centro, {
        radius: RADIO_DE_ANALISIS_M,
        color,
        weight: activa ? 3 : 1.5,
        fillColor: color,
        // Cuanto mejor puntaje, más se marca la zona.
        fillOpacity: (activa ? 0.12 : 0.04) + 0.14 * (item.puntaje / 10),
      }).addTo(grupo);
      marker(centro, {
        title: `Puesto ${indice + 1}: ${item.zona.nombre} (${formatoPuntaje(item.puntaje)})`,
        alt: `Puesto ${indice + 1}: ${item.zona.nombre}`,
        icon: divIcon({
          className:
            `donde-pin ${activa ? 'activa' : ''} ${indice > 2 && !esElegido ? 'chico' : ''} ${esElegido ? 'punto' : ''}`
              .replace(/\s+/g, ' ')
              .trim(),
          html: `<span style="--color:${color}">${esElegido ? '★' : indice + 1}</span>`,
          iconSize: [32, 32],
          iconAnchor: [16, 16],
        }),
      })
        .on('click', () => seleccionar.current(item.zona.id))
        .addTo(grupo);
    });

    try {
      const primera = ranking.at(0);
      if (primerAjuste.current && primera) {
        const limites = latLngBounds([
          local.current?.latitud != null && local.current.longitud != null
            ? [local.current.latitud, local.current.longitud]
            : [primera.zona.latitud, primera.zona.longitud],
        ]);
        ranking
          .slice(0, 3)
          .forEach((item) => limites.extend([item.zona.latitud, item.zona.longitud]));
        instancia.fitBounds(limites, { padding: [40, 40], maxZoom: 15, animate: false });
        primerAjuste.current = false;
      } else if (seleccionadaId && seleccionadaId !== ultimaSeleccion.current) {
        // Solo se mueve el mapa cuando la persona elige otra zona, no en cada cambio de filtro.
        const elegida = ranking.find((item) => item.zona.id === seleccionadaId);
        if (elegida)
          instancia.panTo([elegida.zona.latitud, elegida.zona.longitud], { animate: true });
      }
      ultimaSeleccion.current = seleccionadaId;
    } catch {
      // Sin tamaño (por ejemplo, en pruebas): el mapa queda donde está.
    }
  }, [ranking, descartadas, seleccionadaId, puntoElegidoId]);

  // El lugar tocado se ve al instante; cuando llega la medición, entra al ranking como una zona más.
  useEffect(() => {
    const grupo = grupoDelElegido.current;
    if (!grupo) return;
    grupo.clearLayers();
    if (!punto) return;
    circle([punto.latitud, punto.longitud], {
      radius: RADIO_DE_ANALISIS_M,
      color: COLOR_DEL_ELEGIDO,
      weight: 2,
      dashArray: '2 6',
      fillOpacity: 0.04,
      interactive: false,
    }).addTo(grupo);
    circleMarker([punto.latitud, punto.longitud], {
      radius: 5,
      color: '#ffffff',
      weight: 2,
      fillColor: COLOR_DEL_ELEGIDO,
      fillOpacity: 1,
      interactive: false,
    }).addTo(grupo);
  }, [punto]);

  useEffect(() => {
    const grupo = grupoDePuntos.current;
    if (!grupo) return;
    grupo.clearLayers();
    puntos
      .filter((punto) => capas[punto.tipo])
      .forEach((punto) => {
        circleMarker([punto.latitud, punto.longitud], {
          radius: 6,
          color: '#ffffff',
          weight: 1.5,
          fillColor: COLOR_DE_PUNTO[punto.tipo],
          fillOpacity: 0.95,
        })
          .bindTooltip(punto.nombre)
          .addTo(grupo);
      });
  }, [puntos, capas]);

  return { contenedor, error };
}
