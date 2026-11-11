import { delay, http, HttpResponse } from 'msw';
import { hoy } from '@/lib/sistema/formato';
import { pasosObjetivoSimulados } from '../db/objetivos';
import { sistemasDe } from '../db/estado';
import { zonaDeLaPersona } from '../db/zona';
import { api, cuenta, LATENCIA_BACKEND_MS } from './comun';

// La simulación completa conserva el contrato real. Cada prueba puede reemplazar la respuesta que necesita.
export const handlersObjetivos = [
  http.get(api('/objetivos/pasos'), async ({ request }) => {
    const { usuario, error } = cuenta(request);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    return HttpResponse.json(
      pasosObjetivoSimulados(sistemasDe(usuario.email), hoy(), zonaDeLaPersona()),
    );
  }),
];
