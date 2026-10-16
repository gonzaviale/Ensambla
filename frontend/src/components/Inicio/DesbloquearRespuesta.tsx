import { useId } from 'react';
import { LockSimpleOpenIcon } from '@phosphor-icons/react';
import { InfoTip, Panel } from '@/components/ui';
import { CandadoPlan } from '@/components/Plan/PlanGate';
import { BotonSumarPieza } from '@/components/Respuestas/BotonSumarPieza';
import { unir } from '@/components/Respuestas/negocioUtil';
import { pieza } from '@/lib/catalogo/piezas';
import type { PiezaId, RespuestaVista } from '@/types/negocio';

type Props = {
  vista: RespuestaVista;
  faltan: PiezaId[];
  /** El plan Gratis ya llegó a su máximo de bloques. */
  tope: boolean;
  sumando: PiezaId | null;
  onSumar: (id: PiezaId) => void;
};

// La respuesta más cerca de desbloquearse: qué bloque falta y un botón para sumarlo.
export function DesbloquearRespuesta({ vista, faltan, tope, sumando, onSumar }: Props) {
  const idTitulo = useId();
  const primera = faltan[0];
  if (!primera) return null;
  const nombres = faltan.map((id) => pieza(id).nombre);

  return (
    <Panel aria-labelledby={idTitulo} className="flex flex-col items-start gap-3">
      <p className="m-0 flex flex-wrap items-center gap-x-2 text-body font-semibold text-accent-800">
        <LockSimpleOpenIcon aria-hidden size={24} weight="bold" />
        <span>
          Desbloqueala sumando <strong>{unir(nombres)}</strong>
        </span>
        <InfoTip tema="qué es un bloque">
          Un bloque es una pieza de tu sistema, por ejemplo Ventas o Gastos. Cada bloque que sumás
          desbloquea respuestas nuevas, como «cuánto te quedó este mes».
        </InfoTip>
      </p>
      <h2 id={idTitulo} className="m-0 text-h3">
        {vista.definicion.pregunta}
      </h2>
      <p className="m-0 text-body text-soft">{vista.definicion.ayuda}</p>
      {tope ? (
        <CandadoPlan
          funcion="bloques-ilimitados"
          beneficio="Con el plan Gratis tenés hasta 3 bloques."
        />
      ) : (
        <BotonSumarPieza id={primera} sumando={sumando} onSumar={onSumar} variant="primary" />
      )}
    </Panel>
  );
}
