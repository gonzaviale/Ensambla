import { useEffect, useMemo, useState } from 'react';
import { useDondeAbrir, type PuntoElegido } from '@/hooks/dondeAbrir/queries';
import {
  CAPAS_INICIALES,
  FILTROS_INICIALES,
  resumenParaCompartir,
  type Exploracion,
  type Filtros,
  type Rubro,
  type TipoDePunto,
  type ZonaPuntuada,
} from '@/lib/dondeAbrir';
import {
  RESPUESTAS_INICIALES,
  cambiosDePuesto,
  filtrosDesde,
  mejoresParaComparar,
  ordenDelRanking,
  puntosAMostrar,
  type DondeMostrar,
  type HistorialDeOrden,
} from '@/lib/dondeAbrirRecorrido';
import { SUPERFICIE_DE_TAMANO, type RespuestasDeSami } from '@/lib/respuestasDeSami';
import { useSistemaStore } from '@/stores/sistemaStore';

export type Paso = 'confirmar' | 'preguntas' | 'resultado';

const EXPLORACION_VACIA: Exploracion = { ranking: [], descartadas: [], evaluadas: 0 };

/** El orden anterior sirve para mostrar quién subió y quién bajó con el último cambio. */
function useCambiosDePuesto(ranking: ZonaPuntuada[], movioFiltros: boolean) {
  const orden = ordenDelRanking(ranking);
  const [historial, setHistorial] = useState<HistorialDeOrden>({ actual: orden, anterior: orden });
  useEffect(() => {
    setHistorial((previo) =>
      previo.actual === orden ? previo : { actual: orden, anterior: previo.actual },
    );
  }, [orden]);
  return useMemo(() => (movioFiltros ? cambiosDePuesto(historial) : {}), [historial, movioFiltros]);
}

/** Qué se dibuja en el mapa además de las zonas, y en cuáles de ellas. */
function useCapasDelMapa() {
  const [capas, setCapas] = useState<Record<TipoDePunto, boolean>>(CAPAS_INICIALES);
  const [dondeMostrar, setDondeMostrar] = useState<DondeMostrar>('elegida');
  const alternarCapa = (tipo: TipoDePunto) =>
    setCapas((actual) => ({ ...actual, [tipo]: !actual[tipo] }));
  return { capas, dondeMostrar, setDondeMostrar, alternarCapa };
}

const avisarEnPantalla = (mensaje: string) => useSistemaStore.getState().avisar(mensaje);

const copiarAlPortapapeles = (texto: string) => navigator.clipboard.writeText(texto);

/** Todo lo que recuerda «Tu próximo negocio» mientras la persona confirma, responde y compara zonas. */
export function useRecorridoDondeAbrir() {
  const [paso, setPaso] = useState<Paso>('confirmar');
  const [respuestas, setRespuestas] = useState<RespuestasDeSami>(RESPUESTAS_INICIALES);
  const [filtros, setFiltros] = useState<Filtros>(FILTROS_INICIALES);
  const [superficie, setSuperficie] = useState(SUPERFICIE_DE_TAMANO[RESPUESTAS_INICIALES.tamano]);
  const [seleccionadaId, setSeleccionadaId] = useState('');
  const [calculoAbierto, setCalculoAbierto] = useState(false);
  // Quién subió o bajó solo se marca cuando la persona mueve un filtro, no por lo que contestó en las preguntas.
  const [movioFiltros, setMovioFiltros] = useState(false);
  // Un lugar cualquiera que la persona tocó en el mapa: el servidor lo mide en vivo y lo suma al ranking.
  const [punto, setPunto] = useState<PuntoElegido | null>(null);
  const capas = useCapasDelMapa();

  const consulta = useDondeAbrir(filtros, paso === 'resultado', punto);
  const exploracion = consulta.data ?? EXPLORACION_VACIA;
  const puntoElegidoId = consulta.data?.puntoElegidoId ?? null;
  const { ranking, descartadas } = exploracion;
  const elegida = ranking.find((item) => item.zona.id === seleccionadaId) ?? ranking[0];
  const mejores = useMemo(() => mejoresParaComparar(ranking, elegida), [ranking, elegida]);
  const cambios = useCambiosDePuesto(ranking, movioFiltros);
  const puntos = useMemo(
    () => puntosAMostrar(capas.dondeMostrar, ranking, elegida),
    [capas.dondeMostrar, ranking, elegida],
  );

  // Al medir un lugar nuevo, el detalle pasa a ser el de ese lugar.
  useEffect(() => {
    if (puntoElegidoId) setSeleccionadaId(puntoElegidoId);
  }, [puntoElegidoId]);

  const cambiarFiltros = (nuevos: Filtros | ((actuales: Filtros) => Filtros)) => {
    setMovioFiltros(true);
    setFiltros(nuevos);
  };

  const cambiarRadio = (distanciaMaxKm: number) =>
    cambiarFiltros((actual) => ({ ...actual, limites: { ...actual.limites, distanciaMaxKm } }));

  const responder = (nuevas: RespuestasDeSami) => {
    setMovioFiltros(false);
    setRespuestas(nuevas);
    setFiltros((actuales) => filtrosDesde(nuevas, actuales));
    setSuperficie(SUPERFICIE_DE_TAMANO[nuevas.tamano]);
  };

  const confirmarRubro = (rubro: Rubro) => {
    setFiltros((actuales) => ({ ...actuales, rubro }));
    setPaso('preguntas');
  };

  const copiarResumen = async () => {
    try {
      await copiarAlPortapapeles(resumenParaCompartir(filtros, exploracion));
      avisarEnPantalla('Copiamos el resumen para que lo pegues donde quieras.');
    } catch {
      avisarEnPantalla('No pudimos copiar el resumen. Probá seleccionarlo y copiarlo a mano.');
    }
  };

  return {
    paso,
    irALasPreguntas: () => setPaso('preguntas'),
    irAlResultado: () => setPaso('resultado'),
    respuestas,
    responder,
    confirmarRubro,
    filtros,
    cambiarFiltros,
    cambiarRadio,
    superficie,
    cambiarSuperficie: setSuperficie,
    consulta,
    ranking,
    descartadas,
    elegida,
    mejores,
    cambios,
    puntos,
    puntoElegidoId,
    seleccionadaId,
    elegirZona: setSeleccionadaId,
    punto,
    elegirPunto: (latitud: number, longitud: number) => setPunto({ latitud, longitud }),
    quitarPunto: () => setPunto(null),
    calculoAbierto,
    alternarCalculo: () => setCalculoAbierto((abierto) => !abierto),
    capas: capas.capas,
    dondeMostrar: capas.dondeMostrar,
    cambiarDondeMostrar: capas.setDondeMostrar,
    alternarCapa: capas.alternarCapa,
    copiarResumen,
  };
}

export type Recorrido = ReturnType<typeof useRecorridoDondeAbrir>;
