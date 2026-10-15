import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { useRedStore } from '@/stores/red';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion, verifyHeadingPresentAsync } from '@/test/testHelpers';

async function abrirPrecios() {
  setupUsuarioConSesion();
  renderApp('/precios');
  await verifyHeadingPresentAsync('Mis precios');
  await screen.findByRole('table', {}, ESPERA);
}

const filaDe = (nombre: string) =>
  screen.getByRole('button', { name: nombre }).closest('tr') as HTMLElement;

async function abrirDetalle(user: ReturnType<typeof userEvent.setup>, nombre: string) {
  await user.click(screen.getByRole('button', { name: nombre }));
  return screen.findByRole('dialog', { name: nombre }, ESPERA);
}

async function sinViolaciones() {
  const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });
  const resumen = resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`);
  expect(resumen).toEqual([]);
}

describe('Mis precios', () => {
  it('lista los 17 productos y marca el aceite como en riesgo', async () => {
    await abrirPrecios();

    // Un renglón de encabezado + 17 productos.
    expect(screen.getAllByRole('row')).toHaveLength(18);
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    expect(within(filaDe('Aceite Natura 900 ml')).getByText(/En riesgo/)).toBeInTheDocument();
    expect(within(filaDe('Coca-Cola 2,25 L')).queryByText(/En riesgo/)).not.toBeInTheDocument();
    // Orden por ganancia ascendente: los de ganancia más baja primero.
    const primeras = screen.getAllByRole('row').slice(1, 3);
    expect(primeras.map((fila) => within(fila).getAllByRole('button')[0]?.textContent)).toEqual([
      'Aceite Natura 900 ml',
      'Yogur Ser 190 g',
    ]);
  });

  it('muestra el resumen con los productos y los que tienen ganancia baja', async () => {
    await abrirPrecios();

    const resumen = screen.getByRole('region', { name: 'Resumen de tus precios' });
    expect(within(resumen).getByText('17')).toBeInTheDocument();
    expect(within(resumen).getByText(/Con ganancia por debajo del mínimo/)).toBeInTheDocument();
    expect(within(resumen).getByText('Revisalos')).toBeInTheDocument();
    expect(within(resumen).getByText('Ganancia promedio')).toBeInTheDocument();
  });

  it('«Sólo con ganancia baja» deja sólo los productos con margen bajo', async () => {
    const user = userEvent.setup();
    await abrirPrecios();

    await user.click(screen.getByRole('checkbox', { name: 'Sólo con ganancia baja' }));

    await waitFor(() => expect(screen.getAllByRole('row')).toHaveLength(3));
    expect(screen.getByRole('button', { name: 'Aceite Natura 900 ml' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Yogur Ser 190 g' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Coca-Cola 2,25 L' })).not.toBeInTheDocument();
  });

  it('avisa cuando la búsqueda no encuentra nada y permite quitar los filtros', async () => {
    const user = userEvent.setup();
    await abrirPrecios();

    await user.type(screen.getByLabelText('Buscar un producto'), 'zzzz');

    expect(
      await screen.findByRole(
        'heading',
        { name: 'No encontramos productos con esos filtros' },
        ESPERA,
      ),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Quitar los filtros' }));
    await waitFor(() => expect(screen.getAllByRole('row')).toHaveLength(18));
  });

  it('abre el detalle del aceite y guardar el precio sugerido cambia el precio en la tabla', async () => {
    const user = userEvent.setup();
    await abrirPrecios();
    expect(within(filaDe('Aceite Natura 900 ml')).getByText(/2\.350/)).toBeInTheDocument();

    const detalle = await abrirDetalle(user, 'Aceite Natura 900 ml');
    expect(within(detalle).getByText(/Con tus reglas:/)).toHaveTextContent(/2\.860/);
    await user.click(within(detalle).getByRole('button', { name: 'Usar este precio' }));
    expect(within(detalle).getByLabelText('Precio nuevo')).toHaveValue('2860');
    await user.click(within(detalle).getByRole('button', { name: 'Guardar precio' }));

    expect(
      await screen.findByText('Listo. Falta cambiar la etiqueta en el mostrador', {}, ESPERA),
    ).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    const fila = filaDe('Aceite Natura 900 ml');
    expect(within(fila).getByText(/2\.860/)).toBeInTheDocument();
    expect(within(fila).queryByText(/En riesgo/)).not.toBeInTheDocument();
    expect(useRedStore.getState().productos.find((p) => p.id === 'p-aceite')?.precio).toBe(2860);
  });

  it('no deja guardar un precio en cero', async () => {
    const user = userEvent.setup();
    await abrirPrecios();
    const detalle = await abrirDetalle(user, 'Aceite Natura 900 ml');

    const campo = within(detalle).getByLabelText('Precio nuevo');
    await user.clear(campo);
    await user.type(campo, '0');
    await user.click(within(detalle).getByRole('button', { name: 'Guardar precio' }));

    expect(within(detalle).getByText('Escribí un precio mayor a cero.')).toBeInTheDocument();
    expect(useRedStore.getState().productos.find((p) => p.id === 'p-aceite')?.precio).toBe(2350);
  });

  it('el historial de costos muestra de qué lista salió cada costo', async () => {
    const user = userEvent.setup();
    await abrirPrecios();

    const detalle = await abrirDetalle(user, 'Aceite Natura 900 ml');

    const historial = within(detalle).getByRole('table', {
      name: 'Costos anteriores de Aceite Natura 900 ml',
    });
    expect(
      within(historial).getByRole('link', { name: 'Lista de precios Distribuidora Sur' }),
    ).toHaveAttribute('href', '/listas/lista-sur-sep');
    expect(within(historial).getAllByText('Cargado a mano')).toHaveLength(2);
    expect(within(historial).getAllByText('Distribuidora Sur').length).toBeGreaterThan(0);
    expect(within(detalle).getByRole('img', { name: /Cómo cambió el costo/ })).toBeInTheDocument();
  });

  it('compara con otros proveedores y con la zona cuando hay suficientes comercios', async () => {
    const user = userEvent.setup();
    await abrirPrecios();

    const detalle = await abrirDetalle(user, 'Aceite Natura 900 ml');

    expect(within(detalle).getByText(/Mayorista Norte/)).toBeInTheDocument();
    expect(within(detalle).getByRole('link', { name: 'Ver en mi red' })).toHaveAttribute(
      'href',
      '/red',
    );
    expect(within(detalle).getByText(/Promedio de la zona/)).toBeInTheDocument();
  });

  it('no muestra datos de la zona si hay menos de 5 comercios', async () => {
    const user = userEvent.setup();
    await abrirPrecios();

    const detalle = await abrirDetalle(user, 'Arroz Gallo Oro 1 kg');

    expect(
      within(detalle).getByText(
        /Todavía no hay suficientes comercios en tu zona para comparar \(3 de 5\)/,
      ),
    ).toBeInTheDocument();
    expect(within(detalle).queryByText(/Promedio de la zona/)).not.toBeInTheDocument();
  });

  it('con la prueba vencida el historial y los proveedores alternativos llevan candado', async () => {
    const user = userEvent.setup();
    useRedStore.getState().simularFinDePrueba();
    await abrirPrecios();

    const detalle = await abrirDetalle(user, 'Aceite Natura 900 ml');

    expect(
      within(detalle).getByRole('heading', {
        name: 'Historial de costos y de qué lista salió cada uno',
      }),
    ).toBeInTheDocument();
    expect(
      within(detalle).getByRole('heading', { name: 'Comparar el costo entre proveedores' }),
    ).toBeInTheDocument();
    expect(
      within(detalle).getByRole('heading', { name: 'Saber si comprás caro frente a tu zona' }),
    ).toBeInTheDocument();
    expect(within(detalle).queryByRole('table')).not.toBeInTheDocument();
    // Aun sin el plan se ve el último costo y de dónde salió.
    expect(within(detalle).getByText('Te cuesta')).toBeInTheDocument();
    expect(
      within(detalle).getByRole('link', { name: 'Lista de precios Distribuidora Sur' }),
    ).toBeInTheDocument();
  });

  it('en las reglas, guardar un margen distinto actualiza la vista previa y las reglas', async () => {
    const user = userEvent.setup();
    await abrirPrecios();
    await user.click(screen.getByRole('tab', { name: 'Reglas de precio' }));

    const panel = screen.getByRole('tabpanel', { name: 'Reglas de precio' });
    const resumenAntes = within(panel).getByText(/Con estas reglas,/).textContent;
    const campo = within(panel).getByLabelText('Ganancia de Almacén (%)');
    await user.clear(campo);
    await user.type(campo, '60');

    await waitFor(() =>
      expect(within(panel).getByText(/Con estas reglas,/).textContent).not.toBe(resumenAntes),
    );
    expect(within(panel).getByText(/Aceite Natura 900 ml/)).toBeInTheDocument();
    // Nada se aplicó todavía.
    expect(useRedStore.getState().reglas.porCategoria['Almacén']?.margen).toBe(33);

    await user.click(screen.getByRole('button', { name: 'Guardar reglas' }));

    expect(useRedStore.getState().reglas.porCategoria['Almacén']?.margen).toBe(60);
    expect(await screen.findByText('Listo. Guardamos tus reglas de precio.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Guardar reglas' })).not.toBeInTheDocument();
  });

  it('aplicar a los que quedan en riesgo pide confirmar y recién ahí cambia los precios', async () => {
    const user = userEvent.setup();
    await abrirPrecios();
    await user.click(screen.getByRole('tab', { name: 'Reglas de precio' }));

    await user.click(screen.getByRole('button', { name: 'Aplicar a los 2 que quedan en riesgo' }));
    const dialogo = await screen.findByRole('alertdialog', {}, ESPERA);
    expect(within(dialogo).getByText(/Aceite Natura 900 ml/)).toBeInTheDocument();
    expect(useRedStore.getState().productos.find((p) => p.id === 'p-aceite')?.precio).toBe(2350);

    await user.click(within(dialogo).getByRole('button', { name: 'No, dejarlos como están' }));
    expect(useRedStore.getState().productos.find((p) => p.id === 'p-aceite')?.precio).toBe(2350);

    await user.click(screen.getByRole('button', { name: 'Aplicar a los 2 que quedan en riesgo' }));
    await user.click(
      within(await screen.findByRole('alertdialog', {}, ESPERA)).getByRole('button', {
        name: 'Sí, cambiar',
      }),
    );

    const productos = useRedStore.getState().productos;
    expect(productos.find((p) => p.id === 'p-aceite')?.precio).toBe(2860);
    expect(productos.find((p) => p.id === 'p-yogur')?.precio).toBe(790);
    expect(await screen.findByText(/Listo\. Cambiamos 2 precios/)).toBeInTheDocument();
  });

  it('no tiene violaciones de accesibilidad (catálogo, detalle y reglas)', async () => {
    const user = userEvent.setup();
    await abrirPrecios();
    await sinViolaciones();

    await abrirDetalle(user, 'Aceite Natura 900 ml');
    await sinViolaciones();
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    await user.click(screen.getByRole('tab', { name: 'Reglas de precio' }));
    await sinViolaciones();
  });
});
