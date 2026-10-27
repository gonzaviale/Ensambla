import { describe, expect, it } from 'vitest';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, delay, http } from 'msw';
import { axe } from 'vitest-axe';
import { consultarRenovacion } from '@/api/pagos';
import { estado } from '@/mocks/db/estado';
import { planesDe } from '@/mocks/db/planes';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import { setupUsuarioConSesion } from '@/test/testHelpers';
import type { Renovacion } from '@/types/pagos';

const ESPERA = { timeout: 10_000 };
const FIN_PRUEBA = new Date(Date.now() + 4 * 24 * 60 * 60 * 1000).toISOString();
const contratoDeEjemplo = (): NonNullable<Renovacion['contrato']> => ({
  id: crypto.randomUUID(),
  estado: 'PENDIENTE',
  importe: 14900,
  moneda: 'ARS',
  primerCobroEn: FIN_PRUEBA,
  proximoCobroEn: null,
  cancelacionSolicitada: false,
  enlace: 'https://www.mercadopago.com.ar/subscriptions/checkout?preapproval_id=contrato-local',
});
const esperarPlanes = () =>
  screen.findByRole('heading', { name: /tu espacio actual|Tu prueba Premium está activa/ }, ESPERA);

function simularRenovacion(datos: Renovacion) {
  server.use(http.get('/api/planes/renovacion', () => HttpResponse.json(datos)));
}

