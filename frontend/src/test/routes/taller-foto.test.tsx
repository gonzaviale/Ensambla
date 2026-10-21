import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { allBlocks, makeTemplate } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  clickButton,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
  verifyToastAsync,
} from '@/test/testHelpers';

type Usuario = ReturnType<typeof userEvent.setup>;

type PedidoAlTaller = {
  mensaje: string;
  historial: Record<string, unknown>[];
  imagen?: { tipo: string; datos: string };
};

const abrirTaller = async () => {
  setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  const router = renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
  const user = userEvent.setup();
  await clickButton(user, /Crear con Sami/);
  await verifyHeadingPresentAsync('Lo armamos juntos.');
  return { router, user };
};

const foto = (contenido: BlobPart = 'pixeles', tipo = 'image/jpeg') =>
  new File([contenido], 'cuaderno.jpg', { type: tipo });

/** Elige la foto con el botón de la cámara, que abre el selector de archivos. */
const adjuntar = async (user: Usuario, archivo: File = foto()) => {
  const selector = document.getElementById('guide-photo-file') as HTMLInputElement;
  expect(selector).toHaveAttribute('accept', 'image/*');
  await user.upload(selector, archivo);
};

const miniatura = () => screen.queryByRole('img', { name: 'La foto que vas a mandar a Sami' });

const espiarElTaller = () => {
  const pedidos: PedidoAlTaller[] = [];
  server.use(
    http.post('/api/sami/taller', async ({ request }) => {
      pedidos.push((await request.json()) as PedidoAlTaller);
      return HttpResponse.json({ respuesta: 'Ya la miré.', acciones: [], opciones: null });
    }),
  );
  return pedidos;
};

