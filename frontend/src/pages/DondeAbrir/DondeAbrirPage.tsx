import { useEffect, useMemo, useState } from 'react';
import { ArrowLeftIcon, CopyIcon } from '@phosphor-icons/react';
import { Sami } from '@/components/sistema/Sami';
import { COLOR_DE_PUNTO } from '@/hooks/dondeAbrir/useMapaDeZonas';
import { useDondeAbrir, type PuntoElegido } from '@/hooks/dondeAbrir/queries';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import {
  CAPAS,
  CAPAS_INICIALES,
  FILTROS_INICIALES,
  NOMBRE_DE_RUBRO,
  PRESETS,
  SIN_LIMITE_KM,
  conPreset,
  filtrosActivos,
  resumenParaCompartir,
  type Filtros,
  type Rubro,
  type TipoDePunto,
} from '@/lib/dondeAbrir';
import { SUPERFICIE_DE_TAMANO, type RespuestasDeSami } from '@/lib/respuestasDeSami';
import { useSistemaStore } from '@/stores/sistemaStore';
import { ConfirmarNegocio } from './ConfirmarNegocio';
import { DetalleDeZona } from './DetalleDeZona';
import { LeyendaDeColores } from './LeyendaDeColores';
import { ListaDeZonas, type CambioDePuesto } from './ListaDeZonas';
import { MapaDeZonas } from './MapaDeZonas';
import { PanelDeFiltros } from './PanelDeFiltros';
import { PreguntasDeSami } from './PreguntasDeSami';
import { ComparacionDeZonas } from './ComparacionDeZonas';
import { RadioDeBusqueda } from './RadioDeBusqueda';

type Paso = 'confirmar' | 'preguntas' | 'resultado';
type DondeMostrar = 'elegida' | 'todas';

const PASOS: { clave: Paso; titulo: string }[] = [
  { clave: 'confirmar', titulo: 'Confirmá tu negocio' },
  { clave: 'preguntas', titulo: 'Contanos qué buscás' },
  { clave: 'resultado', titulo: 'Compará zonas' },
];

const RESPUESTAS_INICIALES: RespuestasDeSami = {
  alcance: 'cerca',
  prioridad: 'equilibrado',
  publico: 'vecinos',
  tamano: 'mediano',
};

/** Traduce lo que la persona contestó a los filtros con los que se calcula. */
function filtrosDesde(respuestas: RespuestasDeSami, actuales: Filtros): Filtros {
  const preset = PRESETS.find((item) => item.id === respuestas.prioridad) ?? PRESETS[0];
  const base = preset ? conPreset(actuales, preset) : actuales;
  return {
    ...base,
    publico: respuestas.publico,
    limites: {
      ...FILTROS_INICIALES.limites,
      distanciaMaxKm:
        respuestas.alcance === 'cerca' ? FILTROS_INICIALES.limites.distanciaMaxKm : SIN_LIMITE_KM,
    },
  };
}

