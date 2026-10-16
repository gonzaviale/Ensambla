import { useState } from 'react';
import { PlusCircleIcon } from '@phosphor-icons/react';
import { Button, InfoTip, Panel } from '@/components/ui';
import { Toast } from '@/components/ui/Toast';
import { CandadoPlan, EtiquetaPlan } from '@/components/Plan/PlanGate';
import { useSumarPieza } from '@/hooks/negocio/useArmarSistema';
import { usePlanActual } from '@/hooks/negocio/usePlan';
import { PIEZAS, nombreDePieza } from '@/lib/catalogo/piezas';
import { definicion, respuestasQueDesbloquea } from '@/lib/respuestas/definiciones';
import type { PiezaId, RubroId } from '@/types/negocio';

type Aviso = { mensaje: string; tipo: 'exito' | 'error' };

type Props = {
  sistemaId: string;
  /** Cuántas tablas tiene hoy el sistema (cuentan contra el límite del plan Gratis). */
  cantidadDeBloques: number;
  piezasPresentes: PiezaId[];
  /** Rubro del sistema, para nombrar el bloque nuevo («Platos» en gastronomía). */
  rubro: RubroId;
};

/** «Sumar un bloque»: las piezas del catálogo que todavía no están, con lo que desbloquean. */
export function SumarBloque({ sistemaId, cantidadDeBloques, piezasPresentes, rubro }: Props) {
  const plan = usePlanActual();
  const sumar = useSumarPieza(sistemaId);
  const [aviso, setAviso] = useState<Aviso | null>(null);
  const faltan = PIEZAS.filter((pieza) => !piezasPresentes.includes(pieza.id));
  const limite = plan.definicion.bloques;
  const conCandado =
    !plan.incluye('bloques-ilimitados') && limite !== null && cantidadDeBloques >= limite;

  async function sumarPieza(id: PiezaId) {
    const nombre = nombreDePieza(id, rubro);
    try {
      await sumar.mutateAsync({ id, rubro });
      setAviso({ tipo: 'exito', mensaje: `Sumaste «${nombre}» a tu sistema.` });
    } catch {
      setAviso({ tipo: 'error', mensaje: `No pudimos sumar «${nombre}». Probá de nuevo.` });
    }
  }

  return (
    <>
      {faltan.length > 0 && (
        <section
          data-recorrido="bloques-sumar"
          aria-labelledby="sumar-bloque"
          className="flex flex-col gap-4"
        >
          <div className="flex flex-col gap-1">
            <h2 id="sumar-bloque" className="m-0 text-h3">
              Sumar un bloque
              <InfoTip tema="qué es un bloque" className="ml-1">
                Un bloque es una pieza de tu sistema: Ventas, Gastos, Productos… Cada bloque que
                sumás desbloquea respuestas nuevas, como cuánto te quedó o quién te debe.
              </InfoTip>
            </h2>
            <p className="m-0 text-body text-soft">Cada bloque nuevo desbloquea más respuestas.</p>
          </div>

          {conCandado && (
            <CandadoPlan
              funcion="bloques-ilimitados"
              beneficio={`El plan Gratis llega hasta ${limite} bloques.`}
            />
          )}

          <ul className="m-0 grid list-none grid-cols-1 gap-4 p-0 md:grid-cols-2">
            {faltan.map((pieza) => {
              const respuestas = respuestasQueDesbloquea(pieza.id, piezasPresentes).map(
                (id) => definicion(id).pregunta,
              );
              const nombre = nombreDePieza(pieza.id, rubro);
              return (
                <li key={pieza.id} className="min-w-0">
                  <Panel flat className="flex h-full flex-col gap-3">
                    <h3 className="m-0 text-h3">{nombre}</h3>
                    <p className="m-0 text-body text-soft">{pieza.descripcion}</p>
                    {respuestas.length > 0 && (
                      <p className="m-0 text-body">
                        <strong>Al sumarlo desbloqueás:</strong> {respuestas.join(' · ')}
                      </p>
                    )}
                    <div className="mt-auto">
                      {conCandado ? (
                        <EtiquetaPlan funcion="bloques-ilimitados" />
                      ) : (
                        <Button
                          variant="primary"
                          aria-label={`Sumar ${nombre}`}
                          loading={sumar.isPending && sumar.variables?.id === pieza.id}
                          disabled={sumar.isPending}
                          onClick={() => void sumarPieza(pieza.id)}
                        >
                          <PlusCircleIcon aria-hidden size={22} />
                          Sumar
                        </Button>
                      )}
                    </div>
                  </Panel>
                </li>
              );
            })}
          </ul>
        </section>
      )}
      {aviso && <Toast {...aviso} onClose={() => setAviso(null)} />}
    </>
  );
}
