import { useEffect, useRef, useState } from 'react';
import {
  divIcon,
  map,
  marker,
  tileLayer,
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
    const centro: [number, number] = [inicio.current.latitud, inicio.current.longitud];
    const instancia = map(contenedor.current, {
      scrollWheelZoom: false,
      zoomControl: true,
    }).setView(centro, cartografia.zoomInicial);
    const capa = tileLayer(cartografia.url, {
      attribution: cartografia.atribucion,
      maxZoom: cartografia.zoomMaximo,
      // No se descargan mosaicos fuera de la vista ni se anticipa su carga.
      keepBuffer: 0,
      updateWhenIdle: true,
    }).addTo(instancia);
    capa.on('tileerror', () => setError(true));
    const marcador = marker(centro, {
      draggable: true,
      autoPan: true,
      title: textos.current.titulo,
      alt: textos.current.alternativo,
      icon: divIcon({
        className: 'ubicacion-pin',
        html: '<span></span>',
        iconSize: [28, 28],
        iconAnchor: [14, 14],
      }),
    }).addTo(instancia);
    marcador.on('dragend', () => {
      const posicion = marcador.getLatLng().wrap();
      mover.current({ latitud: posicion.lat, longitud: posicion.lng });
    });
    instancia.on('click', (evento: LeafletMouseEvent) => {
      const posicion = evento.latlng.wrap();
      mover.current({ latitud: posicion.lat, longitud: posicion.lng });
    });
    mapa.current = instancia;
    pin.current = marcador;
    return () => {
      instancia.remove();
      mapa.current = null;
      pin.current = null;
    };
  }, []);

  useEffect(() => {
    const centro: [number, number] = [punto.latitud, punto.longitud];
    pin.current?.setLatLng(centro);
    mapa.current?.panTo(centro, { animate: false });
  }, [punto.latitud, punto.longitud]);

  return { contenedor, error };
}
