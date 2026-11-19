import { useEffect, useMemo, useRef, useState, type FormEvent, type RefObject } from 'react';
import { buscarDirecciones } from '@/api/ubicacion';
import { leerErrorServidor } from '@/hooks/errores';
import { errorDeConsulta, leerPunto, localidadEscrita, normalizarConsulta } from '@/lib/ubicacion';
import type { Direccion, Punto } from '@/types/ubicacion';

type UbicacionGuardada = { direccion: string | null; latitud: number; longitud: number };

type Parametros = {
  alCambiar: () => void;
  ponerLocalidad: (localidad: string) => void;
  /** Se marca al elegir o mover el punto, para que la localidad del mapa reemplace a la anterior. */
  seguirAlMapa: RefObject<boolean>;
};

/** La dirección del comercio: se escribe completa en un solo campo (calle, altura y localidad). */
export function useDireccionDelComercio({ alCambiar, ponerLocalidad, seguirAlMapa }: Parametros) {
  const [consulta, setConsulta] = useState('');
  const [resultados, setResultados] = useState<Direccion[] | null>(null);
  const [buscando, setBuscando] = useState(false);
  const [errorBusqueda, setErrorBusqueda] = useState('');
  const [direccion, setDireccion] = useState<Direccion | null>(null);
  const [latitud, setLatitud] = useState('');
  const [longitud, setLongitud] = useState('');
  const pedido = useRef<AbortController | null>(null);

  const punto = useMemo(
    () => (direccion ? leerPunto(latitud, longitud) : null),
    [direccion, latitud, longitud],
  );

  useEffect(() => () => pedido.current?.abort(), []);

  const ponerUbicacion = (ubicacion: UbicacionGuardada) => {
    if (!ubicacion.direccion) return;
    setDireccion({
      direccion: ubicacion.direccion,
      latitud: ubicacion.latitud,
      longitud: ubicacion.longitud,
    });
    setLatitud(String(ubicacion.latitud));
    setLongitud(String(ubicacion.longitud));
  };

  const descartarElPuntoAnterior = () => {
    setDireccion(null);
    setLatitud('');
    setLongitud('');
  };

  // Cambiar la dirección escrita descarta la búsqueda y el punto anteriores.
  const cambiarConsulta = (valor: string) => {
    alCambiar();
    pedido.current?.abort();
    setBuscando(false);
    setConsulta(valor);
    setResultados(null);
    setErrorBusqueda('');
    descartarElPuntoAnterior();
  };

  const empezarBusqueda = () => {
    alCambiar();
    pedido.current?.abort();
    const actual = new AbortController();
    pedido.current = actual;
    setBuscando(true);
    setResultados(null);
    setDireccion(null);
    return actual;
  };

  const buscar = async (evento: FormEvent) => {
    evento.preventDefault();
    const error = errorDeConsulta(consulta);
    setErrorBusqueda(error);
    if (error) return;
    const actual = empezarBusqueda();
    try {
      const respuesta = await buscarDirecciones(normalizarConsulta(consulta), actual.signal);
      if (!actual.signal.aborted) setResultados(respuesta.resultados);
    } catch (fallo) {
      if (!actual.signal.aborted) {
        setErrorBusqueda(leerErrorServidor(fallo)?.mensaje ?? 'No pudimos buscar.');
      }
    } finally {
      if (!actual.signal.aborted) setBuscando(false);
    }
  };

  const elegirDireccion = (valor: Direccion) => {
    alCambiar();
    seguirAlMapa.current = true;
    ponerLocalidad(valor.localidad || localidadEscrita(normalizarConsulta(consulta)));
    setDireccion(valor);
    setLatitud(String(valor.latitud));
    setLongitud(String(valor.longitud));
  };

  const moverPunto = (nuevo: Punto) => {
    alCambiar();
    seguirAlMapa.current = true;
    setLatitud(nuevo.latitud.toFixed(6));
    setLongitud(nuevo.longitud.toFixed(6));
  };

  const limpiarBusqueda = () => {
    setResultados(null);
    setConsulta('');
  };

  return {
    consulta,
    cambiarConsulta,
    buscar,
    buscando,
    resultados,
    errorBusqueda,
    direccion,
    elegirDireccion,
    moverPunto,
    punto,
    ponerUbicacion,
    limpiarBusqueda,
  };
}
