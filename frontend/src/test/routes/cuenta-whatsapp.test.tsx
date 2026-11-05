import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { estado, preferenciasDe } from '@/mocks/db/estado';
import { CODIGO_DE_PRUEBA, TELEFONO_OCUPADO } from '@/mocks/handlers/whatsapp';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupServerError,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
} from '@/test/testHelpers';

const abrirWhatsapp = async () => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez', email: 'ana@example.com' });
  renderApp('/cuenta?seccion=whatsapp');
  await verifyHeadingPresentAsync('Tu cuenta');
  return { usuario, user: userEvent.setup() };
};

// Una cuenta que ya verificó su número: es lo que hace falta para pedir el resumen de la mañana.
const abrirConNumeroVinculado = async () => {
  const { user, usuario } = await abrirWhatsapp();
  await pedirCodigo(user, '+54 9 11 2233 4455');
  await user.type(await screen.findByLabelText('Código'), CODIGO_DE_PRUEBA);
  await user.click(screen.getByRole('button', { name: 'Confirmar número' }));
  await screen.findByText('+5491122334455');
  return { user, usuario };
};

const pedirCodigo = async (user: ReturnType<typeof userEvent.setup>, telefono: string) => {
  await user.type(await screen.findByLabelText('Tu número de WhatsApp'), telefono);
  await user.click(screen.getByRole('button', { name: 'Enviarme el código' }));
};

