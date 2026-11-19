import { useEffect, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { listarRubros } from '@/api/ubicacion';
import { leerErrorServidor } from '@/hooks/errores';
import { cambiosDeLaSolapa } from '@/lib/ubicacion';
import type { PorSeccion, SeccionUbicacion } from '@/types/ubicacion';
import { env } from '@/config/env';
import { useConfiguracionNegocio } from '@/hooks/negocio/queries';
import { useActualizarNegocio } from '@/hooks/negocio/mutations';
import type { CambiosNegocio, ConfiguracionNegocio } from '@/types/negocio';
import { useDireccionDelComercio } from './useDireccionDelComercio';
import { useEstadoDeSolapas } from './useEstadoDeSolapas';
import { useLocalidadDelMapa } from './useLocalidadDelMapa';

export type { SeccionUbicacion };

const MENSAJE_SIN_GUARDAR = 'No pudimos guardar. Tus cambios siguen en pantalla.';

// Los cambios son un borrador hasta confirmar el PATCH de su solapa. Guardar una solapa no toca
// lo que está cargado en las otras, y la carga nunca reemplaza una edición en curso.
export function useConfiguracionUbicacion() {
  const configuracion = useConfiguracionNegocio();
  const guardarNegocio = useActualizarNegocio();
  const solapas = useEstadoDeSolapas();
  const [inicializada, setInicializada] = useState(false);
  const [nombre, setNombre] = useState('');
  const [localidad, setLocalidad] = useState('');
  const [comparteDatosZona, setComparteDatosZona] = useState(true);
  // La zona sólo se muestra para la ubicación ya guardada, nunca para un borrador.
  const [zona, setZona] = useState<string | null>(null);
  const [ubicacionGuardada, setUbicacionGuardada] = useState(false);
  const [rubroGuardado, setRubroGuardado] = useState(false);
  const [rubro, setRubro] = useState('');
  const [guardandoEn, setGuardandoEn] = useState<SeccionUbicacion | null>(null);
  const seguirAlMapa = useRef(false);
  const rubros = useQuery({
    queryKey: ['ubicacion', 'rubros'],
    queryFn: ({ signal }) => listarRubros(signal),
    staleTime: Infinity,
    retry: false,
  });
  const direccion = useDireccionDelComercio({
    alCambiar: () => solapas.cambio('ubicacion'),
    ponerLocalidad: setLocalidad,
    seguirAlMapa,
  });
  const mapa = useLocalidadDelMapa({
    punto: direccion.punto,
    localidad,
    ponerLocalidad: setLocalidad,
    seguirAlMapa,
  });

  const cargarLoGuardado = (datos: ConfiguracionNegocio) => {
    setNombre(datos.nombre);
    setLocalidad(datos.ubicacion?.localidad ?? '');
    setComparteDatosZona(datos.comparteDatosZona);
    setZona(datos.zona);
    setUbicacionGuardada(!!datos.ubicacion);
    const conRubro = !!datos.ubicacion || datos.rubro !== 'OTRO';
    setRubro(conRubro ? datos.rubro : '');
    setRubroGuardado(conRubro);
    if (datos.ubicacion) direccion.ponerUbicacion(datos.ubicacion);
  };

  useEffect(() => {
    const datos = configuracion.data;
    if (!datos || configuracion.isFetching || inicializada) return;
    cargarLoGuardado(datos);
    setInicializada(true);
    // Solo la primera carga completa llena el borrador: después, la edición en curso manda.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [configuracion.data, configuracion.isFetching, inicializada]);

  const aplicarGuardado: PorSeccion<(respuesta: ConfiguracionNegocio) => void> = {
    comercio: (respuesta) => {
      setNombre(respuesta.nombre);
      setComparteDatosZona(respuesta.comparteDatosZona);
    },
    ubicacion: (respuesta) => {
      setLocalidad(respuesta.ubicacion?.localidad ?? '');
      setUbicacionGuardada(!!respuesta.ubicacion);
      if (respuesta.ubicacion) direccion.ponerUbicacion(respuesta.ubicacion);
      direccion.limpiarBusqueda();
    },
    rubro: (respuesta) => {
      setRubro(respuesta.rubro);
      setRubroGuardado(true);
    },
  };

  const borrador = () => ({
    nombre,
    comparteDatosZona,
    rubro,
    rubrosDisponibles: rubros.data?.rubros ?? [],
    direccion: direccion.direccion,
    punto: direccion.punto,
    localidad,
    localidadBloquea: mapa.localidadBloquea,
    errorDeLocalidad: mapa.errorDeLocalidad,
  });

  const enviarCambios = async (seccion: SeccionUbicacion, cambios: CambiosNegocio) => {
    setGuardandoEn(seccion);
    try {
      const respuesta = await guardarNegocio.mutateAsync(cambios);
      setZona(respuesta.zona);
      aplicarGuardado[seccion](respuesta);
      solapas.marcarGuardada(seccion);
    } catch (error) {
      solapas.explicarError(seccion, leerErrorServidor(error)?.mensaje ?? MENSAJE_SIN_GUARDAR);
    } finally {
      setGuardandoEn(null);
    }
  };

  const guardar = async (seccion: SeccionUbicacion) => {
    solapas.empezarAGuardar(seccion);
    const { cambios, error } = cambiosDeLaSolapa(seccion, borrador());
    solapas.explicarError(seccion, error ?? '');
    if (cambios) await enviarCambios(seccion, cambios);
  };

  return {
    nombre,
    localidad,
    comparteDatosZona,
    zona,
    ubicacionGuardada,
    cambiarNombre: (valor: string) => {
      solapas.cambio('comercio');
      setNombre(valor);
    },
    localidadDelMapa: mapa.localidadDelMapa,
    errorLocalidad: mapa.errorDeLocalidad,
    verificandoLocalidad: mapa.verificandoLocalidad,
    localidadBloquea: mapa.localidadBloquea,
    usarLocalidadDelMapa: () => {
      if (!mapa.localidadDelMapa) return;
      solapas.cambio('ubicacion');
      setLocalidad(mapa.localidadDelMapa);
    },
    reintentarLocalidad: mapa.reintentarLocalidad,
    cambiarCompartir: (valor: boolean) => {
      solapas.cambio('comercio');
      setComparteDatosZona(valor);
    },
    cargandoConfiguracion: !inicializada && !configuracion.isError,
    errorConfiguracion: configuracion.isError
      ? 'No pudimos cargar la configuración de tu comercio.'
      : '',
    reintentarConfiguracion: () => void configuracion.refetch(),
    guardar,
    guardandoEn,
    guardados: solapas.guardados,
    erroresGuardado: solapas.erroresGuardado,
    sinGuardar: solapas.sinGuardar,
    // Qué solapas ya tienen datos guardados en el servidor.
    conDatosGuardados: {
      comercio: inicializada,
      ubicacion: ubicacionGuardada,
      rubro: rubroGuardado,
    } satisfies PorSeccion<boolean>,
    busquedaSimulada: env.mocks === 'completo',
    consulta: direccion.consulta,
    cambiarConsulta: direccion.cambiarConsulta,
    buscar: direccion.buscar,
    buscando: direccion.buscando,
    resultados: direccion.resultados,
    errorBusqueda: direccion.errorBusqueda,
    direccion: direccion.direccion,
    elegirDireccion: direccion.elegirDireccion,
    moverPunto: direccion.moverPunto,
    punto: direccion.punto,
    rubro,
    cambiarRubro: (valor: string) => {
      solapas.cambio('rubro');
      setRubro(valor);
    },
    rubros: rubros.data?.rubros ?? [],
    cargandoRubros: rubros.isPending,
    errorRubros: rubros.isError ? 'No pudimos cargar los rubros.' : '',
    reintentarRubros: () => void rubros.refetch(),
  };
}
