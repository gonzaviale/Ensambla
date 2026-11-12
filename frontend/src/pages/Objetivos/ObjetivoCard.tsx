import { useId, useMemo, useState } from 'react';
import {
  CalendarCheckIcon,
  CaretDownIcon,
  CheckCircleIcon,
  TrashIcon,
} from '@phosphor-icons/react';
import { usePasosObjetivo, useSemanasObjetivo } from '@/hooks/objetivos/queries';
import { resumenSinVer } from '@/lib/objetivosAutogestion';
import {
  avance,
  brecha,
  formatoPesos,
  ultimoValor,
  variacion,
  type Objetivo,
} from '@/lib/objetivos';
import {
  comparacionConZona,
  escenarioDe,
  montoDe,
  pasosConcretos,
  type Montos,
  type Sensibilidad,
} from '@/lib/objetivosPasos';
import { AjusteDePaso, Sensibilidades } from './AjusteDePaso';
import { Progreso } from './piezas';

type Props = {
  objetivo: Objetivo;
  /** La tarjeta arranca con los pasos a la vista (cuando hay un solo objetivo). */
  abierta?: boolean;
  onQuitar: () => void;
  /** Lleva al seguimiento semanal del objetivo (mini-objetivos, gráfico y reajustes). */
  onVerSemana: () => void;
};

function textoDeTendencia(objetivo: Objetivo) {
  const semanas = objetivo.historial.length - 1;
  if (semanas < 1) return 'Todavía no hay semanas para comparar. Sami lo va midiendo.';
  const cambio = variacion(objetivo);
  const plazo = semanas === 1 ? 'la última semana' : `las últimas ${semanas} semanas`;
  if (cambio === 0) return `Sin cambios en ${plazo}.`;
  return `${cambio > 0 ? 'Subió' : 'Bajó'} ${formatoPesos(Math.abs(cambio))} en ${plazo}.`;
}

export function ObjetivoCard({ objetivo, abierta = false, onQuitar, onVerSemana }: Props) {
  const [verPasos, setVerPasos] = useState(abierta);
  const [elegidos, setElegidos] = useState<ReadonlySet<string>>(new Set());
  const [montos, setMontos] = useState<Montos>({});
  const [sensibilidad, setSensibilidad] = useState<Sensibilidad>('media');
  const idPanel = useId();
  const consultaPasos = usePasosObjetivo();
  const ultimoCierre = useSemanasObjetivo(objetivo.id).data?.ultimoCierre ?? null;
  const resumenListo = resumenSinVer(objetivo.id, ultimoCierre);
  const pasos = useMemo(() => pasosConcretos(consultaPasos.data ?? []), [consultaPasos.data]);
  const escenario = escenarioDe(objetivo, pasos, elegidos, montos, sensibilidad);
  const cumplido = brecha(objetivo) === 0;
  const maximo = Math.max(1, ...objetivo.historial.map((punto) => punto.valor));

  const alternar = (clave: string) =>
    setElegidos((actuales) => {
      const siguientes = new Set(actuales);
      if (!siguientes.delete(clave)) siguientes.add(clave);
      return siguientes;
    });

  return (
    <article className="objetivos-tarjeta">
      <header>
        <div>
          <p className="objetivos-etiqueta">Tu objetivo</p>
          <h2>{objetivo.texto}</h2>
        </div>
        {cumplido && (
          <span className="objetivos-cumplido">
            <CheckCircleIcon aria-hidden size={20} /> Lo lograste
          </span>
        )}
      </header>

      <div className="objetivos-tarjeta-avance">
        <div>
          <div className="objetivos-avance-cifras">
            <strong>{formatoPesos(ultimoValor(objetivo))}</strong>
            <span>
              {cumplido
                ? 'ganás por mes'
                : `ganás por mes · te faltan ${formatoPesos(brecha(objetivo))}`}
            </span>
          </div>
          <Progreso
            valor={avance(objetivo)}
            etiqueta={`Lo que ganás hoy frente a ${formatoPesos(objetivo.meta)}`}
          />
          <p className="objetivos-tendencia-texto">{textoDeTendencia(objetivo)}</p>
        </div>
        <div
          className="objetivos-tendencia"
          role="img"
          aria-label={`Lo que ganaste por mes: ${objetivo.historial
            .map((punto) => `${punto.semana} ${formatoPesos(punto.valor)}`)
            .join(', ')}`}
        >
          {objetivo.historial.map((punto) => (
            <figure key={punto.semana}>
              <i style={{ height: `${Math.max(6, (punto.valor / maximo) * 100)}%` }} />
              <figcaption>{punto.semana}</figcaption>
            </figure>
          ))}
        </div>
      </div>

      {!cumplido && (
        <>
          <button
            type="button"
            className={`objetivos-pasos-boton ${verPasos ? 'abierta' : ''}`.trim()}
            aria-expanded={verPasos}
            aria-controls={idPanel}
            onClick={() => setVerPasos((visible) => !visible)}
          >
            Qué podés hacer
            <CaretDownIcon aria-hidden size={18} />
          </button>
          {verPasos && (
            <div id={idPanel} className="objetivos-pasos">
              {consultaPasos.isPending ? (
                <p role="status" className="objetivos-pasos-vacio">
                  Sami está calculando tus recomendaciones…
                </p>
              ) : consultaPasos.isError ? (
                <div
                  role="alert"
                  aria-label="No pudimos calcular tus pasos"
                  className="objetivos-pasos-error"
                >
                  <strong>No pudimos calcular tus pasos</strong>
                  <p>Tu objetivo sigue guardado. Revisá tu conexión y volvé a intentar.</p>
                  <button
                    type="button"
                    className="btn secondary"
                    disabled={consultaPasos.isFetching}
                    onClick={() => void consultaPasos.refetch()}
                  >
                    {consultaPasos.isFetching ? 'Reintentando…' : 'Reintentar recomendaciones'}
                  </button>
                </div>
              ) : pasos.length === 0 ? (
                <p className="objetivos-pasos-vacio">
                  Cargá tus productos y sus costos en un cuaderno, y Sami te dice qué hacer con cada
                  uno para llegar.
                </p>
              ) : (
                <>
                  {pasos.some((paso) => paso.curva) && (
                    <Sensibilidades sensibilidad={sensibilidad} onCambiar={setSensibilidad} />
                  )}
                  <ol>
                    {pasos.map((paso) => {
                      const monto = montoDe(paso, montos, sensibilidad);
                      const resultado = paso.calcular(monto, sensibilidad);
                      const zonaDelPaso = comparacionConZona(
                        resultado.precioNuevo,
                        paso.precioZona,
                      );
                      return (
                        <li key={paso.clave}>
                          <strong>{resultado.titulo}</strong>
                          <p>{resultado.detalle}</p>
                          <AjusteDePaso
                            paso={paso}
                            valor={monto}
                            sensibilidad={sensibilidad}
                            onCambiar={(valor) =>
                              setMontos((actuales) => ({ ...actuales, [paso.clave]: valor }))
                            }
                          />
                          {zonaDelPaso && <p className="objetivos-paso-zona">{zonaDelPaso}</p>}
                          <span
                            className={`objetivos-paso-efecto ${(resultado.sumaPorMes ?? 0) < 0 ? 'negativo' : ''}`.trim()}
                          >
                            {resultado.efecto}
                          </span>
                          <label className="objetivos-paso-eleccion">
                            <input
                              type="checkbox"
                              checked={elegidos.has(paso.clave)}
                              onChange={() => alternar(paso.clave)}
                            />
                            Lo voy a hacer
                          </label>
                        </li>
                      );
                    })}
                  </ol>
                  <Escenario
                    escenario={escenario}
                    meta={objetivo.meta}
                    hayElegidos={elegidos.size > 0}
                  />
                  <p className="objetivos-pasos-nota">
                    Son cálculos con lo que cargaste. Vos decidís qué hacer: Ensambla no cambia tus
                    precios.
                  </p>
                </>
              )}
            </div>
          )}
        </>
      )}

      <footer className="objetivos-tarjeta-acciones">
        <button type="button" className="btn secondary" onClick={onVerSemana}>
          <CalendarCheckIcon aria-hidden size={18} /> Ver mi semana
        </button>
        {resumenListo && <span className="objetivos-chip-resumen">Resumen de la semana listo</span>}
        <button type="button" className="objetivos-textual" onClick={onQuitar}>
          <TrashIcon aria-hidden size={16} /> Quitar objetivo
        </button>
      </footer>
    </article>
  );
}

