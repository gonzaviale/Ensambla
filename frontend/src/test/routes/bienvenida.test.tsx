import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { crearProyecto, listarProyectos, obtenerProyecto } from '@/api/proyectos';
import { renderApp } from '@/test/renderApp';
import { suscripcionInicial } from '@/lib/planes/suscripcion';
import { useSuscripcionStore } from '@/stores/suscripcion';
import { ESPERA, setupUsuarioConSesion, verifyHeadingPresentAsync } from '@/test/testHelpers';

type Usuario = ReturnType<typeof userEvent.setup>;

async function sinViolaciones() {
  const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });
  const resumen = resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`);
  expect(resumen).toEqual([]);
}

async function abrirBienvenida() {
  setupUsuarioConSesion();
  renderApp('/bienvenida');
  await verifyHeadingPresentAsync('Contanos de tu negocio');
  return userEvent.setup();
}

async function contarNegocio(user: Usuario) {
  await user.type(screen.getByLabelText(/Cómo se llama tu negocio/), 'Viandas de Ana');
  await user.type(
    screen.getByLabelText('¿Qué hacés?'),
    'Vendo viandas por encargo y anoto lo que me deben',
  );
}

async function irAlPasoDos(user: Usuario) {
  await contarNegocio(user);
  await user.click(screen.getByRole('button', { name: 'Seguir' }));
  await verifyHeadingPresentAsync('Esto armé para vos');
}

describe('Bienvenida: tres pasos para armar el sistema', () => {
  it('el paso 1 muestra en vivo lo que Ensambla va entendiendo', async () => {
    const user = await abrirBienvenida();
    expect(screen.getByText(/Contame qué hacés/)).toBeInTheDocument();

    await contarNegocio(user);

    const entendido = screen.getByRole('list', { name: 'Bloques que armaría' });
    expect(within(entendido).getByText('Platos')).toBeInTheDocument();
    expect(within(entendido).getByText('Fiados')).toBeInTheDocument();
    expect(screen.getByText(/Parece un negocio de/)).toHaveTextContent('gastronomía y viandas');
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
  });

  it('pide el nombre del negocio antes de seguir', async () => {
    const user = await abrirBienvenida();
    await user.click(screen.getByRole('button', { name: 'Seguir' }));
    expect(await screen.findByText('Escribí cómo se llama tu negocio.')).toBeInTheDocument();
    expect(screen.getByLabelText(/Cómo se llama tu negocio/)).toHaveFocus();
  });

  it('el paso 2 lista las piezas con su motivo y permite sacar una y deshacer', async () => {
    const user = await abrirBienvenida();
    await irAlPasoDos(user);

    const lista = screen.getByRole('list', { name: 'Bloques de tu sistema' });
    expect(within(lista).getAllByText(/Es lo básico de gastronomía y viandas/)).not.toHaveLength(0);
    expect(within(lista).getByText(/La sumé porque mencionaste «debe»/)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Sacar Fiados' }));
    expect(within(lista).queryByRole('heading', { name: 'Fiados' })).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Sacaste Fiados');

    await user.click(screen.getByRole('button', { name: 'Deshacer' }));
    expect(within(lista).getByRole('heading', { name: 'Fiados' })).toBeInTheDocument();
  });

  it('«Armar mi sistema» crea las tablas y pasa al primer dato', async () => {
    const user = await abrirBienvenida();
    await irAlPasoDos(user);

    await user.click(screen.getByRole('button', { name: 'Armar mi sistema' }));
    await verifyHeadingPresentAsync('Primer dato');

    const [sistema] = await listarProyectos();
    if (!sistema) throw new Error('No se creó el sistema');
    expect(sistema.nombre).toBe('Viandas de Ana');
    const detalle = await obtenerProyecto(sistema.id);
    expect(detalle.bloques.length).toBeGreaterThanOrEqual(3);
    expect(detalle.bloques.map((b) => b.nombre)).toContain('Platos');

    expect(screen.getByRole('link', { name: 'Contá qué pasó' })).toHaveAttribute('href', '/subir');
    expect(screen.getByRole('link', { name: 'Traer mi historia' })).toHaveAttribute(
      'href',
      '/cuaderno',
    );
    expect(screen.getByRole('link', { name: 'Ir a mi inicio' })).toHaveAttribute('href', '/inicio');
  });

  it('en el plan Gratis arma sólo los 3 primeros bloques y lo avisa', async () => {
    useSuscripcionStore.setState({
      suscripcion: { ...suscripcionInicial(), plan: 'gratis', estado: 'gratis' },
    });
    const user = await abrirBienvenida();
    await user.type(screen.getByLabelText(/Cómo se llama tu negocio/), 'Viandas de Ana');
    await user.type(
      screen.getByLabelText('¿Qué hacés?'),
      'Vendo viandas, anoto lo que me deben y uso una receta',
    );
    await user.click(screen.getByRole('button', { name: 'Seguir' }));
    await verifyHeadingPresentAsync('Esto armé para vos');
    expect(screen.getByRole('note')).toHaveTextContent('Tu plan Gratis incluye 3 bloques');

    await user.click(screen.getByRole('button', { name: 'Armar mi sistema' }));
    await verifyHeadingPresentAsync('Primer dato');

    const [sistema] = await listarProyectos();
    if (!sistema) throw new Error('No se creó el sistema');
    expect((await obtenerProyecto(sistema.id)).bloques).toHaveLength(3);
  });

  it('carga datos de ejemplo rotulados en las tablas nuevas', async () => {
    const user = await abrirBienvenida();
    await irAlPasoDos(user);
    await user.click(screen.getByRole('button', { name: 'Armar mi sistema' }));
    await verifyHeadingPresentAsync('Primer dato');

    await waitFor(
      () => expect(screen.getByRole('button', { name: 'Cargar datos de ejemplo' })).toBeEnabled(),
      ESPERA,
    );
    await user.click(screen.getByRole('button', { name: 'Cargar datos de ejemplo' }));

    expect(await screen.findByText(/Listo: cargamos/, {}, ESPERA)).toHaveTextContent(
      'datos de ejemplo',
    );
  });

  it('con un sistema ya creado muestra «Ya tenés tu sistema» y no arma de nuevo', async () => {
    setupUsuarioConSesion();
    await crearProyecto({ nombre: 'Mi almacén' });
    renderApp('/bienvenida');

    expect(
      await screen.findByRole('heading', { name: 'Ya tenés tu sistema' }, ESPERA),
    ).toBeVisible();
    expect(screen.getByRole('link', { name: 'Ir a mi inicio' })).toHaveAttribute('href', '/inicio');
    expect(screen.queryByRole('button', { name: 'Seguir' })).not.toBeInTheDocument();
  });

  it('no tiene violaciones de accesibilidad en los pasos 1 y 2', async () => {
    const user = await abrirBienvenida();
    await sinViolaciones();

    await irAlPasoDos(user);
    await sinViolaciones();
  });
});
