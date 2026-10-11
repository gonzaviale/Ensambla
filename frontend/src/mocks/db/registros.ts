import type { Bloque, Campo } from '@/types/proyecto';
import type { Registro, Valor } from '@/types/registro';
import { estado } from './estado';

export function agregarRegistro(
  bloqueId: string,
  valores: Registro['valores'],
  creadoEn = new Date().toISOString(),
) {
  const registro: Registro = {
    id: crypto.randomUUID(),
    bloqueId,
    valores,
    creadoEn,
    actualizadoEn: creadoEn,
  };
  estado.registros.set(registro.id, registro);
  return registro;
}

function valorDemo(campo: Campo, n: number): Valor {
  switch (campo.tipo) {
    case 'NUMERO':
      return (n * 7) % 100;
    case 'FECHA':
      return `2026-10-${String(1 + (n % 28)).padStart(2, '0')}`;
    case 'OPCION':
      return campo.opciones[n % Math.max(campo.opciones.length, 1)] ?? null;
    case 'SI_NO':
      return n % 2 === 0;
    case 'TEXTO':
      return `${campo.nombre} ${n + 1}`;
  }
}

// Filas de ejemplo para recorrer la tabla en el navegador mientras no existe la carga de datos.
export function sembrarRegistrosDemo(bloques: Bloque[], cantidad = 30) {
  const inicio = Date.UTC(2026, 9, 1);
  for (const bloque of bloques) {
    for (let n = 0; n < cantidad; n++) {
      const valores = Object.fromEntries(
        bloque.campos.map((campo) => [campo.id, valorDemo(campo, n)]),
      );
      agregarRegistro(bloque.id, valores, new Date(inicio + n * 3_600_000).toISOString());
    }
  }
}
