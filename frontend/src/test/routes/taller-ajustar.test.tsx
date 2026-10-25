import { screen, waitFor } from '@testing-library/react';
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

type PedidoAlTaller = {
  mensaje: string;
  estado: {
    nombre: string;
    piezas: { id: string; tipo: string; campos: { etiqueta: string }[] }[];
  };
};

/** Un almacén ya creado, abierto en su pantalla. */
const abrirSistema = async () => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  const sistema = makeTemplate('shop', {}, true);
  sistema.name = 'Almacén de Ana';
  estado.sistemas.set(usuario.email, [sistema]);
  const router = renderApp(`/sistemas/${sistema.id}?modo=usar`);
  await screen.findByRole('heading', { level: 1, name: /Almacén de Ana/ }, ESPERA);
  return { router, user: userEvent.setup(), sistema, usuario };
};

const enElStore = (id: string) => {
  const sistema = useSistemaStore.getState().sistemas.find((item) => item.id === id);
  if (!sistema) throw new Error('Falta el sistema en el store');
  return sistema;
};

const deTipo = (sistema: Sistema | undefined, tipo: string) => {
  const bloque = allBlocks(sistema).find((item) => item.type === tipo);
  if (!bloque) throw new Error(`Falta la pieza ${tipo}`);
  return bloque;
};

const etiquetas = (sistema: Sistema | undefined, tipo: string) =>
  deTipo(sistema, tipo).fields.map((campo) => campo.label);

