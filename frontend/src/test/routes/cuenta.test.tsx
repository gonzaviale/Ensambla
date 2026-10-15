import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { derivarAlertas } from '@/lib/red/alertas';
import { interpretarRelato, sugerirReglas } from '@/lib/red/relato';
import { useRedStore } from '@/stores/red';
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

const store = () => useRedStore.getState();

describe('interpretarRelato', () => {
  it('detecta el rubro y la cantidad de proveedores', () => {
    const relato = interpretarRelato(
      'Tengo un almacén, le compro a dos mayoristas y los precios los cambio a mano',
    );

    expect(relato.rubro).toBe('Almacén');
    expect(relato.proveedores).toBe(2);
    expect(relato.cambiaPreciosAMano).toBe(true);
    expect(relato.bloques).toEqual(
      expect.arrayContaining(['listas', 'reglas', 'margen', 'comparar']),
    );
    expect(relato.bloques).not.toContain('stock');
    expect(relato.bloques).not.toContain('cuaderno');
  });

  it('entiende números escritos con cifras y el rubro sin tilde', () => {
    const relato = interpretarRelato('Soy de una verduleria, compro a 3 proveedores');

    expect(relato.rubro).toBe('Verdulería');
    expect(relato.proveedores).toBe(3);
  });

  it('prende stock si habla de faltantes y cuaderno si lo nombra', () => {
    const relato = interpretarRelato(
      'Anoto todo en un cuaderno, los listas me llegan por WhatsApp y a veces me quedo sin azúcar',
    );

    expect(relato.nombraCuaderno).toBe(true);
    expect(relato.nombraWhatsapp).toBe(true);
    expect(relato.hablaDeFaltantes).toBe(true);
    expect(relato.bloques).toEqual(expect.arrayContaining(['stock', 'cuaderno']));
  });

  it('el rubro elegido con las fichas gana sobre el del texto y sin relato queda lo básico', () => {
    expect(interpretarRelato('tengo un kiosco', 'Almacén').rubro).toBe('Almacén');

    const vacio = interpretarRelato('', null);
    expect(vacio.rubro).toBeNull();
    expect(vacio.proveedores).toBeNull();
    expect(vacio.bloques).toEqual(['listas', 'reglas', 'margen']);
  });

  it('sugiere la ganancia según el rubro', () => {
    expect(sugerirReglas('Almacén').margen).toBe(33);
    expect(sugerirReglas('Kiosco').margen).toBe(50);
    expect(sugerirReglas(null).margen).toBeGreaterThan(0);
  });
});

