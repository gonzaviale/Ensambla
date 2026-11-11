import 'leaflet/dist/leaflet.css';
import { useMapaDeZonas } from '@/hooks/dondeAbrir/useMapaDeZonas';
import type { Descartada, Punto, TipoDePunto, ZonaPuntuada } from '@/lib/dondeAbrir';

type Props = {
  ranking: ZonaPuntuada[];
  descartadas: Descartada[];
  seleccionadaId: string;
  puntos: Punto[];
  capas: Record<TipoDePunto, boolean>;
  onSeleccionar: (id: string) => void;
  localActual?: { latitud: number | null; longitud: number | null; nombre: string };
};

export function MapaDeZonas(props: Props) {
  const { contenedor, error } = useMapaDeZonas(props);
  return (
    <>
      <div
        ref={contenedor}
        className="donde-mapa"
        role="region"
        aria-label="Mapa con las zonas, tu local actual y lo que hay alrededor"
      />
      {error && (
        <p role="status" className="donde-mapa-aviso">
          No se pudo cargar parte del mapa. Las zonas y sus datos se ven igual en la lista.
        </p>
      )}
    </>
  );
}
