import { SparkleIcon } from '@phosphor-icons/react';
import { textoDeCuota } from '@/lib/objetivosSami';
import type { CuotaObjetivos } from '@/types/objetivos';

// «Te quedan 3 de 5 objetivos con IA este mes»: una línea tranquila, sin alarma. Sin dato del servidor no se muestra.
export function CuotaDeObjetivos({ cuota }: { cuota: CuotaObjetivos | null | undefined }) {
  if (!cuota) return null;
  return (
    <p className="sami-cuota" data-agotada={cuota.restantes === 0 ? 'true' : undefined}>
      <SparkleIcon aria-hidden size={16} weight="fill" /> {textoDeCuota(cuota)}
    </p>
  );
}
