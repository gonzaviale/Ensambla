import { SparkleIcon } from '@phosphor-icons/react';
import { textoDeCuota } from '@/lib/objetivosSami';
import type { CuotaObjetivos } from '@/types/objetivos';

const porcentajeQueQueda = ({ restantes, limite }: CuotaObjetivos) =>
  limite > 0 ? Math.round((restantes / limite) * 100) : 0;

function BarraDeLaCuota({ cuota }: { cuota: CuotaObjetivos }) {
  return (
    <span className="sami-cuota-barra" aria-hidden>
      <i style={{ width: `${porcentajeQueQueda(cuota)}%` }} />
    </span>
  );
}

// «Te quedan 3 de 5 objetivos con IA este mes»: una línea chica y tranquila, con una barra que se va vaciando.
// Sin dato del servidor no se muestra.
export function CuotaDeObjetivos({ cuota }: { cuota: CuotaObjetivos | null | undefined }) {
  if (!cuota) return null;
  const agotada = cuota.restantes === 0;
  return (
    <p className="sami-cuota" data-agotada={agotada ? 'true' : undefined}>
      <span>
        <SparkleIcon aria-hidden size={13} weight="fill" /> {textoDeCuota(cuota)}
      </span>
      <BarraDeLaCuota cuota={cuota} />
    </p>
  );
}
