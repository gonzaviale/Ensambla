import { useEffect, useRef, useState, type FormEvent } from 'react';
import { buscarDirecciones } from '@/api/ubicacion';
import { env } from '@/config/env';
import { leerErrorServidor } from '@/hooks/errores';
import { errorDeConsulta, normalizarConsulta } from '@/lib/ubicacion';
import type { Direccion, Punto } from '@/types/ubicacion';
import { useAnalisisDeZona } from './queries';

const MENSAJE_SIN_LUGAR = 'Escribí un barrio o una dirección.';
const MENSAJE_SIN_DATOS = 'Todavía no tenemos datos de esa zona.';
const MENSAJE_FALLO = 'No pudimos analizar esa zona. Probá de nuevo.';
// El servidor acepta hasta 120 caracteres de lugar; el buscador de direcciones, hasta 200.
const LARGO_MAXIMO_DEL_LUGAR = 120;
const redondear = (valor: number) => Number(valor.toFixed(6));

/**
 * «Otra zona»: la persona busca una dirección, la elige, ajusta el punto en el mapa y analiza. El análisis
 * viaja con el punto; si no hay punto pero sí algo escrito, viaja el texto. Cualquier cambio de lugar
 * descarta el resultado anterior para que no se lea contra otro punto.
 */
export function useAnalisisDeOtraZona() {
  const analisis = useAnalisisDeZona();
  const { reset } = analisis;
  const [consulta, setConsulta] = useState('');
  const [resultados, setResultados] = useState<Direccion[] | null>(null);
  const [buscando, setBuscando] = useState(false);
  const [errorBusqueda, setErrorBusqueda] = useState('');
  const [elegida, setElegida] = useState<Direccion | null>(null);
  const [punto, setPunto] = useState<Punto | null>(null);
  const [errorPedido, setErrorPedido] = useState('');
  const pedido = useRef<AbortController | null>(null);

  useEffect(() => () => pedido.current?.abort(), []);

  const descartarAnalisis = () => {
    setErrorPedido('');
    reset();
  };

  const cambiarConsulta = (valor: string) => {
    pedido.current?.abort();
    setBuscando(false);
    setConsulta(valor);
    setResultados(null);
    setElegida(null);
    setPunto(null);
    setErrorBusqueda('');
    descartarAnalisis();
  };

  const buscar = async (evento: FormEvent) => {
    evento.preventDefault();
    const error = errorDeConsulta(consulta);
    setErrorBusqueda(error);
    if (error) return;
    pedido.current?.abort();
    const actual = new AbortController();
    pedido.current = actual;
    setBuscando(true);
    setResultados(null);
    setElegida(null);
    setPunto(null);
    descartarAnalisis();
    try {
      const respuesta = await buscarDirecciones(normalizarConsulta(consulta), actual.signal);
      if (!actual.signal.aborted) setResultados(respuesta.resultados);
    } catch (error) {
      if (!actual.signal.aborted)
        setErrorBusqueda(leerErrorServidor(error)?.mensaje ?? 'No pudimos buscar.');
    } finally {
      if (!actual.signal.aborted) setBuscando(false);
    }
  };

  const elegirDireccion = (direccion: Direccion) => {
    setElegida(direccion);
    setPunto({ latitud: direccion.latitud, longitud: direccion.longitud });
    descartarAnalisis();
  };

  const moverPunto = (nuevo: Punto) => {
    setPunto(nuevo);
    descartarAnalisis();
  };

  const analizar = (evento?: FormEvent) => {
    evento?.preventDefault();
    if (punto) {
      setErrorPedido('');
      analisis.mutate({ latitud: redondear(punto.latitud), longitud: redondear(punto.longitud) });
      return;
    }
    const lugar = normalizarConsulta(consulta).slice(0, LARGO_MAXIMO_DEL_LUGAR);
    if (lugar.length < 2) {
      setErrorPedido(MENSAJE_SIN_LUGAR);
      return;
    }
    setErrorPedido('');
    analisis.mutate({ lugar });
  };

  const errorDelAnalisis = analisis.isError
    ? leerErrorServidor(analisis.error)?.status === 404
      ? MENSAJE_SIN_DATOS
      : MENSAJE_FALLO
    : '';

  return {
    consulta,
    cambiarConsulta,
    buscar,
    buscando,
    resultados,
    errorBusqueda,
    elegida,
    elegirDireccion,
    punto,
    moverPunto,
    analizar,
    analizando: analisis.isPending,
    resultado: analisis.data,
    error: errorPedido || errorDelAnalisis,
    /** Con todo simulado (modo demo) los datos de otra zona son de ejemplo. */
    simulado: env.mocks === 'completo',
  };
}
