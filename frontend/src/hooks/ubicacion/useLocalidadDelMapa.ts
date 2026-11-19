import { useEffect, useState, type RefObject } from 'react';
import { resolverLocalidad } from '@/api/ubicacion';
import { leerErrorServidor } from '@/hooks/errores';
import { coincideLocalidad } from '@/lib/ubicacion';
import type { Punto } from '@/types/ubicacion';

// Espera tras el último movimiento del punto: el proveedor del mapa admite una consulta por segundo.
const ESPERA_LOCALIDAD_MS = 700;

// Las localidades que el mapa reconoce en un punto, con la clave del punto al que pertenecen.
type LocalidadesDelPunto = { clave: string; localidades: string[] };

const claveDelPunto = (punto: Punto | null) => (punto ? `${punto.latitud},${punto.longitud}` : '');

const mensajeDeFallo = (error: unknown) =>
  leerErrorServidor(error)?.mensaje ?? 'No pudimos verificar la localidad con el mapa.';

// Una respuesta vale sólo para el punto que la pidió: al mover el pin deja de contar al instante.
const localidadesVigentes = (delMapa: LocalidadesDelPunto | null, punto: Punto | null) =>
  delMapa?.clave === claveDelPunto(punto) && punto ? delMapa.localidades : null;

const mensajeDeNoCoincidencia = (localidadDelMapa: string | null) =>
  `La localidad no coincide con la del mapa${localidadDelMapa ? ` (${localidadDelMapa})` : ''}.`;

type Parametros = {
  punto: Punto | null;
  localidad: string;
  ponerLocalidad: (localidad: string) => void;
  /** Verdadero tras elegir o mover el punto: la próxima localidad del mapa reemplaza la anterior. */
  seguirAlMapa: RefObject<boolean>;
};

/** Verifica con el mapa que la localidad escrita sea la del punto elegido. */
export function useLocalidadDelMapa({
  punto,
  localidad,
  ponerLocalidad,
  seguirAlMapa,
}: Parametros) {
  const [delMapa, setDelMapa] = useState<LocalidadesDelPunto | null>(null);
  const [errorLocalidad, setErrorLocalidad] = useState('');
  const [reintentos, setReintentos] = useState(0);

  useEffect(() => {
    setErrorLocalidad('');
    if (!punto) return;
    const actual = new AbortController();
    const verificar = async () => {
      try {
        const respuesta = await resolverLocalidad(punto, actual.signal);
        if (actual.signal.aborted) return;
        setDelMapa({ clave: claveDelPunto(punto), localidades: respuesta.localidades });
        if (seguirAlMapa.current && respuesta.localidad) ponerLocalidad(respuesta.localidad);
        seguirAlMapa.current = false;
      } catch (error) {
        if (!actual.signal.aborted) setErrorLocalidad(mensajeDeFallo(error));
      }
    };
    const temporizador = setTimeout(verificar, ESPERA_LOCALIDAD_MS);
    return () => {
      clearTimeout(temporizador);
      actual.abort();
    };
  }, [punto, reintentos, ponerLocalidad, seguirAlMapa]);

  const localidadesDelMapa = localidadesVigentes(delMapa, punto);
  const localidadDelMapa = localidadesDelMapa?.[0] ?? null;
  const noCoincide =
    localidadesDelMapa !== null && !coincideLocalidad(localidad, localidadesDelMapa);

  return {
    localidadDelMapa,
    errorDeLocalidad:
      errorLocalidad || (noCoincide ? mensajeDeNoCoincidencia(localidadDelMapa) : ''),
    verificandoLocalidad: !!punto && localidadesDelMapa === null && !errorLocalidad,
    localidadBloquea: !!punto && (localidadesDelMapa === null || noCoincide),
    reintentarLocalidad: () => setReintentos((cantidad) => cantidad + 1),
  };
}
