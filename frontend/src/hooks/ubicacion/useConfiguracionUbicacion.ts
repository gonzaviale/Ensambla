import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { useQuery } from '@tanstack/react-query';
import { buscarDirecciones, listarRubros, resolverLocalidad } from '@/api/ubicacion';
import { leerErrorServidor } from '@/hooks/errores';
import { coincideLocalidad, errorDeConsulta, leerPunto, normalizarConsulta } from '@/lib/ubicacion';
import type { Direccion, Punto } from '@/types/ubicacion';
import { env } from '@/config/env';
import { useConfiguracionNegocio } from '@/hooks/negocio/queries';
import { useActualizarNegocio } from '@/hooks/negocio/mutations';
import { cambiosNegocioSchema, type CambiosNegocio } from '@/types/negocio';

// Espera tras el último movimiento del punto: el proveedor del mapa admite una consulta por segundo.
const ESPERA_LOCALIDAD_MS = 700;

// Las localidades que el mapa reconoce en un punto, con la clave del punto al que pertenecen.
type LocalidadesDelPunto = { clave: string; localidades: string[] };

// Cada solapa de la pantalla se guarda por separado, con su propio PATCH.
export type SeccionUbicacion = 'comercio' | 'ubicacion' | 'rubro';
type PorSeccion<T> = Record<SeccionUbicacion, T>;
function todasEn<T>(valor: T): PorSeccion<T> {
  return { comercio: valor, ubicacion: valor, rubro: valor };
}

// Lo que sigue a la última coma de la dirección escrita: "Varela 1903, San Justo" → "San Justo".
function localidadEscrita(consulta: string) {
  const partes = consulta.split(',');
  return partes.length > 1 ? (partes[partes.length - 1]?.trim() ?? '') : '';
}