describe('tu cuenta: WhatsApp', () => {
  it('sin número vinculado pide el número y ofrece el camino alternativo', async () => {
    await abrirWhatsapp();

    expect(await screen.findByRole('heading', { name: 'WhatsApp' })).toBeVisible();
    expect(await screen.findByLabelText('Tu número de WhatsApp')).toBeVisible();
    expect(screen.getByText('¿No te llega el código?')).toBeInTheDocument();
  });

  it('un número mal escrito se marca en el campo y no se manda nada', async () => {
    const { user, usuario } = await abrirWhatsapp();

    await pedirCodigo(user, 'abc');

    expect(await screen.findByRole('alert')).toHaveTextContent('Escribí el número con código de país');
    expect(estado.whatsapp.get(usuario.email)?.verificacion).toBeUndefined();
  });

  it('el número se verifica con el código que llega por WhatsApp y queda vinculado', async () => {
    const { user, usuario } = await abrirWhatsapp();

    await pedirCodigo(user, '+54 11 2233 4455');

    expect(await screen.findByText(/Te mandamos un código de 6 dígitos por WhatsApp al \+5491122334455/)).toBeVisible();
    await user.type(screen.getByLabelText('Código'), CODIGO_DE_PRUEBA);
    await user.click(screen.getByRole('button', { name: 'Confirmar número' }));

    expect(await screen.findByText('+5491122334455')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Escribirle a Sami' })).toHaveAttribute(
      'href',
      'https://wa.me/5491155550000',
    );
    expect(estado.whatsapp.get(usuario.email)?.vinculo?.telefono).toBe('5491122334455');
  });

  it('un código incorrecto muestra cuántos intentos quedan y deja volver a probar', async () => {
    const { user } = await abrirWhatsapp();
    await pedirCodigo(user, '+54 9 11 2233 4455');
    await user.type(await screen.findByLabelText('Código'), '000000');
    await user.click(screen.getByRole('button', { name: 'Confirmar número' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('El código no es correcto. Te quedan 4 intentos');
    expect(screen.getByLabelText('Código')).toHaveValue('000000');
  });

  it('un código con menos de 6 dígitos se rechaza en el formulario', async () => {
    const { user } = await abrirWhatsapp();
    await pedirCodigo(user, '+54 9 11 2233 4455');
    await user.type(await screen.findByLabelText('Código'), '12');
    await user.click(screen.getByRole('button', { name: 'Confirmar número' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('El código tiene 6 dígitos');
  });

  it('un número que ya es de otra cuenta muestra el motivo', async () => {
    const { user } = await abrirWhatsapp();

    await pedirCodigo(user, `+${TELEFONO_OCUPADO}`);

    expect(await screen.findByRole('alert')).toHaveTextContent('ya está vinculado a otra cuenta');
  });

  it('cambiar de número vuelve al formulario y el reenvío espera un minuto', async () => {
    const { user } = await abrirWhatsapp();
    await pedirCodigo(user, '+54 9 11 2233 4455');
    await screen.findByLabelText('Código');

    expect(screen.getByRole('button', { name: /Reenviar código \(\d+ s\)/ })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Cambiar número' }));

    expect(await screen.findByLabelText('Tu número de WhatsApp')).toBeVisible();
  });

  it('el camino alternativo genera el código para mandar VINCULAR desde el teléfono', async () => {
    const { user } = await abrirWhatsapp();
    await user.click(await screen.findByText('¿No te llega el código?'));

    await user.click(screen.getByRole('button', { name: 'Generar código' }));

    expect(await screen.findByText('VINCULAR ABC234')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Generar otro código' })).toBeVisible();
  });

  it('desvincular pide confirmación y deja la cuenta sin número', async () => {
    const { user, usuario } = await abrirWhatsapp();
    await pedirCodigo(user, '+54 9 11 2233 4455');
    await user.type(await screen.findByLabelText('Código'), CODIGO_DE_PRUEBA);
    await user.click(screen.getByRole('button', { name: 'Confirmar número' }));
    await screen.findByText('+5491122334455');

    await user.click(screen.getByRole('button', { name: 'Desvincular' }));
    await user.click(screen.getByRole('button', { name: 'Sí, desvincular' }));

    expect(await screen.findByLabelText('Tu número de WhatsApp')).toBeVisible();
    await waitFor(() => expect(estado.whatsapp.get(usuario.email)?.vinculo).toBeNull(), ESPERA);
    expect(within(screen.getByRole('navigation', { name: 'Secciones de tu cuenta' }))
      .getByRole('link', { name: 'WhatsApp' })).toHaveAttribute('aria-current', 'page');
  });

  it('con el número vinculado se puede pedir el resumen de la mañana y queda guardado', async () => {
    const { user, usuario } = await abrirConNumeroVinculado();
    const casilla = screen.getByLabelText('Mandame el resumen de Sami cada mañana');
    expect(casilla).not.toBeChecked();

    await user.click(casilla);

    await waitFor(() => expect(screen.getByLabelText('Mandame el resumen de Sami cada mañana')).toBeChecked(), ESPERA);
    expect(preferenciasDe(usuario.email).avisosPorWhatsapp).toBe(true);
  });

  it('el resumen de la mañana se puede apagar de nuevo', async () => {
    const { user, usuario } = await abrirConNumeroVinculado();
    await user.click(screen.getByLabelText('Mandame el resumen de Sami cada mañana'));
    await waitFor(() => expect(screen.getByLabelText('Mandame el resumen de Sami cada mañana')).toBeChecked(), ESPERA);

    await user.click(screen.getByLabelText('Mandame el resumen de Sami cada mañana'));

    await waitFor(
      () => expect(screen.getByLabelText('Mandame el resumen de Sami cada mañana')).not.toBeChecked(),
      ESPERA,
    );
    expect(preferenciasDe(usuario.email).avisosPorWhatsapp).toBe(false);
  });

  it('si el servidor no deja prenderlo muestra el motivo y la casilla sigue apagada', async () => {
    const { user } = await abrirConNumeroVinculado();
    setupServerError('put', '/preferencias/avisos-por-whatsapp', 400, 'Primero vinculá tu número de WhatsApp');

    await user.click(screen.getByLabelText('Mandame el resumen de Sami cada mañana'));

    expect(await screen.findByRole('alert')).toHaveTextContent('Primero vinculá tu número de WhatsApp');
    expect(screen.getByLabelText('Mandame el resumen de Sami cada mañana')).not.toBeChecked();
  });

  it('sin número vinculado no se ofrece el resumen de la mañana', async () => {
    await abrirWhatsapp();

    await screen.findByLabelText('Tu número de WhatsApp');

    expect(screen.queryByLabelText('Mandame el resumen de Sami cada mañana')).not.toBeInTheDocument();
  });
});