describe('Bienvenida', () => {
  it('el relato prende los bloques, guarda las reglas y termina en la lista de ejemplo', async () => {
    const user = userEvent.setup();
    setupUsuarioConSesion();
    store().guardarNegocio({ onboardingHecho: false });
    const router = renderApp('/bienvenida');
    await verifyHeadingPresentAsync('Armemos Ensambla a tu medida');
    const listasAntes = store().listas.length;

    // Paso 1: contar el comercio.
    const relato = screen.getByLabelText('¿Cómo trabajás hoy?');
    await user.clear(relato);
    await user.type(
      relato,
      'Tengo un kiosco, le compro a tres mayoristas y me quedo sin cigarrillos',
    );
    await user.click(screen.getByRole('button', { name: 'Kiosco' }));
    expect(screen.getByRole('button', { name: 'Kiosco' })).toHaveAttribute('aria-pressed', 'true');
    await clickButton(user, 'Seguir');

    await verifyTextPresentAsync('Entendí esto:');
    expect(screen.getByText(/tenés un kiosco, le comprás a 3 proveedores/)).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: /Stock por agotarse/ })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: /Reglas de precio/ })).toBeChecked();
    await user.click(screen.getByRole('checkbox', { name: /Avisos de margen/ }));
    await clickButton(user, 'Está bien, seguir');

    // Paso 2: reglas de precio con la ganancia sugerida para kiosco.
    await verifyHeadingPresentAsync('Tus reglas de precio');
    expect(screen.getByLabelText('Ganancia general (%)')).toHaveValue(50);
    await clickButton(user, 'Guardar y seguir');

    // Paso 3: la lista de ejemplo.
    await verifyHeadingPresentAsync('Cargá tu primera lista');
    expect(store().negocio.rubro).toBe('Kiosco');
    expect(store().negocio.relato).toContain('cigarrillos');
    expect(store().reglas.general.margen).toBe(50);
    expect(store().umbrales.activas['precio-en-riesgo']).toBe(false);
    expect(store().umbrales.activas['stock-por-agotarse']).toBe(true);
    await clickButton(user, 'Usar la lista de ejemplo de Distribuidora Sur');

    await waitFor(() => {
      expect(router.state.location.pathname).toMatch(/^\/listas\/.+/);
    }, ESPERA);
    expect(store().negocio.onboardingHecho).toBe(true);
    expect(store().listas).toHaveLength(listasAntes + 1);
  });

  it('valida el nombre del comercio y deja saltar la bienvenida desde el primer paso', async () => {
    const user = userEvent.setup();
    setupUsuarioConSesion();
    store().guardarNegocio({ onboardingHecho: false });
    const router = renderApp('/bienvenida');
    await verifyHeadingPresentAsync('Armemos Ensambla a tu medida');

    await user.clear(screen.getByLabelText(/Nombre del comercio/));
    await clickButton(user, 'Seguir');

    await verifyTextPresentAsync('Escribí cómo se llama tu comercio.');
    await waitFor(() => expect(screen.getByLabelText(/Nombre del comercio/)).toHaveFocus());

    await clickButton(user, 'Saltar');

    await verifyPathAsync(router, '/hoy');
    expect(store().negocio.onboardingHecho).toBe(true);
  });
});

describe('Mi cuenta', () => {
  it('guardar los datos del comercio los refleja y avisa', async () => {
    const user = userEvent.setup();
    setupUsuarioConSesion();
    renderApp('/cuenta');
    await verifyHeadingPresentAsync('Mi cuenta');

    const nombre = screen.getByLabelText(/Nombre del comercio/);
    await user.clear(nombre);
    await user.type(nombre, 'Kiosco Doña Rosa');
    const telefono = screen.getByLabelText('Teléfono');
    await user.clear(telefono);
    await user.type(telefono, '11 4444-1234');
    await clickButton(user, 'Guardar los datos');

    await verifyTextPresentAsync('Guardamos los datos de tu comercio.');
    expect(store().negocio.nombre).toBe('Kiosco Doña Rosa');
    expect(store().negocio.telefono).toBe('11 4444-1234');
    expect(screen.getByLabelText(/Nombre del comercio/)).toHaveValue('Kiosco Doña Rosa');
  });

  it('el nombre es obligatorio y el foco va al campo con error', async () => {
    const user = userEvent.setup();
    setupUsuarioConSesion();
    renderApp('/cuenta');
    await verifyHeadingPresentAsync('Mi cuenta');
    const nombreAntes = store().negocio.nombre;

    await user.clear(screen.getByLabelText(/Nombre del comercio/));
    await clickButton(user, 'Guardar los datos');

    await verifyTextPresentAsync('Escribí cómo se llama tu comercio.');
    expect(screen.getByLabelText(/Nombre del comercio/)).toHaveAttribute('aria-invalid', 'true');
    await waitFor(() => expect(screen.getByLabelText(/Nombre del comercio/)).toHaveFocus());
    expect(store().negocio.nombre).toBe(nombreAntes);
  });

  it('volver a los datos de ejemplo pide confirmación y nombra lo que se pierde', async () => {
    const user = userEvent.setup();
    setupUsuarioConSesion();
    store().guardarNegocio({ nombre: 'Mi comercio cambiado' });
    renderApp('/cuenta');
    await verifyHeadingPresentAsync('Mi cuenta');

    await clickButton(user, 'Volver a los datos de ejemplo');
    const dialogo = await screen.findByRole('alertdialog', {
      name: '¿Volver a los datos de ejemplo?',
    });
    expect(within(dialogo).getByText(/Vas a perder todo lo que hiciste/)).toBeInTheDocument();
    expect(store().negocio.nombre).toBe('Mi comercio cambiado');

    await user.click(within(dialogo).getByRole('button', { name: 'No, dejar todo como está' }));
    expect(screen.queryByRole('alertdialog')).toBeNull();
    expect(store().negocio.nombre).toBe('Mi comercio cambiado');

    await clickButton(user, 'Volver a los datos de ejemplo');
    const otra = await screen.findByRole('alertdialog');
    await user.click(
      within(otra).getByRole('button', { name: 'Sí, volver a los datos de ejemplo' }),
    );

    await waitFor(() => expect(store().negocio.nombre).toBe('Almacén Don Carlos'));
    await verifyTextPresentAsync('Listo, volvimos a los datos de ejemplo.');
    expect(screen.getByLabelText(/Nombre del comercio/)).toHaveValue('Almacén Don Carlos');
  });

  it('muestra de nuevo los avisos que se ignoraron', async () => {
    const user = userEvent.setup();
    setupUsuarioConSesion();
    const [alerta] = derivarAlertas(store());
    if (!alerta) throw new Error('La demostración debería traer avisos');
    store().ignorarAlerta(alerta.id);
    renderApp('/cuenta');
    await verifyHeadingPresentAsync('Mi cuenta');
    await verifyTextPresentAsync('Hoy tenés 1 aviso ignorado.');

    await clickButton(user, 'Mostrar de nuevo los avisos que ignoré');

    await verifyTextPresentAsync('No ignoraste ningún aviso.');
    expect(store().alertas).toEqual({});
  });
});

