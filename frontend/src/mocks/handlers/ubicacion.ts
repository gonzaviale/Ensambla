import { http, HttpResponse } from 'msw';
import { errorDeConsulta, normalizarConsulta } from '@/lib/ubicacion';
import { puntoSchema } from '@/types/ubicacion';
import {
  DIRECCION_EN_ZONA_CHICA_DE_PRUEBA,
  DIRECCION_SIN_ZONA_DE_PRUEBA,
  DIRECCIONES_DE_PRUEBA,
  localidadDelPuntoDePrueba,
  RUBROS_DE_PRUEBA,
} from '../db/ubicacion';
import { errorResponse } from '../errores';
import { api, cuenta, pathDe } from './comun';

export const handlersUbicacion = [
  http.get(api('/ubicacion/rubros'), ({ request }) => {
    const { error } = cuenta(request);
    return error ?? HttpResponse.json({ rubros: RUBROS_DE_PRUEBA });
  }),
  http.post(api('/ubicacion/direcciones'), async ({ request }) => {
    const { error } = cuenta(request, true);
    if (error) return error;
    const body = (await request.json()) as { consulta?: unknown };
    const consulta = typeof body?.consulta === 'string' ? normalizarConsulta(body.consulta) : '';
    if (errorDeConsulta(consulta)) return errorResponse(400, 'Dirección inválida', pathDe(request));
    const resultados = /varela|san justo/i.test(consulta)
      ? DIRECCIONES_DE_PRUEBA
      : /ramos mej/i.test(consulta)
        ? [DIRECCION_EN_ZONA_CHICA_DE_PRUEBA]
        : /palermo/i.test(consulta)
          ? [DIRECCION_SIN_ZONA_DE_PRUEBA]
          : [];
    return HttpResponse.json({ resultados });
  }),
  http.post(api('/ubicacion/localidad'), async ({ request }) => {
    const { error } = cuenta(request, true);
    if (error) return error;
    const punto = puntoSchema.safeParse(await request.json());
    if (!punto.success) return errorResponse(400, 'Punto inválido', pathDe(request));
    const localidad = localidadDelPuntoDePrueba(punto.data);
    return HttpResponse.json({ localidad, localidades: [localidad] });
  }),
];
