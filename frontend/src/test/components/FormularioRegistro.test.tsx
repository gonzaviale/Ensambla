import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { FormularioRegistro } from '@/components/FormRenderer/FormularioRegistro';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { campoPorTipo, camposFormulario } from '@/test/formularioTestData';
import { setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';
import type { Proyecto } from '@/types/proyecto';

function preparar() {
  const usuario = setupUsuarioConSesion();
  const bloque = {
    id: crypto.randomUUID(),
    nombre: 'Productos',
    posicion: 0,
    campos: camposFormulario,
  };
  const proyecto: Proyecto = {
    id: crypto.randomUUID(),
    nombre: 'Taller',
    descripcion: null,
    plantillaId: null,
    creadoEn: '',
    actualizadoEn: '',
    bloques: [bloque],
  };
  estado.proyectos.set(proyecto.id, { usuarioId: usuario.id, proyecto });
  render(
    <QueryClientProvider client={new QueryClient()}>
      <FormularioRegistro bloque={bloque} />
    </QueryClientProvider>,
  );
  return bloque;
}

describe('ENS-53: guardado y feedback', () => {
  it('guarda una sola vez ante envíos inmediatos y deshabilita los controles hasta recibir respuesta', async () => {
    const bloque = preparar();
    let terminar: (() => void) | undefined;
    const espera = new Promise<void>((resolve) => {
      terminar = resolve;
    });
    let solicitudes = 0;
    server.use(
      http.post(`/api/bloques/${bloque.id}/registros`, async () => {
        solicitudes++;
        await espera;
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
    const boton = screen.getByRole('button', { name: 'Guardar fila' });
    const form = boton.closest('form');
    if (!form) throw new Error('Falta formulario');
    fireEvent.submit(form);
    fireEvent.submit(form);
    expect(screen.getByRole('button', { name: 'Guardando…' })).toBeDisabled();
    expect(screen.getByLabelText('Nombre')).toBeDisabled();
    await waitFor(() => expect(solicitudes).toBe(1));
    terminar?.();
    expect(await screen.findByRole('status')).toHaveTextContent('Fila guardada');
    expect(screen.getByRole('button', { name: 'Guardar fila' })).toBeEnabled();
    expect(solicitudes).toBe(1);
  });

  it('ubica los errores del servidor junto a cada campo y conserva lo escrito para corregirlo', async () => {
    const bloque = preparar();
    const user = userEvent.setup();
    const nombre = screen.getByLabelText('Nombre');
    await user.clear(nombre);
    await user.type(nombre, 'Mi producto');
    setupServerError('post', `/bloques/${bloque.id}/registros`, 400, 'Revisá el nombre', {
      [campoPorTipo('TEXTO').id]: 'Este nombre ya existe',
    });
    await user.click(screen.getByRole('button', { name: 'Guardar fila' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Revisá el nombre');
    expect(nombre).toHaveValue('Mi producto');
    expect(nombre).toHaveAccessibleDescription('Este nombre ya existe');
    expect(nombre).toHaveFocus();
    expect(screen.getByLabelText('Precio')).toHaveValue('0');
    server.resetHandlers();
    await user.clear(nombre);
    await user.type(nombre, 'Otro producto');
    await user.click(screen.getByRole('button', { name: 'Guardar fila' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Fila guardada');
    expect(estado.registros.size).toBe(1);
    expect(nombre).toHaveValue('Producto');
    await user.click(screen.getByRole('button', { name: 'Cerrar aviso' }));
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it.each(['servidor', 'conexión'] as const)(
    'conserva datos y no reintenta automáticamente ante un error de %s',
    async (tipo) => {
      const bloque = preparar();
      const user = userEvent.setup();
      let solicitudes = 0;
      server.use(
        http.post(`/api/bloques/${bloque.id}/registros`, () => {
          solicitudes++;
          return tipo === 'conexión'
            ? HttpResponse.error()
            : HttpResponse.json(
                {
                  status: 500,
                  message: 'Servicio no disponible',
                  path: '/',
                  timestamp: '',
                  traceId: 'traza-test',
                  error: 'Error',
                },
                { status: 500 },
              );
        }),
      );
      await user.clear(screen.getByLabelText('Nombre'));
      await user.type(screen.getByLabelText('Nombre'), 'Conservar');
      await user.click(screen.getByRole('button', { name: 'Guardar fila' }));
      expect(await screen.findByRole('alert')).toHaveTextContent(
        tipo === 'conexión' ? 'No pudimos conectarnos' : 'Servicio no disponible',
      );
      expect(screen.getByLabelText('Nombre')).toHaveValue('Conservar');
      expect(screen.getByRole('button', { name: 'Guardar fila' })).toBeEnabled();
      expect(solicitudes).toBe(1);
    },
  );
});
