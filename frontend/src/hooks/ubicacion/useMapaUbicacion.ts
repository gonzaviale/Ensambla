import { useEffect, useRef, useState } from 'react';
import {
  divIcon,
  map,
  marker,
  tileLayer,
  type LatLng,
  type LeafletMouseEvent,
  type Map,
  type Marker,
} from 'leaflet';
import { cartografia } from '@/config/ubicacion';
import type { Punto } from '@/types/ubicacion';

// Lo que lee un lector de pantalla del marcador; cambia según qué se está ubicando.
export type TextosDelMarcador = { titulo: string; alternativo: string };

const MARCADOR_DEL_COMERCIO: TextosDelMarcador = {
  titulo: 'Ubicación de tu comercio',
  alternativo: 'Punto de tu comercio: arrastrá para ajustarlo',
};

type Coordenada = [number, number];

const coordenadasDe = (punto: Punto): Coordenada => [punto.latitud, punto.longitud];

const puntoDe = (posicion: LatLng): Punto => {
  const { lat, lng } = posicion.wrap();
  return { latitud: lat, longitud: lng };
};

function crearMapa(contenedor: HTMLDivElement, centro: Coordenada, alFallar: () => void): Map {
  const instancia = map(contenedor, { scrollWheelZoom: false, zoomControl: true }).setView(
    centro,
    cartografia.zoomInicial,
  );
  tileLayer(cartografia.url, {
    attribution: cartografia.atribucion,
    maxZoom: cartografia.zoomMaximo,
    // No se descargan mosaicos fuera de la vista ni se anticipa su carga.
    keepBuffer: 0,
    updateWhenIdle: true,
  })
    .addTo(instancia)
    .on('tileerror', alFallar);
  return instancia;
}

function crearMarcador(centro: Coordenada, textos: TextosDelMarcador): Marker {
  return marker(centro, {
    draggable: true,
    autoPan: true,
    title: textos.titulo,
    alt: textos.alternativo,
    icon: divIcon({
      className: 'ubicacion-pin',
      html: '<span></span>',
      iconSize: [28, 28],
      iconAnchor: [14, 14],
    }),
  });
}

export function useMapaUbicacion(
  punto: Punto,
  onMover: (punto: Punto) => void,
  marcadorTextos: TextosDelMarcador = MARCADOR_DEL_COMERCIO,
) {
  const contenedor = useRef<HTMLDivElement>(null);
  const mapa = useRef<Map | null>(null);
  const pin = useRef<Marker | null>(null);
  const mover = useRef(onMover);
  const inicio = useRef(punto);
  const textos = useRef(marcadorTextos);
  const [error, setError] = useState(false);

  useEffect(() => {
    mover.current = onMover;
  }, [onMover]);

  useEffect(() => {
    if (!contenedor.current) return;
    const centro = coordenadasDe(inicio.current);
    const instancia = crearMapa(contenedor.current, centro, () => setError(true));
    const marcador = crearMarcador(centro, textos.current).addTo(instancia);
    marcador.on('dragend', () => mover.current(puntoDe(marcador.getLatLng())));
    instancia.on('click', (evento: LeafletMouseEvent) => mover.current(puntoDe(evento.latlng)));
    mapa.current = instancia;
    pin.current = marcador;
    return () => {
      instancia.remove();
      mapa.current = null;
      pin.current = null;
    };
  }, []);

  useEffect(() => {
    const centro = coordenadasDe({ latitud: punto.latitud, longitud: punto.longitud });
    pin.current?.setLatLng(centro);
    mapa.current?.panTo(centro, { animate: false });
  }, [punto.latitud, punto.longitud]);

  return { contenedor, error };
}