describe('contratación y renovación automática', () => {
  it('muestra el precio aprobado y mantiene deshabilitada la contratación sin configurar Mercado Pago', async () => {
    setupUsuarioConSesion();
    renderApp('/planes');
    await esperarPlanes();

    expect(await screen.findByRole('button', { name: 'Contratar Premium' }, ESPERA)).toBeDisabled();
    expect(screen.getByText('Cada 30 días · renovación automática')).toBeVisible();
    expect(screen.getByText(/14\.900/)).toBeVisible();
  });

  it('explica que el primer cobro conserva los días de prueba y permite volver sin contratar', async () => {
    const usuario = setupUsuarioConSesion();
    estado.pruebasPremium.set(usuario.email, {
      inicioEn: new Date(Date.now() - 3 * 86400000).toISOString(),
      finEn: FIN_PRUEBA,
      paginasIaUtilizadas: 8,
      limitePaginasIa: 10,
    });
    simularRenovacion({ habilitada: true, periodoDias: 30, contrato: null });
    renderApp('/planes');
    await esperarPlanes();
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Contratar Premium' }, ESPERA));

    const dialogo = screen.getByRole('dialog', { name: 'Contratar Premium' });
    expect(within(dialogo).getByText(/14\.900.*finales cada 30 días/)).toBeVisible();
    expect(within(dialogo).getByText(/El primer cobro se programa para/)).toBeVisible();
    expect(within(dialogo).getByText(/diez páginas totales/)).toBeVisible();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(screen.getByRole('button', { name: 'Contratar Premium' })).toHaveFocus();
  });

  it('prepara una sola contratación con CSRF sin enviar precio ni dar acceso pago desde el retorno', async () => {
    setupUsuarioConSesion();
    const datos: Renovacion = { habilitada: true, periodoDias: 30, contrato: null };
    simularRenovacion(datos);
    let intentos = 0;
    let token: string | null = null;
    let body = '';
    server.use(
      http.post('/api/planes/renovacion', async ({ request }) => {
        intentos++;
        token = request.headers.get('X-CSRF-TOKEN');
        body = await request.text();
        await delay(180);
        datos.contrato = contratoDeEjemplo();
        return HttpResponse.json(datos);
      }),
    );
    renderApp('/planes?status=approved');
    await esperarPlanes();
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Contratar Premium' }, ESPERA));

    await user.dblClick(screen.getByRole('button', { name: 'Preparar contratación' }));

    expect(
      await screen.findByRole('link', { name: 'Continuar en Mercado Pago' }, ESPERA),
    ).toHaveAttribute(
      'href',
      'https://www.mercadopago.com.ar/subscriptions/checkout?preapproval_id=contrato-local',
    );
    expect(intentos).toBe(1);
    expect(token).toBeTruthy();
    expect(body).toBe('');
    expect(screen.getByRole('heading', { name: 'Gratuito, tu espacio actual' })).toBeVisible();
    expect(estado.pruebasPremium.size).toBe(0);
  });

  it('una autorización confirmada sigue esperando el pago y no se presenta como período cobrado', async () => {
    setupUsuarioConSesion();
    const contrato = contratoDeEjemplo();
    contrato.estado = 'AUTORIZADA';
    simularRenovacion({ habilitada: true, periodoDias: 30, contrato });
    renderApp('/planes');
    await esperarPlanes();

    expect(await screen.findByText('Renovación automática autorizada.', {}, ESPERA)).toBeVisible();
    expect(screen.getByText(/El acceso pago se habilita al confirmar/)).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Gratuito, tu espacio actual' })).toBeVisible();
  });

  it('cancelar pide confirmación y conserva el Premium ya pagado', async () => {
    const usuario = setupUsuarioConSesion();
    const planes = planesDe(usuario.email);
    planes.suscripcion = {
      codigoPlanActual: 'PREMIUM',
      pruebaActiva: false,
      puedeIniciarPrueba: false,
      inicioEn: new Date().toISOString(),
      finEn: FIN_PRUEBA,
    };
    planes.uso.limitePaginasIa = 100;
    const contrato = contratoDeEjemplo();
    contrato.estado = 'AUTORIZADA';
    const datos: Renovacion = { habilitada: true, periodoDias: 30, contrato };
    simularRenovacion(datos);
    let cancelaciones = 0;
    server.use(
      http.get('/api/planes', () => HttpResponse.json(planes)),
      http.post('/api/planes/renovacion/cancelar', () => {
        cancelaciones++;
        contrato.cancelacionSolicitada = true;
        contrato.estado = 'CANCELADA';
        return HttpResponse.json(datos);
      }),
    );
    renderApp('/planes');
    await esperarPlanes();
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Cancelar renovación' }, ESPERA));
    const dialogo = screen.getByRole('dialog', { name: 'Cancelar renovación automática' });
    expect(within(dialogo).getByText(/período ya pagado/)).toBeVisible();
    expect(cancelaciones).toBe(0);
    await user.click(within(dialogo).getByRole('button', { name: 'Confirmar cancelación' }));

    expect(
      await screen.findByText('La renovación automática está cancelada.', {}, ESPERA),
    ).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Premium, tu espacio actual' })).toBeVisible();
    expect(screen.getByText(/Tu período pago está disponible hasta/)).toBeVisible();
    expect(cancelaciones).toBe(1);
  });

  it('una baja no confirmada muestra el error y conserva cancelación pendiente sin prometer que terminó', async () => {
    setupUsuarioConSesion();
    const contrato = contratoDeEjemplo();
    contrato.estado = 'AUTORIZADA';
    simularRenovacion({ habilitada: true, periodoDias: 30, contrato });
    server.use(
      http.post('/api/planes/renovacion/cancelar', () => {
        contrato.cancelacionSolicitada = true;
        return HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 502,
            error: 'Bad Gateway',
            message: 'Mercado Pago todavía no confirmó la baja.',
            path: '/api/planes/renovacion/cancelar',
            traceId: 'local',
          },
          { status: 502 },
        );
      }),
    );
    renderApp('/planes');
    await esperarPlanes();
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Cancelar renovación' }, ESPERA));

    await user.click(screen.getByRole('button', { name: 'Confirmar cancelación' }));

    expect(await screen.findByRole('alert', {}, ESPERA)).toHaveTextContent('todavía no confirmó');
    expect(screen.getByText('Cancelación pendiente de confirmación.')).toBeVisible();
    expect(screen.queryByText('La renovación automática está cancelada.')).toBeNull();
  });

  it('rechaza enlaces de pago que no pertenecen al checkout de Mercado Pago', async () => {
    setupUsuarioConSesion();
    const contrato = contratoDeEjemplo();
    contrato.enlace = 'https://un-sitio-ajeno.example/robar-datos';
    simularRenovacion({ habilitada: true, periodoDias: 30, contrato });

    await expect(consultarRenovacion()).rejects.toThrow();
  });

  it('una falla al consultar permite reintentar y no muestra un botón habilitado con datos inventados', async () => {
    setupUsuarioConSesion();
    server.use(http.get('/api/planes/renovacion', () => new HttpResponse(null, { status: 503 })));
    renderApp('/planes');
    await esperarPlanes();

    expect(
      await screen.findByRole('button', { name: 'Reintentar renovación' }, ESPERA),
    ).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Contratar Premium' })).toBeNull();
  });

  it('la confirmación de contratación conserva las reglas de accesibilidad', async () => {
    setupUsuarioConSesion();
    simularRenovacion({ habilitada: true, periodoDias: 30, contrato: null });
    renderApp('/planes');
    await esperarPlanes();
    await userEvent
      .setup()
      .click(await screen.findByRole('button', { name: 'Contratar Premium' }, ESPERA));

    const resultado = await axe(document.body, {
      rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
    });

    expect(resultado.violations.map((item) => item.id)).toEqual([]);
  });
});
