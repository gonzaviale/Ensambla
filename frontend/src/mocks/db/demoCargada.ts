import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { PREFERENCIAS_INICIALES } from '@/types/preferencias';
import { estado, guardarPreferenciasDe, guardarSistemasDe, sistemasDe } from './estado';

const DIA_MS = 24 * 60 * 60 * 1000;

/**
 * Deja la cuenta de demostración lista para mostrar: el almacén con muchos datos (con las ventas de los últimos 90 días
 * contados desde hoy), los avisos sin descartar y Premium por un año. Se corre en cada arranque de `npm run dev:demo`,
 * así la demo siempre empieza igual; los cuadernos que armó la persona a mano se conservan.
 */
export function sembrarDemoCargada(email: string, ahora = new Date()) {
  const propios = sistemasDe(email).filter((sistema) => !sistema.demoAlmacen);
  guardarSistemasDe(email, [crearAlmacenCargado(hoy()), ...propios]);
  guardarPreferenciasDe(email, PREFERENCIAS_INICIALES);
  estado.pruebasPremium.set(email, {
    inicioEn: new Date(ahora.getTime() - DIA_MS).toISOString(),
    finEn: new Date(ahora.getTime() + 365 * DIA_MS).toISOString(),
    paginasIaUtilizadas: 0,
    limitePaginasIa: 10,
  });
  estado.alCambiar?.();
}
