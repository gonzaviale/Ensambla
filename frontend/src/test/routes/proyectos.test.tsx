import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { crearProyecto } from '@/api/proyectos';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupUsuarioConSesion,
  setupServerError,
  verifyHeadingPresentAsync,
} from '@/test/testHelpers';

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

  it('elige una plantilla y muestra los bloques y campos copiados', async () => {
    setupUsuarioConSesion();
    const user = userEvent.setup();
    renderApp('/proyectos');
    await verifyHeadingPresentAsync('Plantillas disponibles');

    await user.click(screen.getByRole('button', { name: 'Usar plantilla Inventario' }));

    await verifyHeadingPresentAsync('Inventario');
    await verifyHeadingPresentAsync('Productos');
    expect(await screen.findByText('Stock', {}, ESPERA)).toBeInTheDocument();
    expect(screen.getByText('Rubro')).toBeInTheDocument();
  });

  it('lista proyectos existentes y los abre', async () => {
    setupUsuarioConSesion();
    await crearProyecto({ nombre: 'Mi almacén' });
    const user = userEvent.setup();
    renderApp('/proyectos');
    await verifyHeadingPresentAsync('Mi almacén');

    await user.click(screen.getByRole('link', { name: 'Abrir Mi almacén' }));

    await verifyHeadingPresentAsync('Tus bloques');
  });

  it('renombra un proyecto desde su tarjeta', async () => {
    setupUsuarioConSesion();
    await crearProyecto({ nombre: 'Original' });
    const user = userEvent.setup();
    renderApp('/proyectos');
    await verifyHeadingPresentAsync('Original');

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

    await user.click(screen.getByRole('button', { name: 'Eliminar' }));
    expect(screen.getByRole('heading', { name: 'Temporal' })).toBeInTheDocument();
    expect(screen.getByText(/Esta acción no se puede deshacer/)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Sí, eliminar proyecto' }));

    await waitFor(() =>
      expect(screen.queryByRole('heading', { name: 'Temporal' })).not.toBeInTheDocument(),
    );
    expect(screen.getByText(/Todavía no tenés proyectos/)).toBeInTheDocument();
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
