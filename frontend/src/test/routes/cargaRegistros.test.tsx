import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { delay, http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { crearProyecto, crearProyectoDesdePlantilla } from '@/api/proyectos';
import { estado, plantillasMock } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import {
  setupServerError,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
} from '@/test/testHelpers';
import type { Proyecto } from '@/types/proyecto';

async function prepararInventario() {
  setupUsuarioConSesion();
  const plantilla = plantillasMock.find((item) => item.nombre === 'Inventario');
  if (!plantilla) throw new Error('Falta Inventario');
  return crearProyectoDesdePlantilla(plantilla.id);
}

function primerBloque(proyecto: Proyecto) {
  const bloque = proyecto.bloques[0];
  if (!bloque) throw new Error('Falta el bloque');
  return bloque;
}

describe('ENS-6: carga de registros por bloque', () => {
  it('guarda un registro sin herramientas de edición', async () => {
    const proyecto = await prepararInventario();
    const user = userEvent.setup();
    renderApp(`/proyectos/${proyecto.id}/usar`);
    await verifyHeadingPresentAsync('Inventario');
    await user.click(await screen.findByRole('tab', { name: 'Cargar' }));
    expect(screen.queryByText('Constructor de bloques y campos')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Stock (obligatorio)')).toHaveValue(0);
    await user.type(screen.getByLabelText('Nombre (obligatorio)'), 'Harina');
    await user.selectOptions(screen.getByLabelText('Rubro'), 'Insumos');
    await user.click(screen.getByRole('button', { name: 'Guardar registro' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Registro guardado');
    expect(estado.registros.size).toBe(1);
    const registro = [...estado.registros.values()][0];
    expect(registro?.bloqueId).toBe(primerBloque(proyecto).id);
    expect(Object.values(registro?.valores ?? {})).toEqual(['Harina', 0, 'Insumos']);
    expect(screen.getByLabelText('Nombre (obligatorio)')).toHaveValue('');
    expect(screen.getByLabelText('Stock (obligatorio)')).toHaveValue(0);
  });

  it('conserva un borrador al cambiar de bloque y guarda en el bloque seleccionado', async () => {
    const proyecto = await prepararInventario();
    const original = primerBloque(proyecto);
    const segundo = {
      id: crypto.randomUUID(),
      nombre: 'Clientes',
      posicion: 1,
      campos: [
        {
          ...original.campos[0],
          id: crypto.randomUUID(),
          nombre: 'Cliente',
          tipo: 'TEXTO' as const,
          obligatorio: true,
          valorInicial: null,
          opciones: [],
          posicion: 0,
        },
      ],
    };
    proyecto.bloques.push(segundo);
    const item = estado.proyectos.get(proyecto.id);
    if (!item) throw new Error('Falta el proyecto simulado');
    item.proyecto = proyecto;
    const user = userEvent.setup();
    renderApp(`/proyectos/${proyecto.id}/usar`);
    await user.click(await screen.findByRole('tab', { name: 'Cargar' }));
    await verifyHeadingPresentAsync('Productos');
    await user.type(screen.getByLabelText('Nombre (obligatorio)'), 'Borrador');
    await user.click(screen.getByRole('radio', { name: 'Clientes' }));
    const clientes = screen.getByRole('region', { name: 'Clientes' });
    await user.type(within(clientes).getByLabelText('Cliente (obligatorio)'), 'Ana');
    await user.click(within(clientes).getByRole('button', { name: 'Guardar registro' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Registro guardado');
    expect([...estado.registros.values()][0]?.bloqueId).toBe(segundo.id);
    await user.click(screen.getByRole('radio', { name: 'Productos' }));
    expect(screen.getByLabelText('Nombre (obligatorio)')).toHaveValue('Borrador');
  });

  it('bloquea el selector durante el guardado', async () => {
    const proyecto = await prepararInventario();
    const bloque = primerBloque(proyecto);
    server.use(
      http.post(`/api/bloques/${bloque.id}/registros`, async () => {
        await delay(250);
        return HttpResponse.json(
          {
            id: crypto.randomUUID(),
            bloqueId: bloque.id,
            valores: {},
            creadoEn: '',
            actualizadoEn: '',
          },
          { status: 201 },
        );
      }),
    );
    const user = userEvent.setup();
    renderApp(`/proyectos/${proyecto.id}/usar`);
    await user.click(await screen.findByRole('tab', { name: 'Cargar' }));
    await verifyHeadingPresentAsync('Productos');
    await user.type(screen.getByLabelText('Nombre (obligatorio)'), 'Producto');
    await user.click(screen.getByRole('button', { name: 'Guardar registro' }));
    expect(screen.getByRole('radio', { name: 'Productos' })).toBeDisabled();
    await waitFor(() => expect(screen.getByRole('radio', { name: 'Productos' })).toBeEnabled());
  });

  it('explica un proyecto sin bloques', async () => {
    setupUsuarioConSesion();
    const proyecto = await crearProyecto({ nombre: 'Vacío' });
    renderApp(`/proyectos/${proyecto.id}/usar`);
    await verifyHeadingPresentAsync('Este proyecto no tiene bloques');
    expect(screen.queryByRole('button', { name: 'Guardar registro' })).not.toBeInTheDocument();
  });

  it('no ofrece guardado si el bloque no tiene campos', async () => {
    const proyecto = await prepararInventario();
    primerBloque(proyecto).campos = [];
    const item = estado.proyectos.get(proyecto.id);
    if (!item) throw new Error('Falta el proyecto simulado');
    item.proyecto = proyecto;
    const user = userEvent.setup();
    renderApp(`/proyectos/${proyecto.id}/usar`);
    await user.click(await screen.findByRole('tab', { name: 'Cargar' }));
    await verifyHeadingPresentAsync('Productos');
    expect(screen.getByText(/todavía no tiene datos/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Guardar registro' })).not.toBeInTheDocument();
  });

  it('conserva el formulario ante errores de la API', async () => {
    const proyecto = await prepararInventario();
    setupServerError(
      'post',
      `/bloques/${primerBloque(proyecto).id}/registros`,
      500,
      'No pudimos guardar',
    );
    const user = userEvent.setup();
    renderApp(`/proyectos/${proyecto.id}/usar`);
    await user.click(await screen.findByRole('tab', { name: 'Cargar' }));
    await verifyHeadingPresentAsync('Productos');
    await user.type(screen.getByLabelText('Nombre (obligatorio)'), 'Conservar');
    await user.click(screen.getByRole('button', { name: 'Guardar registro' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos guardar');
    expect(screen.getByLabelText('Nombre (obligatorio)')).toHaveValue('Conservar');
    expect(estado.registros.size).toBe(0);
  });

  it('redirige al login al entrar sin sesión', async () => {
    renderApp(`/proyectos/${crypto.randomUUID()}/usar`);
    await verifyHeadingPresentAsync('Ingresá a tu cuenta');
  });

  it('no carga el formulario de un proyecto ajeno', async () => {
    const proyecto = await prepararInventario();
    setupUsuarioConSesion({ email: 'otro@example.com' });
    renderApp(`/proyectos/${proyecto.id}/usar`);
    await verifyHeadingPresentAsync('No encontramos esta página');
    expect(screen.queryByRole('button', { name: 'Guardar registro' })).not.toBeInTheDocument();
  });
});
