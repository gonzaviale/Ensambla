import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { crearProyecto, crearProyectoDesdePlantilla } from '@/api/proyectos';
import { plantillasMock } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupUsuarioConSesion,
  setupServerError,
  verifyHeadingPresentAsync,
  verifyPathAsync,
} from '@/test/testHelpers';

function inventario() {
  const plantilla = plantillasMock.find((item) => item.nombre === 'Inventario');
  if (!plantilla) throw new Error('Falta la plantilla Inventario');
  return plantilla;
}

describe('ENS-4: crear y gestionar proyectos', () => {
  it('presenta el inicio vacío y explica la disponibilidad de fotos', async () => {
    setupUsuarioConSesion();
    const user = userEvent.setup();
    renderApp('/proyectos');

    await verifyHeadingPresentAsync('¿Por dónde querés empezar?');
    expect(screen.getByText(/Todavía no tenés proyectos/)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Subir foto de mi cuaderno' }));
    expect(screen.getByRole('status')).toHaveTextContent(
      'La carga de fotos estará disponible próximamente',
    );
  });

  it('crea en blanco y abre el proyecto con orientación para agregar el primer bloque', async () => {
    setupUsuarioConSesion();
    const user = userEvent.setup();
    const router = renderApp('/proyectos');
    await verifyHeadingPresentAsync('¿Por dónde querés empezar?');

    await user.click(screen.getByRole('button', { name: 'Empezar en blanco' }));
    await user.type(screen.getByLabelText('Nombre del proyecto'), 'Mi taller');
    await user.click(screen.getByRole('button', { name: 'Crear proyecto' }));

    await verifyHeadingPresentAsync('Mi taller');
    await verifyHeadingPresentAsync('Todavía no hay bloques en este proyecto');
    expect(router.state.location.pathname).toMatch(/\/proyectos\/[^/]+\/construir$/);
  });

  it('elige una plantilla y cae en Usar con sus columnas y un aviso para cambiar la estructura', async () => {
    setupUsuarioConSesion();
    const user = userEvent.setup();
    const router = renderApp('/proyectos');
    await verifyHeadingPresentAsync('Plantillas disponibles');

    await user.click(screen.getByRole('button', { name: 'Usar plantilla Inventario' }));

    expect(await screen.findByText(/Tu proyecto ya viene armado/, {}, ESPERA)).toBeInTheDocument();
    expect(router.state.location.pathname).toMatch(/\/proyectos\/[^/]+\/usar$/);
    expect(screen.getByRole('heading', { level: 1, name: 'Inventario' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Cambiar la estructura' })).toBeInTheDocument();
    for (const columna of ['Nombre', 'Stock', 'Rubro']) {
      expect(
        await screen.findByRole('columnheader', { name: columna }, ESPERA),
      ).toBeInTheDocument();
    }

    await user.click(screen.getByRole('button', { name: 'Entendido' }));

    await waitFor(() =>
      expect(screen.queryByText(/Tu proyecto ya viene armado/)).not.toBeInTheDocument(),
    );
  });

  it('conserva lo escrito y muestra el error si falla la creación', async () => {
    setupUsuarioConSesion();
    setupServerError('post', '/proyectos', 500, 'No pudimos guardar el proyecto');
    const user = userEvent.setup();
    renderApp('/proyectos');
    await verifyHeadingPresentAsync('¿Por dónde querés empezar?');

    await user.click(screen.getByRole('button', { name: 'Empezar en blanco' }));
    await user.type(screen.getByLabelText('Nombre del proyecto'), 'Mi taller');
    await user.click(screen.getByRole('button', { name: 'Crear proyecto' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos guardar el proyecto');
    expect(screen.getByLabelText('Nombre del proyecto')).toHaveValue('Mi taller');
  });
});

describe('Tarjetas de Tus proyectos', () => {
  it('muestra los bloques del proyecto y cuántos datos tiene cada uno', async () => {
    setupUsuarioConSesion();
    await crearProyectoDesdePlantilla(inventario().id);
    renderApp('/proyectos');

    const bloques = await screen.findByRole('list', { name: 'Bloques de Inventario' }, ESPERA);
    expect(within(bloques).getByRole('listitem')).toHaveTextContent('Productos · 3 datos');
  });

  it('la acción principal es Usar y la secundaria lleva a Construir', async () => {
    setupUsuarioConSesion();
    const proyecto = await crearProyectoDesdePlantilla(inventario().id);
    const user = userEvent.setup();
    const router = renderApp('/proyectos');
    await screen.findByRole('list', { name: 'Bloques de Inventario' }, ESPERA);
    expect(screen.getByRole('link', { name: 'Cambiar estructura de Inventario' })).toHaveAttribute(
      'href',
      `/proyectos/${proyecto.id}/construir`,
    );

    await user.click(screen.getByRole('link', { name: 'Usar Inventario' }));

    await verifyPathAsync(router, `/proyectos/${proyecto.id}/usar`);
    expect(await screen.findByRole('columnheader', { name: 'Stock' }, ESPERA)).toBeInTheDocument();
  });

  it('un proyecto sin bloques explica que hay que armarlos y lleva a Construir', async () => {
    setupUsuarioConSesion();
    await crearProyecto({ nombre: 'Mi almacén' });
    const user = userEvent.setup();
    renderApp('/proyectos');
    await verifyHeadingPresentAsync('Mi almacén');

    expect(await screen.findByText(/Todavía no tiene bloques/, {}, ESPERA)).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Usar Mi almacén' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('link', { name: 'Armar bloques de Mi almacén' }));

    await verifyHeadingPresentAsync('Tus bloques');
  });

  it('Más opciones se abre y se cierra con Escape devolviendo el foco', async () => {
    setupUsuarioConSesion();
    await crearProyecto({ nombre: 'Original' });
    const user = userEvent.setup();
    renderApp('/proyectos');
    await verifyHeadingPresentAsync('Original');
    const boton = screen.getByRole('button', { name: 'Más opciones de Original' });
    expect(boton).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('button', { name: 'Renombrar' })).not.toBeInTheDocument();

    await user.click(boton);
    expect(boton).toHaveAttribute('aria-expanded', 'true');
    await user.tab();
    expect(screen.getByRole('button', { name: 'Renombrar' })).toHaveFocus();
    await user.keyboard('{Escape}');

    expect(screen.queryByRole('button', { name: 'Renombrar' })).not.toBeInTheDocument();
    expect(boton).toHaveFocus();
  });

  it('renombra un proyecto desde su tarjeta', async () => {
    setupUsuarioConSesion();
    await crearProyecto({ nombre: 'Original' });
    const user = userEvent.setup();
    renderApp('/proyectos');
    await verifyHeadingPresentAsync('Original');

    await user.click(screen.getByRole('button', { name: 'Más opciones de Original' }));
    await user.click(screen.getByRole('button', { name: 'Renombrar' }));
    await user.clear(screen.getByLabelText('Nuevo nombre'));
    await user.type(screen.getByLabelText('Nuevo nombre'), 'Nombre nuevo');
    await user.click(screen.getByRole('button', { name: 'Guardar nombre' }));

    await verifyHeadingPresentAsync('Nombre nuevo');
    expect(screen.queryByRole('heading', { name: 'Original' })).not.toBeInTheDocument();
  });

  it('requiere confirmación para eliminar y actualiza el listado', async () => {
    setupUsuarioConSesion();
    await crearProyecto({ nombre: 'Temporal' });
    const user = userEvent.setup();
    renderApp('/proyectos');
    await verifyHeadingPresentAsync('Temporal');

    await user.click(screen.getByRole('button', { name: 'Más opciones de Temporal' }));
    await user.click(screen.getByRole('button', { name: 'Eliminar' }));
    expect(screen.getByRole('heading', { name: 'Temporal' })).toBeInTheDocument();
    expect(screen.getByText(/Esta acción no se puede deshacer/)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Sí, eliminar proyecto' }));

    await waitFor(() =>
      expect(screen.queryByRole('heading', { name: 'Temporal' })).not.toBeInTheDocument(),
    );
    expect(screen.getByText(/Todavía no tenés proyectos/)).toBeInTheDocument();
  });
});
