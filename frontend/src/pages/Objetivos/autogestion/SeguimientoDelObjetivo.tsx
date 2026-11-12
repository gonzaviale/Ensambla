import { useRef, useState } from 'react';
import { useAbandonarMiniObjetivo, useDecidirPaso } from '@/hooks/objetivos/mutations';
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
} from '@/lib/objetivosAutogestion';
import { hoy } from '@/lib/sistema/formato';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { MiniObjetivo } from '@/types/objetivos';
import { GraficoSemanal } from './GraficoSemanal';
import { HistorialReajustes } from './HistorialReajustes';
import { MiniObjetivoCard } from './MiniObjetivoCard';
import { RecorridoDeLaSemana } from './RecorridoDeLaSemana';
import { ResumenListo } from './ResumenListo';

// Lo que Sami hace solo con un objetivo: los 3 mini-objetivos, cómo fue cada semana y qué cambió los domingos.
export function SeguimientoDelObjetivo({ objetivoId }: { objetivoId: string }) {
  const [verTodasLasSemanas, setVerTodasLasSemanas] = useState(false);
  const minis = useMiniObjetivos(objetivoId);
  const progreso = useProgresoObjetivo(objetivoId);
  const decisiones = useDecisionesObjetivo(objetivoId);
  const semanas = useSemanasObjetivo(objetivoId);
  const abandonar = useAbandonarMiniObjetivo(objetivoId);
  const decidir = useDecidirPaso(objetivoId);
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const [resumenOculto, setResumenOculto] = useState(false);
  const resumen = useRef<HTMLElement>(null);
  const recorrido = useRef<HTMLDivElement>(null);

  const lista = todasLasDecisiones(decisiones.data);
  const estaSemana = lunesDe(hoy());
  const cantidadDeSemanas = new Set([
    ...lista.map((decision) => decision.semanaInicio),
    ...(semanas.data?.semanas ?? []).map((semana) => semana.semana),
  ]).size;
  const ocupado = abandonar.isPending || decidir.isPending;
  const ultimoCierre = semanas.data?.ultimoCierre ?? null;
  const ultimaSemana = semanas.data?.semanas[0];
  const mostrarResumenListo =
    !resumenOculto && ultimoCierre !== null && resumenSinVer(objetivoId, ultimoCierre);
  const falloAlgo = abandonar.isError || decidir.isError;

  const ocultarResumen = () => {
    if (ultimoCierre) marcarResumenVisto(objetivoId, ultimoCierre);
    setResumenOculto(true);
  };

  const quitar = (mini: MiniObjetivo) =>
    abrirModal({
      tipo: 'confirmar',
      titulo: '¿Quitar este mini-objetivo?',
      mensaje: `Dejás de seguir «${mini.titulo}». El domingo Sami te propone otro para este lugar.`,
      etiqueta: 'Quitar',
      onConfirmar: () => abandonar.mutate(mini.id),
    });

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
            Cargá tus ventas, productos y costos en un cuaderno, y Sami te propone tres pasos para
            llegar.
          </p>
        ) : (
          <div className="mini-objetivos">
            {minis.data.minis.map((mini) => (
              <MiniObjetivoCard
                key={mini.id}
                mini={mini}
                decidido={decisionDeLaSemana(lista, mini.id, estaSemana)}
                ocupado={ocupado}
                onDecidir={(tipo) => decidir.mutate({ miniId: mini.id, tipo })}
                onCambiar={() => abrirModal({ tipo: 'cambiarMini', objetivoId, mini })}
                onQuitar={() => quitar(mini)}
              />
            ))}
          </div>
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