describe('Recuperar la contraseña', () => {
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
    expect(router.state.location.search).toMatchObject({ token: 'demo' });
  });

  it('elegir una contraseña nueva exige 8 caracteres y que coincidan', async () => {
    const user = userEvent.setup();
    renderApp('/restablecer-contrasena?token=demo');
    await verifyHeadingPresentAsync('Elegí una contraseña nueva');

    await typeLabel(user, 'Contraseña nueva', 'corta');
    await typeLabel(user, 'Repetí la contraseña nueva', 'distinta');
    await clickButton(user, 'Guardar la contraseña nueva');
    await verifyTextPresentAsync('Usá al menos 8 caracteres.');
    await verifyTextPresentAsync('Las contraseñas no coinciden.');
    await waitFor(() => expect(screen.getByLabelText('Contraseña nueva')).toHaveFocus());

    await user.clear(screen.getByLabelText('Contraseña nueva'));
    await user.clear(screen.getByLabelText('Repetí la contraseña nueva'));
    await typeLabel(user, 'Contraseña nueva', 'ClaveNueva123');
    await typeLabel(user, 'Repetí la contraseña nueva', 'ClaveNueva123');
    await clickButton(user, 'Guardar la contraseña nueva');

    await verifyHeadingPresentAsync('Listo, ya podés ingresar');
    expect(screen.getByRole('link', { name: 'Ir a ingresar' })).toHaveAttribute(
      'href',
      expect.stringContaining('/login'),
    );
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
  it('Bienvenida, en cada paso', async () => {
    const user = userEvent.setup();
    setupUsuarioConSesion();
    renderApp('/bienvenida');
    await verifyHeadingPresentAsync('Armemos Ensambla a tu medida');
    await sinViolaciones();

    await clickButton(user, 'Seguir');
    await verifyTextPresentAsync('Entendí esto:');
    await sinViolaciones();

    await clickButton(user, 'Está bien, seguir');
    await verifyHeadingPresentAsync('Tus reglas de precio');
    await sinViolaciones();

    await clickButton(user, 'Guardar y seguir');
    await verifyHeadingPresentAsync('Cargá tu primera lista');
    await sinViolaciones();
  });

  it('Mi cuenta', async () => {
    setupUsuarioConSesion();
    renderApp('/cuenta');
    await verifyHeadingPresentAsync('Mi cuenta');
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

  it('Restablecer la contraseña, con y sin enlace', async () => {
    renderApp('/restablecer-contrasena?token=demo');
    await verifyHeadingPresentAsync('Elegí una contraseña nueva');
    await sinViolaciones();
  });
});
