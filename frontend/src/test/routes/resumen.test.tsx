import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { ultimosDias } from '@/lib/sistema/periodoResumen';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';

const guardarArchivo = vi.hoisted(() => vi.fn());
vi.mock('@/lib/descargas', () => ({ guardarArchivo }));

const abrirAlmacen = async (premium: boolean) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  if (premium) {
    estado.pruebasPremium.set(usuario.email, {
      inicioEn: new Date(Date.now() - 1000).toISOString(),
      finEn: new Date(Date.now() + 7 * 86400000).toISOString(),
      paginasIaUtilizadas: 0,
      limitePaginasIa: 10,
    });
  }
  const sistema = crearAlmacenDeEjemplo(hoy());
  estado.sistemas.set(usuario.email, [sistema]);
  const user = userEvent.setup();
  renderApp(`/sistemas/${sistema.id}?modo=usar`);
  await screen.findByRole('heading', { level: 1, name: /Almacén de ejemplo/ }, ESPERA);
  await user.click(await screen.findByRole('button', { name: /Resumen en PDF/ }, ESPERA));
  const modal = await screen.findByRole('dialog', {}, ESPERA);
  // El botón espera a saber qué plan tiene la persona.
  if (premium) {
    await waitFor(
      () => expect(within(modal).getByRole('button', { name: /Descargar PDF/ })).toBeEnabled(),
      ESPERA,
    );
  }
  return { user, modal, sistema };
};

/** Escucha el pedido del PDF y guarda la dirección con la que llegó. */
const escucharPedidos = () => {
  const direcciones: URL[] = [];
  server.use(
    http.get('/api/proyectos/:sistemaId/resumen/pdf', ({ request }) => {
      direcciones.push(new URL(request.url));
      return new HttpResponse(new TextEncoder().encode('%PDF-1.4'), {
        headers: {
          'Content-Type': 'application/pdf',
          'Content-Disposition': 'attachment; filename="resumen-almacen-20261019.pdf"',
        },
      });
    }),
  );
  return direcciones;
};

describe('el resumen del negocio en PDF', () => {
  beforeEach(() => guardarArchivo.mockClear());

  it('con Premium se baja el PDF de los últimos 30 días y se cierra el modal', async () => {
    const direcciones = escucharPedidos();
    const { user, modal, sistema } = await abrirAlmacen(true);

    expect(
      within(modal).getByRole('heading', { name: 'Resumen de tu negocio en PDF' }),
    ).toBeVisible();
    expect(within(modal).getByLabelText('Período')).toHaveValue('30');
    await user.click(within(modal).getByRole('button', { name: /Descargar PDF/ }));

    await waitFor(() => expect(guardarArchivo).toHaveBeenCalledTimes(1), ESPERA);
    expect(guardarArchivo.mock.calls[0]?.[0]).toMatchObject({
      nombre: 'resumen-almacen-20261019.pdf',
    });
    expect(direcciones).toHaveLength(1);
    expect(direcciones[0]?.pathname).toBe(`/api/proyectos/${sistema.id}/resumen/pdf`);
    expect(direcciones[0]?.searchParams.get('desde')).toBe(ultimosDias(30).desde);
    expect(direcciones[0]?.searchParams.get('hasta')).toBe(hoy());
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument(), ESPERA);
  });

  it('el período se elige entre los últimos 7, 30 o 90 días', async () => {
    const direcciones = escucharPedidos();
    const { user, modal } = await abrirAlmacen(true);

    await user.selectOptions(within(modal).getByLabelText('Período'), '7');
    await user.click(within(modal).getByRole('button', { name: /Descargar PDF/ }));

    await waitFor(() => expect(direcciones).toHaveLength(1), ESPERA);
    expect(direcciones[0]?.searchParams.get('desde')).toBe(ultimosDias(7).desde);
  });

  it('también se pueden elegir las fechas, y se avisa si están al revés', async () => {
    const direcciones = escucharPedidos();
    const { user, modal } = await abrirAlmacen(true);

    await user.selectOptions(within(modal).getByLabelText('Período'), 'otro');
    await user.click(within(modal).getByRole('button', { name: /Descargar PDF/ }));
    expect(await within(modal).findByRole('alert')).toHaveTextContent('Elegí las dos fechas.');

    await user.type(within(modal).getByLabelText('Desde'), '2026-10-08');
    await user.type(within(modal).getByLabelText('Hasta'), '2026-10-07');
    expect(await within(modal).findByRole('alert')).toHaveTextContent(
      'La fecha desde no puede ser posterior a la fecha hasta.',
    );
    expect(direcciones).toHaveLength(0);

    await user.clear(within(modal).getByLabelText('Desde'));
    await user.type(within(modal).getByLabelText('Desde'), '2026-10-01');
    await user.click(within(modal).getByRole('button', { name: /Descargar PDF/ }));

    await waitFor(() => expect(direcciones).toHaveLength(1), ESPERA);
    expect(direcciones[0]?.searchParams.get('desde')).toBe('2026-10-01');
    expect(direcciones[0]?.searchParams.get('hasta')).toBe('2026-10-07');
  });

  it('si el servidor no puede armarlo, lo dice y el modal sigue abierto', async () => {
    setupServerError('get', '/proyectos/:sistemaId/resumen/pdf', 500, 'Error interno');
    const { user, modal } = await abrirAlmacen(true);

    await user.click(within(modal).getByRole('button', { name: /Descargar PDF/ }));

    expect(await within(modal).findByRole('alert')).toBeVisible();
    expect(guardarArchivo).not.toHaveBeenCalled();
    expect(screen.getByRole('dialog')).toBeVisible();
  });

  it('sin Premium el modal explica que es una función paga y lleva a los planes, sin ofrecer la descarga', async () => {
    const { user, modal } = await abrirAlmacen(false);

    expect(await within(modal).findByText('El resumen en PDF es parte de Premium.')).toBeVisible();
    expect(within(modal).queryByRole('button', { name: /Descargar PDF/ })).not.toBeInTheDocument();
    await user.click(within(modal).getByRole('link', { name: 'Ver planes' }));

    expect(
      await screen.findByRole('heading', { name: 'Gratuito, tu espacio actual' }, ESPERA),
    ).toBeVisible();
  });

  it('el botón también está al editar la sección', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    const sistema = crearAlmacenDeEjemplo(hoy());
    estado.sistemas.set(usuario.email, [sistema]);
    renderApp(`/sistemas/${sistema.id}?modo=construir`);
    await screen.findByRole('heading', { level: 1, name: /Almacén de ejemplo/ }, ESPERA);

    expect(screen.getByRole('button', { name: /Resumen en PDF/ })).toBeVisible();
  });
});
