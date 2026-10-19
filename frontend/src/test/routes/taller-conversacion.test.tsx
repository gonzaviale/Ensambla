import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse, type JsonBodyType } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '@/mocks/node';
import { useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  clickButton,
  setupServerError,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyToastAsync,
} from '@/test/testHelpers';

const abrirTaller = async () => {
  setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
  const user = userEvent.setup();
  await clickButton(user, /Armémoslo con Sami/);
  await verifyHeadingPresentAsync('Lo armamos juntos.');
  return user;
};

const conPropuesta = async () => {
  const user = await abrirTaller();
  await clickButton(user, /Almacén o kiosco/);
  await screen.findByLabelText('Nombre de tu sistema', {}, ESPERA);
  return user;
};

const campo = () => screen.getByRole('textbox', { name: /Contame un poco|¿Qué le falta/ });

const escribir = async (user: ReturnType<typeof userEvent.setup>, texto: string) => {
  await user.type(campo(), texto);
  await clickButton(user, /Pensalo conmigo|Ajustalo conmigo/);
};

const botonera = () => screen.findByRole('group', { name: 'Respuestas rápidas' }, ESPERA);

/** Atiende POST /api/sami/taller con `responder` y devuelve los pedidos que llegaron. */
const etiquetasDelInventario = () =>
  (useSistemaStore.getState().taller?.borrador?.sections ?? [])
    .flatMap((seccion) => seccion.blocks)
    .filter((bloque) => bloque.type === 'inventory')
    .flatMap((bloque) => bloque.fields.map((campo) => campo.label));

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

