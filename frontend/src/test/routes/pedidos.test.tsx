import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { estado, guardarOrdenesDe, ordenesDe } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupServerError,
  setupServerResponse,
  setupUsuarioConSesion,
} from '@/test/testHelpers';
import type { OrdenDelServidor } from '@/types/pedidos';

const orden = (parcial: Partial<OrdenDelServidor> = {}): OrdenDelServidor => ({
  id: crypto.randomUUID(),
  clienteTelefono: '5491155550001',
  estado: 'PENDIENTE',
  items: [{ nombre: 'Yerba Playadito 1 kg', cantidad: 2, precioUnitario: 3500, subtotal: 7000 }],
  total: 7000,
  motivoRechazo: null,
  creadoEn: '2026-11-11T12:00:00Z',
  actualizadoEn: '2026-11-11T12:00:00Z',
  ...parcial,
});

const tarjetaDe = (telefono: string) => screen.getByRole('article', { name: `+${telefono}` });

describe('pedidos', () => {
  it('se abre desde la navegación principal', async () => {
    setupUsuarioConSesion();
    const router = renderApp('/inicio');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Pedidos' }, ESPERA));
    await screen.findByRole('heading', { name: 'Pedidos de tus clientes' }, ESPERA);

    expect(router.state.location.pathname).toBe('/pedidos');
    expect(screen.getByRole('button', { name: 'Pedidos' })).toHaveAttribute('aria-current', 'page');
  });

  it('sin pedidos explica de dónde llegan', async () => {
    setupUsuarioConSesion();
    renderApp('/pedidos');

    expect(
      await screen.findByRole('heading', { name: 'Todavía no te llegaron pedidos' }, ESPERA),
    ).toBeVisible();
    expect(screen.queryByRole('group', { name: 'Filtrar pedidos' })).not.toBeInTheDocument();
  });

  it('muestra cada pedido con quién lo hizo, qué pidió, el total y en qué etapa está', async () => {
    const { email } = setupUsuarioConSesion();
    guardarOrdenesDe(email, [
      orden({
        items: [
          { nombre: 'Yerba Playadito 1 kg', cantidad: 2, precioUnitario: 3500, subtotal: 7000 },
          { nombre: 'Aceite Cocinero 900 ml', cantidad: 1, precioUnitario: 2000, subtotal: 2000 },
        ],
        total: 9000,
      }),
    ]);
    renderApp('/pedidos');

    const tarjeta = within(await screen.findByRole('article', { name: '+5491155550001' }, ESPERA));
    expect(tarjeta.getByText('2 × Yerba Playadito 1 kg')).toBeVisible();
    expect(tarjeta.getByText('$7.000')).toBeVisible();
    expect(tarjeta.getByText('1 × Aceite Cocinero 900 ml')).toBeVisible();
    expect(tarjeta.getByText('$9.000')).toBeVisible();
    expect(tarjeta.getByText('Pendiente')).toBeVisible();
    expect(tarjeta.getByRole('button', { name: 'Aceptar' })).toBeEnabled();
    expect(tarjeta.getByRole('button', { name: 'Rechazar' })).toBeEnabled();
  });

  it('no muestra los pedidos de otra cuenta', async () => {
    const { email } = setupUsuarioConSesion();
    guardarOrdenesDe('otra@ensambla.test', [orden({ clienteTelefono: '5491100000099' })]);
    guardarOrdenesDe(email, [orden({ clienteTelefono: '5491155550001' })]);
    renderApp('/pedidos');

    await screen.findByRole('article', { name: '+5491155550001' }, ESPERA);
    expect(screen.queryByRole('article', { name: '+5491100000099' })).not.toBeInTheDocument();
  });

  it('filtra por etapa y cuenta los pedidos de cada una', async () => {
    const { email } = setupUsuarioConSesion();
    guardarOrdenesDe(email, [
      orden({ clienteTelefono: '5491100000001', estado: 'PENDIENTE' }),
      orden({ clienteTelefono: '5491100000002', estado: 'EN_PREPARACION' }),
      orden({ clienteTelefono: '5491100000003', estado: 'LISTA' }),
      orden({ clienteTelefono: '5491100000004', estado: 'RECHAZADA', motivoRechazo: 'Sin stock' }),
    ]);
    renderApp('/pedidos');
    const user = userEvent.setup();

    await screen.findByRole('button', { name: 'Todos (4)' }, ESPERA);
    expect(screen.getByRole('button', { name: 'Pendientes (1)' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'En curso (1)' })).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'En curso (1)' }));
    expect(screen.getByRole('button', { name: 'En curso (1)' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    expect(screen.getAllByRole('article')).toHaveLength(1);
    expect(tarjetaDe('5491100000002')).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Rechazados (1)' }));
    expect(within(tarjetaDe('5491100000004')).getByText('Motivo: Sin stock')).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Todos (4)' }));
    expect(screen.getAllByRole('article')).toHaveLength(4);
  });

  it('una etapa sin pedidos lo dice', async () => {
    const { email } = setupUsuarioConSesion();
    guardarOrdenesDe(email, [orden()]);
    renderApp('/pedidos');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Listos (0)' }, ESPERA));

    expect(screen.getByText('No hay pedidos en esta etapa.')).toBeVisible();
  });

  it('acepta el pedido, lo pone en preparación y lo deja listo para retirar', async () => {
    const { email } = setupUsuarioConSesion();
    guardarOrdenesDe(email, [orden()]);
    renderApp('/pedidos');
    const user = userEvent.setup();

    await user.click(
      within(await screen.findByRole('article', { name: '+5491155550001' }, ESPERA)).getByRole(
        'button',
        {
          name: 'Aceptar',
        },
      ),
    );
    expect(
      await within(tarjetaDe('5491155550001')).findByText('Aceptado', undefined, ESPERA),
    ).toBeVisible();
    expect(
      within(tarjetaDe('5491155550001')).queryByRole('button', { name: 'Aceptar' }),
    ).not.toBeInTheDocument();

    await user.click(
      within(tarjetaDe('5491155550001')).getByRole('button', { name: 'Poner en preparación' }),
    );
    expect(
      await within(tarjetaDe('5491155550001')).findByText('En preparación', undefined, ESPERA),
    ).toBeVisible();

    await user.click(
      within(tarjetaDe('5491155550001')).getByRole('button', { name: 'Dejar lista para retirar' }),
    );
    expect(
      await within(tarjetaDe('5491155550001')).findByText('Lista para retirar', undefined, ESPERA),
    ).toBeVisible();
    expect(within(tarjetaDe('5491155550001')).queryByRole('button')).not.toBeInTheDocument();
    expect(ordenesDe(email)[0]?.estado).toBe('LISTA');
  });

  it('una orden aceptada también se puede dejar lista sin pasar por la preparación', async () => {
    const { email } = setupUsuarioConSesion();
    guardarOrdenesDe(email, [orden({ estado: 'ACEPTADA' })]);
    renderApp('/pedidos');
    const user = userEvent.setup();

    await user.click(
      await screen.findByRole('button', { name: 'Dejar lista para retirar' }, ESPERA),
    );

    expect(await screen.findByText('Lista para retirar', undefined, ESPERA)).toBeVisible();
  });

  it('rechaza el pedido con un motivo y lo muestra', async () => {
    const { email } = setupUsuarioConSesion();
    guardarOrdenesDe(email, [orden()]);
    renderApp('/pedidos');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Rechazar' }, ESPERA));
    await user.type(
      screen.getByLabelText('Motivo del rechazo (opcional)'),
      '  Hoy no me queda stock ',
    );
    await user.click(screen.getByRole('button', { name: 'Confirmar rechazo' }));

    expect(await screen.findByText('Rechazado', undefined, ESPERA)).toBeVisible();
    expect(screen.getByText('Motivo: Hoy no me queda stock')).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Confirmar rechazo' })).not.toBeInTheDocument();
    expect(ordenesDe(email)[0]?.motivoRechazo).toBe('Hoy no me queda stock');
  });

  it('rechaza sin explicar el motivo', async () => {
    const { email } = setupUsuarioConSesion();
    guardarOrdenesDe(email, [orden()]);
    renderApp('/pedidos');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Rechazar' }, ESPERA));
    await user.click(screen.getByRole('button', { name: 'Confirmar rechazo' }));

    expect(await screen.findByText('Rechazado', undefined, ESPERA)).toBeVisible();
    expect(screen.queryByText(/Motivo:/)).not.toBeInTheDocument();
  });

  it('Volver cancela el rechazo y deja los pasos como estaban', async () => {
    const { email } = setupUsuarioConSesion();
    guardarOrdenesDe(email, [orden()]);
    renderApp('/pedidos');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Rechazar' }, ESPERA));
    await user.click(screen.getByRole('button', { name: 'Volver' }));

    expect(screen.getByRole('button', { name: 'Aceptar' })).toBeVisible();
    expect(screen.queryByLabelText('Motivo del rechazo (opcional)')).not.toBeInTheDocument();
    expect(ordenesDe(email)[0]?.estado).toBe('PENDIENTE');
  });

  it('si el servidor no deja aceptar (por ejemplo sin stock) muestra el motivo y la orden sigue pendiente', async () => {
    const { email } = setupUsuarioConSesion();
    const pendiente = orden();
    guardarOrdenesDe(email, [pendiente]);
    setupServerError(
      'patch',
      `/ordenes/${pendiente.id}/estado`,
      400,
      'No alcanza el stock de Yerba Playadito 1 kg: hay 1 y la orden pide 2',
    );
    renderApp('/pedidos');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Aceptar' }, ESPERA));

    expect(await screen.findByRole('alert', undefined, ESPERA)).toHaveTextContent(
      'No alcanza el stock de Yerba Playadito 1 kg: hay 1 y la orden pide 2',
    );
    expect(screen.getByText('Pendiente')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Aceptar' })).toBeEnabled();
  });

  it('si Sami no pudo avisarle al cliente por WhatsApp, se lo dice al dueño con el número para que le avise', async () => {
    const { email } = setupUsuarioConSesion();
    const aceptada = orden({ estado: 'ACEPTADA' });
    guardarOrdenesDe(email, [aceptada]);
    setupServerResponse('patch', `/ordenes/${aceptada.id}/estado`, 200, {
      ...aceptada,
      estado: 'LISTA',
      clienteAvisado: false,
    });
    renderApp('/pedidos');
    const user = userEvent.setup();

    await user.click(
      await screen.findByRole('button', { name: 'Dejar lista para retirar' }, ESPERA),
    );

    const aviso = await screen.findByRole('alert', undefined, ESPERA);
    expect(aviso).toHaveTextContent('quedó lista para retirar');
    expect(aviso).toHaveTextContent('no pudimos avisarle a tu cliente por WhatsApp');
    expect(aviso).toHaveTextContent('+5491155550001');
  });

  it('cuando el aviso al cliente salió no muestra ninguna advertencia', async () => {
    const { email } = setupUsuarioConSesion();
    guardarOrdenesDe(email, [orden()]);
    renderApp('/pedidos');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Aceptar' }, ESPERA));

    expect(await screen.findByText('Aceptado', undefined, ESPERA)).toBeVisible();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  describe('el QR para que los clientes pidan por WhatsApp', () => {
    const vincular = (email: string, numeroBot = '5491155550000') =>
      estado.whatsapp.set(email, {
        vinculo: { telefono: '5491122334455', vinculadoEn: '2026-10-01T12:00:00Z', numeroBot },
        verificacion: null,
      });

    it('con el WhatsApp vinculado muestra el QR y el enlace que abre el chat con Sami, aun sin pedidos', async () => {
      vincular(setupUsuarioConSesion().email);
      renderApp('/pedidos');

      expect(
        await screen.findByRole(
          'img',
          { name: 'Código QR que abre el chat de WhatsApp con Sami' },
          ESPERA,
        ),
      ).toBeVisible();
      expect(screen.getByRole('link', { name: 'Abrir el chat con Sami' })).toHaveAttribute(
        'href',
        'https://wa.me/5491155550000?text=Hola%20Sami%2C%20quiero%20hacer%20un%20pedido',
      );
      expect(
        await screen.findByRole('heading', { name: 'Todavía no te llegaron pedidos' }, ESPERA),
      ).toBeVisible();
    });

    it('también aparece debajo de los pedidos', async () => {
      const { email } = setupUsuarioConSesion();
      vincular(email);
      guardarOrdenesDe(email, [orden()]);
      renderApp('/pedidos');

      await screen.findByRole('article', { name: '+5491155550001' }, ESPERA);
      expect(await screen.findByRole('img', { name: /Código QR/ }, ESPERA)).toBeVisible();
    });

    it('descarga el QR como un archivo SVG para imprimirlo', async () => {
      vincular(setupUsuarioConSesion().email);
      const crearUrl = vi.fn(() => 'blob:qr');
      const revocarUrl = vi.fn();
      vi.stubGlobal(
        'URL',
        Object.assign(URL, { createObjectURL: crearUrl, revokeObjectURL: revocarUrl }),
      );
      const clic = vi
        .spyOn(HTMLAnchorElement.prototype, 'click')
        .mockImplementation(() => undefined);
      renderApp('/pedidos');
      const user = userEvent.setup();

      await user.click(await screen.findByRole('button', { name: 'Descargar el QR' }, ESPERA));

      const [archivo] = crearUrl.mock.calls[0] as unknown as [Blob];
      expect(archivo.type).toBe('image/svg+xml');
      expect(await archivo.text()).toContain('<svg');
      expect(clic).toHaveBeenCalledTimes(1);
      expect((clic.mock.contexts[0] as HTMLAnchorElement).download).toBe('qr-pedidos-sami.svg');
      expect(revocarUrl).toHaveBeenCalledWith('blob:qr');
      clic.mockRestore();
      vi.unstubAllGlobals();
    });

    it('sin el WhatsApp vinculado pide vincularlo y lleva a Tu cuenta', async () => {
      setupUsuarioConSesion();
      const router = renderApp('/pedidos');
      const user = userEvent.setup();

      await user.click(await screen.findByRole('button', { name: 'Vincular mi WhatsApp' }, ESPERA));

      await waitFor(() => expect(router.state.location.pathname).toBe('/cuenta'), ESPERA);
      expect(router.state.location.search).toEqual({ seccion: 'whatsapp' });
      expect(screen.queryByRole('img', { name: /Código QR/ })).not.toBeInTheDocument();
    });

    it('si todavía no hay un número de Sami configurado lo explica y no muestra el QR', async () => {
      vincular(setupUsuarioConSesion().email, '');
      renderApp('/pedidos');

      expect(
        await screen.findByText(/Todavía no hay un número de Sami configurado/, undefined, ESPERA),
      ).toBeVisible();
      expect(screen.queryByRole('img', { name: /Código QR/ })).not.toBeInTheDocument();
      expect(
        screen.queryByRole('link', { name: 'Abrir el chat con Sami' }),
      ).not.toBeInTheDocument();
    });

    it('si no se puede consultar la vinculación no muestra el panel', async () => {
      setupUsuarioConSesion();
      setupServerError('get', '/whatsapp/vinculacion', 500, 'Error del servidor');
      renderApp('/pedidos');

      await screen.findByRole('heading', { name: 'Todavía no te llegaron pedidos' }, ESPERA);
      await waitFor(
        () =>
          expect(
            screen.queryByRole('heading', { name: /pidan|te pidan por WhatsApp/ }),
          ).not.toBeInTheDocument(),
        ESPERA,
      );
    });
  });

  it('si no se pueden cargar los pedidos lo avisa', async () => {
    setupUsuarioConSesion();
    setupServerError('get', '/ordenes', 500, 'Error del servidor');
    renderApp('/pedidos');

    expect(await screen.findByRole('alert', undefined, ESPERA)).toHaveTextContent(
      'No pudimos cargar tus pedidos',
    );
  });
});
