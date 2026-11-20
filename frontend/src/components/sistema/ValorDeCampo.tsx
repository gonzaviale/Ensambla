import { comportamientoDe } from '@/lib/sistema/comportamiento';
import { ESTADOS_BUENOS, ESTADOS_EN_CURSO } from '@/lib/sistema/formato';
import type { Campo, Sistema, Valor } from '@/lib/sistema/tipos';

function claseDelEstado(estado: string): string {
  if (ESTADOS_BUENOS.includes(estado)) return 'good';
  if (ESTADOS_EN_CURSO.includes(estado)) return 'progress';
  return '';
}

function EstadoDelRegistro({ estado }: { estado: string }) {
  return <span className={`status-badge ${claseDelEstado(estado)}`.trim()}>{estado}</span>;
}

// Cómo se ve el valor de un campo en una tabla: cada tipo sabe mostrarse (dinero en pesos, fechas dd/mm/aaaa) y
// los estados llevan etiqueta.
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
  if (campo.key === 'status') return <EstadoDelRegistro estado={String(valor)} />;
  return <>{comportamientoDe(campo.type).enTabla(campo, valor, sistema)}</>;
}
