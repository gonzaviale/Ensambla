import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  negocioDePrueba,
  setupServerError,
  setupUsuarioSinSesion,
  typeLabel,
  verifyAlertPresentAsync,
  verifyPathAsync,
} from '@/test/testHelpers';

const USUARIO = { email: 'dos-negocios@example.com', contrasenia: 'una-contrasenia-segura' };

const ALMACEN = negocioDePrueba({
  id: 'almacen',
  nombre: 'Almacén San Justo',
  rubro: 'ALMACEN',
  creadoEn: '2026-10-02T14:03:11Z',
});
const KIOSCO = negocioDePrueba({
  id: 'kiosco',
  nombre: 'Kiosco de la estación',
  rubro: 'KIOSCO',
  creadoEn: '2026-11-05T09:41:57Z',
});

const conNegocios = (...negocios: (typeof ALMACEN)[]) => {
  setupUsuarioSinSesion(USUARIO);
  const activoId = negocios[0]?.id ?? ALMACEN.id;
  estado.negociosDeLaCuenta.set(USUARIO.email, { activoId, negocios });
};

const iniciarSesion = async (ruta = '/login') => {
  const router = renderApp(ruta);
  const user = userEvent.setup();
  await screen.findByRole('heading', { name: 'Ingresá a tu cuenta' }, ESPERA);
  await typeLabel(user, 'Email', USUARIO.email);
  await typeLabel(user, 'Contraseña', USUARIO.contrasenia);
  await user.click(screen.getByRole('button', { name: 'Ingresar' }));
  return { router, user };
};

const popUp = () => screen.findByRole('dialog', { name: '¿Con qué negocio trabajás hoy?' }, ESPERA);

const activoDelServidor = () => estado.negociosDeLaCuenta.get(USUARIO.email)?.activoId;

describe('elegir con qué negocio trabajar al iniciar sesión', () => {
  it('con un solo negocio entra directo, sin preguntar', async () => {
    conNegocios(ALMACEN);
    const { router } = await iniciarSesion();

    await verifyPathAsync(router, '/inicio');
    expect(screen.queryByRole('dialog', { name: '¿Con qué negocio trabajás hoy?' })).toBeNull();
  });

  it('con más de uno pregunta antes de entrar y activa el elegido', async () => {
    conNegocios(ALMACEN, KIOSCO);
    const { router, user } = await iniciarSesion();

    const dialogo = await popUp();
    expect(dialogo).toHaveTextContent('Tenés 2 negocios');
    expect(router.state.location.pathname).toBe('/elegir-negocio');
    expect(screen.queryByRole('heading', { name: 'Ingresá a tu cuenta' })).toBeNull();
    const kiosco = await within(dialogo).findByRole(
      'button',
      { name: 'Kiosco de la estación' },
      ESPERA,
    );
    expect(
      within(dialogo).getByRole('button', { name: 'Almacén San Justo' }),
    ).toHaveAccessibleDescription(/El último que usaste/);
    await user.click(kiosco);

    await verifyPathAsync(router, '/inicio');
    expect(activoDelServidor()).toBe(KIOSCO.id);
  });

  it('elegir el último que se usó o «Seguir con…» entra sin cambiar de negocio', async () => {
    conNegocios(ALMACEN, KIOSCO);
    let cambios = 0;
    server.events.on('request:start', ({ request }) => {
      if (request.method === 'PUT' && request.url.endsWith('/activo')) cambios += 1;
    });
    const { router, user } = await iniciarSesion();

    const dialogo = await popUp();
    await user.click(within(dialogo).getByRole('button', { name: 'Seguir con Almacén San Justo' }));

    await verifyPathAsync(router, '/inicio');
    expect(activoDelServidor()).toBe(ALMACEN.id);
    expect(cambios).toBe(0);
    server.events.removeAllListeners();
  });

  it('Esc entra con el último negocio que se usó', async () => {
    conNegocios(ALMACEN, KIOSCO);
    const { router, user } = await iniciarSesion();
    await popUp();

    await user.keyboard('{Escape}');

    await verifyPathAsync(router, '/inicio');
    expect(activoDelServidor()).toBe(ALMACEN.id);
  });

  it('Esc con el cambio de negocio en camino no entra a la app antes de que termine', async () => {
    conNegocios(ALMACEN, KIOSCO);
    let terminar: () => void = () => undefined;
    const demora = new Promise<void>((resolver) => {
      terminar = resolver;
    });
    server.use(
      http.put('/api/negocios/:id/activo', async () => {
        await demora;
        estado.negociosDeLaCuenta.set(USUARIO.email, {
          activoId: KIOSCO.id,
          negocios: [ALMACEN, KIOSCO],
        });
        return HttpResponse.json({
          activoId: KIOSCO.id,
          limite: 6,
          puedeCrear: true,
          negocios: [
            { ...ALMACEN, activo: false },
            { ...KIOSCO, activo: true },
          ],
        });
      }),
    );
    const { router, user } = await iniciarSesion();
    const dialogo = await popUp();
    await user.click(
      await within(dialogo).findByRole('button', { name: 'Kiosco de la estación' }, ESPERA),
    );

    await user.keyboard('{Escape}');
    expect(router.state.location.pathname).toBe('/elegir-negocio');
    terminar();

    await verifyPathAsync(router, '/inicio');
    expect(activoDelServidor()).toBe(KIOSCO.id);
  });

  it('si no se pudo cambiar de negocio lo dice y deja seguir', async () => {
    conNegocios(ALMACEN, KIOSCO);
    server.use(
      http.put('/api/negocios/:id/activo', () =>
        HttpResponse.json({ status: 500, message: 'Error interno' }, { status: 500 }),
      ),
    );
    const { router, user } = await iniciarSesion();

    const dialogo = await popUp();
    await user.click(
      await within(dialogo).findByRole('button', { name: 'Kiosco de la estación' }, ESPERA),
    );

    await verifyAlertPresentAsync('No pudimos abrir ese negocio.');
    expect(router.state.location.pathname).toBe('/elegir-negocio');
    await user.click(within(dialogo).getByRole('button', { name: 'Seguir con Almacén San Justo' }));
    await verifyPathAsync(router, '/inicio');
  });

  it('después de elegir va a donde se quería ir antes de ingresar', async () => {
    conNegocios(ALMACEN, KIOSCO);
    const { router, user } = await iniciarSesion('/login?redirect=%2Fplanes');

    const dialogo = await popUp();
    await user.click(within(dialogo).getByRole('button', { name: 'Seguir con Almacén San Justo' }));

    await verifyPathAsync(router, '/planes');
  });

  it('si no llega la lista de negocios entra igual', async () => {
    conNegocios(ALMACEN, KIOSCO);
    setupServerError('get', '/negocios', 500, 'Error interno');
    const { router } = await iniciarSesion();

    await verifyPathAsync(router, '/inicio');
  });
});
