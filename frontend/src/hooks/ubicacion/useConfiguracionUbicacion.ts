import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { useQuery } from '@tanstack/react-query';
import { buscarDirecciones, listarRubros, resolverLocalidad } from '@/api/ubicacion';
import { leerErrorServidor } from '@/hooks/errores';
import {
  coincideLocalidad,
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

// Espera tras el último movimiento del punto: el proveedor del mapa admite una consulta por segundo.
const ESPERA_LOCALIDAD_MS = 700;

// Las localidades que el mapa reconoce en un punto, con la clave del punto al que pertenecen.
type LocalidadesDelPunto = { clave: string; localidades: string[] };

// Los cambios son un borrador hasta confirmar el PATCH. La carga nunca reemplaza una edición en curso.
export function useConfiguracionUbicacion() {
  const configuracion = useConfiguracionNegocio();
  const guardarNegocio = useActualizarNegocio();
  const [inicializada, setInicializada] = useState(false);
  const [nombre, setNombre] = useState('');
  const [localidad, setLocalidad] = useState('');
  const [comparteDatosZona, setComparteDatosZona] = useState(true);
  // La zona sólo se muestra para la ubicación ya guardada, nunca para un borrador.
  const [zona, setZona] = useState<string | null>(null);
  const [ubicacionGuardada, setUbicacionGuardada] = useState(false);
  const [guardado, setGuardado] = useState(false);
  const [errorGuardado, setErrorGuardado] = useState('');
  const rubros = useQuery({
    queryKey: ['ubicacion', 'rubros'],
    queryFn: ({ signal }) => listarRubros(signal),
    staleTime: Infinity,
    retry: false,
  });
  // La dirección se escribe en dos partes; la localidad es la del comercio, que también se usa para buscar.
  const [calle, setCalle] = useState('');
  const [altura, setAltura] = useState('');
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
  const [delMapa, setDelMapa] = useState<LocalidadesDelPunto | null>(null);
  const [errorLocalidad, setErrorLocalidad] = useState('');
  const [reintentosLocalidad, setReintentosLocalidad] = useState(0);
  // Verdadero tras elegir o mover el punto: la próxima localidad del mapa reemplaza la escrita.
  const seguirAlMapa = useRef(false);

  const punto = useMemo(
    () => (direccion ? leerPunto(latitud, longitud) : null),
    [direccion, latitud, longitud],
  );
  const clavePunto = punto ? `${punto.latitud},${punto.longitud}` : '';

  useEffect(() => {
    setErrorLocalidad('');
    if (!punto) return;
    const actual = new AbortController();
    const temporizador = setTimeout(async () => {
      try {
        const respuesta = await resolverLocalidad(punto, actual.signal);
        if (actual.signal.aborted) return;
        setDelMapa({
          clave: `${punto.latitud},${punto.longitud}`,
          localidades: respuesta.localidades,
        });
        if (seguirAlMapa.current && respuesta.localidad) setLocalidad(respuesta.localidad);
        seguirAlMapa.current = false;
      } catch (error) {
        if (!actual.signal.aborted)
          setErrorLocalidad(
            leerErrorServidor(error)?.mensaje ?? 'No pudimos verificar la localidad con el mapa.',
          );
      }
    }, ESPERA_LOCALIDAD_MS);
    return () => {
      clearTimeout(temporizador);
      actual.abort();
    };
  }, [punto, reintentosLocalidad]);

  // Una respuesta vale sólo para el punto que la pidió: al mover el pin deja de contar al instante.
  const localidadesDelMapa = delMapa?.clave === clavePunto && punto ? delMapa.localidades : null;
  const localidadDelMapa = localidadesDelMapa?.[0] ?? null;
  const verificandoLocalidad = !!punto && localidadesDelMapa === null && !errorLocalidad;
  const localidadNoCoincide =
    localidadesDelMapa !== null && !coincideLocalidad(localidad, localidadesDelMapa);
  const errorDeLocalidad =
    errorLocalidad ||
    (localidadNoCoincide
      ? `La localidad no coincide con la del mapa${localidadDelMapa ? ` (${localidadDelMapa})` : ''}.`
      : '');
  const localidadBloquea = !!punto && (localidadesDelMapa === null || localidadNoCoincide);

  useEffect(() => {
    const datos = configuracion.data;
    if (!datos || configuracion.isFetching || inicializada) return;
    setNombre(datos.nombre);
    setLocalidad(datos.ubicacion?.localidad ?? '');
    setComparteDatosZona(datos.comparteDatosZona);
    setZona(datos.zona);
    setUbicacionGuardada(!!datos.ubicacion);
    setRubro(datos.ubicacion || datos.rubro !== 'OTRO' ? datos.rubro : '');
    const ubicacion = datos.ubicacion;
    if (ubicacion?.direccion) {
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

  // Cambiar la calle o la altura descarta la búsqueda y el punto anteriores.
  const cambiarDireccion = (parte: 'calle' | 'altura', valor: string) => {
    cambio();
    pedido.current?.abort();
    setBuscando(false);
    (parte === 'calle' ? setCalle : setAltura)(valor);
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
    const consulta = [`${calle.trim()} ${altura.trim()}`.trim(), localidad.trim()]
      .filter(Boolean)
      .join(', ');
    const error =
      !calle.trim() || !altura.trim() || !localidad.trim()
        ? 'Escribí calle, altura y localidad.'
        : errorDeConsulta(consulta);
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
    seguirAlMapa.current = true;
    if (valor.localidad) setLocalidad(valor.localidad);
    setDireccion(valor);
    setLatitud(String(valor.latitud));
    setLongitud(String(valor.longitud));
    setErrorRevision('');
    setRevision(null);
  };

  const moverPunto = (nuevo: Punto) => {
    cambio();
    seguirAlMapa.current = true;
    setLatitud(nuevo.latitud.toFixed(6));
    setLongitud(nuevo.longitud.toFixed(6));
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
    if (localidadBloquea) {
      setErrorGuardado(
        errorDeLocalidad || 'Esperá a que verifiquemos la localidad con el mapa para guardar.',
      );
      return;
    }
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
      setZona(respuesta.zona);
      setUbicacionGuardada(!!respuesta.ubicacion);
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
    zona,
    ubicacionGuardada,
    cambiarNombre: (valor: string) => {
      cambio();
      setNombre(valor);
    },
    cambiarLocalidad: (valor: string) => {
      cambio();
      seguirAlMapa.current = false;
      setLocalidad(valor);
    },
    localidadDelMapa,
    errorLocalidad: errorDeLocalidad,
    verificandoLocalidad,
    localidadBloquea,
    usarLocalidadDelMapa: () => {
      if (!localidadDelMapa) return;
      cambio();
      setLocalidad(localidadDelMapa);
    },
    reintentarLocalidad: () => setReintentosLocalidad((cantidad) => cantidad + 1),
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
    calle,
    altura,
    cambiarCalle: (valor: string) => cambiarDireccion('calle', valor),
    cambiarAltura: (valor: string) => cambiarDireccion('altura', valor),
    buscar,
    buscando,
    resultados,
    errorBusqueda,
    direccion,
    elegirDireccion,
    latitud,
    longitud,
    moverPunto,
    punto,
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
