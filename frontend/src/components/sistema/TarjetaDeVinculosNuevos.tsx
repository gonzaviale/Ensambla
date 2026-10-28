import type { VinculoPendiente } from '@/lib/sistema/acciones';
import { findBlock } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import { Icon } from './Icon';

type TarjetaDeVinculosNuevosProps = {
  pendientes: VinculoPendiente[];
  borrador: Sistema;
  pensando: boolean;
  /** `true` los suma a sus piezas y los vincula; `false` deja los vínculos vacíos. */
  onResolver: (sumar: boolean) => void;
};

// Los clientes y productos que Sami nombró (por ejemplo, al leer una foto de ventas) y que el sistema no tiene. No se
// crean solos: se listan por pieza y la persona elige si los suma. Mientras tanto, esas ventas quedan sin vincular.
export function TarjetaDeVinculosNuevos({
  pendientes,
  borrador,
  pensando,
  onResolver,
}: TarjetaDeVinculosNuevosProps) {
  const porPieza = new Map<string, string[]>();
  for (const pendiente of pendientes) {
    const pieza = findBlock(borrador, pendiente.destinoId)?.name ?? 'su pieza';
    porPieza.set(pieza, [...(porPieza.get(pieza) ?? []), pendiente.nombre]);
  }
  if (!porPieza.size) return null;
  const cuantos = pendientes.length === 1 ? 'Sumarlo' : 'Sumarlos';

  return (
    <div className="conversation-examples" role="group" aria-label="Registros que no encontré">
      <span>No los encontré en tu sistema. ¿Los sumo?</span>
      <ul className="vinculos-nuevos">
        {[...porPieza].map(([pieza, nombres]) => (
          <li key={pieza}>
            <strong>{pieza}:</strong> {nombres.join(', ')}
          </li>
        ))}
      </ul>
      <button type="button" disabled={pensando} onClick={() => onResolver(true)}>
        {cuantos}
        <Icon name="arrow" />
      </button>
      <button type="button" disabled={pensando} onClick={() => onResolver(false)}>
        No, dejalos sin vincular
      </button>
    </div>
  );
}
