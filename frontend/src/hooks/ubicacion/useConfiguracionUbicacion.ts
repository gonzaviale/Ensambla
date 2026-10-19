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
import { useConfiguracionNegocio } from '@/hooks/negocio/queries';
import { useActualizarNegocio } from '@/hooks/negocio/mutations';
import { cambiosNegocioSchema } from '@/types/negocio';

// Los cambios son un borrador hasta confirmar el PATCH. La carga nunca reemplaza una edición en curso.
export function useConfiguracionUbicacion() {
  const configuracion = useConfiguracionNegocio();
  const guardarNegocio = useActualizarNegocio();
  const [inicializada, setInicializada] = useState(false);
  const [nombre, setNombre] = useState('');
  const [localidad, setLocalidad] = useState('');
  const [comparteDatosZona, setComparteDatosZona] = useState(true);
  const [guardado, setGuardado] = useState(false);
  const [errorGuardado, setErrorGuardado] = useState('');
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

  useEffect(() => {
    const datos = configuracion.data;
    if (!datos || configuracion.isFetching || inicializada) return;
    setNombre(datos.nombre);
    setLocalidad(datos.ubicacion?.localidad ?? '');
    setComparteDatosZona(datos.comparteDatosZona);
    setRubro(datos.ubicacion || datos.rubro !== 'OTRO' ? datos.rubro : '');
    const ubicacion = datos.ubicacion;
    if (ubicacion?.direccion) {
      setConsulta(ubicacion.direccion);
      setDireccion({
        direccion: ubicacion.direccion,
        latitud: ubicacion.latitud,
        longitud: ubicacion.longitud,
      });
      setLatitud(String(ubicacion.latitud));
      setLongitud(String(ubicacion.longitud));
    }
    setInicializada(true);
  }, [configuracion.data, configuracion.isFetching, inicializada]);

  const cambio = () => {
    setGuardado(false);
    setErrorGuardado('');
    setRevision(null);
    setErrorRevision('');
  };

  useEffect(() => () => pedido.current?.abort(), []);

  const cambiarConsulta = (valor: string) => {
    cambio();
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
    cambio();
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
    cambio();
    setDireccion(valor);
    setLatitud(String(valor.latitud));
    setLongitud(String(valor.longitud));
    setErrorRevision('');
    setRevision(null);
  };

  const moverPunto = (punto: Punto) => {
    cambio();
    setLatitud(punto.latitud.toFixed(6));
    setLongitud(punto.longitud.toFixed(6));
    setRevision(null);
    setErrorRevision('');
  };

  const cambiarCoordenada = (eje: 'latitud' | 'longitud', valor: string) => {
    cambio();
    (eje === 'latitud' ? setLatitud : setLongitud)(valor);
    setRevision(null);
    setErrorRevision('');
  };

  const cambiarRubro = (valor: string) => {
    cambio();
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
    return resultado.configuracion;
  };

  const guardar = async () => {
    setGuardado(false);
    setErrorGuardado('');
    const revisada = revisar();
    if (!revisada) return;
    const datos = cambiosNegocioSchema.safeParse({
      nombre,
      rubro: revisada.rubro.codigo,
      ubicacion: { direccion: revisada.direccion, localidad, ...revisada.punto },
      comparteDatosZona,
    });
    if (!datos.success) {
      setErrorGuardado('Completá el nombre y la localidad del comercio (hasta 120 caracteres).');
      return;
    }
    try {
      const respuesta = await guardarNegocio.mutateAsync(datos.data);
      setNombre(respuesta.nombre);
      setLocalidad(respuesta.ubicacion?.localidad ?? '');
      const ubicacion = respuesta.ubicacion;
      if (ubicacion?.direccion) {
        setDireccion({
          direccion: ubicacion.direccion,
          latitud: ubicacion.latitud,
          longitud: ubicacion.longitud,
        });
        setLatitud(String(ubicacion.latitud));
        setLongitud(String(ubicacion.longitud));
      }
      setGuardado(true);
      setRevision(null);
    } catch (error) {
      setErrorGuardado(
        leerErrorServidor(error)?.mensaje ?? 'No pudimos guardar. Tus cambios siguen en pantalla.',
      );
    }
  };

  return {
    nombre,
    localidad,
    comparteDatosZona,
    cambiarNombre: (valor: string) => {
      cambio();
      setNombre(valor);
    },
    cambiarLocalidad: (valor: string) => {
      cambio();
      setLocalidad(valor);
    },
    cambiarCompartir: (valor: boolean) => {
      cambio();
      setComparteDatosZona(valor);
    },
    cargandoConfiguracion: !inicializada && !configuracion.isError,
    errorConfiguracion: configuracion.isError
      ? 'No pudimos cargar la configuración de tu comercio.'
      : '',
    reintentarConfiguracion: () => void configuracion.refetch(),
    guardar,
    guardando: guardarNegocio.isPending,
    guardado,
    errorGuardado,
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