const pedirASami = async (user: ReturnType<typeof userEvent.setup>, texto: string) => {
  await user.type(screen.getByRole('textbox', { name: /¿Qué le falta/ }), texto);
  await clickButton(user, /Ajustalo conmigo/);
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

describe('ajustar con Sami un sistema que ya existe', () => {
  it('abre el taller con el sistema, aplica lo que pide Sami y al guardar lo reemplaza y vuelve a él', async () => {
    const pedidos: PedidoAlTaller[] = [];
    server.use(
      http.post('/api/sami/taller', async ({ request }) => {
        const pedido = (await request.json()) as PedidoAlTaller;
        pedidos.push(pedido);
        const ventas = pedido.estado.piezas.find((pieza) => pieza.tipo === 'sales');
        return HttpResponse.json({
          respuesta: 'Listo, ahora es Clienta y va primero en las ventas.',
          acciones: [
            {
              herramienta: 'editar_campo',
              argumentos: { pieza: ventas?.id, campo: 'Cliente', etiqueta: 'Clienta', posicion: 1 },
            },
          ],
          opciones: null,
        });
      }),
    );
    const { router, user, sistema, usuario } = await abrirSistema();
    const filasAntes = allBlocks(sistema).map((bloque) => bloque.rows.length);

    await clickButton(user, /Ajustar con Sami/);

    await verifyHeadingPresentAsync('Lo ajustamos juntos.');
    expect(router.state.location.search).toEqual({ sistema: sistema.id });
    expect(screen.getByText(/Estoy mirando «Almacén de Ana»/)).toBeVisible();
    expect(screen.getByLabelText('Nombre de tu sistema')).toHaveValue('Almacén de Ana');
    expect(screen.getByRole('button', { name: /Guardar cambios/ })).toBeVisible();
    expect(screen.queryByRole('button', { name: /Sí, crear mi sistema/ })).toBeNull();
    expect(screen.queryByText(/TU PRIMER SISTEMA/)).toBeNull();
    await sinViolaciones();

    await pedirASami(user, 'cambiale el nombre a Cliente por Clienta y ponelo primero');

    expect(await screen.findByText(/ahora es Clienta/, {}, ESPERA)).toBeVisible();
    expect(pedidos[0]?.estado.piezas.map((pieza) => pieza.tipo)).toEqual([
      'inventory',
      'sales',
      'clients',
    ]);
    // Hasta guardar, el sistema no cambia.
    expect(etiquetas(enElStore(sistema.id), 'sales')[0]).toBe('Producto');
    expect(screen.getByText('Cambios sin guardar')).toBeVisible();

    await clickButton(user, /Guardar cambios/);

    await verifyPathAsync(router, `/sistemas/${sistema.id}`);
    await screen.findByRole('heading', { level: 1, name: /Almacén de Ana/ }, ESPERA);
    await verifyToastAsync('Guardé los cambios en «Almacén de Ana».');
    const guardado = enElStore(sistema.id);
    expect(etiquetas(guardado, 'sales')).toEqual([
      'Clienta',
      'Producto',
      'Cantidad',
      'Precio',
      'Fecha',
      'Fiado',
    ]);
    expect(deTipo(guardado, 'sales').fields[0]).toMatchObject({
      key: 'customer',
      type: 'relation',
    });
    expect(allBlocks(guardado).map((bloque) => bloque.rows.length)).toEqual(filasAntes);
    // Y viaja al servidor como cualquier otro cambio.
    await waitFor(() => {
      const enElServidor = estado.sistemas.get(usuario.email)?.[0];
      expect(etiquetas(enElServidor, 'sales')[0]).toBe('Clienta');
    }, ESPERA);
  });

  it('con el Sami simulado: «poné Cliente primero»; si se vuelve sin guardar, el sistema no cambia', async () => {
    const { router, user, sistema } = await abrirSistema();
    await clickButton(user, /Ajustar con Sami/);
    await verifyHeadingPresentAsync('Lo ajustamos juntos.');

    await pedirASami(user, 'poné Cliente primero');

    expect(await screen.findByText(/puse «Cliente» primero en ventas/, {}, ESPERA)).toBeVisible();
    const borrador = useSistemaStore.getState().taller?.borrador ?? undefined;
    expect(etiquetas(borrador, 'sales')[0]).toBe('Cliente');

    await clickButton(user, /Volver a Almacén de Ana/);

    await verifyPathAsync(router, `/sistemas/${sistema.id}`);
    expect(etiquetas(enElStore(sistema.id), 'sales')[0]).toBe('Producto');
    expect(useSistemaStore.getState().pendientes).not.toContain(sistema.id);
  });

  it('con el Sami simulado: «cambiale el nombre a Teléfono por Celular»', async () => {
    const { user } = await abrirSistema();
    await clickButton(user, /Ajustar con Sami/);
    await verifyHeadingPresentAsync('Lo ajustamos juntos.');

    await pedirASami(user, 'cambiale el nombre a Teléfono por Celular');

    expect(
      await screen.findByText(/«Teléfono» ahora se llama «Celular»/, {}, ESPERA),
    ).toBeVisible();
    const borrador = useSistemaStore.getState().taller?.borrador ?? undefined;
    expect(etiquetas(borrador, 'clients')).toContain('Celular');
  });

  it('deja ver la tabla de una pieza de la propuesta a pantalla completa, como en el sistema', async () => {
    const { user } = await abrirSistema();
    await clickButton(user, /Ajustar con Sami/);
    await verifyHeadingPresentAsync('Lo ajustamos juntos.');
    const clientes = deTipo(useSistemaStore.getState().taller?.borrador ?? undefined, 'clients');

    await clickButton(user, `Ver ${clientes.name} a pantalla completa`);

    const tabla = await screen.findByRole('dialog', { name: clientes.name }, ESPERA);
    expect(tabla).toBeVisible();
    expect(screen.getByLabelText(`Buscar en ${clientes.name}`)).toBeVisible();
    await clickButton(user, /Volver a la propuesta/);
    expect(screen.queryByRole('dialog', { name: clientes.name })).toBeNull();
    expect(useSistemaStore.getState().tablaCompletaId).toBeNull();
  });

  it('un sistema que no existe abre el taller para armar uno nuevo', async () => {
    setupUsuarioConSesion({ nombre: 'Ana Pérez' });

    renderApp('/taller?sistema=no-existe');

    await verifyHeadingPresentAsync('Lo armamos juntos.');
    expect(useSistemaStore.getState().taller).toMatchObject({ sistemaId: null, borrador: null });
    expect(screen.queryByRole('button', { name: /Guardar cambios/ })).toBeNull();
  });
});