type EscenarioProps = {
  escenario: ReturnType<typeof escenarioDe>;
  meta: number;
  hayElegidos: boolean;
};

// «Probá un escenario», dentro de la tarjeta: tildás los pasos y ves cómo quedaría lo que ganás por mes.
function Escenario({ escenario, meta, hayElegidos }: EscenarioProps) {
  const ancho = (valor: number) => `${Math.min(100, Math.max(2, (valor / meta) * 100))}%`;
  return (
    <section className="objetivos-escenario" aria-live="polite" aria-label="Cómo quedaría">
      <h3>Cómo quedaría</h3>
      {hayElegidos ? (
        <>
          <dl>
            <div>
              <dt>Hoy ganás</dt>
              <dd>
                <i style={{ width: ancho(escenario.hoy) }} />
                <b>{formatoPesos(escenario.hoy)}</b>
              </dd>
            </div>
            <div>
              <dt>Con tus pasos</dt>
              <dd className="con-pasos">
                <i style={{ width: ancho(escenario.conPasos) }} />
                <b>{formatoPesos(escenario.conPasos)}</b>
              </dd>
            </div>
            <div>
              <dt>Tu objetivo</dt>
              <dd className="meta">
                <i style={{ width: '100%' }} />
                <b>{formatoPesos(meta)}</b>
              </dd>
            </div>
          </dl>
          <p>
            {escenario.faltaria === 0
              ? '¡Con esto llegás a tu objetivo!'
              : `Con esto todavía te faltarían ${formatoPesos(escenario.faltaria)} por mes.`}
            {escenario.liberada > 0 &&
              ` Además recuperás hasta ${formatoPesos(escenario.liberada)} de mercadería parada.`}
          </p>
        </>
      ) : (
        <p>Tildá «Lo voy a hacer» en los pasos que te sirvan y mirá cómo quedaría lo que ganás.</p>
      )}
    </section>
  );
}
