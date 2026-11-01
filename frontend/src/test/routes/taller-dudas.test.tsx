import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse, type JsonBodyType } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '@/mocks/node';
import { useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  clickButton,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
} from '@/test/testHelpers';

const DUDAS = [
  {
    tema: 'Tildes',
    pregunta: '¿Qué significan los tildes?',
    opciones: ['Ya entregado y cobrado', 'Entregado sin cobrar'],
  },
  {
    tema: 'Número de la derecha',
    pregunta: '¿El número de la derecha es costo o lo que deben?',
    opciones: ['El costo', 'Lo que me deben'],
  },
  {
    tema: 'Total de Giz. Pilates',
    pregunta: '¿Cuál es el total correcto de Giz. Pilates?',
    opciones: ['37.000', '28.800'],
  },
];

const abrirTaller = async () => {
  setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
  const user = userEvent.setup();
  await clickButton(user, /Crear con Sami/);
  await verifyHeadingPresentAsync('Lo armamos juntos.');
  return user;
};

const espiarElTaller = (responder: (pedido: Record<string, unknown>) => JsonBodyType) => {
  const pedidos: Record<string, unknown>[] = [];
  server.use(
    http.post('/api/sami/taller', async ({ request }) => {
      const pedido = (await request.json()) as Record<string, unknown>;
      pedidos.push(pedido);
      return HttpResponse.json(responder(pedido));
    }),
  );
  return pedidos;
};

/** El primer turno deja tres dudas; el siguiente (con las respuestas juntas) contesta sin más preguntas. */
const conTresDudas = async () => {
  const pedidos = espiarElTaller((pedido) =>
    pedido.mensaje === 'tengo una dietética'
      ? { respuesta: 'Veo tu cuaderno.', acciones: [], preguntas: DUDAS }
      : { respuesta: 'Listo, ya lo ajusté.', acciones: [], preguntas: [] },
  );
  const user = await abrirTaller();
  await user.type(screen.getByRole('textbox', { name: /Contame un poco/ }), 'tengo una dietética');
  await clickButton(user, /Pensalo conmigo/);
  return { user, pedidos };
};

const botonera = () => screen.findByRole('group', { name: 'Respuestas rápidas' }, ESPERA);

describe('varias dudas de Sami en un mismo turno', () => {
  it('las muestra de a una con su progreso y manda todas las respuestas juntas en un solo mensaje', async () => {
    const { user, pedidos } = await conTresDudas();

    expect(within(await botonera()).getByText('Duda 1 de 3')).toBeVisible();
    expect(screen.getByText('¿Qué significan los tildes?')).toBeVisible();
    expect(
      screen.queryByText('¿Cuál es el total correcto de Giz. Pilates?'),
    ).not.toBeInTheDocument();

    await user.click(
      within(await botonera()).getByRole('button', { name: /^Ya entregado y cobrado/ }),
    );
    expect(within(await botonera()).getByText('Duda 2 de 3')).toBeVisible();
    expect(pedidos).toHaveLength(1);

    await user.click(within(await botonera()).getByRole('button', { name: /^Lo que me deben/ }));
    expect(within(await botonera()).getByText('Duda 3 de 3')).toBeVisible();
    expect(pedidos).toHaveLength(1);

    await user.click(within(await botonera()).getByRole('button', { name: /^28\.800/ }));

    expect(await screen.findByText('Listo, ya lo ajusté.', {}, ESPERA)).toBeVisible();
    expect(pedidos).toHaveLength(2);
    expect(pedidos[1]?.mensaje).toBe(
      'Tildes: Ya entregado y cobrado. Número de la derecha: Lo que me deben. Total de Giz. Pilates: 28.800.',
    );
    expect(screen.queryByRole('group', { name: 'Respuestas rápidas' })).not.toBeInTheDocument();
  });

  it('la historia que viaja a Sami incluye las dudas que había planteado', async () => {
    const { user, pedidos } = await conTresDudas();
    for (const nombre of [/^Ya entregado/, /^El costo/, /^37\.000/]) {
      await user.click(within(await botonera()).getByRole('button', { name: nombre }));
    }
    await screen.findByText('Listo, ya lo ajusté.', {}, ESPERA);

    const historial = (pedidos[1]?.historial ?? []) as { quien: string; texto: string }[];
    expect(historial.at(-1)).toEqual({
      quien: 'sami',
      texto:
        '¿Qué significan los tildes? ¿El número de la derecha es costo o lo que deben? ¿Cuál es el total correcto de Giz. Pilates?',
    });
  });

  it('se puede saltear una duda y escribir otra respuesta; lo salteado no viaja', async () => {
    const { user, pedidos } = await conTresDudas();

    await user.click(within(await botonera()).getByRole('button', { name: 'Saltear esta duda' }));
    await user.click(
      within(await botonera()).getByRole('button', { name: /Escribir otra respuesta/ }),
    );
    await user.type(
      screen.getByRole('textbox', { name: 'Tu respuesta a esta duda' }),
      'Es lo que ya pagaron',
    );
    await user.click(screen.getByRole('button', { name: 'Responder' }));
    expect(within(await botonera()).getByText('Duda 3 de 3')).toBeVisible();
    await user.click(within(await botonera()).getByRole('button', { name: /^28\.800/ }));

    await screen.findByText('Listo, ya lo ajusté.', {}, ESPERA);
    expect(pedidos[1]?.mensaje).toBe(
      'Número de la derecha: Es lo que ya pagaron. Total de Giz. Pilates: 28.800.',
    );
  });

  it('si se saltean todas no se manda nada y la botonera desaparece', async () => {
    const { user, pedidos } = await conTresDudas();

    for (let n = 0; n < 3; n++) {
      await user.click(within(await botonera()).getByRole('button', { name: 'Saltear esta duda' }));
    }

    expect(screen.queryByRole('group', { name: 'Respuestas rápidas' })).not.toBeInTheDocument();
    expect(pedidos).toHaveLength(1);
    expect(useSistemaStore.getState().taller?.dudas).toEqual([]);
  });

  it('una sola duda sigue siendo la botonera de siempre: sin progreso ni saltear, y la respuesta va tal cual', async () => {
    const pedidos = espiarElTaller((pedido) =>
      pedido.mensaje === 'Sí'
        ? { respuesta: 'Perfecto.', acciones: [], preguntas: [] }
        : {
            respuesta: 'Hola.',
            acciones: [],
            preguntas: [{ pregunta: '¿Sumamos gastos?', opciones: ['Sí', 'No'] }],
          },
    );
    const user = await abrirTaller();
    await user.type(screen.getByRole('textbox', { name: /Contame un poco/ }), 'hola sami');
    await clickButton(user, /Pensalo conmigo/);

    const grupo = await botonera();
    expect(within(grupo).queryByText(/Duda 1 de/)).not.toBeInTheDocument();
    expect(
      within(grupo).queryByRole('button', { name: 'Saltear esta duda' }),
    ).not.toBeInTheDocument();
    await user.click(within(grupo).getByRole('button', { name: /^Sí/ }));

    expect(await screen.findByText('Perfecto.', {}, ESPERA)).toBeVisible();
    expect(pedidos.map((pedido) => pedido.mensaje)).toEqual(['hola sami', 'Sí']);
  });
});
