import type { Punto } from '@/types/ubicacion';
import { MapaUbicacion } from './MapaUbicacion';

type Props = {
  punto: Punto | null;
  onMover: (punto: Punto) => void;
};

export function AjusteUbicacion(props: Props) {
  return (
    <section className="ubicacion-panel ubicacion-ajuste" aria-labelledby="titulo-punto">
      <h2 id="titulo-punto">Revisá el punto en el mapa</h2>
      <p>Si hace falta, arrastrá el marcador al lugar exacto.</p>
      {props.punto ? (
        <MapaUbicacion punto={props.punto} onMover={props.onMover} />
      ) : (
        <div className="ubicacion-sin-mapa">Elegí una dirección para ver tu punto.</div>
      )}
    </section>
  );
}
