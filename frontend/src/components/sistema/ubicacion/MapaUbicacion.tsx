import 'leaflet/dist/leaflet.css';
import { useMapaUbicacion } from '@/hooks/ubicacion/useMapaUbicacion';
import type { Punto } from '@/types/ubicacion';

export function MapaUbicacion({
  punto,
  onMover,
}: {
  punto: Punto;
  onMover: (punto: Punto) => void;
}) {
  const { contenedor, error } = useMapaUbicacion(punto, onMover);
  return (
    <>
      <div
        ref={contenedor}
        className="ubicacion-mapa"
        role="region"
        aria-label="Mapa de tu comercio"
      />
      {error && (
        <p role="status">No se pudo cargar parte del mapa. Podés ajustar las coordenadas abajo.</p>
      )}
    </>
  );
}