// Los cambios son un borrador hasta confirmar el PATCH de su solapa. Guardar una solapa no toca
// lo que está cargado en las otras, y la carga nunca reemplaza una edición en curso.
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
  const [rubroGuardado, setRubroGuardado] = useState(false);
  const [sinGuardar, setSinGuardar] = useState(todasEn(false));
  const [guardados, setGuardados] = useState(todasEn(false));
  const [erroresGuardado, setErroresGuardado] = useState(todasEn(''));
  const [guardandoEn, setGuardandoEn] = useState<SeccionUbicacion | null>(null);
  const rubros = useQuery({
    queryKey: ['ubicacion', 'rubros'],
    queryFn: ({ signal }) => listarRubros(signal),
    staleTime: Infinity,
    retry: false,
  });
  // La dirección se escribe completa en un solo campo: calle, altura y localidad.
  const [consulta, setConsulta] = useState('');
  const [resultados, setResultados] = useState<Direccion[] | null>(null);
  const [buscando, setBuscando] = useState(false);
  const [errorBusqueda, setErrorBusqueda] = useState('');
  const [direccion, setDireccion] = useState<Direccion | null>(null);
  const [latitud, setLatitud] = useState('');
  const [longitud, setLongitud] = useState('');
  const [rubro, setRubro] = useState('');
  const pedido = useRef<AbortController | null>(null);
  const [delMapa, setDelMapa] = useState<LocalidadesDelPunto | null>(null);
  const [errorLocalidad, setErrorLocalidad] = useState('');
  const [reintentosLocalidad, setReintentosLocalidad] = useState(0);
  // Verdadero tras elegir o mover el punto: la próxima localidad del mapa reemplaza la anterior.
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

  const ponerUbicacion = (ubicacion: {
    direccion: string | null;
    latitud: number;
    longitud: number;
  }) => {
    if (!ubicacion.direccion) return;
    setDireccion({
      direccion: ubicacion.direccion,
      latitud: ubicacion.latitud,
      longitud: ubicacion.longitud,
    });
    setLatitud(String(ubicacion.latitud));
    setLongitud(String(ubicacion.longitud));
  };

  useEffect(() => {
    const datos = configuracion.data;
    if (!datos || configuracion.isFetching || inicializada) return;
    setNombre(datos.nombre);
    setLocalidad(datos.ubicacion?.localidad ?? '');
    setComparteDatosZona(datos.comparteDatosZona);
    setZona(datos.zona);
    setUbicacionGuardada(!!datos.ubicacion);
    const conRubro = !!datos.ubicacion || datos.rubro !== 'OTRO';
    setRubro(conRubro ? datos.rubro : '');
    setRubroGuardado(conRubro);
    if (datos.ubicacion) ponerUbicacion(datos.ubicacion);
    setInicializada(true);
  }, [configuracion.data, configuracion.isFetching, inicializada]);

  // Un cambio deja la solapa "sin guardar" y borra su aviso de guardado o de error.
  const cambio = (seccion: SeccionUbicacion) => {
    setSinGuardar((actual) => ({ ...actual, [seccion]: true }));
    setGuardados((actual) => ({ ...actual, [seccion]: false }));
    setErroresGuardado((actual) => ({ ...actual, [seccion]: '' }));
  };

  useEffect(() => () => pedido.current?.abort(), []);

  // Cambiar la dirección escrita descarta la búsqueda y el punto anteriores.
  const cambiarConsulta = (valor: string) => {
    cambio('ubicacion');
    pedido.current?.abort();
    setBuscando(false);
    setConsulta(valor);
    setResultados(null);
    setDireccion(null);
    setLatitud('');
    setLongitud('');
    setErrorBusqueda('');
  };

  const buscar = async (evento: FormEvent) => {
    evento.preventDefault();
    const error = errorDeConsulta(consulta);
    setErrorBusqueda(error);
    if (error) return;
    cambio('ubicacion');
    pedido.current?.abort();
    const actual = new AbortController();
    pedido.current = actual;
    setBuscando(true);
    setResultados(null);
    setDireccion(null);
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
    cambio('ubicacion');
    seguirAlMapa.current = true;
    setLocalidad(valor.localidad || localidadEscrita(normalizarConsulta(consulta)));
    setDireccion(valor);
    setLatitud(String(valor.latitud));
    setLongitud(String(valor.longitud));
  };

  const moverPunto = (nuevo: Punto) => {
    cambio('ubicacion');
    seguirAlMapa.current = true;
    setLatitud(nuevo.latitud.toFixed(6));
    setLongitud(nuevo.longitud.toFixed(6));
  };

  // Arma el PATCH de una solapa con sólo sus campos, o explica qué le falta.
  const cambiosDe = (seccion: SeccionUbicacion): { cambios?: CambiosNegocio; error?: string } => {
    if (seccion === 'comercio') {
      const datos = cambiosNegocioSchema.safeParse({ nombre, comparteDatosZona });
      return datos.success
        ? { cambios: datos.data }
        : { error: 'Escribí el nombre del comercio (hasta 120 caracteres).' };
    }
    if (seccion === 'rubro') {
      return rubros.data?.rubros.some((item) => item.codigo === rubro)
        ? { cambios: { rubro } }
        : { error: 'Elegí el rubro de tu comercio.' };
    }
    if (!direccion || !punto) return { error: 'Buscá y elegí la dirección de tu comercio.' };
    if (localidadBloquea)
      return {
        error:
          errorDeLocalidad || 'Esperá a que verifiquemos la localidad con el mapa para guardar.',
      };
    const datos = cambiosNegocioSchema.safeParse({
      ubicacion: { direccion: direccion.direccion, localidad, ...punto },
    });
    return datos.success
      ? { cambios: datos.data }
      : { error: 'No pudimos saber la localidad. Buscá la dirección con la localidad al final.' };
  };

  const guardar = async (seccion: SeccionUbicacion) => {
    setGuardados((actual) => ({ ...actual, [seccion]: false }));
    const { cambios, error } = cambiosDe(seccion);
    setErroresGuardado((actual) => ({ ...actual, [seccion]: error ?? '' }));
    if (!cambios) return;
    setGuardandoEn(seccion);
    try {
      const respuesta = await guardarNegocio.mutateAsync(cambios);
      setZona(respuesta.zona);
      if (seccion === 'comercio') {
        setNombre(respuesta.nombre);
        setComparteDatosZona(respuesta.comparteDatosZona);
      }
      if (seccion === 'ubicacion') {
        setLocalidad(respuesta.ubicacion?.localidad ?? '');
        setUbicacionGuardada(!!respuesta.ubicacion);
        if (respuesta.ubicacion) ponerUbicacion(respuesta.ubicacion);
        setResultados(null);
        setConsulta('');
      }
      if (seccion === 'rubro') {
        setRubro(respuesta.rubro);
        setRubroGuardado(true);
      }
      setSinGuardar((actual) => ({ ...actual, [seccion]: false }));
      setGuardados((actual) => ({ ...actual, [seccion]: true }));
    } catch (error) {
      setErroresGuardado((actual) => ({
        ...actual,
        [seccion]:
          leerErrorServidor(error)?.mensaje ??
          'No pudimos guardar. Tus cambios siguen en pantalla.',
      }));
    } finally {
      setGuardandoEn(null);
    }
  };

  return {
    nombre,
    localidad,
    comparteDatosZona,
    zona,
    ubicacionGuardada,
    cambiarNombre: (valor: string) => {
      cambio('comercio');
      setNombre(valor);
    },
    localidadDelMapa,
    errorLocalidad: errorDeLocalidad,
    verificandoLocalidad,
    localidadBloquea,
    usarLocalidadDelMapa: () => {
      if (!localidadDelMapa) return;
      cambio('ubicacion');
      setLocalidad(localidadDelMapa);
    },
    reintentarLocalidad: () => setReintentosLocalidad((cantidad) => cantidad + 1),
    cambiarCompartir: (valor: boolean) => {
      cambio('comercio');
      setComparteDatosZona(valor);
    },
    cargandoConfiguracion: !inicializada && !configuracion.isError,
    errorConfiguracion: configuracion.isError
      ? 'No pudimos cargar la configuración de tu comercio.'
      : '',
    reintentarConfiguracion: () => void configuracion.refetch(),
    guardar,
    guardandoEn,
    guardados,
    erroresGuardado,
    sinGuardar,
    // Qué solapas ya tienen datos guardados en el servidor.
    conDatosGuardados: {
      comercio: inicializada,
      ubicacion: ubicacionGuardada,
      rubro: rubroGuardado,
    } satisfies PorSeccion<boolean>,
    busquedaSimulada: env.mocks === 'completo',
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
    rubro,
    cambiarRubro: (valor: string) => {
      cambio('rubro');
      setRubro(valor);
    },
    rubros: rubros.data?.rubros ?? [],
    cargandoRubros: rubros.isPending,
    errorRubros: rubros.isError ? 'No pudimos cargar los rubros.' : '',
    reintentarRubros: () => void rubros.refetch(),
  };
}
