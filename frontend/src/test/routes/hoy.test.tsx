import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { derivarAlertas } from '@/lib/red/alertas';
import { useRedStore } from '@/stores/red';
import type { TipoAlerta } from '@/types/red';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';

async function abrirHoy() {
  setupUsuarioConSesion();
  renderApp('/hoy');
  await screen.findByRole('heading', { level: 1, name: /Almacén Don Carlos/ }, ESPERA);
}

/** Primer aviso de un tipo, tal como lo deriva la demostración (los ids y textos salen de los datos). */
function avisoDeTipo(tipo: TipoAlerta) {
  const aviso = derivarAlertas(useRedStore.getState()).find((a) => a.tipo === tipo);
  if (!aviso) throw new Error(`La semilla no tiene un aviso de tipo ${tipo}`);
  // Intl pone espacios duros en los importes; el texto de la pantalla se compara ya normalizado.
  return { ...aviso, titulo: aviso.titulo.replace(/\s+/g, ' ') };
}

const tarjeta = (titulo: string) => {
  const item = screen.getByText(titulo).closest('li');
  if (!item) throw new Error(`No hay tarjeta para «${titulo}»`);
  return within(item);
};

const seccion = (nombre: string) => {
  const region = screen.getByRole('region', { name: nombre });
  return within(region);
};

