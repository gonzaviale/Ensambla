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

describe('ENS-4: crear y gestionar sistemas', () => {
  it('presenta el inicio vacío y lleva a subir la foto del cuaderno', async () => {
    setupUsuarioConSesion();
    const user = userEvent.setup();
    const router = renderApp('/proyectos');

    await verifyHeadingPresentAsync('¿Por dónde querés empezar?');
    expect(screen.getByText(/Todavía no tenés sistemas/)).toBeInTheDocument();
    await user.click(screen.getByRole('link', { name: 'Subir foto de mi cuaderno' }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/cuaderno'));
    await verifyHeadingPresentAsync('Subir foto de mi cuaderno');
  });

  it('con sistemas, el inicio muestra primero Mis sistemas y deja las formas de empezar abajo', async () => {
    setupUsuarioConSesion();
    await crearProyecto({ nombre: 'Mi almacén' });
    renderApp('/proyectos');

    await verifyHeadingPresentAsync('Mis sistemas');
    expect(screen.queryByRole('heading', { name: '¿Por dónde querés empezar?' })).toBeNull();
    expect(screen.queryByText(/Todavía no tenés sistemas/)).toBeNull();
    const lista = screen.getByRole('region', { name: 'Tus sistemas' });
    const nuevo = screen.getByRole('heading', { name: 'Empezar otro sistema' });
    expect(within(lista).getByRole('heading', { name: 'Mi almacén' })).toBeInTheDocument();
    expect(lista.compareDocumentPosition(nuevo) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Empezar en blanco' })).toBeInTheDocument();
  });

  it('crea en blanco y abre el sistema con orientación para agregar la primera tabla', async () => {
    setupUsuarioConSesion();
    const user = userEvent.setup();
    const router = renderApp('/proyectos');
    await verifyHeadingPresentAsync('¿Por dónde querés empezar?');

    await user.click(screen.getByRole('button', { name: 'Empezar en blanco' }));
    await user.type(screen.getByLabelText('Nombre del sistema'), 'Mi taller');
    await user.click(screen.getByRole('button', { name: 'Crear sistema' }));

    await verifyHeadingPresentAsync('Mi taller');
    await verifyHeadingPresentAsync('Todavía no hay tablas en este sistema');
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
    await user.type(screen.getByLabelText('Nombre del sistema'), 'Mi taller');
    await user.click(screen.getByRole('button', { name: 'Crear sistema' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos guardar el proyecto');
    expect(screen.getByLabelText('Nombre del sistema')).toHaveValue('Mi taller');
  });
});

describe('Tarjetas de Tus sistemas', () => {
  it('resume cuántas tablas y columnas tiene cada sistema', async () => {
    setupUsuarioConSesion();
    await crearProyectoDesdePlantilla(inventario().id);
    renderApp('/proyectos');

    const lista = await screen.findByRole('region', { name: 'Tus sistemas' }, ESPERA);
    expect(await within(lista).findByText(/1 tabla · 3 columnas/, {}, ESPERA)).toBeInTheDocument();
  });

  it('la acción principal es Abrir y la secundaria lleva a Armar tablas', async () => {
    setupUsuarioConSesion();
    const proyecto = await crearProyectoDesdePlantilla(inventario().id);
    const user = userEvent.setup();
    const router = renderApp('/proyectos');
    await screen.findByText(/1 tabla · 3 columnas/, {}, ESPERA);
    expect(screen.getByRole('link', { name: 'Armar tablas de Inventario' })).toHaveAttribute(
      'href',
      `/proyectos/${proyecto.id}/construir`,
    );

    await user.click(screen.getByRole('link', { name: 'Abrir Inventario' }));

    await verifyPathAsync(router, `/proyectos/${proyecto.id}/usar`);
    expect(await screen.findByRole('columnheader', { name: 'Stock' }, ESPERA)).toBeInTheDocument();
  });

  it('un sistema sin tablas explica que hay que armarlas y lleva a Armar', async () => {
    setupUsuarioConSesion();
    await crearProyecto({ nombre: 'Mi almacén' });
    const user = userEvent.setup();
    renderApp('/proyectos');
    await verifyHeadingPresentAsync('Mi almacén');

    expect(await screen.findByText(/Todavía no tiene tablas/, {}, ESPERA)).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Abrir Mi almacén' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('link', { name: 'Armar tablas de Mi almacén' }));

    await verifyHeadingPresentAsync('Tus tablas');
  });

  it('Más acciones se abre y se cierra con Escape devolviendo el foco', async () => {
    setupUsuarioConSesion();
    await crearProyecto({ nombre: 'Original' });
    const user = userEvent.setup();
    renderApp('/proyectos');
    await verifyHeadingPresentAsync('Original');
    const boton = screen.getByRole('button', { name: 'Más acciones de Original' });
    expect(boton).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('menuitem', { name: 'Cambiar nombre' })).not.toBeInTheDocument();

    await user.click(boton);
    expect(boton).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('menuitem', { name: 'Cambiar nombre' })).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('menuitem', { name: 'Eliminar sistema' })).toHaveFocus();
    await user.keyboard('{Escape}');

    expect(screen.queryByRole('menuitem', { name: 'Cambiar nombre' })).not.toBeInTheDocument();
    expect(boton).toHaveFocus();
  });

  it('renombra un sistema desde su tarjeta', async () => {
    setupUsuarioConSesion();
    await crearProyecto({ nombre: 'Original' });
    const user = userEvent.setup();
    renderApp('/proyectos');
    await verifyHeadingPresentAsync('Original');

    await user.click(screen.getByRole('button', { name: 'Más acciones de Original' }));
    await user.click(screen.getByRole('menuitem', { name: 'Cambiar nombre' }));
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

    await user.click(screen.getByRole('button', { name: 'Más acciones de Temporal' }));
    await user.click(screen.getByRole('menuitem', { name: 'Eliminar sistema' }));
    expect(screen.getByRole('heading', { name: 'Temporal' })).toBeInTheDocument();
    expect(screen.getByText(/Esta acción no se puede deshacer/)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Sí, eliminar sistema' }));

    await waitFor(() =>
      expect(screen.queryByRole('heading', { name: 'Temporal' })).not.toBeInTheDocument(),
    );
    expect(screen.getByText(/Todavía no tenés sistemas/)).toBeInTheDocument();
  });
});
