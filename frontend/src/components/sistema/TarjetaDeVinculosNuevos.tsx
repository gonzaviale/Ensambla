import { useState } from 'react';
import type { VinculoPendiente } from '@/lib/sistema/acciones';
import { findBlock } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import { Icon } from './Icon';

/** Cuántos nombres se listan por pieza antes de resumir el resto en «y N más». */
const NOMBRES_A_LA_VISTA = 4;

type TarjetaDeVinculosNuevosProps = {
  pendientes: VinculoPendiente[];
  borrador: Sistema;
  pensando: boolean;
  /** `true` los suma a sus piezas y los vincula; `false` deja los vínculos vacíos. */
  onResolver: (sumar: boolean) => void;
};

// Los clientes y productos que Sami nombró (por ejemplo, al leer una foto de ventas) y que el sistema no tiene. No se
// crean solos: se listan por pieza y la persona elige si los suma. Mientras tanto, esas ventas quedan sin vincular.
// La lista es corta: con muchos nombres se ven los primeros y el resto se despliega a pedido.
export function TarjetaDeVinculosNuevos({
  pendientes,
  borrador,
  pensando,
  onResolver,
}: TarjetaDeVinculosNuevosProps) {
  const [verTodos, setVerTodos] = useState(false);
  const porPieza = new Map<string, string[]>();
  for (const pendiente of pendientes) {
    const pieza = findBlock(borrador, pendiente.destinoId)?.name ?? 'su pieza';
    porPieza.set(pieza, [...(porPieza.get(pieza) ?? []), pendiente.nombre]);
  }
  if (!porPieza.size) return null;
  const cuantos = pendientes.length === 1 ? 'Sumarlo' : 'Sumarlos';
  const ocultos = [...porPieza.values()].reduce(
    (total, nombres) => total + Math.max(0, nombres.length - NOMBRES_A_LA_VISTA),
    0,
  );

  return (
    <div className="conversation-examples" role="group" aria-label="Registros que no encontré">
      <span>No los encontré en tu sistema. ¿Los sumo?</span>
      <ul className="vinculos-nuevos">
        {[...porPieza].map(([pieza, nombres]) => {
          const vistos = verTodos ? nombres : nombres.slice(0, NOMBRES_A_LA_VISTA);
          const faltan = nombres.length - vistos.length;
          return (
            <li key={pieza}>
              <strong>{pieza}:</strong> {vistos.join(', ')}
              {faltan > 0 && ` y ${faltan} más`}
            </li>
          );
        })}
      </ul>
      {ocultos > 0 && (
        <button
          type="button"
          className="vinculos-nuevos-mas"
          aria-expanded={verTodos}
          onClick={() => setVerTodos((valor) => !valor)}
        >
          {verTodos ? 'Ver menos' : `Ver los ${ocultos} que faltan`}
        </button>
      )}
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
