import { useEffect, useMemo, useState } from 'react';
import { ArrowLeftIcon, CopyIcon, XIcon } from '@phosphor-icons/react';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Sami } from '@/components/sistema/Sami';
import { COLOR_DE_PUNTO } from '@/hooks/dondeAbrir/useMapaDeZonas';
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
  explorar,
  filtrosActivos,
  puntosDeLaZona,
  resumenParaCompartir,
  sinFiltro,
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

type Paso = 'confirmar' | 'preguntas' | 'resultado';
type DondeMostrar = 'elegida' | 'todas';

const PASOS: { clave: Paso; titulo: string }[] = [
  { clave: 'confirmar', titulo: 'Tu negocio' },
  { clave: 'preguntas', titulo: 'Preguntas' },
  { clave: 'resultado', titulo: 'Zonas y trámites' },
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
  useTituloPagina('Dónde abrir');
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

  const exploracion = useMemo(() => explorar(filtros), [filtros]);
  const { ranking, descartadas } = exploracion;
  const elegida = ranking.find((item) => item.zona.id === seleccionadaId) ?? ranking[0];
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
    return zonas.flatMap((item) => puntosDeLaZona(item.zona, filtros.rubro));
  }, [dondeMostrar, ranking, elegida, filtros.rubro]);

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
    <>
      <CabeceraComun active="open" />
      <main id="main" tabIndex={-1} className="page-wrap objetivos-page donde-page reveal">
        <button
          type="button"
          className="objetivos-volver"
          onClick={() => void navegacion.irAObjetivos()}
        >
          <ArrowLeftIcon aria-hidden size={20} /> Volver a objetivos
        </button>
        <header className="donde-encabezado">
          <div>
            <p className="objetivos-etiqueta">Dónde abrir</p>
            <h1>Encontrá la mejor zona para tu próximo local</h1>
          </div>
          <ol className="donde-pasos-cabeza" aria-label="Dónde estás">
            {PASOS.map((item) => (
              <li key={item.clave} aria-current={item.clave === paso ? 'step' : undefined}>
                {item.titulo}
              </li>
            ))}
          </ol>
        </header>
        <p className="donde-demo" role="note">
          <strong>Demostración.</strong> Las zonas y los números son de ejemplo. Los trámites salen
          de las páginas oficiales.
        </p>

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
                <DetalleDeZona
                  key={elegida.zona.id}
                  item={elegida}
                  filtros={filtros}
                  superficie={superficie}
                  onSuperficie={setSuperficie}
                  calculoAbierto={calculoAbierto}
                  onAlternarCalculo={() => setCalculoAbierto((abierto) => !abierto)}
                />
              </section>
            )}

            <PanelDeFiltros filtros={filtros} activos={activos.length} onCambiar={cambiarFiltros} />
            <div className="donde-barra-de-control">
              <LeyendaDeColores />
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

            {activos.length > 0 && (
              <ul className="donde-chips" aria-label="Filtros activos">
                {activos.map((activo) => (
                  <li key={activo.id}>
                    <button
                      type="button"
                      aria-label={`Quitar filtro: ${activo.texto}`}
                      onClick={() => cambiarFiltros((actual) => sinFiltro(actual, activo.id))}
                    >
                      {activo.texto} <XIcon aria-hidden size={12} weight="bold" />
                    </button>
                  </li>
                ))}
              </ul>
            )}

            <div className="donde-cuerpo">
              <ListaDeZonas
                ranking={ranking}
                descartadas={descartadas}
                seleccionadaId={elegida?.zona.id ?? ''}
                cambios={cambios}
                onElegir={setSeleccionadaId}
              />
              <div className="donde-mapa-columna">
                <MapaDeZonas
                  ranking={ranking}
                  descartadas={descartadas}
                  seleccionadaId={elegida?.zona.id ?? ''}
                  puntos={puntos}
                  capas={capas}
                  onSeleccionar={setSeleccionadaId}
                />
                <details className="donde-capas">
                  <summary>Qué ver en el mapa</summary>
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
              </div>
            </div>

            <p className="donde-nota-chica">
              Es una estimación para decidir mejor, no una predicción de ventas.
            </p>
          </div>
        )}
      </main>
    </>
  );
}
