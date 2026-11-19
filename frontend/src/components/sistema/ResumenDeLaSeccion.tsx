import { totals } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';

// Los indicadores de un bloque de resumen: toman los registros de todos los bloques de la sección.
export function ResumenDeLaSeccion({ sistema }: { sistema: Sistema }) {
  const total = totals(sistema);
  const indicadores: [number, string][] = [
    [total.records, 'Registros en tu sección'],
    [total.pending, 'Pedidos pendientes'],
    [total.stock, 'Unidades en inventario'],
    [total.clients, 'Clientes registrados'],
  ];
  return (
    <div className="summary-block">
      {indicadores.map(([valor, etiqueta]) => (
        <div key={etiqueta}>
          <div className="summary-value">{valor}</div>
          <div className="summary-label">{etiqueta}</div>
        </div>
      ))}
    </div>
  );
}