async function sinViolaciones() {
  const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });
  const resumen = resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`);
  expect(resumen).toEqual([]);
}

describe('Hoy: avisos, valor y pedido', () => {
  it('muestra el valor que aporta y los avisos agrupados por plata en juego', async () => {
    await abrirHoy();

    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    const valor = seccion('El valor que aporta Ensambla');
    expect(valor.getByText(/por mes que dejaste de perder/)).toBeInTheDocument();
    expect(valor.getByText(/de trabajo que te ahorraste/)).toBeInTheDocument();
    expect(valor.getByText('¿Cómo lo calculamos?')).toBeInTheDocument();

    const avisos = seccion('Lo que importa hoy');
    expect(avisos.getByRole('heading', { level: 3, name: /Para corregir ya/ })).toBeInTheDocument();
    expect(
      avisos.getByRole('heading', { level: 3, name: /Para comprar mejor/ }),
    ).toBeInTheDocument();
    expect(avisos.getByText(avisoDeTipo('precio-en-riesgo').titulo)).toBeInTheDocument();
    expect(avisos.getByText(avisoDeTipo('stock-por-agotarse').titulo)).toBeInTheDocument();
    expect(avisos.getAllByText(/por mes en juego/).length).toBeGreaterThan(0);
  });

  it('aplicar el precio de un aviso de precio en riesgo lo hace desaparecer y avisa qué falta', async () => {
    const user = userEvent.setup();
    await abrirHoy();
    const riesgo = avisoDeTipo('precio-en-riesgo');
    if (riesgo.accion.tipo !== 'aplicar-precio') throw new Error('Acción inesperada');

    await user.click(tarjeta(riesgo.titulo).getByRole('button', { name: 'Aplicar' }));

    expect(await screen.findByText(/Listo: .* ahora cuesta/, {}, ESPERA)).toHaveTextContent(
      /Falta cambiar la etiqueta en el mostrador/,
    );
    expect(screen.getByRole('button', { name: 'Ir al mostrador' })).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByText(riesgo.titulo)).not.toBeInTheDocument());
    const producto = useRedStore.getState().productos.find((p) => p.id === riesgo.productoId);
    expect(producto?.precio).toBe(riesgo.accion.precioNuevo);
  });

  it('agregar al pedido desde un aviso de stock lo refleja en «Tu pedido»', async () => {
    const user = userEvent.setup();
    await abrirHoy();
    const stock = avisoDeTipo('stock-por-agotarse');
    const pedido = seccion('Tu pedido');
    expect(pedido.getByText(/Todavía no armaste un pedido/)).toBeInTheDocument();

    await user.click(tarjeta(stock.titulo).getByRole('button', { name: 'Agregar al pedido' }));

    const producto = useRedStore.getState().productos.find((p) => p.id === stock.productoId);
    expect(await pedido.findByText(producto?.nombre ?? '', {}, ESPERA)).toBeInTheDocument();
    expect(pedido.getByRole('link', { name: /Compartir pedido/ })).toHaveAttribute(
      'href',
      expect.stringContaining('https://wa.me/?text='),
    );
    expect(tarjeta(stock.titulo).getByText(/En tu pedido/)).toBeInTheDocument();
    expect(useRedStore.getState().pedido).toHaveLength(1);
  });

  it('permite cambiar la cantidad, quitar y vaciar el pedido con confirmación', async () => {
    const user = userEvent.setup();
    await abrirHoy();
    const stock = avisoDeTipo('stock-por-agotarse');
    await user.click(tarjeta(stock.titulo).getByRole('button', { name: 'Agregar al pedido' }));
    const pedido = seccion('Tu pedido');
    const producto = useRedStore.getState().productos.find((p) => p.id === stock.productoId);
    const inicial = useRedStore.getState().pedido[0]?.cantidad ?? 0;

    await user.click(
      await pedido.findByRole('button', { name: `Una unidad más de ${producto?.nombre}` }, ESPERA),
    );
    expect(useRedStore.getState().pedido[0]?.cantidad).toBe(inicial + 1);

    await user.click(pedido.getByRole('button', { name: 'Vaciar' }));
    const dialogo = await screen.findByRole('alertdialog', {}, ESPERA);
    expect(dialogo).toHaveTextContent(/Vas a sacar 1 producto de 1 proveedor/);
    await user.click(within(dialogo).getByRole('button', { name: /Sí, vaciar el pedido/ }));

    await waitFor(() => expect(useRedStore.getState().pedido).toEqual([]));
    expect(await pedido.findByText(/Todavía no armaste un pedido/, {}, ESPERA)).toBeInTheDocument();
  });

  it('ignorar un aviso lo manda a «Avisos que ignoraste» y se puede deshacer', async () => {
    const user = userEvent.setup();
    await abrirHoy();
    const riesgo = avisoDeTipo('precio-en-riesgo');

    await user.click(tarjeta(riesgo.titulo).getByRole('button', { name: 'Ignorar' }));

    await waitFor(() => expect(screen.getAllByText(riesgo.titulo)).toHaveLength(1));
    expect(screen.getByText(/Avisos que ignoraste \(1\)/)).toBeInTheDocument();

    await user.click(await screen.findByRole('button', { name: 'Deshacer' }, ESPERA));
    await waitFor(() => expect(screen.queryByText(/Avisos que ignoraste/)).not.toBeInTheDocument());
    expect(screen.getByText(riesgo.titulo)).toBeInTheDocument();
  });

  it('con la prueba vencida invita al plan con los avisos bloqueados y el resumen con candado', async () => {
    useRedStore.getState().simularFinDePrueba();
    await abrirHoy();

    const bloqueadas = screen.getByRole('region', { name: /avisos? que no estás viendo/ });
    expect(bloqueadas).toHaveTextContent(/por mes/);
    expect(within(bloqueadas).getByRole('link', { name: /Ver el plan/ })).toHaveAttribute(
      'href',
      '/plan',
    );
    expect(
      within(bloqueadas).getByText(avisoDeTipo('stock-por-agotarse').titulo),
    ).toBeInTheDocument();
    // Los títulos se ven pero no hay acción: el botón del aviso bloqueado no existe.
    expect(within(bloqueadas).queryByRole('button')).not.toBeInTheDocument();
    expect(screen.getByText(/Está incluida en el plan/)).toBeInTheDocument();
    await sinViolaciones();
  });

  it('con el plan completo muestra el resumen del día', async () => {
    await abrirHoy();

    const resumen = seccion('Resumen del día');
    expect(resumen.getByText(/de margen promedio/)).toBeInTheDocument();
    expect(resumen.getAllByText(/estimado/i).length).toBeGreaterThan(0);
  });

  it('ajustar avisos: apagar un tipo de aviso lo saca de la pantalla', async () => {
    const user = userEvent.setup();
    await abrirHoy();
    const stock = avisoDeTipo('stock-por-agotarse');

    await user.click(screen.getByRole('button', { name: 'Ajustar avisos' }));
    const hoja = await screen.findByRole('dialog', { name: 'Ajustar avisos' }, ESPERA);
    await user.click(within(hoja).getByRole('checkbox', { name: /Se termina/ }));
    await user.click(within(hoja).getByRole('button', { name: 'Guardar ajustes' }));

    await waitFor(() => expect(screen.queryByText(stock.titulo)).not.toBeInTheDocument());
    expect(useRedStore.getState().umbrales.activas['stock-por-agotarse']).toBe(false);
  });

  it('ajustar avisos: valida los números antes de guardar', async () => {
    const user = userEvent.setup();
    await abrirHoy();

    await user.click(screen.getByRole('button', { name: 'Ajustar avisos' }));
    const hoja = await screen.findByRole('dialog', { name: 'Ajustar avisos' }, ESPERA);
    const margen = within(hoja).getByLabelText(/Ganancia mínima/);
    await user.clear(margen);
    await user.type(margen, '150');
    await user.click(within(hoja).getByRole('button', { name: 'Guardar ajustes' }));

    expect(
      await within(hoja).findByText(/Tiene que estar entre 0 y 100/, {}, ESPERA),
    ).toBeVisible();
    expect(useRedStore.getState().reglas.margenMinimo).toBe(12);

    await user.clear(margen);
    await user.type(margen, '20');
    await user.click(within(hoja).getByRole('button', { name: 'Guardar ajustes' }));
    await waitFor(() => expect(useRedStore.getState().reglas.margenMinimo).toBe(20));
  });

  it('sin avisos activos muestra «todo en orden» y el vínculo a la red', async () => {
    const { umbrales } = useRedStore.getState();
    useRedStore.getState().guardarUmbrales({
      ...umbrales,
      activas: Object.fromEntries(
        Object.keys(umbrales.activas).map((tipo) => [tipo, false]),
      ) as typeof umbrales.activas,
    });
    await abrirHoy();

    expect(await screen.findByText('Todo en orden por hoy')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver mi red' })).toHaveAttribute('href', '/red');
  });

  it('no tiene violaciones de accesibilidad', async () => {
    const user = userEvent.setup();
    await abrirHoy();
    await sinViolaciones();

    await user.click(screen.getByRole('button', { name: 'Ajustar avisos' }));
    await screen.findByRole('dialog', { name: 'Ajustar avisos' }, ESPERA);
    await sinViolaciones();
  });
});
