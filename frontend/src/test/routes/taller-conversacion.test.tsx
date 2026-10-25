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

/** Con el ejemplo del almacén: el mock de Sami arma productos, ventas y clientes. */
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

const borrador = () => useSistemaStore.getState().taller?.borrador ?? null;
const piezasDelBorrador = () => (borrador()?.sections ?? []).flatMap((seccion) => seccion.blocks);
const pieza = (nombre: string) => {
  const encontrada = piezasDelBorrador().find((bloque) => bloque.name === nombre);
  if (!encontrada) throw new Error(`Falta la pieza ${nombre}`);
  return encontrada;
};

const etiquetasDelInventario = () =>
  piezasDelBorrador()
    .filter((bloque) => bloque.type === 'inventory')
    .flatMap((bloque) => bloque.fields.map((campo) => campo.label));

/** Atiende POST /api/sami/taller con `responder` y devuelve los pedidos que llegaron. */
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

type PedidoAlTaller = {
  mensaje: string;
  estado: { nombre: string; piezas: { tipo: string; nombre: string; campos: unknown[] }[] };
  historial: { quien: string; texto: string }[];
};

describe('la conversación de Sami en el taller', () => {
  it('el primer mensaje va a Sami con un sistema vacío y lo que ensambla se ve a la derecha', async () => {
    const pedidos = espiarElTaller(() => ({
      respuesta: 'Te armé las ventas y los clientes.',
      acciones: [
        { herramienta: 'agregar_pieza', argumentos: { tipo: 'clients', nombre: 'Clientas' } },
        {
          herramienta: 'agregar_pieza',
          argumentos: {
            tipo: 'form',
            nombre: 'Encargos',
            campos: [{ etiqueta: 'Clienta', tipo: 'text' }],
          },
        },
        {
          herramienta: 'conectar',
          argumentos: { desde: 'Encargos', hacia: 'Clientas', etiqueta: 'Clienta' },
        },
      ],
      opciones: null,
    }));
    const user = await abrirTaller();

    await escribir(user, 'Tengo un almacén y vendo productos');

    expect(await screen.findByText('Te armé las ventas y los clientes.', {}, ESPERA)).toBeVisible();
    expect(screen.getByLabelText('Nombre de tu sistema')).toHaveValue('Mi sistema');
    expect(screen.getByText('2 piezas conectadas')).toBeVisible();
    const [pedido] = pedidos as PedidoAlTaller[];
    expect(pedido?.mensaje).toBe('Tengo un almacén y vendo productos');
    expect(pedido?.estado).toMatchObject({ nombre: 'Mi sistema', piezas: [] });
    expect(pedido?.historial.map((mensaje) => mensaje.quien)).toEqual(['sami']);
    expect(pieza('Encargos').fields.find((dato) => dato.label === 'Clienta')).toMatchObject({
      type: 'relation',
      target: pieza('Clientas').id,
    });
    // Lo primero que arma Sami no se puede «deshacer»: no había nada antes.
    expect(screen.queryByRole('button', { name: /Deshacer lo último/ })).toBeNull();
  });

  it('con una descripción vaga Sami pregunta el rubro con una botonera que siempre deja escribir otra cosa', async () => {
    const user = await abrirTaller();

    await escribir(user, 'no sé por dónde empezar');

    const opciones = within(await botonera());
    expect(opciones.getByText('¿De qué es tu negocio?')).toBeVisible();
    expect(opciones.getByRole('button', { name: /Hago velas artesanales/ })).toBeVisible();
    expect(opciones.getByRole('button', { name: /Escribir otra cosa/ })).toBeVisible();
    expect(screen.queryByLabelText('Nombre de tu sistema')).toBeNull();
    expect(borrador()).toBeNull();
  });

  it('elegir una opción es como escribirla: Sami ensambla las piezas de ese negocio', async () => {
    const user = await abrirTaller();
    await escribir(user, 'no sé por dónde empezar');

    await user.click(
      within(await botonera()).getByRole('button', { name: /Hago velas artesanales/ }),
    );

    expect(await screen.findByLabelText('Nombre de tu sistema', {}, ESPERA)).toHaveValue(
      'Mis velas',
    );
    expect(screen.getByText(/Armé tus velas/)).toBeVisible();
    expect(piezasDelBorrador().map((bloque) => bloque.name)).toEqual([
      'Velas',
      'Clientas',
      'Pedidos',
    ]);
    expect(within(await botonera()).getByText(/gastás en cera, mechas y esencias/)).toBeVisible();
  });

  it('con el ejemplo de las velas, los pedidos quedan conectados con las velas y sus clientas', async () => {
    const user = await abrirTaller();

    await clickButton(user, /Velas artesanales/);

    await screen.findByLabelText('Nombre de tu sistema', {}, ESPERA);
    const velas = pieza('Velas');
    expect(velas.fields.map((dato) => dato.label)).toContain('Aroma');
    expect(velas.rows.map((fila) => fila.name)).toContain('Vela de soja lavanda');
    const pedidos = pieza('Pedidos');
    expect(pedidos.fields.find((dato) => dato.label === 'Vela')).toMatchObject({
      type: 'relation',
      target: velas.id,
    });
    const lavanda = velas.rows.find((fila) => fila.name === 'Vela de soja lavanda');
    const carla = pieza('Clientas').rows.find((fila) => fila.name === 'Carla Gómez');
    expect(pedidos.rows[0]).toMatchObject({ vela: lavanda?.id, customer: carla?.id });
  });

  it('con el ejemplo de la peluquería, Sami suma una tabla de turnos a medida', async () => {
    const user = await abrirTaller();

    await clickButton(user, /Una peluquería/);

    await screen.findByLabelText('Nombre de tu sistema', {}, ESPERA);
    const turnos = pieza('Turnos');
    expect(turnos.type).toBe('form');
    expect(turnos.fields.map((dato) => dato.label)).toEqual([
      'Nombre',
      'Detalle',
      'Fecha',
      'Cliente',
      'Servicio',
      'Hora',
    ]);
    const sofia = pieza('Clientes').rows.find((fila) => fila.name === 'Sofía Ruiz');
    expect(turnos.rows[0]).toMatchObject({ cliente: sofia?.id, hora: '10:30' });
    expect(screen.getByText('3 piezas conectadas')).toBeVisible();
  });

  it('Sami puede armar el sistema desde cero con herramientas y dejar una pregunta', async () => {
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

  it('si Sami armó algo sin decir nada, la charla cuenta lo que hizo', async () => {
    espiarElTaller(() => ({
      respuesta: '',
      acciones: [{ herramienta: 'agregar_pieza', argumentos: { tipo: 'form', nombre: 'Turnos' } }],
      opciones: null,
    }));
    const user = await abrirTaller();

    await escribir(user, 'Doy turnos para cortes');

    expect(
      await screen.findByText(
        'Sumé turnos. Ya podés verlo a la derecha. ¿Qué cambiarías?',
        {},
        ESPERA,
      ),
    ).toBeVisible();
  });

  it('si Sami no armó nada ni dijo nada, lo cuenta y sigue sin propuesta', async () => {
    espiarElTaller(() => ({
      respuesta: '',
      acciones: [{ herramienta: 'agregar_pieza', argumentos: { tipo: 'turnos' } }],
      opciones: null,
    }));
    const user = await abrirTaller();

    await escribir(user, 'Doy turnos para cortes');

    expect(await screen.findByText(/No pude hacer ese cambio/, {}, ESPERA)).toBeVisible();
    expect(borrador()).toBeNull();
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

  it('al elegir una respuesta rápida, la pregunta de la botonera viaja en la charla aunque Sami no la haya escrito', async () => {
    const pedidos = espiarElTaller((pedido) =>
      pedido.mensaje === 'Sólo servicios'
        ? { respuesta: 'Listo.', acciones: [], opciones: null }
        : {
            respuesta: 'Te armé turnos y servicios.',
            acciones: [],
            opciones: {
              pregunta: '¿Vendés productos o sólo servicios?',
              opciones: ['Productos', 'Sólo servicios'],
            },
          },
    );
    const user = await abrirTaller();
    await escribir(user, 'tengo una peluquería');

    await user.click(within(await botonera()).getByRole('button', { name: /^Sólo servicios/ }));

    expect(await screen.findByText('Listo.', {}, ESPERA)).toBeVisible();
    const segundo = pedidos[1] as PedidoAlTaller;
    expect(segundo.historial.slice(-2)).toEqual([
      { quien: 'sami', texto: 'Te armé turnos y servicios.' },
      { quien: 'sami', texto: '¿Vendés productos o sólo servicios?' },
    ]);
  });

  it('cada ajuste va a Sami con el sistema resumido y la charla reciente', async () => {
    const user = await conPropuesta();
    const pedidos = espiarElTaller(() => ({
      respuesta: 'Lo pienso.',
      acciones: [],
      opciones: null,
    }));

    await escribir(user, '¿por qué me pusiste esas piezas?');

    expect(await screen.findByText('Lo pienso.', {}, ESPERA)).toBeVisible();
    const pedido = pedidos[0] as PedidoAlTaller;
    expect(pedido.mensaje).toBe('¿por qué me pusiste esas piezas?');
    expect(pedido.estado.nombre).toBe('Mi almacén');
    expect(pedido.estado.piezas.map((item) => item.tipo)).toEqual([
      'inventory',
      'sales',
      'clients',
    ]);
    // Sami ve las primeras filas (id y primeros datos) para poder cambiar o quitar un registro, pero no todo el sistema.
    const inventario = pedido.estado.piezas.find((item) => item.tipo === 'inventory') as unknown as {
      filas: { valores: string[] }[];
    };
    expect(inventario.filas.map((fila) => fila.valores[0])).toContain('Yerba Playadito 1 kg');
    expect(JSON.stringify(pedido.estado)).not.toContain('Correo electrónico');
    expect(pedido.historial.length).toBeLessThanOrEqual(6);
    expect(pedido.historial.at(-1)?.quien).toBe('sami');
  });

  it('una orden simple como «sumá gastos» también la resuelve Sami', async () => {
    const user = await conPropuesta();
    const pedidos = espiarElTaller(() => ({
      respuesta: 'Listo, sumé los gastos.',
      acciones: [{ herramienta: 'agregar_pieza', argumentos: { tipo: 'expenses' } }],
      opciones: null,
    }));

    await escribir(user, 'sumá gastos');

    expect(await screen.findByText('Listo, sumé los gastos.', {}, ESPERA)).toBeVisible();
    expect(pedidos.map((pedido) => pedido.mensaje)).toEqual(['sumá gastos']);
    expect(screen.getByText('4 piezas conectadas')).toBeVisible();
  });

  it('sacar una pieza y renombrar el sistema también se le piden a Sami', async () => {
    const user = await conPropuesta();

    await escribir(user, 'sacá los clientes');
    expect(await screen.findByText('Listo, saqué clientes.', {}, ESPERA)).toBeVisible();
    expect(screen.getByText('2 piezas conectadas')).toBeVisible();

    await escribir(user, 'llamalo Almacén Don Luis');
    expect(
      await screen.findByText('Listo, ahora se llama Almacén Don Luis.', {}, ESPERA),
    ).toBeVisible();
    expect(screen.getByLabelText('Nombre de tu sistema')).toHaveValue('Almacén Don Luis');
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

  it('si Sami sólo conversa, no cambia la propuesta ni ofrece deshacer', async () => {
    const user = await conPropuesta();

    await escribir(user, 'gracias, Sami');

    expect(await screen.findByText(/Cuando quieras seguimos/, {}, ESPERA)).toBeVisible();
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

  it('si el primer mensaje falla, avisa y sigue sin propuesta', async () => {
    setupServerError('post', '/sami/taller', 400, 'Hoy ya usaste todas las consultas con IA.');
    const user = await abrirTaller();

    await escribir(user, 'Tengo un almacén y vendo productos');

    await verifyToastAsync(/ya usaste todas las consultas con IA/);
    expect(borrador()).toBeNull();
    expect(screen.getByText('O probá con un ejemplo:')).toBeVisible();
  });

  it('Sami puede renombrar el sistema sin pisar el nombre que la persona escribió mientras tanto', async () => {
    const user = await conPropuesta();
    espiarElTaller(() => ({
      respuesta: 'Listo, lo llamé Velas de Ana.',
      acciones: [{ herramienta: 'renombrar_sistema', argumentos: { nombre: 'Velas de Ana' } }],
      opciones: null,
    }));

    await escribir(user, 'ponele un nombre lindo');

    expect(await screen.findByText('Listo, lo llamé Velas de Ana.', {}, ESPERA)).toBeVisible();
    expect(screen.getByLabelText('Nombre de tu sistema')).toHaveValue('Velas de Ana');
  });

  it('un mensaje de una sola letra sigue pidiendo más detalle', async () => {
    const user = await abrirTaller();

    await escribir(user, 'a');

    await verifyToastAsync(/Contame un poquito más/);
  });
});
