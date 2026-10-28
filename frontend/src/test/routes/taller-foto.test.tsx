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
    await screen.findByLabelText('Nombre de tu cuaderno', {}, ESPERA);

    await adjuntar(user);
    await screen.findByRole('img', { name: 'La foto que vas a mandar a Sami' });
    await clickButton(user, /Ajustalo conmigo/);
    expect(await screen.findByText(/Miré tu foto: anotaste tres ventas/, {}, ESPERA)).toBeVisible();
    // Los productos y las clientas de la foto no están: se suman sólo al confirmarlo.
    const tarjeta = screen.getByRole('group', { name: 'Registros que no encontré' });
    expect(tarjeta).toHaveTextContent('Lucía, Marta');
    expect(tarjeta).toHaveTextContent('Vela lavanda, Vela vainilla, Vela flotante');
    await sinViolaciones();
    await user.click(within(tarjeta).getByRole('button', { name: /Sumarlos/ }));
    expect(screen.queryByRole('group', { name: 'Registros que no encontré' })).toBeNull();

    await clickButton(user, /Sí, crear mi cuaderno/);

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

  it('Sami corrige un registro que leyó de la foto sin volver a cargarlo', async () => {
    const { user } = await abrirTaller();
    await clickButton(user, /Almacén o kiosco/);
    await screen.findByLabelText('Nombre de tu cuaderno', {}, ESPERA);
    await adjuntar(user);
    await screen.findByRole('img', { name: 'La foto que vas a mandar a Sami' });
    await clickButton(user, /Ajustalo conmigo/);
    await screen.findByText(/Miré tu foto/, {}, ESPERA);
    // Se saltean las dos dudas de la foto para escribir la corrección.
    await clickButton(user, 'Saltear esta duda');
    await clickButton(user, 'Saltear esta duda');

    await user.type(screen.getByRole('textbox', { name: /¿Qué le falta/ }), 'El precio era 9000');
    await clickButton(user, /Ajustalo conmigo/);

    expect(await screen.findByText(/corregí el precio a 9000/, {}, ESPERA)).toBeVisible();
    const ventas = deTipo(useSistemaStore.getState().taller?.borrador ?? undefined, 'sales').rows;
    expect(ventas.map((fila) => fila.price)).toEqual([9000, 8200, 6000]);
  });

  it('al ajustar un sistema, las ventas usan la clienta y el producto que ya están y sólo pregunta por los demás', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    const sistema = makeTemplate('shop', {}, false);
    sistema.name = 'Almacén de Ana';
    deTipo(sistema, 'clients').rows.push({ id: 'e1111111-cli', name: 'Lucía' });
    deTipo(sistema, 'inventory').rows.push({ id: 'e2222222-pro', name: 'Vela lavanda' });
    estado.sistemas.set(usuario.email, [sistema]);
    renderApp(`/sistemas/${sistema.id}?modo=usar`);
    await screen.findByRole('heading', { level: 1, name: /Almacén de Ana/ }, ESPERA);
    const user = userEvent.setup();
    await clickButton(user, /Ajustar con Sami/);
    await verifyHeadingPresentAsync('Lo ajustamos juntos.');
    const pedidos: { estado: { piezas: { tipo: string; nombres?: string[] }[] } }[] = [];
    server.events.on('request:start', async ({ request }) => {
      if (request.url.endsWith('/api/sami/taller')) pedidos.push(await request.clone().json());
    });

    await adjuntar(user);
    await screen.findByRole('img', { name: 'La foto que vas a mandar a Sami' });
    await clickButton(user, /Ajustalo conmigo/);
    await screen.findByText(/Miré tu foto/, {}, ESPERA);

    // Sami recibe los nombres que ya existen, para usarlos tal cual.
    const piezas = pedidos[0]?.estado.piezas ?? [];
    expect(piezas.find((pieza) => pieza.tipo === 'clients')?.nombres).toEqual(['Lucía']);
    expect(piezas.find((pieza) => pieza.tipo === 'inventory')?.nombres).toEqual(['Vela lavanda']);
    const tarjeta = screen.getByRole('group', { name: 'Registros que no encontré' });
    expect(tarjeta).toHaveTextContent('Marta');
    expect(tarjeta).toHaveTextContent('Vela vainilla, Vela flotante');
    expect(tarjeta).not.toHaveTextContent('Lucía');
    expect(tarjeta).not.toHaveTextContent('Vela lavanda');
    const borrador = useSistemaStore.getState().taller?.borrador ?? undefined;
    expect(deTipo(borrador, 'sales').rows.map((fila) => [fila.product, fila.customer])).toEqual([
      ['e2222222-pro', 'e1111111-cli'],
      ['', ''],
      ['', 'e1111111-cli'],
    ]);
    server.events.removeAllListeners();
  });

  it('si la persona no quiere sumarlos, las ventas quedan sin vincular y no se crea nada', async () => {
    const { user } = await abrirTaller();
    await clickButton(user, /Almacén o kiosco/);
    await screen.findByLabelText('Nombre de tu cuaderno', {}, ESPERA);
    await adjuntar(user);
    await screen.findByRole('img', { name: 'La foto que vas a mandar a Sami' });
    await clickButton(user, /Ajustalo conmigo/);
    await screen.findByText(/Miré tu foto/, {}, ESPERA);

    await clickButton(user, /No, dejalos sin vincular/);

    expect(screen.queryByRole('group', { name: 'Registros que no encontré' })).toBeNull();
    const borrador = useSistemaStore.getState().taller?.borrador ?? undefined;
    expect(deTipo(borrador, 'clients').rows.filter((fila) => !fila.__example)).toEqual([]);
    expect(deTipo(borrador, 'inventory').rows.filter((fila) => !fila.__example)).toEqual([]);
    expect(deTipo(borrador, 'sales').rows.map((fila) => fila.customer)).toEqual(['', '', '']);
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
    await clickButton(user, /Sumarlos/);
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
