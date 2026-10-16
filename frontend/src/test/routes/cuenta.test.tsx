import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { crearProyecto, obtenerProyecto } from '@/api/proyectos';
import { useSuscripcionStore } from '@/stores/suscripcion';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  clickButton,
  clickLink,
  setupUsuarioConSesion,
  typeLabel,
  verifyHeadingPresentAsync,
  verifyPathAsync,
  verifyTextPresentAsync,
} from '@/test/testHelpers';

async function sinViolaciones() {
  const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });
  const resumen = resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`);
  expect(resumen).toEqual([]);
}

const suscripcion = () => useSuscripcionStore.getState().suscripcion;

async function abrirCuenta() {
  setupUsuarioConSesion();
  const proyecto = await crearProyecto({ nombre: 'Almacén Don Carlos' });
  const router = renderApp('/cuenta');
  await verifyHeadingPresentAsync('Mi cuenta');
  await screen.findByLabelText(/Nombre de tu negocio/);
  return { proyecto, router };
}

describe('Mi cuenta: tu negocio', () => {
  it('muestra quién ingresó y el nombre del sistema', async () => {
    await abrirCuenta();

    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    const panel = screen.getByRole('region', { name: 'Tu negocio' });
    expect(within(panel).getByText('Lucía Gómez')).toBeInTheDocument();
    expect(within(panel).getByText('lucia@example.com')).toBeInTheDocument();
    expect(screen.getByLabelText(/Nombre de tu negocio/)).toHaveValue('Almacén Don Carlos');
  });

  it('renombrar el sistema lo guarda y avisa', async () => {
    const user = userEvent.setup();
    const { proyecto } = await abrirCuenta();

    const nombre = screen.getByLabelText(/Nombre de tu negocio/);
    await user.clear(nombre);
    await user.type(nombre, 'Kiosco Doña Rosa');
    await clickButton(user, 'Guardar el nombre');

    await verifyTextPresentAsync('Listo, cambiamos el nombre de tu negocio.');
    expect((await obtenerProyecto(proyecto.id)).nombre).toBe('Kiosco Doña Rosa');
    expect(screen.getByLabelText(/Nombre de tu negocio/)).toHaveValue('Kiosco Doña Rosa');
  });

  it('el nombre es obligatorio y el foco va al campo con error', async () => {
    const user = userEvent.setup();
    const { proyecto } = await abrirCuenta();

    await user.clear(screen.getByLabelText(/Nombre de tu negocio/));
    await clickButton(user, 'Guardar el nombre');

    await verifyTextPresentAsync('Escribí cómo se llama tu negocio.');
    const campo = screen.getByLabelText(/Nombre de tu negocio/);
    expect(campo).toHaveAttribute('aria-invalid', 'true');
    await waitFor(() => expect(campo).toHaveFocus());
    expect((await obtenerProyecto(proyecto.id)).nombre).toBe('Almacén Don Carlos');
  });

  it('sin sistema ofrece armarlo desde la bienvenida', async () => {
    const user = userEvent.setup();
    setupUsuarioConSesion();
    const router = renderApp('/cuenta');
    await verifyHeadingPresentAsync('Mi cuenta');

    await user.click(await screen.findByRole('link', { name: 'Armar mi sistema' }, ESPERA));

    await verifyPathAsync(router, '/bienvenida');
  });
});

describe('Mi cuenta: plan, ayuda y privacidad', () => {
  it('muestra el plan y enlaza a /plan', async () => {
    const user = userEvent.setup();
    const { router } = await abrirCuenta();

    expect(screen.getByRole('heading', { name: 'Tu plan' })).toBeInTheDocument();
    expect(screen.getByText('Plan Negocio +')).toBeInTheDocument();
    expect(screen.getByText('Prueba gratis')).toBeInTheDocument();

    await clickLink(user, 'Ver o cambiar mi plan');

    await verifyPathAsync(router, '/plan');
  });

  it('la ayuda trae las preguntas de esta propuesta y la bienvenida', async () => {
    const user = userEvent.setup();
    const { router } = await abrirCuenta();

    for (const pregunta of [
      '¿Qué es un bloque?',
      '¿Cómo se arma mi sistema?',
      '¿Qué desbloquea cada bloque?',
      '¿Tengo que sacar fotos?',
      '¿Qué pasa con mis datos si vuelvo al plan Gratis?',
    ]) {
      expect(screen.getByText(pregunta)).toBeInTheDocument();
    }
    expect(screen.getByRole('heading', { name: 'Privacidad y datos' })).toBeInTheDocument();

    await clickLink(user, 'Ver la bienvenida otra vez');

    await verifyPathAsync(router, '/bienvenida');
  });
});

describe('Mi cuenta: modo demostración', () => {
  it('volver a empezar la prueba pide confirmación y no cambia nada si se conserva', async () => {
    const user = userEvent.setup();
    await abrirCuenta();
    useSuscripcionStore.getState().simularFinDePrueba();
    const antes = suscripcion();
    expect(antes.estado).toBe('prueba');

    await clickButton(user, 'Volver a empezar la prueba');
    const dialogo = await screen.findByRole('alertdialog', {
      name: '¿Volver a empezar la prueba?',
    });
    expect(
      within(dialogo).getByText(/Los bloques y los datos de tu negocio no se tocan/),
    ).toBeVisible();

    await user.click(within(dialogo).getByRole('button', { name: 'No, dejar todo como está' }));
    expect(screen.queryByRole('alertdialog')).toBeNull();
    expect(suscripcion()).toEqual(antes);

    await clickButton(user, 'Volver a empezar la prueba');
    const otro = await screen.findByRole('alertdialog');
    await user.click(within(otro).getByRole('button', { name: 'Sí, volver a empezar' }));

    await verifyTextPresentAsync('Listo, volvió a empezar la prueba de 7 días.');
    expect(suscripcion().pruebaHasta).not.toBe(antes.pruebaHasta);
    expect(new Date(suscripcion().pruebaHasta ?? 0).getTime()).toBeGreaterThan(Date.now());
  });
});

describe('Recuperar la contraseña (simulado)', () => {
  it('valida el email y responde igual exista o no la cuenta', async () => {
    const user = userEvent.setup();
    const router = renderApp('/olvide-contrasena');
    await verifyHeadingPresentAsync('Recuperá tu contraseña');

    await clickButton(user, 'Enviarme el enlace');
    await verifyTextPresentAsync('Ingresá tu email.');
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true');

    await typeLabel(user, 'Email', 'no-es-un-mail');
    await clickButton(user, 'Enviarme el enlace');
    await verifyTextPresentAsync(/Ingresá un email válido/);

    await user.clear(screen.getByLabelText('Email'));
    await typeLabel(user, 'Email', 'nadie@example.com');
    await clickButton(user, 'Enviarme el enlace');

    await verifyTextPresentAsync(/Si el correo existe, te enviamos un enlace/);
    expect(screen.getByText('Demostración')).toBeInTheDocument();
    await clickLink(user, 'Abrir el enlace de ejemplo');
    await verifyPathAsync(router, '/restablecer-contrasena');
  });

  it('elegir una contraseña nueva exige 8 caracteres y que coincidan', async () => {
    const user = userEvent.setup();
    renderApp('/restablecer-contrasena?token=demo');
    await verifyHeadingPresentAsync('Elegí una contraseña nueva');
    expect(screen.getAllByText('Simulación').length).toBeGreaterThan(0);

    await typeLabel(user, 'Contraseña nueva', 'corta');
    await typeLabel(user, 'Repetí la contraseña nueva', 'distinta');
    await clickButton(user, 'Guardar la contraseña nueva');
    await verifyTextPresentAsync('Usá al menos 8 caracteres.');
    await verifyTextPresentAsync('Las contraseñas no coinciden.');

    await user.clear(screen.getByLabelText('Contraseña nueva'));
    await user.clear(screen.getByLabelText('Repetí la contraseña nueva'));
    await typeLabel(user, 'Contraseña nueva', 'ClaveNueva123');
    await typeLabel(user, 'Repetí la contraseña nueva', 'ClaveNueva123');
    await clickButton(user, 'Guardar la contraseña nueva');

    await verifyHeadingPresentAsync('Listo, ya podés ingresar');
  });

  it('sin token explica que el enlace no sirve y ofrece pedir otro', async () => {
    const user = userEvent.setup();
    const router = renderApp('/restablecer-contrasena');
    await verifyHeadingPresentAsync('El enlace venció o no es válido');

    await clickLink(user, 'Pedir un enlace nuevo');

    await verifyPathAsync(router, '/olvide-contrasena');
  });

  it('el login ofrece el enlace para recuperar la contraseña', async () => {
    const user = userEvent.setup();
    const router = renderApp('/login');
    await verifyHeadingPresentAsync('Ingresá a tu cuenta');

    await clickLink(user, '¿Te olvidaste la contraseña?');

    await verifyPathAsync(router, '/olvide-contrasena');
  });
});

describe('accesibilidad (axe)', () => {
  it('Mi cuenta', async () => {
    await abrirCuenta();
    await sinViolaciones();
  });

  it('Mi cuenta, con la confirmación abierta', async () => {
    const user = userEvent.setup();
    await abrirCuenta();
    await clickButton(user, 'Volver a empezar la prueba');
    await screen.findByRole('alertdialog');
    await sinViolaciones();
  });

  it('Olvidé mi contraseña, antes y después de enviar', async () => {
    const user = userEvent.setup();
    renderApp('/olvide-contrasena');
    await verifyHeadingPresentAsync('Recuperá tu contraseña');
    await sinViolaciones();

    await typeLabel(user, 'Email', 'alguien@example.com');
    await clickButton(user, 'Enviarme el enlace');
    await verifyTextPresentAsync(/Si el correo existe/);
    await sinViolaciones();
  });

  it('Restablecer la contraseña', async () => {
    renderApp('/restablecer-contrasena?token=demo');
    await verifyHeadingPresentAsync('Elegí una contraseña nueva');
    await sinViolaciones();
  });
});
