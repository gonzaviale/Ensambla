import userEvent from '@testing-library/user-event';
import { crearProyecto } from '@/api/proyectos';
import { describe, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import {
  clickButton,
  setupUsuarioConSesion,
  setupUsuarioSinSesion,
  typeLabel,
  verifyAlertPresentAsync,
  verifyHeadingPresentAsync,
  verifyPathAsync,
  verifyTextPresentAsync,
} from '@/test/testHelpers';

describe('navegación y guards', () => {
  it('muestra la landing a quien no tiene sesión', async () => {
    renderApp('/');

    await verifyHeadingPresentAsync('Tu sistema, un bloque a la vez.');
  });

  it('manda al login a quien entra a una pantalla privada sin sesión', async () => {
    const router = renderApp('/proyectos');

    await verifyHeadingPresentAsync('Ingresá a tu cuenta');
    await verifyPathAsync(router, '/login');
  });

  it('lleva a Mis proyectos a quien ya tiene sesión y entra a la landing', async () => {
    setupUsuarioConSesion();
    const router = renderApp('/');

    await verifyPathAsync(router, '/proyectos');
    await verifyHeadingPresentAsync('¿Por dónde querés empezar?');
  });

  it('lista las plantillas del backend en Mis proyectos', async () => {
    setupUsuarioConSesion();
    renderApp('/proyectos');

    await verifyHeadingPresentAsync('Plantillas disponibles');
    await verifyTextPresentAsync('Controlá tus productos, su stock y el costo de cada uno.');
  });

  it('abre el constructor de un proyecto con sesión', async () => {
    setupUsuarioConSesion();
    const proyecto = await crearProyecto({ nombre: 'Mi proyecto' });
    renderApp(`/proyectos/${proyecto.id}/construir`);

    await verifyHeadingPresentAsync('Constructor de bloques y campos');
  });

  it('inicia sesión y vuelve a la pantalla que se quería ver', async () => {
    const user = userEvent.setup();
    setupUsuarioSinSesion({ email: 'lucia@example.com', contrasenia: 'una-contrasenia-segura' });
    const router = renderApp('/proyectos');
    await verifyHeadingPresentAsync('Ingresá a tu cuenta');

    await typeLabel(user, 'Email', 'lucia@example.com');
    await typeLabel(user, 'Contraseña', 'una-contrasenia-segura');
    await clickButton(user, 'Ingresar');

    await verifyPathAsync(router, '/proyectos');
    await verifyHeadingPresentAsync('¿Por dónde querés empezar?');
  });

  it('avisa cuando las credenciales no son correctas', async () => {
    const user = userEvent.setup();
    setupUsuarioSinSesion({ email: 'lucia@example.com' });
    renderApp('/login');
    await verifyHeadingPresentAsync('Ingresá a tu cuenta');

    await typeLabel(user, 'Email', 'lucia@example.com');
    await typeLabel(user, 'Contraseña', 'incorrecta');
    await clickButton(user, 'Ingresar');

    await verifyAlertPresentAsync('El email o la contraseña no son correctos.');
  });
});
