import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { useRedStore } from '@/stores/red';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
} from '@/test/testHelpers';

// El contraste de color no se puede medir en jsdom (sin estilos): se verifica a mano con los tokens.
async function sinViolaciones() {
  const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });
  const resumen = resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`);
  expect(resumen).toEqual([]);
}

const producto = (id: string) => {
  const encontrado = useRedStore.getState().productos.find((p) => p.id === id);
  if (!encontrado) throw new Error(`Falta el producto ${id}`);
  return encontrado;
};

async function abrir(ruta: string, titulo: string) {
  setupUsuarioConSesion();
  const router = renderApp(ruta);
  await verifyHeadingPresentAsync(titulo);
  return router;
}

async function irAlPaso(user: ReturnType<typeof userEvent.setup>, nombre: string, veces: number) {
  for (let i = 0; i < veces; i += 1) {
    await user.click(screen.getByRole('button', { name: 'Seguir' }));
  }
  expect(
    await screen.findByRole('heading', { level: 2, name: nombre }, ESPERA),
  ).toBeInTheDocument();
}

describe('Listas de proveedores', () => {
  it('muestra las dos listas por revisar con su resumen y el historial', async () => {
    await abrir('/listas', 'Listas de tus proveedores');

    const sur = screen.getByRole('region', { name: 'Distribuidora Sur' });
    expect(sur).toHaveTextContent('6 productos suben');
    expect(sur).toHaveTextContent('3 líneas para revisar');
    expect(
      within(sur).getByRole('link', { name: /Revisar la lista de Distribuidora Sur/ }),
    ).toBeInTheDocument();

    const norte = screen.getByRole('region', { name: 'Mayorista Norte' });
    expect(norte).toHaveTextContent('6 productos suben');
    expect(norte).toHaveTextContent('1 línea para revisar');
    expect(norte).toHaveTextContent('1 cambio raro');

    const historial = screen.getByRole('heading', { level: 2, name: 'Ya aplicadas' });
    expect(historial.closest('section')).toHaveTextContent('Lácteos del Valle');
    expect(screen.getAllByText('Aplicada').length).toBeGreaterThan(0);
  });

  it('carga una lista de Lácteos del Valle y navega a ella', async () => {
    const user = userEvent.setup();
    const router = await abrir('/listas', 'Listas de tus proveedores');
    const antes = useRedStore.getState().listas.length;

    await user.click(screen.getByRole('button', { name: 'Cargar una lista' }));
    await user.selectOptions(
      await screen.findByLabelText('¿De qué proveedor es?'),
      'Lácteos del Valle',
    );
    expect(screen.getByRole('radio', { name: 'PDF' })).toBeChecked();
    await user.click(screen.getByRole('radio', { name: 'WhatsApp' }));
    await user.click(screen.getByRole('button', { name: 'Cargar lista' }));

    await verifyHeadingPresentAsync('Lista de Lácteos del Valle');
    expect(router.state.location.pathname).toMatch(/^\/listas\/lista-/);
    const listas = useRedStore.getState().listas;
    expect(listas).toHaveLength(antes + 1);
    expect(listas[0]).toMatchObject({
      proveedorId: 'prov-valle',
      origen: 'whatsapp',
      estado: 'por-revisar',
    });
  });

  it('resuelve la duda de unidad o bulto de Sprite y muestra el costo por unidad', async () => {
    const user = userEvent.setup();
    await abrir('/listas/lista-sur-oct', 'Lista de Distribuidora Sur');
    await irAlPaso(user, 'Revisá las dudas', 1);

    const linea = screen.getByText('SPRITE 1,5 L').closest('li');
    expect(linea).not.toBeNull();
    const duda = within(linea as HTMLElement);
    expect(duda.getByText(/es por unidad o por bulto de 6/)).toBeInTheDocument();
    expect(duda.getByText(/Creemos que es/)).toHaveTextContent('Sprite 1,5 L');

    await user.click(duda.getByRole('radio', { name: /Por bulto de 6/ }));
    await user.click(duda.getByRole('button', { name: 'Es este producto' }));

    expect(await duda.findByText('Listo', {}, ESPERA)).toBeInTheDocument();
    expect(linea).toHaveTextContent(/Costo por unidad:\s*\$\s*1\.580/);
    const item = useRedStore
      .getState()
      .listas.find((l) => l.id === 'lista-sur-oct')
      ?.items.find((i) => i.textoOriginal === 'SPRITE 1,5 L');
    expect(item).toMatchObject({ presentacion: 'BULTO', unidadesPorBulto: 6, confirmado: true });

    // Se puede deshacer: vuelve a la lectura original de la línea.
    await user.click(duda.getByRole('button', { name: 'Deshacer' }));
    expect(
      await duda.findByRole('button', { name: 'Es este producto' }, ESPERA),
    ).toBeInTheDocument();
  });

  it('permite ignorar una línea que no es de ningún producto', async () => {
    const user = userEvent.setup();
    await abrir('/listas/lista-sur-oct', 'Lista de Distribuidora Sur');
    await irAlPaso(user, 'Revisá las dudas', 1);

    const linea = screen.getByText('TURRON ARCOR MANI X50').closest('li') as HTMLElement;
    expect(linea).toHaveTextContent('No encontramos ningún producto tuyo');
    await user.click(within(linea).getByRole('button', { name: 'Ignorar esta línea' }));

    expect(await within(linea).findByText(/la ignoramos/, {}, ESPERA)).toBeInTheDocument();
  });

  it('aplica la lista, actualiza el precio del aceite y deja los cambios en el mostrador', async () => {
    const user = userEvent.setup();
    const router = await abrir('/listas/lista-sur-oct', 'Lista de Distribuidora Sur');
    expect(producto('p-aceite')).toMatchObject({ costo: 2150, precio: 2350 });

    await irAlPaso(user, 'Tus precios', 2);
    expect(screen.getAllByText(/Tus reglas: 33 % de ganancia en Almacén/).length).toBeGreaterThan(
      0,
    );
    await irAlPaso(user, 'Confirmar', 1);
    expect(screen.getByText(/Nada cambia en el local hasta que lo hagas vos/)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /^Aplicar \d+ precios$/ }));

    await verifyHeadingPresentAsync(/Listo, cambiamos \d+ precios/);
    expect(producto('p-aceite')).toMatchObject({ costo: 2210, precio: 2940 });
    expect(useRedStore.getState().listas.find((l) => l.id === 'lista-sur-oct')?.estado).toBe(
      'aplicada',
    );

    await user.click(screen.getByRole('link', { name: 'Ver los cambios para el mostrador' }));
    await verifyHeadingPresentAsync('Cambios para el mostrador');
    await verifyPathAsync(router, '/mostrador');
    const fila = screen
      .getByRole('heading', { level: 3, name: 'Aceite Natura 900 ml' })
      .closest('section') as HTMLElement;
    expect(fila).toHaveTextContent(/2\.350/);
    expect(fila).toHaveTextContent(/2\.940/);
    expect(fila).toHaveTextContent('De la lista de Distribuidora Sur');
  });

  it('respeta el precio que el comercio escribe y el que deja sin cambiar', async () => {
    const user = userEvent.setup();
    await abrir('/listas/lista-sur-oct', 'Lista de Distribuidora Sur');
    await irAlPaso(user, 'Tus precios', 2);

    const aceite = screen.getByLabelText(/Precio nuevo de Aceite Natura 900 ml/);
    await user.clear(aceite);
    await user.type(aceite, '3000');
    const azucar = screen
      .getByRole('heading', { level: 3, name: 'Azúcar Ledesma 1 kg' })
      .closest('section') as HTMLElement;
    await user.click(within(azucar).getByRole('button', { name: 'Dejar mi precio actual' }));
    expect(within(azucar).getByLabelText(/Precio nuevo/)).toHaveValue('1380');

    await irAlPaso(user, 'Confirmar', 1);
    await user.click(screen.getByRole('button', { name: /^Aplicar \d+ precios$/ }));
    await verifyHeadingPresentAsync(/Listo, cambiamos/);

    expect(producto('p-aceite').precio).toBe(3000);
    expect(producto('p-azucar')).toMatchObject({ precio: 1380, costo: 1070 });
  });

  it('descarta una lista después de confirmar y no cambia ningún precio', async () => {
    const user = userEvent.setup();
    await abrir('/listas/lista-norte-oct', 'Lista de Mayorista Norte');
    await irAlPaso(user, 'Confirmar', 3);

    await user.click(screen.getByRole('button', { name: 'Descartar esta lista' }));
    const dialogo = await screen.findByRole('alertdialog', {}, ESPERA);
    expect(dialogo).toHaveTextContent('No vamos a cambiar ningún costo');
    await user.click(within(dialogo).getByRole('button', { name: 'Descartar la lista' }));

    expect(
      await screen.findByText(/no se cambió ningún costo ni ningún precio/, {}, ESPERA),
    ).toBeInTheDocument();
    expect(useRedStore.getState().listas.find((l) => l.id === 'lista-norte-oct')?.estado).toBe(
      'descartada',
    );
    expect(producto('p-harina').precio).toBe(1050);
  });

  it('muestra una lista ya aplicada de sólo lectura, con la trazabilidad de los costos', async () => {
    await abrir('/listas/lista-valle-oct', 'Lista de Lácteos del Valle');

    expect(
      screen.getByRole('heading', {
        level: 2,
        name: 'Los costos de estos 2 productos salen de esta lista',
      }),
    ).toBeInTheDocument();
    expect(screen.getByText('Leche La Serenísima 1 L')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Seguir' })).toBeNull();
  });

  it('avisa si la lista no existe y lleva a Listas', async () => {
    await abrir('/listas/no-existe', 'No encontramos esa lista');

    expect(screen.getByRole('link', { name: 'Ir a Listas' })).toHaveAttribute('href', '/listas');
  });

  it('bloquea la carga con la prueba vencida y el tope de listas del mes', async () => {
    setupUsuarioConSesion();
    const { simularFinDePrueba, subirLista } = useRedStore.getState();
    simularFinDePrueba();
    // Asegura el tope del plan gratis (2 listas) en el mes en curso.
    subirLista('prov-valle', 'pdf');
    subirLista('prov-valle', 'pdf');
    renderApp('/listas');
    await verifyHeadingPresentAsync('Listas de tus proveedores');

    expect(screen.queryByRole('button', { name: 'Cargar una lista' })).toBeNull();
    expect(
      await screen.findByText(/Usaste 2 de 2 listas de este mes/, {}, ESPERA),
    ).toBeInTheDocument();
    expect(screen.getByText('2 de 2 listas')).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { level: 2, name: 'Listas de proveedores sin límite por mes' }),
    ).toBeInTheDocument();
    const candado = screen.getByRole('region', { name: /Listas de proveedores sin límite/ });
    expect(within(candado).getByRole('link', { name: 'Ver planes' })).toHaveAttribute(
      'href',
      '/plan',
    );
  });
});

describe('Cambios para el mostrador', () => {
  it('lista lo que falta cambiar y baja el contador al marcar uno como trasladado', async () => {
    const user = userEvent.setup();
    await abrir('/mostrador', 'Cambios para el mostrador');

    expect(screen.getByText('Te faltan 2')).toBeInTheDocument();
    expect(screen.getAllByText(/De la lista de Lácteos del Valle del/).length).toBeGreaterThan(0);

    await user.click(
      screen.getByRole('checkbox', { name: /Ya lo cambié en el local.*Yogur Ser 190 g/ }),
    );

    expect(await screen.findByText('Te falta 1', {}, ESPERA)).toBeInTheDocument();
    const cambio = useRedStore.getState().cambios.find((c) => c.productoId === 'p-yogur');
    expect(cambio?.trasladado).toBe(true);
    expect(screen.getByText(/Ya trasladados \(\d+\)/)).toBeInTheDocument();
  });

  it('marca todos como cambiados y permite deshacerlo', async () => {
    const user = userEvent.setup();
    await abrir('/mostrador', 'Cambios para el mostrador');

    await user.click(screen.getByRole('button', { name: 'Marcar todos como cambiados' }));
    expect(
      await screen.findByRole(
        'heading',
        { level: 2, name: 'No te falta cambiar ninguna etiqueta' },
        ESPERA,
      ),
    ).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Deshacer' }));
    expect(await screen.findByText('Te faltan 2', {}, ESPERA)).toBeInTheDocument();
  });

  it('ofrece las etiquetas para imprimir y copiar con el plan que las incluye', async () => {
    await abrir('/mostrador', 'Cambios para el mostrador');

    const etiquetas = screen
      .getByRole('heading', { level: 2, name: 'Etiquetas para imprimir' })
      .closest('section') as HTMLElement;
    expect(
      within(etiquetas).getByRole('button', { name: 'Imprimir las etiquetas' }),
    ).toBeInTheDocument();
    expect(
      within(etiquetas).getByRole('button', { name: 'Copiar la lista de cambios' }),
    ).toBeInTheDocument();
    expect(within(etiquetas).getByText('Yogur Ser 190 g')).toBeInTheDocument();
  });

  it('muestra el candado de las etiquetas con el plan gratis', async () => {
    setupUsuarioConSesion();
    useRedStore.getState().simularFinDePrueba();
    renderApp('/mostrador');
    await verifyHeadingPresentAsync('Cambios para el mostrador');

    expect(
      await screen.findByRole(
        'heading',
        { name: 'Etiquetas y lista de cambios para el mostrador' },
        ESPERA,
      ),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByRole('button', { name: 'Imprimir las etiquetas' })).toBeNull(),
    );
  });
});

describe('Accesibilidad (axe) de las listas', () => {
  it('Listas, con la hoja para cargar una lista abierta', async () => {
    const user = userEvent.setup();
    await abrir('/listas', 'Listas de tus proveedores');
    await sinViolaciones();

    await user.click(screen.getByRole('button', { name: 'Cargar una lista' }));
    await screen.findByLabelText('¿De qué proveedor es?', {}, ESPERA);
    await sinViolaciones();
  });

  it('Revisar una lista, en sus cuatro pasos', async () => {
    const user = userEvent.setup();
    await abrir('/listas/lista-sur-oct', 'Lista de Distribuidora Sur');
    await sinViolaciones();
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);

    await irAlPaso(user, 'Revisá las dudas', 1);
    await sinViolaciones();
    await irAlPaso(user, 'Tus precios', 1);
    await sinViolaciones();
    await irAlPaso(user, 'Confirmar', 1);
    await sinViolaciones();
  });

  it('Cambios para el mostrador', async () => {
    await abrir('/mostrador', 'Cambios para el mostrador');
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    await sinViolaciones();
  });
});