const sinViolaciones = async () => {
  const resultado = await axe(document.body, {
    rules: {
      'color-contrast': { enabled: false },
      region: { enabled: false },
      'heading-order': { enabled: false },
    },
  });
  expect(resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html}`)).toEqual([]);
};

const deTipo = (sistema: Sistema | undefined, tipo: string) => {
  const bloque = allBlocks(sistema).find((item) => item.type === tipo);
  if (!bloque) throw new Error(`Falta la pieza ${tipo}`);
  return bloque;
};

describe('una foto para Sami en el taller', () => {
  it('se adjunta con la cámara, se ve en miniatura, se puede quitar y viaja sola con el próximo mensaje', async () => {
    const pedidos = espiarElTaller();
    const { user } = await abrirTaller();

    await adjuntar(user);
    expect(
      await screen.findByRole('img', { name: 'La foto que vas a mandar a Sami' }),
    ).toBeVisible();
    expect(screen.getByRole('button', { name: 'Cambiar la foto para Sami' })).toBeVisible();
    await sinViolaciones();

    await user.click(screen.getByRole('button', { name: 'Quitar foto' }));
    expect(miniatura()).toBeNull();
    expect(useSistemaStore.getState().taller?.foto).toBeNull();

    await adjuntar(user);
    await screen.findByRole('img', { name: 'La foto que vas a mandar a Sami' });
    await clickButton(user, /Pensalo conmigo/);

    expect(await screen.findByText('Ya la miré.', {}, ESPERA)).toBeVisible();
    expect(pedidos).toHaveLength(1);
    const [pedido] = pedidos;
    expect(pedido?.mensaje).toBe('Te mando una foto.');
    expect(pedido?.imagen).toEqual({ tipo: 'image/jpeg', datos: btoa('pixeles') });
    // En la charla se ve la foto con el mensaje; ya no queda adjunta.
    expect(screen.getByText('Te mando una foto.')).toBeVisible();
    expect(screen.getByRole('img', { name: 'La foto que le mandaste a Sami' })).toHaveAttribute(
      'src',
      `data:image/jpeg;base64,${btoa('pixeles')}`,
    );
    expect(miniatura()).toBeNull();
    await sinViolaciones();

    // La foto no queda en el historial que se le manda a Sami: sólo el texto.
    await user.type(screen.getByRole('textbox', { name: /Contame un poco/ }), 'Gracias');
    await clickButton(user, /Pensalo conmigo/);
    await waitFor(() => expect(pedidos).toHaveLength(2), ESPERA);
    expect(pedidos[1]?.imagen).toBeUndefined();
    expect(pedidos[1]?.historial).toContainEqual({ quien: 'you', texto: 'Te mando una foto.' });
    expect(JSON.stringify(pedidos[1]?.historial)).not.toContain(btoa('pixeles'));
  });

  it('la miniatura se expande en un diálogo que se cierra con la X, afuera o Escape', async () => {
    const { user } = await abrirTaller();
    await adjuntar(user);
    await screen.findByRole('img', { name: 'La foto que vas a mandar a Sami' });
    const abrir = screen.getByRole('button', { name: 'Ver la foto en grande' });
    expect(screen.queryByRole('dialog', { name: 'La foto en grande' })).toBeNull();

    await user.click(abrir);
    const dialogo = screen.getByRole('dialog', { name: 'La foto en grande' });
    expect(within(dialogo).getByRole('img')).toHaveAttribute(
      'src',
      `data:image/jpeg;base64,${btoa('pixeles')}`,
    );
    expect(within(dialogo).getByRole('button', { name: 'Cerrar' })).toHaveFocus();
    await sinViolaciones();

    await user.click(within(dialogo).getByRole('button', { name: 'Cerrar' }));
    expect(screen.queryByRole('dialog', { name: 'La foto en grande' })).toBeNull();
    expect(abrir).toHaveFocus();

    await user.click(abrir);
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog', { name: 'La foto en grande' })).toBeNull();

    await user.click(abrir);
    await user.click(screen.getByRole('dialog').parentElement as HTMLElement);
    expect(screen.queryByRole('dialog', { name: 'La foto en grande' })).toBeNull();
    // La foto sigue adjunta.
    expect(miniatura()).toBeVisible();
  });

  it('con texto, la foto viaja con lo que se escribió', async () => {
    const pedidos = espiarElTaller();
    const { user } = await abrirTaller();

    await adjuntar(user, foto('otra', 'image/png'));
    await screen.findByRole('img', { name: 'La foto que vas a mandar a Sami' });
    await user.type(screen.getByRole('textbox', { name: /Contame un poco/ }), 'Mis ventas de ayer');
    await clickButton(user, /Pensalo conmigo/);

    await screen.findByText('Ya la miré.', {}, ESPERA);
    expect(pedidos[0]).toMatchObject({
      mensaje: 'Mis ventas de ayer',
      imagen: { tipo: 'image/png', datos: btoa('otra') },
    });
  });

  it('si la foto pesa más de 5 MB aun achicada, avisa y no la adjunta', async () => {
    const pedidos = espiarElTaller();
    const { user } = await abrirTaller();

    await adjuntar(user, foto(new Uint8Array(6 * 1024 * 1024)));

    await verifyToastAsync(/pesa más de 5 MB/);
    expect(miniatura()).toBeNull();
    expect(useSistemaStore.getState().taller?.foto).toBeNull();
    expect(pedidos).toHaveLength(0);
  });

  it('con el Sami simulado, lo leído de la foto queda como registros reales al crear el sistema', async () => {
    const { user, router } = await abrirTaller();
    await clickButton(user, /Almacén o kiosco/);
    await screen.findByLabelText('Nombre de tu sistema', {}, ESPERA);

    await adjuntar(user);
    await screen.findByRole('img', { name: 'La foto que vas a mandar a Sami' });
    await clickButton(user, /Ajustalo conmigo/);
    expect(await screen.findByText(/Miré tu foto: anotaste tres ventas/, {}, ESPERA)).toBeVisible();

    await clickButton(user, /Sí, crear mi sistema/);

    await waitFor(() => expect(useSistemaStore.getState().sistemas).toHaveLength(1), ESPERA);
    const [sistema] = useSistemaStore.getState().sistemas;
    await verifyPathAsync(router, `/sistemas/${sistema?.id}`);
    const filas = allBlocks(sistema).flatMap((bloque) => bloque.rows);
    expect(filas.some((fila) => fila.__example)).toBe(false);
    const productos = deTipo(sistema, 'inventory').rows;
    const clientes = deTipo(sistema, 'clients').rows;
    expect(productos.map((fila) => fila.name)).toEqual([
      'Vela lavanda',
      'Vela vainilla',
      'Vela flotante',
    ]);
    expect(clientes.map((fila) => fila.name)).toEqual(['Lucía', 'Marta']);
    expect(
      deTipo(sistema, 'sales').rows.map((fila) => [fila.product, fila.customer, fila.quantity]),
    ).toEqual([
      [productos[0]?.id, clientes[0]?.id, 2],
      [productos[1]?.id, clientes[1]?.id, 1],
      [productos[2]?.id, clientes[0]?.id, 4],
    ]);
  });

  it('al ajustar un sistema con Sami, la foto también va y los registros leídos se guardan', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    const sistema = makeTemplate('shop', {}, false);
    sistema.name = 'Almacén de Ana';
    estado.sistemas.set(usuario.email, [sistema]);
    const router = renderApp(`/sistemas/${sistema.id}?modo=usar`);
    await screen.findByRole('heading', { level: 1, name: /Almacén de Ana/ }, ESPERA);
    const user = userEvent.setup();
    await clickButton(user, /Ajustar con Sami/);
    await verifyHeadingPresentAsync('Lo ajustamos juntos.');

    await adjuntar(user);
    await screen.findByRole('img', { name: 'La foto que vas a mandar a Sami' });
    await clickButton(user, /Ajustalo conmigo/);
    await screen.findByText(/Miré tu foto/, {}, ESPERA);
    await clickButton(user, /Guardar cambios/);

    await verifyPathAsync(router, `/sistemas/${sistema.id}`);
    const guardado = useSistemaStore.getState().sistemas.find((item) => item.id === sistema.id);
    expect(deTipo(guardado, 'sales').rows.map((fila) => fila.quantity)).toEqual([2, 1, 4]);
    expect(deTipo(guardado, 'inventory').rows.map((fila) => fila.name)).toEqual([
      'Vela lavanda',
      'Vela vainilla',
      'Vela flotante',
    ]);
    expect(
      allBlocks(guardado)
        .flatMap((bloque) => bloque.rows)
        .some((fila) => fila.__example),
    ).toBe(false);
  });
});
