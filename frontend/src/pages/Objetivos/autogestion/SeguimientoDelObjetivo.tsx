import { useRef, useState } from 'react';
import { useDecidirPaso } from '@/hooks/objetivos/mutations';
import {
  useDecisionesObjetivo,
  useMiniObjetivos,
  useProgresoObjetivo,
  useSemanasObjetivo,
} from '@/hooks/objetivos/queries';
import {
  decisionDeLaSemana,
  diaCorto,
  domingoDe,
  lunesDe,
  marcarResumenVisto,
  resumenSinVer,
  todasLasDecisiones,
  cuantoCubren,
} from '@/lib/objetivosAutogestion';
import { hoy } from '@/lib/sistema/formato';
import { GraficoSemanal } from './GraficoSemanal';
import { HistorialReajustes } from './HistorialReajustes';
import { MiniObjetivoCard } from './MiniObjetivoCard';
import { RecorridoDeLaSemana } from './RecorridoDeLaSemana';
import { ResumenListo } from './ResumenListo';

// Lo que Sami hace solo con un objetivo: los 3 mini-objetivos, cómo fue cada semana y qué cambió los domingos.
export function SeguimientoDelObjetivo({
  objetivoId,
  teFaltan = 0,
}: {
  objetivoId: string;
  /** Lo que falta por mes para la meta: se compara con lo que suman los mini-objetivos. */
  teFaltan?: number;
}) {
  const [verTodasLasSemanas, setVerTodasLasSemanas] = useState(false);
  const minis = useMiniObjetivos(objetivoId);
  const progreso = useProgresoObjetivo(objetivoId);
  const decisiones = useDecisionesObjetivo(objetivoId);
  const semanas = useSemanasObjetivo(objetivoId);
  const decidir = useDecidirPaso(objetivoId);
  // Los minis que la persona dejó para después y restauró: vuelve a ver los botones hasta que decida otra vez.
  const [restaurados, setRestaurados] = useState<ReadonlySet<string>>(new Set());
  const [resumenOculto, setResumenOculto] = useState(false);
  const resumen = useRef<HTMLElement>(null);
  const recorrido = useRef<HTMLDivElement>(null);

  const lista = todasLasDecisiones(decisiones.data);
  const estaSemana = lunesDe(hoy());
  const cantidadDeSemanas = new Set([
    ...lista.map((decision) => decision.semanaInicio),
    ...(semanas.data?.semanas ?? []).map((semana) => semana.semana),
  ]).size;
  const ocupado = decidir.isPending;
  const ultimoCierre = semanas.data?.ultimoCierre ?? null;
  const ultimaSemana = semanas.data?.semanas[0];
  const mostrarResumenListo =
    !resumenOculto && ultimoCierre !== null && resumenSinVer(objetivoId, ultimoCierre);
  const falloAlgo = decidir.isError;

  const ocultarResumen = () => {
    if (ultimoCierre) marcarResumenVisto(objetivoId, ultimoCierre);
    setResumenOculto(true);
  };

  const conRestaurado = (miniId: string, restaurado: boolean) =>
    setRestaurados((actuales) => {
      const siguientes = new Set(actuales);
      if (restaurado) siguientes.add(miniId);
      else siguientes.delete(miniId);
      return siguientes;
    });

  const cuanto = minis.data ? cuantoCubren(minis.data.minis, teFaltan) : null;

  if (minis.isPending) {
    return (
      <p role="status" className="objetivos-cargando">
        Cargando tus mini-objetivos…
      </p>
    );
  }
  if (minis.isError) {
    return (
      <p role="alert" className="objetivos-pasos-error">
        No pudimos traer tus mini-objetivos. Tu objetivo sigue guardado: probá de nuevo en un rato.
      </p>
    );
  }

  return (
    <div className="seguimiento-objetivo">
      {mostrarResumenListo && ultimoCierre && (
        <ResumenListo
          ultimoCierre={ultimoCierre}
          onVer={() => {
            resumen.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
            resumen.current?.focus();
            ocultarResumen();
          }}
          onOcultar={ocultarResumen}
        />
      )}

      <section aria-labelledby="minis-titulo">
        <h2 id="minis-titulo" className="objetivos-subtitulo">
          Tus 3 mini-objetivos
        </h2>
        {minis.data.faltanDatos === 'OBJETIVO_ALCANZADO' ? (
          <p className="objetivos-pasos-vacio">
            Ya llegaste a tu meta: no hace falta seguir con mini-objetivos. Podés definir un
            objetivo más alto.
          </p>
        ) : minis.data.minis.length === 0 ? (
          <p className="objetivos-pasos-vacio">
            Cargá tus ventas, productos y costos en una sección, y Sami te propone tres pasos para
            llegar.
          </p>
        ) : (
          <>
            {cuanto && <p className="mini-objetivos-cubren">{cuanto}</p>}
            <div className="mini-objetivos">
              {minis.data.minis.map((mini) => (
                <MiniObjetivoCard
                  key={mini.id}
                  mini={mini}
                  decidido={
                    restaurados.has(mini.id)
                      ? undefined
                      : decisionDeLaSemana(lista, mini.id, estaSemana)
                  }
                  ocupado={ocupado}
                  onDecidir={(tipo) =>
                    decidir.mutate(
                      { miniId: mini.id, tipo },
                      { onSuccess: () => conRestaurado(mini.id, false) },
                    )
                  }
                  onRestaurar={() => conRestaurado(mini.id, true)}
                />
              ))}
            </div>
          </>
        )}
        {falloAlgo && (
          <p role="alert" className="objetivos-pasos-error">
            No pudimos guardar el cambio. Revisá tu conexión y volvé a intentar.
          </p>
        )}
      </section>

      <section
        aria-labelledby="semana-titulo"
        className="objetivos-semana"
        ref={resumen}
        tabIndex={-1}
      >
        <h2 id="semana-titulo" className="objetivos-subtitulo">
          Cómo te fue cada semana
        </h2>
        <div className="objetivos-semana-layout">
          <div className="objetivos-semana-grafico">
            {ultimaSemana && (
              <article className="objetivos-que-funciono">
                <p className="objetivos-etiqueta">
                  Cierre del domingo {diaCorto(domingoDe(ultimaSemana.semana))}
                </p>
                <p>{ultimaSemana.queFunciono}</p>
              </article>
            )}
            {progreso.data ? (
              <GraficoSemanal progreso={progreso.data} decisiones={lista} />
            ) : progreso.isPending ? (
              <p role="status">Cargando el gráfico…</p>
            ) : (
              <p role="alert">No pudimos dibujar el gráfico.</p>
            )}
          </div>
          <div className="objetivos-semana-recorrido">
            <div className="objetivos-semana-recorrido-caja">
              <h3>Lo que pasó</h3>
              <div
                id="recorrido-semanas"
                ref={recorrido}
                className={`recorrido-scroll ${verTodasLasSemanas ? 'abierto' : ''}`.trim()}
                tabIndex={verTodasLasSemanas ? 0 : undefined}
              >
                <RecorridoDeLaSemana
                  decisiones={lista}
                  semanas={semanas.data?.semanas ?? []}
                  estaSemana={estaSemana}
                />
              </div>
              {cantidadDeSemanas > 2 && (
                <button
                  type="button"
                  className="btn secondary recorrido-ver-mas"
                  aria-expanded={verTodasLasSemanas}
                  aria-controls="recorrido-semanas"
                  onClick={() => {
                    // Al volver a las últimas, el recorrido sube hasta ellas aunque la persona haya bajado.
                    if (verTodasLasSemanas && recorrido.current) recorrido.current.scrollTop = 0;
                    setVerTodasLasSemanas(!verTodasLasSemanas);
                  }}
                >
                  {verTodasLasSemanas ? 'Ver solo las últimas' : 'Ver todas las semanas'}
                </button>
              )}
            </div>
          </div>
        </div>
      </section>

      <section aria-labelledby="reajustes-titulo">
        <h2 id="reajustes-titulo" className="objetivos-subtitulo">
          Qué cambió Sami
        </h2>
        {semanas.data ? (
          <HistorialReajustes semanas={semanas.data.semanas} />
        ) : semanas.isPending ? (
          <p role="status">Cargando los cambios…</p>
        ) : (
          <p role="alert">No pudimos traer los cambios de los domingos.</p>
        )}
      </section>
    </div>
  );
}
