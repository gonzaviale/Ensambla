import { esDeudaAntigua } from '@/lib/caja/antiguedadDeuda';
import type { VentaCaja } from '@/types/caja';

export function AntiguedadDeudaCaja({ ventas }: { ventas: VentaCaja[] }) {
  if (!ventas.some((venta) => esDeudaAntigua(venta))) return null;
  return <span className="caja-deuda-antigua">Fiado de 30 días o más</span>;
}
