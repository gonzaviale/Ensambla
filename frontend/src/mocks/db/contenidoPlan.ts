import { estado } from './estado';
import { planesDe } from './planes';
import type { AccesoContenido } from '@/types/contenidoPlan';
import type { Zona } from '@/lib/sistema/zona';

const LIMITE_GRATUITO = 6;

/** Cada pantalla de «Mi zona» gasta su propio cupo gratuito, igual que el servidor. */
export type TipoDeCupo = 'AVISOS' | 'ZONA' | 'ZONA_CERCA' | 'ZONA_OTRA';

export function contenidoDe(email: string, tipo: TipoDeCupo, claves: string[]): AccesoContenido {
  const ahora = new Date();
  const periodo = ahora.toISOString().slice(0, 7);
  const finPeriodo = new Date(
    Date.UTC(ahora.getUTCFullYear(), ahora.getUTCMonth() + 1, 1),
  ).toISOString();
  const candidatas = [...new Set(claves)];
  if (planesDe(email).suscripcion.codigoPlanActual === 'PREMIUM')
    return {
      autorizadas: candidatas,
      usadas: 0,
      limite: null,
      total: candidatas.length,
      bloqueadas: 0,
      finPeriodo,
    };
  const clave = `${email}|${tipo}|${periodo}`;
  const conocidas = new Set(estado.contenidosDesbloqueados.get(clave) ?? []);
  const autorizadas = candidatas.filter((id) => {
    if (conocidas.has(id)) return true;
    if (conocidas.size >= LIMITE_GRATUITO) return false;
    conocidas.add(id);
    return true;
  });
  estado.contenidosDesbloqueados.set(clave, [...conocidas]);
  estado.alCambiar?.();
  return {
    autorizadas,
    usadas: conocidas.size,
    limite: LIMITE_GRATUITO,
    total: candidatas.length,
    bloqueadas: candidatas.length - autorizadas.length,
    finPeriodo,
  };
}

/** La zona con sólo lo que el cupo `tipo` deja ver (Precios gasta `ZONA`; las otras pestañas, el suyo). */
export function zonaConAcceso(email: string, zona: Zona, tipo: TipoDeCupo = 'ZONA'): Zona {
  const claves = zona.productos.map((producto) => `zona:${zona.nombre}|${producto.nombre}`);
  const acceso = contenidoDe(email, tipo, claves);
  return {
    ...zona,
    acceso,
    productos: zona.productos.filter((_, indice) =>
      acceso.autorizadas.includes(claves[indice] ?? ''),
    ),
  };
}