// «Dónde abrir»: Sami confirma el negocio, pregunta, recomienda zonas y explica qué hay que tramitar para abrir en la
// que se elija. Cada filtro que se mueve recalcula todo al instante. Es una demostración con datos de ejemplo.
export function DondeAbrirPage() {
  useTituloPagina('Tu próximo negocio');
  const navegacion = useNavegacion();
  const avisar = useSistemaStore((estado) => estado.avisar);
  const [paso, setPaso] = useState<Paso>('confirmar');
  const [respuestas, setRespuestas] = useState<RespuestasDeSami>(RESPUESTAS_INICIALES);
  const [filtros, setFiltros] = useState<Filtros>(FILTROS_INICIALES);
  const [superficie, setSuperficie] = useState(SUPERFICIE_DE_TAMANO[RESPUESTAS_INICIALES.tamano]);
  const [seleccionadaId, setSeleccionadaId] = useState('');
  const [calculoAbierto, setCalculoAbierto] = useState(false);
  const [capas, setCapas] = useState<Record<TipoDePunto, boolean>>(CAPAS_INICIALES);
  const [dondeMostrar, setDondeMostrar] = useState<DondeMostrar>('elegida');
  // Quién subió o bajó solo se marca cuando la persona mueve un filtro, no por lo que contestó en las preguntas.
  const [movioFiltros, setMovioFiltros] = useState(false);
  // Un lugar cualquiera que la persona tocó en el mapa: el servidor lo mide en vivo y lo suma al ranking.
  const [punto, setPunto] = useState<PuntoElegido | null>(null);

  const consulta = useDondeAbrir(filtros, paso === 'resultado', punto);
  const exploracion = consulta.data ?? { ranking: [], descartadas: [], evaluadas: 0 };
  const puntoElegidoId = consulta.data?.puntoElegidoId ?? null;
  const { ranking, descartadas } = exploracion;
  const elegida = ranking.find((item) => item.zona.id === seleccionadaId) ?? ranking[0];

  // Al medir un lugar nuevo, el detalle pasa a ser el de ese lugar.
  useEffect(() => {
    if (puntoElegidoId) setSeleccionadaId(puntoElegidoId);
  }, [puntoElegidoId]);
  const activos = useMemo(() => filtrosActivos(filtros), [filtros]);
  // Las tres mejores para comparar de un vistazo; si se eligió otra, también aparece.
  const mejores = useMemo(() => {
    const podio = ranking.slice(0, 3);
    return elegida && !podio.some((item) => item.zona.id === elegida.zona.id)
      ? [...podio, elegida]
      : podio;
  }, [ranking, elegida]);

  // El orden anterior sirve para mostrar quién subió y quién bajó con el último cambio.
  const orden = ranking.map((item) => item.zona.id).join('|');
  const [historial, setHistorial] = useState({ actual: orden, anterior: orden });
  useEffect(() => {
    setHistorial((previo) =>
      previo.actual === orden ? previo : { actual: orden, anterior: previo.actual },
    );
  }, [orden]);
  const cambios = useMemo(() => {
    if (!movioFiltros) return {};
    const antes = historial.anterior ? historial.anterior.split('|') : [];
    const ahora = historial.actual ? historial.actual.split('|') : [];
    const resultado: Record<string, CambioDePuesto> = {};
    ahora.forEach((id, puesto) => {
      const previo = antes.indexOf(id);
      resultado[id] = previo === -1 ? 'nueva' : previo - puesto;
    });
    return resultado;
  }, [historial, movioFiltros]);

  const puntos = useMemo(() => {
    const zonas = dondeMostrar === 'todas' ? ranking : elegida ? [elegida] : [];
    return zonas.flatMap((item) => item.puntos ?? []);
  }, [dondeMostrar, ranking, elegida]);

  const cambiarFiltros = (nuevos: Filtros | ((actuales: Filtros) => Filtros)) => {
    setMovioFiltros(true);
    setFiltros(nuevos);
  };
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

  const copiar = async () => {
    try {
      await navigator.clipboard.writeText(resumenParaCompartir(filtros, exploracion));
      avisar('Copiamos el resumen para que lo pegues donde quieras.');
    } catch {
      avisar('No pudimos copiar el resumen. Probá seleccionarlo y copiarlo a mano.');
    }
  };

  return (
    <main id="main" tabIndex={-1} className="page-wrap objetivos-page donde-page reveal">
      <button type="button" className="objetivos-volver" onClick={() => void navegacion.irAZona()}>
        <ArrowLeftIcon aria-hidden size={20} /> Volver a Mi zona
      </button>
      <header className="donde-encabezado">
        <div>
          <p className="objetivos-etiqueta">Zona</p>
          <h1>Tu próximo negocio</h1>
          <p className="donde-bajada">
            Compará zonas y entendé qué necesitás para abrir un nuevo local.
          </p>
        </div>
        <ol className="donde-pasos-cabeza" aria-label="Progreso de Tu próximo negocio">
          {PASOS.map((item) => (
            <li key={item.clave} aria-current={item.clave === paso ? 'step' : undefined}>
              {item.titulo}
            </li>
          ))}
        </ol>
      </header>
      {paso === 'confirmar' && (
        <ConfirmarNegocio rubro={filtros.rubro} onConfirmar={confirmarRubro} />
      )}
      {paso === 'preguntas' && (
        <PreguntasDeSami
          respuestas={respuestas}
          onResponder={responder}
          onTerminar={() => setPaso('resultado')}
        />
      )}

      {paso === 'resultado' && (
        <div className="donde-resultado">
          {consulta.isPending && (
            <p className="donde-estado" role="status">
              Estoy comparando las zonas…
            </p>
          )}
          {consulta.isError && (
            <div className="donde-estado error" role="alert">
              <p>
                {punto
                  ? 'No pudimos medir ese lugar. Probá con otro punto o volvé a intentar.'
                  : 'No pudimos calcular el ranking. Tus respuestas siguen guardadas.'}
              </p>
              {punto && (
                <button type="button" className="btn ghost" onClick={() => setPunto(null)}>
                  Quitar el lugar
                </button>
              )}
              <button
                type="button"
                className="btn secondary"
                onClick={() => void consulta.refetch()}
              >
                Volver a intentar
              </button>
            </div>
          )}
          {consulta.data?.avisos.map((aviso) => (
            <p className="donde-aviso-datos" role="note" key={aviso}>
              {aviso}
            </p>
          ))}
          {elegida && (
            <section className="donde-recomienda" aria-label="Recomendación de Sami">
              <div className="donde-recomienda-cabeza">
                <div className="donde-sami-cara" aria-hidden="true">
                  <Sami pose="think" />
                </div>
                <div>
                  <h2>
                    {elegida.zona.id === ranking[0]?.zona.id
                      ? `Mi recomendación para tu ${NOMBRE_DE_RUBRO[filtros.rubro]}`
                      : 'Lo que sé de la zona que elegiste'}
                  </h2>
                  <p>
                    Elegí una zona para ver por qué la recomiendo y qué tenés que tramitar para
                    abrir ahí.
                  </p>
                </div>
              </div>
              <ol className="donde-mejores" aria-label="Mejores zonas">
                {mejores.map((item) => (
                  <li key={item.zona.id}>
                    <button
                      type="button"
                      aria-pressed={item.zona.id === elegida.zona.id}
                      className={`${item.coincidencia} ${item.zona.id === elegida.zona.id ? 'activa' : ''}`.trim()}
                      onClick={() => setSeleccionadaId(item.zona.id)}
                    >
                      <i aria-hidden />
                      {item.zona.nombre}
                    </button>
                  </li>
                ))}
              </ol>
            </section>
          )}

          {mejores.length > 0 && (
            <ComparacionDeZonas zonas={mejores.slice(0, 3)} filtros={filtros} />
          )}

          <div className="donde-barra-de-control">
            <p>Explorá el mapa y ajustá solo lo que necesites.</p>
            <div className="donde-acciones">
              <button type="button" className="btn ghost small" onClick={copiar}>
                <CopyIcon aria-hidden size={16} /> Copiar resumen
              </button>
              <button
                type="button"
                className="btn ghost small"
                onClick={() => setPaso('preguntas')}
              >
                Cambiar mis respuestas
              </button>
            </div>
          </div>

          <div className="donde-explorador">
            <aside className="donde-explorador-controles">
              <PanelDeFiltros
                filtros={filtros}
                activos={activos.length}
                onCambiar={cambiarFiltros}
              />
              <details className="donde-capas">
                <summary>Qué mostrar en el mapa</summary>
                <div className="donde-capas-lista">
                  {CAPAS.map(({ tipo, etiqueta }) => (
                    <label key={tipo} className={capas[tipo] ? 'marcada' : ''}>
                      <input
                        type="checkbox"
                        checked={capas[tipo]}
                        onChange={() =>
                          setCapas((actual) => ({ ...actual, [tipo]: !actual[tipo] }))
                        }
                      />
                      <i aria-hidden style={{ background: COLOR_DE_PUNTO[tipo] }} />
                      {etiqueta}
                    </label>
                  ))}
                </div>
                <div className="donde-donde" role="radiogroup" aria-label="Dónde mostrarlo">
                  <label className={dondeMostrar === 'elegida' ? 'marcada' : ''}>
                    <input
                      type="radio"
                      name="donde-mostrar"
                      checked={dondeMostrar === 'elegida'}
                      onChange={() => setDondeMostrar('elegida')}
                    />
                    Solo en la zona elegida
                  </label>
                  <label className={dondeMostrar === 'todas' ? 'marcada' : ''}>
                    <input
                      type="radio"
                      name="donde-mostrar"
                      checked={dondeMostrar === 'todas'}
                      onChange={() => setDondeMostrar('todas')}
                    />
                    En todas las zonas
                  </label>
                </div>
              </details>
            </aside>
            <div className="donde-mapa-columna">
              <RadioDeBusqueda
                valor={filtros.limites.distanciaMaxKm}
                onCambiar={(valor) =>
                  cambiarFiltros((actual) => ({
                    ...actual,
                    limites: { ...actual.limites, distanciaMaxKm: valor },
                  }))
                }
              />
              <MapaDeZonas
                ranking={ranking}
                descartadas={descartadas}
                seleccionadaId={elegida?.zona.id ?? ''}
                puntos={puntos}
                capas={capas}
                onSeleccionar={setSeleccionadaId}
                onElegirPunto={(latitud, longitud) => setPunto({ latitud, longitud })}
                punto={punto}
                puntoElegidoId={puntoElegidoId}
                localActual={consulta.data?.negocioActual}
              />
              <div className="donde-punto" role="status">
                {punto && consulta.isFetching ? (
                  <p>Midiendo ese lugar con datos de OpenStreetMap…</p>
                ) : punto && !consulta.isError ? (
                  <p>
                    Mediste un lugar a mano: se cuenta lo que hay a 500 m, ahora mismo.{' '}
                    <button
                      type="button"
                      className="btn ghost small"
                      onClick={() => setPunto(null)}
                    >
                      Quitar el lugar
                    </button>
                  </p>
                ) : (
                  <p>Tocá cualquier lugar del mapa para medirlo y compararlo con las zonas.</p>
                )}
              </div>
              <LeyendaDeColores capas={capas} hayLugarElegido={Boolean(punto)} />
            </div>
          </div>

          <section className="donde-ranking-completo" aria-labelledby="donde-ranking-titulo">
            <div className="donde-seccion-cabeza">
              <div>
                <p className="objetivos-etiqueta">Ranking completo</p>
                <h2 id="donde-ranking-titulo">Todas las zonas analizadas</h2>
              </div>
              <p>Elegí una para ver el detalle y los trámites.</p>
            </div>
            <ListaDeZonas
              ranking={ranking}
              descartadas={descartadas}
              seleccionadaId={elegida?.zona.id ?? ''}
              cambios={cambios}
              onElegir={setSeleccionadaId}
            />
          </section>

          {elegida && (
            <DetalleDeZona
              item={elegida}
              filtros={filtros}
              superficie={superficie}
              onSuperficie={setSuperficie}
              calculoAbierto={calculoAbierto}
              onAlternarCalculo={() => setCalculoAbierto((abierto) => !abierto)}
            />
          )}

          <p className="donde-nota-chica">
            Es una estimación para decidir mejor, no una predicción de ventas.
          </p>
        </div>
      )}
    </main>
  );
}
