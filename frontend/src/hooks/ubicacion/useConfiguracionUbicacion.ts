import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useQuery } from '@tanstack/react-query';
import { buscarDirecciones, listarRubros } from '@/api/ubicacion';
import { leerErrorServidor } from '@/hooks/errores';
import {
  errorDeConsulta,
  leerPunto,
  normalizarConsulta,
  revisarConfiguracion,
} from '@/lib/ubicacion';
import type { ConfiguracionUbicacion, Direccion, Punto } from '@/types/ubicacion';
import { env } from '@/config/env';

// Borrador acotado a esta pantalla: no escribe preferencias, localStorage ni configuración del negocio.
export function useConfiguracionUbicacion() {
  const rubros = useQuery({
    queryKey: ['ubicacion', 'rubros'],
    queryFn: ({ signal }) => listarRubros(signal),
    staleTime: Infinity,
    retry: false,
  });
  const [consulta, setConsulta] = useState('');
  const [resultados, setResultados] = useState<Direccion[] | null>(null);
  const [buscando, setBuscando] = useState(false);
  const [errorBusqueda, setErrorBusqueda] = useState('');
  const [direccion, setDireccion] = useState<Direccion | null>(null);
  const [latitud, setLatitud] = useState('');
  const [longitud, setLongitud] = useState('');
  const [rubro, setRubro] = useState('');
  const [errorRevision, setErrorRevision] = useState('');
  const [revision, setRevision] = useState<ConfiguracionUbicacion | null>(null);
  const pedido = useRef<AbortController | null>(null);

  useEffect(() => () => pedido.current?.abort(), []);

  const cambiarConsulta = (valor: string) => {
    pedido.current?.abort();
    setBuscando(false);
    setConsulta(valor);
    setResultados(null);
    setDireccion(null);
    setLatitud('');
    setLongitud('');
    setErrorBusqueda('');
    setErrorRevision('');
    setRevision(null);
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
    setDireccion(null);
    setRevision(null);
    setErrorRevision('');
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

  const elegirDireccion = (valor: Direccion) => {
    setDireccion(valor);
    setLatitud(String(valor.latitud));
    setLongitud(String(valor.longitud));
    setErrorRevision('');
    setRevision(null);
  };

  const moverPunto = (punto: Punto) => {
    setLatitud(punto.latitud.toFixed(6));
    setLongitud(punto.longitud.toFixed(6));
    setRevision(null);
    setErrorRevision('');
  };

  const cambiarCoordenada = (eje: 'latitud' | 'longitud', valor: string) => {
    (eje === 'latitud' ? setLatitud : setLongitud)(valor);
    setRevision(null);
    setErrorRevision('');
  };

  const cambiarRubro = (valor: string) => {
    setRubro(valor);
    setRevision(null);
    setErrorRevision('');
  };

  const revisar = () => {
    const resultado = revisarConfiguracion(
      direccion,
      latitud,
      longitud,
      rubro,
      rubros.data?.rubros ?? [],
    );
    setErrorRevision(resultado.error);
    setRevision(resultado.configuracion);
  };

  return {
    busquedaSimulada: env.mocks === 'completo',
    consulta,
    cambiarConsulta,
    buscar,
    buscando,
    resultados,
    errorBusqueda,
    direccion,
    elegirDireccion,
    latitud,
    longitud,
    cambiarCoordenada,
    moverPunto,
    punto: direccion ? leerPunto(latitud, longitud) : null,
    rubro,
    cambiarRubro,
    rubros: rubros.data?.rubros ?? [],
    cargandoRubros: rubros.isPending,
    errorRubros: rubros.isError ? 'No pudimos cargar los rubros.' : '',
    reintentarRubros: () => void rubros.refetch(),
    revisar,
    revision,
    errorRevision,
  };
}
