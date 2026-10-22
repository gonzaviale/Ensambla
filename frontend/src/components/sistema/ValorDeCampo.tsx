import {
  ESTADOS_BUENOS,
  ESTADOS_EN_CURSO,
  formatoFecha,
  formatoPesos,
  textoPlano,
} from '@/lib/sistema/formato';
import type { Campo, Sistema, Valor } from '@/lib/sistema/tipos';

// Cómo se ve el valor de un campo en una tabla: dinero en pesos, fechas dd/mm/aaaa, estados con etiqueta.
export function ValorDeCampo({
  sistema,
  campo,
  valor,
}: {
  sistema: Sistema | undefined;
  campo: Campo;
  valor: Valor;
}) {
  if (valor === '' || valor == null) return <span className="muted">—</span>;
  if (campo.type === 'currency') return <>{formatoPesos(valor)}</>;
  if (campo.type === 'date') return <>{formatoFecha(valor)}</>;
  if (campo.key === 'status') {
    const estado = String(valor);
    const clase = ESTADOS_BUENOS.includes(estado)
      ? 'good'
      : ESTADOS_EN_CURSO.includes(estado)
        ? 'progress'
        : '';
    return <span className={`status-badge ${clase}`.trim()}>{estado}</span>;
  }
  return <>{textoPlano(sistema, campo, valor)}</>;
}
