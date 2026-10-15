import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { useRedStore } from '@/stores/red';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion, verifyHeadingPresentAsync } from '@/test/testHelpers';

async function sinViolaciones() {
  const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });
  const resumen = resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`);
  expect(resumen).toEqual([]);
}

async function abrirRed() {
  setupUsuarioConSesion();
  const router = renderApp('/red');
  await verifyHeadingPresentAsync('Mi red');
  return router;
}

const seccion = (titulo: string) => {
  const region = screen.getByRole('heading', { level: 2, name: titulo }).closest('section');
  if (!region) throw new Error(`No hay sección «${titulo}»`);
  return region;
};

describe('Mi red (/red)', () => {
  it('explica cómo funciona la red con las cifras del barrio y la promesa de privacidad', async () => {
    await abrirRed();

    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    expect(
      screen.getByText(/cruza los precios de 31 comercios y 6 proveedores de San Justo/),
    ).toBeVisible();
    expect(screen.getByText('Cada lista que reenviás mejora la red para todos.')).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Tus datos son tuyos' })).toBeVisible();
    expect(screen.getByText(/sólo aparecen cuando hay 5 comercios o más/)).toBeVisible();
    expect(screen.getByRole('link', { name: 'Ver cómo lo ve un mayorista' })).toHaveAttribute(
      'href',
      '/red/mayorista',
    );
  });

  it('muestra las ofertas que sirven (Coca y fideos) y atenúa las que no vendés', async () => {
    await abrirRed();
    const ofertas = seccion('Ofertas que te sirven');

    expect(within(ofertas).getByRole('heading', { name: 'Coca-Cola 2,25 L' })).toBeVisible();
    expect(within(ofertas).getByRole('heading', { name: 'Fideos Matarazzo 500 g' })).toBeVisible();
    expect(within(ofertas).getByText(/8 % menos/)).toBeVisible();
    expect(
      within(ofertas).getByRole('heading', { name: 'Otras ofertas del barrio que no vendés' }),
    ).toBeVisible();
    expect(within(ofertas).getByText(/No te las avisamos porque no las vendés/)).toBeVisible();
    expect(within(ofertas).getByText('Dulce de leche La Serenísima 400 g')).toBeVisible();
    expect(within(ofertas).getByText('Pañales Pampers x30')).toBeVisible();
  });

  it('agregar una oferta al pedido actualiza el pedido con los bultos mínimos', async () => {
    const user = userEvent.setup();
    await abrirRed();

    await user.click(screen.getByRole('button', { name: 'Agregar al pedido de Coca-Cola 2,25 L' }));

    // 2 bultos mínimos × 8 unidades por bulto.
    expect(useRedStore.getState().pedido).toEqual([{ productoId: 'p-coca', cantidad: 16 }]);
    expect(await screen.findByText(/Agregamos 16 unidades de Coca-Cola 2,25 L/)).toBeVisible();
    expect(screen.getByText('En tu pedido: 16 unidades')).toBeVisible();
  });

  it('compara proveedores: mejor alternativa, diferencia y ahorro por mes', async () => {
    const user = userEvent.setup();
    await abrirRed();
    const seccionComparar = seccion('Comparar proveedores');

    expect(within(seccionComparar).getByText(/Podrías ahorrar/)).toBeVisible();
    const azucar = within(seccionComparar)
      .getByRole('heading', { name: 'Azúcar Ledesma 1 kg' })
      .closest('li') as HTMLElement;
    expect(within(azucar).getByText('Mayorista Norte', { selector: 'strong' })).toBeVisible();
    expect(within(azucar).getByText(/menos \(11,8 %\)/)).toBeVisible();

    await user.click(within(azucar).getByRole('button', { name: /Pedir a Mayorista Norte/ }));

    expect(useRedStore.getState().pedido.map((item) => item.productoId)).toContain('p-azucar');
  });

  it('«¿Comprás caro?» muestra la harina con su diferencia y oculta el número del arroz', async () => {
    await abrirRed();
    const caro = seccion('¿Comprás caro?');

    const harina = within(caro)
      .getByRole('heading', { name: 'Harina 000 Pureza 1 kg' })
      .closest('section') as HTMLElement;
    expect(within(harina).getByText('Pagás 14 % más')).toBeVisible();
    expect(within(harina).getByText(/Promedio de 9 comercios de la zona/)).toBeVisible();

    const arroz = within(caro)
      .getByRole('heading', { name: 'Arroz Gallo Oro 1 kg' })
      .closest('section') as HTMLElement;
    expect(
      within(arroz).getByText(/Todavía no hay suficientes comercios para comparar \(3 de 5\)/),
    ).toBeVisible();
    expect(arroz.textContent).not.toMatch(/1\.290/);
    expect(arroz.textContent).not.toMatch(/%/);
  });

  it('el catálogo maestro junta los nombres con los que escriben cada producto', async () => {
    await abrirRed();
    const catalogo = seccion('Un mismo producto, muchos nombres');

    expect(within(catalogo).getByText(/sirve para todos los comercios/)).toBeVisible();
    expect(within(catalogo).getByText('ACEITE NATURA GIRASOL 900 ML X12')).toBeVisible();
  });

  it('con la prueba vencida, las secciones de pago muestran candado', async () => {
    useRedStore.getState().simularFinDePrueba();
    await abrirRed();

    expect(
      (await screen.findAllByRole('link', { name: 'Ver planes' })).length,
    ).toBeGreaterThanOrEqual(3);
    expect(
      screen.getByRole('heading', { name: 'Ofertas de proveedores de lo que vendés' }),
    ).toBeVisible();
    expect(
      screen.getByRole('heading', { name: 'Comparar el costo entre proveedores' }),
    ).toBeVisible();
    expect(
      screen.getByRole('heading', { name: 'Saber si comprás caro frente a tu zona' }),
    ).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Ofertas que te sirven' })).toBeNull();
    // Lo gratuito sigue a la vista.
    expect(screen.getByRole('heading', { name: 'Cómo funciona la red' })).toBeVisible();
  });

  it('no tiene violaciones de accesibilidad', async () => {
    await abrirRed();
    await screen.findByRole('heading', { name: 'Ofertas que te sirven' }, ESPERA);

    await sinViolaciones();
  });
});

describe('Vista del mayorista (/red/mayorista)', () => {
  async function abrirMayorista() {
    setupUsuarioConSesion();
    renderApp('/red/mayorista');
    await verifyHeadingPresentAsync('Vista del mayorista');
  }

  it('avisa que es una demostración y muestra la regla de neutralidad', async () => {
    await abrirMayorista();

    expect(screen.getByText('Vista de demostración: así ve Ensambla un mayorista')).toBeVisible();
    expect(screen.getByText(/Tu lista llega a/)).toHaveTextContent(
      'Tu lista llega a 18 de los 31 comercios de la red.',
    );
    expect(screen.getByText(/nunca por esconder información/)).toBeVisible();
    expect(screen.getByText(/Nunca vemos qué compra cada comercio/)).toBeVisible();
  });

  it('simular que sube una lista la deja para revisar en el comercio', async () => {
    const user = userEvent.setup();
    await abrirMayorista();
    const antes = useRedStore.getState().listas.length;

    await user.click(screen.getByRole('button', { name: 'Simular que subo una lista' }));

    expect(await screen.findByText(/Los comercios que te compran ya la tienen/)).toBeVisible();
    expect(useRedStore.getState().listas).toHaveLength(antes + 1);
    expect(screen.getByRole('link', { name: 'Ver cómo la recibe el comercio' })).toHaveAttribute(
      'href',
      '/listas',
    );
  });

  it('publicar una oferta de azúcar la deja en las ofertas y no deja que sea mayor al costo', async () => {
    const user = userEvent.setup();
    await abrirMayorista();
    const antes = useRedStore.getState().ofertas.length;

    await user.selectOptions(screen.getByLabelText('Producto'), 'Azúcar Ledesma 1 kg');
    expect(screen.getByText(/\(≈ 11 de 31\)/)).toBeVisible();

    // Mayor al costo habitual ($ 1.020): se rechaza.
    await user.type(screen.getByLabelText(/Costo de la oferta/), '1100');
    await user.click(screen.getByRole('button', { name: 'Publicar oferta' }));
    expect(
      await screen.findByText(/La oferta tiene que ser menor al costo habitual/),
    ).toBeVisible();
    expect(useRedStore.getState().ofertas).toHaveLength(antes);

    // Menor: se publica y se ve el descuento en la vista previa.
    await user.clear(screen.getByLabelText(/Costo de la oferta/));
    await user.type(screen.getByLabelText(/Costo de la oferta/), '900');
    expect(screen.getByText(/11,8 %/)).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Publicar oferta' }));

    expect(await screen.findByText(/Publicada. Mirá cómo le llega al comercio/)).toBeVisible();
    const publicada = useRedStore.getState().ofertas[0];
    expect(publicada).toMatchObject({
      proveedorId: 'prov-sur',
      productoId: 'p-azucar',
      costoHabitual: 1020,
      costoOferta: 900,
      minimoBultos: 1,
    });
    expect(useRedStore.getState().ofertas).toHaveLength(antes + 1);
    expect(screen.getByRole('link', { name: 'Ver el aviso en Hoy' })).toHaveAttribute(
      'href',
      '/hoy',
    );
  });

  it('no tiene violaciones de accesibilidad', async () => {
    await abrirMayorista();

    await sinViolaciones();
  });
});
