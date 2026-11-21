import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import * as sonidos from '@/api/sonidosEscaneo';
import { allBlocks, makeTemplate } from '@/lib/sistema/core';
import { prepararCaja } from '@/lib/caja';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';
import type { SolicitudCobro } from '@/types/caja';
import {
  cuandoAgregaDosGaseosas,
  cuandoRestaUnaGaseosa,
  cuandoQuitaLaGaseosa,
  cuandoDeshaceElCambio,
  entoncesCantidadDeGaseosa,
  entoncesGaseosaRetirada,
} from '@/test/caja/cantidadesCajaHelpers';
import {
  dadoLaCajaConUnFavoritoYUnoQueNoLoEs,
  cuandoBuscaEnElStock,
  entoncesElFavoritoSeMarcaComoElegido,
  entoncesElQueNoEsFavoritoSePuedeAgregar,
  entoncesNingunaTarjetaMuestraElIconoDeAgregar,
} from '@/test/caja/favoritosCajaHelpers';

async function abrir(escanear = false) {
  const usuario = setupUsuarioConSesion();
  const sistema = makeTemplate('shop', {}, false);
  const inventario = allBlocks(sistema).find((b) => b.type === 'inventory');
  if (!inventario) throw new Error('Falta la sección de stock');
  inventario.rows = [
    {
      id: crypto.randomUUID(),
      name: 'Coca-Cola',
      barcode: '7790895000997',
      quantity: 10,
      price: 2700,
      favorite: true,
    },
    {
      id: crypto.randomUUID(),
      name: 'Pan francés',
      quantity: 2,
      price: 4000,
      saleMode: 'peso',
      favorite: true,
    },
  ];
  prepararCaja(sistema, inventario.id);
  estado.sistemas.set(usuario.email, [sistema]);
  renderApp('/caja');
  await screen.findByRole('heading', { name: 'Caja', level: 1 }, ESPERA);
  const user = userEvent.setup();
  if (!escanear) await user.click(screen.getByRole('tab', { name: 'Favoritos' }));
  return { user, sistema, inventario };
}

function comprobante(request: SolicitudCobro) {
  return {
    id: request.id,
    lineas: request.lineas.map((l) => ({
      ...l,
      nombre: 'Producto',
      importe: l.cantidad * l.precio,
    })),
    medio: request.medio,
    total: request.lineas.reduce((sum, l) => sum + l.cantidad * l.precio, 0),
    recibido: request.recibido ?? 2700,
    vuelto: 0,
    creadoEn: new Date().toISOString(),
  };
}

