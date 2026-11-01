import 'leaflet/dist/leaflet.css';
import { useMapaUbicacion, type TextosDelMarcador } from '@/hooks/ubicacion/useMapaUbicacion';
import type { Punto } from '@/types/ubicacion';

export function MapaUbicacion({
  punto,
  onMover,
  etiqueta = 'Mapa de tu comercio',
  marcador,
  ayudaSinMapa = 'No se pudo cargar parte del mapa. Igual se usa el punto de la dirección que elegiste.',
}: {
  punto: Punto;
  onMover: (punto: Punto) => void;
  /** Nombre del mapa para quien usa lector de pantalla. */
  etiqueta?: string;
  marcador?: TextosDelMarcador;
  /** Qué hacer si el mapa no carga del todo (depende de si la pantalla tiene coordenadas editables). */
  ayudaSinMapa?: string;
}) {
  const { contenedor, error } = useMapaUbicacion(punto, onMover, marcador);
  return (
    <>
      <div ref={contenedor} className="ubicacion-mapa" role="region" aria-label={etiqueta} />
      {error && <p role="status">{ayudaSinMapa}</p>}
    </>
  );
}
