import type { Punto } from '@/types/ubicacion';
import { MapaUbicacion } from './MapaUbicacion';

type Props = {
  elegida: boolean;
  punto: Punto | null;
  latitud: string;
  longitud: string;
  onCoordenada: (eje: 'latitud' | 'longitud', valor: string) => void;
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
        <div className="ubicacion-sin-mapa">
          {props.elegida
            ? 'Revisá las coordenadas para volver a ver el mapa.'
            : 'Elegí una dirección para ver tu punto.'}
        </div>
      )}
      {props.elegida && (
        <div className="ubicacion-coordenadas">
          <div className="field">
            <label htmlFor="ubicacion-latitud">Latitud</label>
            <input
              id="ubicacion-latitud"
              className="input"
              inputMode="decimal"
              value={props.latitud}
              aria-describedby="limites-coordenadas"
              onChange={(evento) => props.onCoordenada('latitud', evento.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="ubicacion-longitud">Longitud</label>
            <input
              id="ubicacion-longitud"
              className="input"
              inputMode="decimal"
              value={props.longitud}
              aria-describedby="limites-coordenadas"
              onChange={(evento) => props.onCoordenada('longitud', evento.target.value)}
            />
          </div>
          <p id="limites-coordenadas" className="ubicacion-ayuda">
            Latitud de -90 a 90 y longitud de -180 a 180.
          </p>
        </div>
      )}
    </section>
  );
}