describe('caja integrada con el cliente HTTP', () => {
  it('permite buscar, revisar y consultar actividad sin perder la compra', async () => {
    const vacio = {
      ventas: [],
      total: 0,
      efectivo: 0,
      transferencia: 0,
      tarjeta: 0,
      desde: null,
      revision: 'vacia',
    };
    server.use(
      http.get('/api/caja/resumen', () =>
        HttpResponse.json({
          fecha: '2026-11-14',
          dia: vacio,
          abierta: vacio,
          cierres: [],
          pagina: 0,
          hayMas: false,
        }),
      ),
    );
    const { user } = await abrir(true);
    expect(screen.queryByLabelText('Código de barras')).not.toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'Escanear' }));
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('tab', { name: 'Buscar' })).toHaveAttribute('aria-selected', 'true');
    await user.type(screen.getByLabelText('Buscar producto en tu stock'), 'Coca');
    await user.click(screen.getByRole('button', { name: /Coca-Cola.*2.700/ }));
    await user.click(screen.getByRole('button', { name: 'Revisar compra' }));
    expect(screen.getByRole('button', { name: 'Continuar al cobro' })).toBeEnabled();
    await user.click(screen.getByRole('button', { name: 'Actividad' }));
    await user.click(screen.getByRole('button', { name: 'Vender' }));
    expect(screen.getByLabelText('Cantidad de Coca-Cola')).toHaveValue('1');
    await user.click(screen.getByRole('button', { name: 'Seguir agregando' }));
    expect(screen.getByRole('tab', { name: 'Buscar' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByLabelText('Buscar producto en tu stock')).toHaveValue('Coca');
  });
  it('emite un solo aviso por lectura aunque agregue varias unidades y distingue códigos desconocidos', async () => {
    const sonar = vi.spyOn(sonidos, 'sonarEscaneo');
    try {
      const { user } = await abrir(true);
      await user.click(screen.getByRole('button', { name: '×2' }));
      await user.click(screen.getByRole('button', { name: 'Escribir código · lector USB' }));
      await user.type(screen.getByLabelText('Código de barras'), '7790895000997{Enter}');
      expect(screen.getByLabelText('Cantidad de Coca-Cola')).toHaveValue('2');
      await user.type(screen.getByLabelText('Código de barras'), '77903754{Enter}');
      await screen.findByRole('dialog');
      expect(sonar.mock.calls).toEqual([['reconocido'], ['atencion']]);
    } finally {
      sonar.mockRestore();
    }
  });
  it('protege un artículo sin guardar con un diálogo propio y conserva los datos al seguir editando', async () => {
    const { user } = await abrir();
    await user.click(screen.getByRole('button', { name: 'Agregar artículo sin código' }));
    await user.type(screen.getByLabelText('Nombre del artículo'), 'Yerba de prueba');
    await user.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(
      await screen.findByRole('alertdialog', { name: 'Hay cambios sin guardar' }),
    ).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Seguir editando' }));
    expect(screen.getByLabelText('Nombre del artículo')).toHaveValue('Yerba de prueba');
    await user.keyboard('{Escape}');
    await user.click(await screen.findByRole('button', { name: 'Descartar y continuar' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
  });
  it('consume los atajos una sola vez, permite desmarcarlos y conserva las unidades de un código nuevo', async () => {
    const { user } = await abrir(true);
    expect(screen.queryByLabelText('Cantidad para la próxima lectura')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '×1' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '×3' }));
    await user.click(screen.getByRole('button', { name: '×3' }));
    expect(screen.getByRole('button', { name: '×3' })).toHaveAttribute('aria-pressed', 'false');
    await user.click(screen.getByRole('button', { name: '×5' }));
    await user.click(screen.getByRole('button', { name: 'Escribir código · lector USB' }));
    await user.type(screen.getByLabelText('Código de barras'), '7790895000997{Enter}');
    expect(screen.getByLabelText('Cantidad de Coca-Cola')).toHaveValue('5');
    await user.type(screen.getByLabelText('Código de barras'), '7790895000997{Enter}');
    expect(screen.getByLabelText('Cantidad de Coca-Cola')).toHaveValue('6');
    await user.click(screen.getByRole('button', { name: 'Listo' }));
    await user.click(screen.getByRole('button', { name: '×3' }));
    await user.click(screen.getByRole('button', { name: 'Escribir código · lector USB' }));
    await user.type(screen.getByLabelText('Código de barras'), '77903754{Enter}');
    const dialogo = await screen.findByRole('dialog', { name: 'Agregar artículo' });
    expect(within(dialogo).getByLabelText('Cantidad')).toHaveValue('3');
    await user.type(within(dialogo).getByLabelText('Nombre del artículo'), 'Alfajor');
    await user.type(within(dialogo).getByLabelText('Precio por unidad ($)'), '850');
    await user.click(within(dialogo).getByRole('button', { name: 'Agregar a la compra' }));
    expect(screen.getByLabelText('Cantidad de Alfajor')).toHaveValue('3');
    await screen.findByRole('dialog', { name: 'Escribir código' });
    await user.type(screen.getByLabelText('Código de barras'), '7790895000997{Enter}');
    expect(screen.getByLabelText('Cantidad de Coca-Cola')).toHaveValue('7');
  });
  it('permite corregir las unidades y recuperar el artículo quitado', async () => {
    const { user } = await abrir();
    await cuandoAgregaDosGaseosas(user);
    entoncesCantidadDeGaseosa('2');
    await cuandoRestaUnaGaseosa(user);
    entoncesCantidadDeGaseosa('1');
    await cuandoQuitaLaGaseosa(user);
    entoncesGaseosaRetirada();
    await cuandoDeshaceElCambio(user);
    entoncesCantidadDeGaseosa('1');
  });

  it('toma gramos para un favorito por kilo y permite corregir el peso final', async () => {
    const { user } = await abrir();
    await user.click(screen.getByRole('button', { name: /Pan francés.*4.000/ }));
    const dialogo = screen.getByRole('dialog');
    await user.clear(within(dialogo).getByLabelText('Gramos'));
    await user.type(within(dialogo).getByLabelText('Gramos'), '500');
    await user.click(within(dialogo).getByRole('button', { name: 'Agregar a la compra' }));
    const gramos = screen.getByLabelText('Cantidad de Pan francés en gramos');
    expect(gramos).toHaveValue('500');
    await user.clear(gramos);
    await user.type(gramos, '550');
    await user.tab();
    expect(screen.getByRole('button', { name: /Cobrar.*2.200/ })).toBeEnabled();
  });

  it('un código desconocido permite un alta rápida y continuar la venta', async () => {
    const { user } = await abrir(true);
    await user.click(screen.getByRole('button', { name: 'Escribir código · lector USB' }));
    await user.type(screen.getByLabelText('Código de barras'), '77903754{Enter}');
    const dialogo = await screen.findByRole('dialog');
    expect(within(dialogo).getByText(/Artículo no reconocido/)).toBeVisible();
    await user.type(within(dialogo).getByLabelText('Nombre del artículo'), 'Alfajor');
    await user.type(within(dialogo).getByLabelText('Precio por unidad ($)'), '850');
    await user.click(within(dialogo).getByRole('button', { name: 'Agregar a la compra' }));
    await screen.findByRole('dialog', { name: 'Escribir código' });
    expect(screen.queryByRole('dialog', { name: 'Agregar artículo' })).not.toBeInTheDocument();
    expect(screen.getByLabelText('Cantidad de Alfajor')).toHaveValue('1');
    expect(screen.getByLabelText('Código de barras')).toHaveValue('');
  });

  it('usa la API de cobro y muestra éxito después de recibir confirmación', async () => {
    const solicitudes: SolicitudCobro[] = [];
    server.use(
      http.post('/api/caja/ventas', async ({ request }) => {
        const body = (await request.json()) as SolicitudCobro;
        solicitudes.push(body);
        return HttpResponse.json(comprobante(body));
      }),
    );
    const { user } = await abrir();
    await user.click(screen.getByRole('button', { name: /Coca-Cola.*2.700/ }));
    await user.click(screen.getByRole('button', { name: /Cobrar.*2.700/ }));
    await user.click(screen.getByRole('button', { name: 'Confirmar cobro' }));
    await screen.findByRole('heading', { name: 'Venta guardada' }, ESPERA);
    expect(solicitudes).toHaveLength(1);
    expect(solicitudes[0]?.lineas[0]).toMatchObject({ cantidad: 1, precio: 2700, modo: 'UNIDAD' });
  });

  it('un resultado incierto se verifica con el mismo id sin crear otro cobro', async () => {
    const solicitudes: SolicitudCobro[] = [];
    server.use(
      http.post('/api/caja/ventas', async ({ request }) => {
        const body = (await request.json()) as SolicitudCobro;
        solicitudes.push(body);
        return solicitudes.length === 1
          ? HttpResponse.error()
          : HttpResponse.json(comprobante(body));
      }),
    );
    const { user } = await abrir();
    await user.click(screen.getByRole('button', { name: /Coca-Cola.*2.700/ }));
    await user.click(screen.getByRole('button', { name: /Cobrar.*2.700/ }));
    await user.click(screen.getByRole('button', { name: 'Confirmar cobro' }));
    await user.click(
      await screen.findByRole('button', { name: 'Verificar misma operación' }, ESPERA),
    );
    await screen.findByRole('heading', { name: 'Venta guardada' }, ESPERA);
    expect(solicitudes).toHaveLength(2);
    expect(solicitudes[1]?.id).toBe(solicitudes[0]?.id);
  });
});

describe('favoritos de la caja', () => {
  it('marca con una estrella llena al producto que ya es favorito', async () => {
    const user = await dadoLaCajaConUnFavoritoYUnoQueNoLoEs();
    await cuandoBuscaEnElStock(user);
    entoncesElFavoritoSeMarcaComoElegido('Coca-Cola');
  });

  it('ofrece agregar a favoritos al producto que todavía no lo es', async () => {
    const user = await dadoLaCajaConUnFavoritoYUnoQueNoLoEs();
    await cuandoBuscaEnElStock(user);
    entoncesElQueNoEsFavoritoSePuedeAgregar('Yerba');
  });

  it('ya no muestra un «+» en las tarjetas de producto', async () => {
    const user = await dadoLaCajaConUnFavoritoYUnoQueNoLoEs();
    await cuandoBuscaEnElStock(user);
    entoncesNingunaTarjetaMuestraElIconoDeAgregar();
  });
});
