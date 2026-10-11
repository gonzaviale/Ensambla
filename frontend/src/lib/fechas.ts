const MS_DIA = 24 * 60 * 60 * 1000;

// "hoy", "ayer", "hace 3 días", "hace 2 semanas" o la fecha: para resúmenes de "usado por última vez".
export function haceCuanto(fechaIso: string, ahora: Date = new Date()): string {
  const fecha = new Date(fechaIso);
  if (Number.isNaN(fecha.getTime())) return '';
  const dias = Math.floor((ahora.getTime() - fecha.getTime()) / MS_DIA);
  if (dias <= 0) return 'hoy';
  if (dias === 1) return 'ayer';
  if (dias < 14) return `hace ${dias} días`;
  if (dias < 60) return `hace ${Math.floor(dias / 7)} semanas`;
  return fecha.toLocaleDateString('es-AR', { day: 'numeric', month: 'short', year: 'numeric' });
}
