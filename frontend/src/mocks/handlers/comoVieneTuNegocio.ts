import { delay, http, HttpResponse } from 'msw';
import { negocioDe } from '@/lib/objetivosNegocio';
import { hoy } from '@/lib/sistema/formato';
import type { Sistema } from '@/lib/sistema/tipos';
import type { ComoVieneTuNegocio } from '@/types/comoVieneTuNegocio';
import { sistemasDe } from '../db/estado';
import { api, cuenta, LATENCIA_BACKEND_MS } from './comun';

const SEMANAS_DE_HISTORIAL = 6;

const haceDias = (iso: string, dias: number) => {
  const [anio = 0, mes = 1, dia = 1] = iso.split('-').map(Number);
  const fecha = new Date(anio, mes - 1, dia - dias);
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
};

// Las mismas cuentas que hace el backend con las secciones de la sesión: los últimos 30 días y, para cada una de las
// últimas seis semanas, lo que quedaba de los 30 días que terminan ese día.
export function comoVieneTuNegocioDe(sistemas: Sistema[], hoy: string): ComoVieneTuNegocio {
  const actual = negocioDe(sistemas, hoy);
  if (!actual.hayVentas) {
    return {
      hayVentas: false,
      vendiste: 0,
      mercaderia: 0,
      gastos: 0,
      queda: 0,
      teDeben: 0,
      semanas: [],
    };
  }
  const vendiste = Math.round(actual.vendiste);
  const mercaderia = Math.round(actual.mercaderia);
  const gastos = Math.round(actual.gastos);
  return {
    hayVentas: true,
    vendiste,
    mercaderia,
    gastos,
    queda: vendiste - mercaderia - gastos,
    teDeben: Math.round(actual.teDeben),
    semanas: Array.from({ length: SEMANAS_DE_HISTORIAL }, (_, indice) => {
      const atras = SEMANAS_DE_HISTORIAL - 1 - indice;
      return {
        semana: atras === 0 ? 'Hoy' : `Hace ${atras} sem.`,
        queda: Math.round(negocioDe(sistemas, haceDias(hoy, atras * 7)).queda),
      };
    }),
  };
}

export const handlersComoVieneTuNegocio = [
  http.get(api('/objetivos/negocio'), async ({ request }) => {
    await delay(LATENCIA_BACKEND_MS);
    const sesion = cuenta(request);
    if (sesion.error) return sesion.error;
    return HttpResponse.json(comoVieneTuNegocioDe(sistemasDe(sesion.usuario.email), hoy()));
  }),
];
