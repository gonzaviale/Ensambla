import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { crearBloque } from '@/api/bloques';
import { crearProyecto } from '@/api/proyectos';
import { fechaLarga } from '@/lib/formato';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion, verifyHeadingPresentAsync } from '@/test/testHelpers';

async function abrirPlan() {
  setupUsuarioConSesion();
  const user = userEvent.setup();
  renderApp('/plan');
  await verifyHeadingPresentAsync('Mi plan');
  return user;
}

async function sinViolaciones() {
  const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });
  const resumen = resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`);
  expect(resumen).toEqual([]);
}

async function sembrarNegocio() {
  const proyecto = await crearProyecto({ nombre: 'Mi negocio' });
  await crearBloque({ proyectoId: proyecto.id, nombre: 'Productos' });
}

const tarjeta = (nombre: string) => screen.getByRole('article', { name: nombre });
const historial = () => screen.getByRole('region', { name: 'Historial de pagos' });

/** Abre la hoja de pago de un plan y llega al paso «Pago». */
async function irAlPago(user: ReturnType<typeof userEvent.setup>, plan: string) {
  await user.click(within(tarjeta(plan)).getByRole('button', { name: `Elegir ${plan}` }));
  const hoja = await screen.findByRole('dialog', { name: `Suscribirte a ${plan}` });
  await user.click(within(hoja).getByRole('button', { name: 'Seguir al pago' }));
  await within(hoja).findByText('Es una simulación: no ingreses datos reales.');
  return hoja;
}

async function pagarConMercadoPago(user: ReturnType<typeof userEvent.setup>, plan: string) {
  const hoja = await irAlPago(user, plan);
  await user.click(within(hoja).getByRole('button', { name: 'Pagar con Mercado Pago' }));
  expect(await within(hoja).findByText('Procesando…')).toBeInTheDocument();
  await within(hoja).findByRole('heading', { name: /Ya tenés el plan/ }, ESPERA);
  return hoja;
}

describe('Mi plan (H-20)', () => {
  it('muestra la prueba en curso, lo que desbloqueás y las tres tarjetas', async () => {
    await abrirPlan();

    expect(screen.getByText(/te quedan 5 días/)).toBeInTheDocument();
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    expect(screen.getByRole('heading', { name: 'Lo que desbloqueás' })).toBeVisible();
    expect(
      await screen.findByText(/Todavía no armaste tu negocio/, {}, ESPERA),
    ).toBeInTheDocument();
    expect(screen.getByText(/Bloques de tu negocio/)).toBeInTheDocument();
    expect(screen.getByText('Cargas de historia (foto, lista o planilla)')).toBeInTheDocument();
    for (const nombre of ['Gratis', 'Negocio', 'Negocio +']) {
      expect(tarjeta(nombre)).toBeInTheDocument();
    }
    expect(within(tarjeta('Gratis')).getByRole('button', { name: 'Pasar a Gratis' })).toBeEnabled();
    expect(
      within(tarjeta('Negocio +')).getByRole('button', { name: 'Elegir Negocio +' }),
    ).toBeEnabled();
    expect(screen.getByText(/ilustrativos/)).toBeInTheDocument();
    expect(screen.getByRole('table', { name: 'Qué incluye cada plan' })).toBeInTheDocument();
    expect(within(historial()).getByText('Todavía no hiciste pagos')).toBeInTheDocument();
  });

  it('cuenta las respuestas que ya tenés y las que suma el plan al terminar la prueba', async () => {
    setupUsuarioConSesion();
    await sembrarNegocio();
    const user = userEvent.setup();
    renderApp('/plan');
    await verifyHeadingPresentAsync('Mi plan');

    expect(await screen.findByText(/Hoy tenés \d+ respuestas?\./, {}, ESPERA)).toBeInTheDocument();
    expect(screen.getByText(/1 de 3 bloques|1 bloque \(sin límite\)/)).toBeInTheDocument();

    await user.click(screen.getByText('Sólo para la demo'));
    await user.click(screen.getByRole('button', { name: 'Adelantar el fin de la prueba' }));

    expect(
      await screen.findByText(/con el plan Negocio ves 1 más/, {}, ESPERA),
    ).toBeInTheDocument();
    expect(screen.getByText('1 de 3 bloques')).toBeInTheDocument();
  });

  it('el selector anual muestra el precio mensual equivalente', async () => {
    const user = await abrirPlan();
    expect(within(tarjeta('Negocio')).getByText(/9\.900/)).toBeInTheDocument();

    await user.click(screen.getByRole('radio', { name: 'Anual' }));

    expect(within(tarjeta('Negocio')).getByText(/8\.250/)).toBeInTheDocument();
    expect(within(tarjeta('Negocio')).getByText(/10 meses, usás 12/)).toBeInTheDocument();
    expect(within(tarjeta('Negocio +')).getByText(/14\.917/)).toBeInTheDocument();
  });

  it('suscribirse a Negocio con Mercado Pago activa el plan y queda en el historial', async () => {
    const user = await abrirPlan();

    const hoja = await pagarConMercadoPago(user, 'Negocio');
    expect(within(hoja).getByRole('link', { name: 'Ir a Inicio' })).toHaveAttribute(
      'href',
      '/inicio',
    );
    await user.click(within(hoja).getByRole('button', { name: 'Volver a Mi plan' }));

    expect(await screen.findByText('Plan Negocio activo.')).toBeInTheDocument();
    expect(screen.getByText('Mercado Pago')).toBeInTheDocument();
    expect(within(historial()).getByText('Plan Negocio · mensual')).toBeInTheDocument();
    expect(within(historial()).getByText('Aprobado')).toBeInTheDocument();
    expect(
      within(tarjeta('Negocio')).getByRole('button', { name: 'Tu plan actual' }),
    ).toBeDisabled();
    expect(
      within(tarjeta('Negocio +')).getByRole('button', { name: 'Subir a Negocio +' }),
    ).toBeEnabled();
  });

  it('una tarjeta que termina en 0002 se rechaza y no activa nada', async () => {
    const user = await abrirPlan();
    const hoja = await irAlPago(user, 'Negocio');

    await user.click(within(hoja).getByRole('button', { name: 'Pagar con tarjeta' }));
    await user.type(within(hoja).getByLabelText(/Número de la tarjeta/), '4000000000000002');
    await user.type(within(hoja).getByLabelText(/Nombre en la tarjeta/), 'Don Carlos');
    await user.type(within(hoja).getByLabelText(/Vencimiento/), '1230');
    await user.click(within(hoja).getByRole('button', { name: /^Pagar \$/ }));

    expect(await within(hoja).findByRole('alert', {}, ESPERA)).toHaveTextContent(
      'La tarjeta fue rechazada. No te cobramos nada',
    );
    await user.click(within(hoja).getByRole('button', { name: 'Probar con otro medio' }));
    expect(within(hoja).getByRole('button', { name: 'Pagar con Mercado Pago' })).toBeVisible();
    await user.click(within(hoja).getByRole('button', { name: 'Cerrar' }));

    expect(screen.getByText(/te quedan 5 días/)).toBeInTheDocument();
    expect(within(historial()).getByText('Todavía no hiciste pagos')).toBeInTheDocument();
  });

  it('valida la tarjeta antes de pagar', async () => {
    const user = await abrirPlan();
    const hoja = await irAlPago(user, 'Negocio');

    await user.click(within(hoja).getByRole('button', { name: 'Pagar con tarjeta' }));
    await user.type(within(hoja).getByLabelText(/Número de la tarjeta/), '4242');
    await user.click(within(hoja).getByRole('button', { name: /^Pagar \$/ }));

    expect(await within(hoja).findByText(/entre 13 y 19 dígitos/)).toBeInTheDocument();
    expect(within(hoja).getByText(/Escribí el nombre/)).toBeInTheDocument();
    expect(within(hoja).getByText(/Escribilo así/)).toBeInTheDocument();
  });

  it('con una tarjeta aprobada sólo se guardan los últimos cuatro dígitos', async () => {
    const user = await abrirPlan();
    const hoja = await irAlPago(user, 'Negocio +');

    await user.click(within(hoja).getByRole('button', { name: 'Pagar con tarjeta' }));
    await user.type(within(hoja).getByLabelText(/Número de la tarjeta/), '4242424242424242');
    await user.type(within(hoja).getByLabelText(/Nombre en la tarjeta/), 'Don Carlos');
    await user.type(within(hoja).getByLabelText(/Vencimiento/), '1230');
    await user.click(within(hoja).getByRole('button', { name: /^Pagar \$/ }));
    await within(hoja).findByRole('heading', { name: /Ya tenés el plan/ }, ESPERA);
    await user.click(within(hoja).getByRole('button', { name: 'Volver a Mi plan' }));

    expect(screen.getByText('Visa •••• 4242')).toBeInTheDocument();
    expect(document.body.textContent).not.toContain('4242 4242 4242 4242');
    expect(localStorage.getItem('ensambla-suscripcion-demo-v1') ?? '').not.toMatch(
      /4242424242424242/,
    );
  });

  it('cancelar pide confirmación y dice hasta cuándo sigue activo lo pagado', async () => {
    const user = await abrirPlan();
    const hoja = await pagarConMercadoPago(user, 'Negocio');
    await user.click(within(hoja).getByRole('button', { name: 'Volver a Mi plan' }));

    await user.click(screen.getByRole('button', { name: 'Cancelar suscripción' }));
    const dialogo = await screen.findByRole('alertdialog', { name: '¿Cancelar tu suscripción?' });
    const hasta = fechaLarga(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000));
    expect(within(dialogo).getByText(hasta)).toBeInTheDocument();
    expect(
      within(dialogo).getByText(/No borramos tus datos: vas a seguir con el plan Gratis/),
    ).toBeVisible();

    await user.click(within(dialogo).getByRole('button', { name: 'Sí, cancelar' }));

    expect(
      await screen.findByText(
        new RegExp(`Cancelaste: seguís con el plan Negocio hasta el ${hasta}`),
      ),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Volver a activar' }));
    expect(await screen.findByText('Plan Negocio activo.')).toBeInTheDocument();
  });

  it('al adelantar el fin de la prueba queda el plan Gratis y se puede elegir un plan', async () => {
    const user = await abrirPlan();

    await user.click(screen.getByText('Sólo para la demo'));
    await user.click(screen.getByRole('button', { name: 'Adelantar el fin de la prueba' }));

    expect(
      await screen.findByText(/Terminó tu prueba: estás en el plan Gratis/),
    ).toBeInTheDocument();
    expect(
      within(tarjeta('Gratis')).getByRole('button', { name: 'Tu plan actual' }),
    ).toBeDisabled();
    expect(
      within(tarjeta('Negocio')).getByRole('button', { name: 'Elegir Negocio' }),
    ).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Adelantar el fin de la prueba' })).toBeDisabled();
  });

  it('terminar la prueba desde el plan Gratis pide confirmación y no borra nada', async () => {
    const user = await abrirPlan();

    await user.click(within(tarjeta('Gratis')).getByRole('button', { name: 'Pasar a Gratis' }));
    const dialogo = await screen.findByRole('alertdialog', { name: '¿Terminar la prueba ahora?' });
    await user.click(within(dialogo).getByRole('button', { name: 'No, seguir probando' }));
    expect(screen.getByText(/te quedan 5 días/)).toBeInTheDocument();

    await user.click(within(tarjeta('Gratis')).getByRole('button', { name: 'Pasar a Gratis' }));
    await user.click(await screen.findByRole('button', { name: 'Sí, terminar la prueba' }));

    expect(await screen.findByText('Plan Gratis.')).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
  });

  it('no tiene violaciones de accesibilidad (pantalla y hoja de pago)', async () => {
    const user = await abrirPlan();
    await sinViolaciones();

    await irAlPago(user, 'Negocio');
    await user.click(screen.getByRole('button', { name: 'Pagar con tarjeta' }));
    await sinViolaciones();
  });
});
