import { delay, http, HttpResponse } from 'msw';
import { allBlocks, clone, createGuided, refineDraft } from '@/lib/sistema/core';
import { pedidoDePropuestaSchema } from '@/types/sami';
import { errorResponse } from '../errores';
import { api, cuenta, pathDe } from './comun';

// Sami "piensa" un momento: en la pantalla se ve el globo con los puntos. En los tests no se espera.
const DEMORA_DE_SAMI_MS = import.meta.env.MODE === 'test' ? 0 : 650;

// El backend real va a pedirle esto a Sami; mientras tanto responde con las mismas reglas del mockup.
export const handlersSami = [
  http.post(api('/sami/propuesta'), async ({ request }) => {
    await delay(DEMORA_DE_SAMI_MS);
    const { error } = cuenta(request, true);
    if (error) return error;
    const pedido = pedidoDePropuestaSchema.safeParse(await request.json());
    if (!pedido.success) {
      return errorResponse(
        400,
        'Contame un poquito más para encontrar un punto de partida.',
        pathDe(request),
      );
    }

    if (pedido.data.tipo === 'crear') {
      const { texto, nombre, actividad } = pedido.data;
      const { system } = createGuided(texto, nombre, actividad);
      const piezas = allBlocks(system)
        .map((bloque) => bloque.name.toLowerCase())
        .join(', ');
      return HttpResponse.json({
        sistema: system,
        respuesta: `Te propongo esta base: ${piezas}. Ya podés verla a la derecha. ¿Qué te falta? ¿Cambiarías algo de lugar?`,
      });
    }

    const sistema = clone(pedido.data.sistema);
    const respuesta = refineDraft(sistema, pedido.data.texto);
    return HttpResponse.json({ sistema, respuesta });
  }),
];
