import { useCuotaObjetivos } from '@/hooks/objetivos/queries';
import { usePlanActual } from '@/hooks/planes/queries';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { LIMITE_PREMIUM_DE_OBJETIVOS } from '@/lib/objetivosSami';
import { CuotaDeObjetivos } from './CuotaDeObjetivos';

// La cuota del mes en la pantalla de Objetivos: «Te quedan 3 de 5» mientras haya, y al agotarse un aviso tranquilo que
// explica qué pasa y, en el plan Gratuito, lleva a Planes. Sin dato del servidor no muestra nada.
export function CuotaEnObjetivos() {
  const { cuota } = useCuotaObjetivos();
  const plan = usePlanActual();
  const navegacion = useNavegacion();
  if (!cuota) return null;
  if (cuota.restantes > 0) return <CuotaDeObjetivos cuota={cuota} />;
  return (
    <div className="sami-aparte" role="note" aria-label="Objetivos con IA de este mes">
      <CuotaDeObjetivos cuota={cuota} />
      <p>
        {plan === 'GRATUITO' && `Con Premium tenés ${LIMITE_PREMIUM_DE_OBJETIVOS} por mes. `}
        {plan === 'PREMIUM' && 'Se renuevan el mes que viene. '}
        Mientras tanto podés definir un objetivo con meta en pesos o guardar un seguimiento manual.
      </p>
      {plan === 'GRATUITO' && (
        <div className="sami-aparte-acciones">
          <button type="button" className="btn primary" onClick={() => void navegacion.irAPlanes()}>
            Ver planes
          </button>
        </div>
      )}
    </div>
  );
}