describe('la conversación de Sami en el taller', () => {
  it('con una descripción clara arma la base sin consultar a la IA', async () => {
    const pedidos = espiarElTaller(() => ({ respuesta: 'x', acciones: [], opciones: null }));
    const user = await abrirTaller();

    await escribir(user, 'Tengo un almacén y vendo productos');

    expect(await screen.findByLabelText('Nombre de tu sistema', {}, ESPERA)).toBeVisible();
    expect(screen.getByText(/Te propongo esta base/)).toBeVisible();
    expect(pedidos).toHaveLength(0);
  });

  it('con una descripción vaga Sami pregunta con una botonera que siempre deja escribir otra cosa', async () => {
    const user = await abrirTaller();

    await escribir(user, 'no sé por dónde empezar');

    const opciones = within(await botonera());
    expect(opciones.getByText('¿Qué querés hacer con tu sistema?')).toBeVisible();
    expect(opciones.getByRole('button', { name: /Sumar gastos/ })).toBeVisible();
    expect(opciones.getByRole('button', { name: /Escribir otra cosa/ })).toBeVisible();
    expect(screen.queryByLabelText('Nombre de tu sistema')).toBeNull();
  });

  it('elegir una opción es como escribirla: si las reglas la entienden, arma la propuesta', async () => {
    const user = await abrirTaller();
    await escribir(user, 'no sé por dónde empezar');

    await user.click(within(await botonera()).getByRole('button', { name: /Sumar gastos/ }));

    expect(await screen.findByLabelText('Nombre de tu sistema', {}, ESPERA)).toBeVisible();
    expect(screen.getByText(/Te propongo esta base/)).toBeVisible();
    expect(screen.queryByRole('group', { name: 'Respuestas rápidas' })).toBeNull();
  });

  it('Sami puede armar el sistema desde cero con herramientas cuando la descripción no alcanza', async () => {
    espiarElTaller(() => ({
      respuesta: 'Empecemos por tus ventas y tus clientes.',
      acciones: [
        { herramienta: 'agregar_pieza', argumentos: { tipo: 'sales' } },
        { herramienta: 'agregar_pieza', argumentos: { tipo: 'clients' } },
      ],
      opciones: { pregunta: '¿Llevás stock?', opciones: ['Sí', 'No'] },
    }));
    const user = await abrirTaller();

    await escribir(user, 'no sé por dónde empezar');

    expect(
      await screen.findByText('Empecemos por tus ventas y tus clientes.', {}, ESPERA),
    ).toBeVisible();
    expect(await screen.findByLabelText('Nombre de tu sistema', {}, ESPERA)).toBeVisible();
    expect(screen.getByText('2 piezas conectadas')).toBeVisible();
    expect(within(await botonera()).getByText('¿Llevás stock?')).toBeVisible();
  });

  it('«Escribir otra cosa…» deja la botonera y enfoca el campo de texto', async () => {
    const user = await abrirTaller();
    await escribir(user, 'no sé por dónde empezar');

    await user.click(within(await botonera()).getByRole('button', { name: /Escribir otra cosa/ }));

    expect(screen.queryByRole('group', { name: 'Respuestas rápidas' })).toBeNull();
    expect(campo()).toHaveFocus();
  });

  it('una respuesta rápida corta como «Sí» se manda tal cual', async () => {
    const pedidos = espiarElTaller((pedido) =>
      pedido.mensaje === 'Sí'
        ? { respuesta: 'Perfecto.', acciones: [], opciones: null }
        : {
            respuesta: '¿Sumamos gastos?',
            acciones: [],
            opciones: { pregunta: '¿Sumamos gastos?', opciones: ['Sí', 'No'] },
          },
    );
    const user = await abrirTaller();
    await escribir(user, 'no sé por dónde empezar');

    await user.click(within(await botonera()).getByRole('button', { name: /^Sí/ }));

    expect(await screen.findByText('Perfecto.', {}, ESPERA)).toBeVisible();
    expect(pedidos.map((pedido) => pedido.mensaje)).toEqual(['no sé por dónde empezar', 'Sí']);
  });

  it('un pedido que las reglas no entienden va a la IA con el sistema resumido y la charla reciente', async () => {
    const pedidos = espiarElTaller(() => ({
      respuesta: 'Lo pienso.',
      acciones: [],
      opciones: null,
    }));
    const user = await conPropuesta();

    await escribir(user, '¿por qué me pusiste esas piezas?');

    expect(await screen.findByText('Lo pienso.', {}, ESPERA)).toBeVisible();
    const pedido = pedidos[0] as {
      mensaje: string;
      estado: { nombre: string; piezas: { tipo: string; campos: unknown[] }[] };
      historial: { quien: string }[];
    };
    expect(pedido.mensaje).toBe('¿por qué me pusiste esas piezas?');
    expect(pedido.estado.nombre).toBe('Mi almacén');
    expect(pedido.estado.piezas.map((pieza) => pieza.tipo)).toEqual([
      'inventory',
      'sales',
      'clients',
    ]);
    expect(JSON.stringify(pedido.estado)).not.toContain('Yerba');
    expect(pedido.historial.length).toBeLessThanOrEqual(6);
    expect(pedido.historial.at(-1)?.quien).toBe('sami');
  });

  it('lo que Sami pide con herramientas se aplica a la propuesta y se puede deshacer', async () => {
    const user = await conPropuesta();
    expect(etiquetasDelInventario()).not.toContain('Proveedor');

    await escribir(user, 'agregá un proveedor al inventario');

    expect(
      await screen.findByText('Listo, sumé el proveedor al inventario.', {}, ESPERA),
    ).toBeVisible();
    await waitFor(() => expect(etiquetasDelInventario()).toContain('Proveedor'), ESPERA);
    await clickButton(user, /Deshacer lo último/);

    expect(etiquetasDelInventario()).not.toContain('Proveedor');
    expect(screen.getByText(/Listo, volví atrás/)).toBeVisible();
    expect(screen.queryByRole('button', { name: /Deshacer lo último/ })).toBeNull();
  });

  it('lo que las reglas del taller ya resuelven no consulta a la IA y también se puede deshacer', async () => {
    const pedidos = espiarElTaller(() => ({ respuesta: 'x', acciones: [], opciones: null }));
    const user = await conPropuesta();

    await clickButton(user, 'Sumar gastos');

    expect(await screen.findByText(/Sumé gastos\./, {}, ESPERA)).toBeVisible();
    expect(pedidos).toHaveLength(0);
    await clickButton(user, /Deshacer lo último/);
    expect(screen.queryByText(/4 piezas conectadas/)).toBeNull();
    expect(screen.getByText('3 piezas conectadas')).toBeVisible();
  });

  it('una orden simple escrita a mano también se resuelve sin IA', async () => {
    const pedidos = espiarElTaller(() => ({ respuesta: 'x', acciones: [], opciones: null }));
    const user = await conPropuesta();

    await escribir(user, 'sumá gastos');

    expect(await screen.findByText(/Sumé gastos\./, {}, ESPERA)).toBeVisible();
    expect(pedidos).toHaveLength(0);
  });

  it('un pedido de un dato no se confunde con una pieza: lo interpreta la IA', async () => {
    const pedidos = espiarElTaller(() => ({
      respuesta: 'Listo.',
      acciones: [
        {
          herramienta: 'agregar_campo',
          argumentos: { pieza: 'inventory', etiqueta: 'Proveedor', tipo: 'text' },
        },
      ],
      opciones: null,
    }));
    const user = await conPropuesta();

    await escribir(user, 'agregá un campo de proveedor al inventario');

    expect(await screen.findByText('Listo.', {}, ESPERA)).toBeVisible();
    expect(pedidos).toHaveLength(1);
    expect(screen.getByText('3 piezas conectadas')).toBeVisible();
    expect(etiquetasDelInventario()).toContain('Proveedor');
  });

  it('si Sami contesta que no tiene algo, no cambia la propuesta ni ofrece deshacer', async () => {
    const user = await conPropuesta();

    await escribir(user, 'agregá un sistema de turnos');

    expect(await screen.findByText(/todavía no lo tengo como pieza/, {}, ESPERA)).toBeVisible();
    expect(screen.getByText('3 piezas conectadas')).toBeVisible();
    expect(screen.queryByRole('button', { name: /Deshacer lo último/ })).toBeNull();
  });

  it('si el servidor rechaza el pedido avisa con su mensaje y deja seguir escribiendo', async () => {
    const user = await conPropuesta();
    setupServerError('post', '/sami/taller', 400, 'Hoy ya usaste todas las consultas con IA.');

    await escribir(user, '¿por qué me pusiste esas piezas?');

    await verifyToastAsync(/ya usaste todas las consultas con IA/);
    expect(campo()).toBeEnabled();
    expect(useSistemaStore.getState().taller?.pensando).toBe(false);
  });

  it('Sami puede renombrar el sistema sin pisar el nombre que la persona escribió mientras tanto', async () => {
    espiarElTaller(() => ({
      respuesta: 'Listo, lo llamé Velas de Ana.',
      acciones: [{ herramienta: 'renombrar_sistema', argumentos: { nombre: 'Velas de Ana' } }],
      opciones: null,
    }));
    const user = await conPropuesta();

    await escribir(user, 'ponele un nombre lindo');

    expect(await screen.findByText('Listo, lo llamé Velas de Ana.', {}, ESPERA)).toBeVisible();
    expect(screen.getByLabelText('Nombre de tu sistema')).toHaveValue('Velas de Ana');
  });

  it('una descripción muy corta sigue pidiendo más detalle', async () => {
    const user = await abrirTaller();

    await escribir(user, 'hola');

    await verifyToastAsync(/Contame un poquito más/);
  });
});
