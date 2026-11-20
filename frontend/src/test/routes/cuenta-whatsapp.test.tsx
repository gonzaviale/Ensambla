import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { estado } from '@/mocks/db/estado';
import { CODIGO_DE_PRUEBA, TELEFONO_OCUPADO } from '@/mocks/handlers/whatsapp';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion, verifyHeadingPresentAsync } from '@/test/testHelpers';

type Usuario = ReturnType<typeof userEvent.setup>;

let user: Usuario;
let email = '';

describe('tu cuenta: WhatsApp', () => {
  it('sin número vinculado pide el número', async () => {
    await dadaLaCuentaSinWhatsapp();

    await entoncesSePideElNumero();
  });

  it('sin número vinculado ofrece el camino alternativo', async () => {
    await dadaLaCuentaSinWhatsapp();

    await entoncesSeOfreceElCaminoAlternativo();
  });

  it('un número mal escrito se marca en el campo', async () => {
    await dadaLaCuentaSinWhatsapp();

    await cuandoSePideElCodigoParaElNumero('abc');

    await entoncesSePideElCodigoDePais();
  });

  it('un número mal escrito no manda nada', async () => {
    await dadaLaCuentaSinWhatsapp();

    await cuandoSePideElCodigoParaElNumero('abc');
    await entoncesSePideElCodigoDePais();

    entoncesNoSeMandoNingunaVerificacion();
  });

  it('el número se verifica con el código que llega por WhatsApp', async () => {
    await dadaLaCuentaSinWhatsapp();
    await cuandoSePideElCodigoParaElNumero('+54 11 2233 4455');

    await entoncesSeAvisaQueSeMandoElCodigoAl('+5491122334455');
  });

  it('con el código correcto el número queda vinculado', async () => {
    await dadaLaCuentaSinWhatsapp();
    await cuandoSePideElCodigoParaElNumero('+54 11 2233 4455');

    await cuandoSeConfirmaElCodigo(CODIGO_DE_PRUEBA);

    await entoncesElNumeroQuedaVinculado('+5491122334455');
  });

  it('un código incorrecto muestra cuántos intentos quedan', async () => {
    await dadaLaCuentaSinWhatsapp();
    await cuandoSePideElCodigoParaElNumero('+54 9 11 2233 4455');

    await cuandoSeConfirmaElCodigo('000000');

    await entoncesSeAvisaQueElCodigoNoEsCorrectoYQuedanIntentos(4);
  });

  it('un código incorrecto deja volver a probar', async () => {
    await dadaLaCuentaSinWhatsapp();
    await cuandoSePideElCodigoParaElNumero('+54 9 11 2233 4455');

    await cuandoSeConfirmaElCodigo('000000');

    entoncesElCampoDelCodigoConserva('000000');
  });

  it('un código con menos de 6 dígitos se rechaza en el formulario', async () => {
    await dadaLaCuentaSinWhatsapp();
    await cuandoSePideElCodigoParaElNumero('+54 9 11 2233 4455');

    await cuandoSeConfirmaElCodigo('12');

    await entoncesSeAvisaQueElCodigoTieneSeisDigitos();
  });

  it('un número que ya es de otra cuenta muestra el motivo', async () => {
    await dadaLaCuentaSinWhatsapp();

    await cuandoSePideElCodigoParaElNumero(`+${TELEFONO_OCUPADO}`);

    await entoncesSeAvisaQueElNumeroEsDeOtraCuenta();
  });

  it('cambiar de número vuelve al formulario', async () => {
    await dadaLaCuentaSinWhatsapp();
    await cuandoSePideElCodigoParaElNumero('+54 9 11 2233 4455');
    await screen.findByLabelText('Código');

    await cuandoSePideCambiarDeNumero();

    await entoncesSePideElNumeroDeNuevo();
  });

  it('el reenvío del código espera un minuto', async () => {
    await dadaLaCuentaSinWhatsapp();

    await cuandoSePideElCodigoParaElNumero('+54 9 11 2233 4455');
    await screen.findByLabelText('Código');

    entoncesElReenvioEstaEnEspera();
  });

  it('el camino alternativo genera el código para mandar VINCULAR desde el teléfono', async () => {
    await dadaLaCuentaSinWhatsapp();

    await cuandoSeGeneraElCodigoAlternativo();

    await entoncesSeMuestraElCodigoParaVincular('VINCULAR ABC234');
  });

  it('desvincular deja la cuenta sin número', async () => {
    await dadaLaCuentaConWhatsappVinculado();

    await cuandoSeDesvincula();

    await entoncesLaCuentaQuedoSinNumero();
  });

  it('al desvincular la sección de WhatsApp sigue siendo la página actual', async () => {
    await dadaLaCuentaConWhatsappVinculado();

    await cuandoSeDesvincula();

    entoncesWhatsappSigueSiendoLaPaginaActual();
  });
});

async function dadaLaCuentaSinWhatsapp() {
  email = setupUsuarioConSesion({ nombre: 'Ana Pérez', email: 'ana@example.com' }).email;
  renderApp('/cuenta?seccion=whatsapp');
  await verifyHeadingPresentAsync('Tu cuenta');
  user = userEvent.setup();
}

async function dadaLaCuentaConWhatsappVinculado() {
  await dadaLaCuentaSinWhatsapp();
  await cuandoSePideElCodigoParaElNumero('+54 9 11 2233 4455');
  await cuandoSeConfirmaElCodigo(CODIGO_DE_PRUEBA);
  await screen.findByText('+5491122334455');
}

async function cuandoSePideElCodigoParaElNumero(telefono: string) {
  await user.type(await screen.findByLabelText('Tu número de WhatsApp'), telefono);
  await user.click(screen.getByRole('button', { name: 'Enviarme el código' }));
}

async function cuandoSeConfirmaElCodigo(codigo: string) {
  await user.type(await screen.findByLabelText('Código'), codigo);
  await user.click(screen.getByRole('button', { name: 'Confirmar número' }));
}

async function cuandoSePideCambiarDeNumero() {
  await user.click(screen.getByRole('button', { name: 'Cambiar número' }));
}

async function cuandoSeGeneraElCodigoAlternativo() {
  await user.click(await screen.findByText('¿No te llega el código?'));
  await user.click(screen.getByRole('button', { name: 'Generar código' }));
}

async function cuandoSeDesvincula() {
  await user.click(screen.getByRole('button', { name: 'Desvincular' }));
  await user.click(screen.getByRole('button', { name: 'Sí, desvincular' }));
}

async function entoncesSePideElNumero() {
  expect(await screen.findByRole('heading', { name: 'WhatsApp' })).toBeVisible();
  expect(await screen.findByLabelText('Tu número de WhatsApp')).toBeVisible();
}

async function entoncesSeOfreceElCaminoAlternativo() {
  expect(await screen.findByText('¿No te llega el código?')).toBeInTheDocument();
}

async function entoncesSePideElCodigoDePais() {
  expect(await screen.findByRole('alert')).toHaveTextContent(
    'Escribí el número con código de país',
  );
}

function entoncesNoSeMandoNingunaVerificacion() {
  expect(estado.whatsapp.get(email)?.verificacion).toBeUndefined();
}

async function entoncesSeAvisaQueSeMandoElCodigoAl(telefono: string) {
  const aviso = new RegExp(`Te mandamos un código de 6 dígitos por WhatsApp al \\${telefono}`);
  expect(await screen.findByText(aviso)).toBeVisible();
}

async function entoncesElNumeroQuedaVinculado(telefono: string) {
  expect(await screen.findByText(telefono)).toBeVisible();
  expect(screen.getByRole('link', { name: 'Escribirle a Sami' })).toHaveAttribute(
    'href',
    'https://wa.me/5491155550000',
  );
  expect(estado.whatsapp.get(email)?.vinculo?.telefono).toBe(telefono.replace('+', ''));
}

async function entoncesSeAvisaQueElCodigoNoEsCorrectoYQuedanIntentos(intentos: number) {
  expect(await screen.findByRole('alert')).toHaveTextContent(
    `El código no es correcto. Te quedan ${intentos} intentos`,
  );
}

function entoncesElCampoDelCodigoConserva(codigo: string) {
  expect(screen.getByLabelText('Código')).toHaveValue(codigo);
}

async function entoncesSeAvisaQueElCodigoTieneSeisDigitos() {
  expect(await screen.findByRole('alert')).toHaveTextContent('El código tiene 6 dígitos');
}

async function entoncesSeAvisaQueElNumeroEsDeOtraCuenta() {
  expect(await screen.findByRole('alert')).toHaveTextContent('ya está vinculado a otra cuenta');
}

async function entoncesSePideElNumeroDeNuevo() {
  expect(await screen.findByLabelText('Tu número de WhatsApp')).toBeVisible();
}

function entoncesElReenvioEstaEnEspera() {
  expect(screen.getByRole('button', { name: /Reenviar código \(\d+ s\)/ })).toBeDisabled();
}

async function entoncesSeMuestraElCodigoParaVincular(codigo: string) {
  expect(await screen.findByText(codigo)).toBeVisible();
  expect(screen.getByRole('button', { name: 'Generar otro código' })).toBeVisible();
}

async function entoncesLaCuentaQuedoSinNumero() {
  expect(await screen.findByLabelText('Tu número de WhatsApp')).toBeVisible();
  await waitFor(() => expect(estado.whatsapp.get(email)?.vinculo).toBeNull(), ESPERA);
}

function entoncesWhatsappSigueSiendoLaPaginaActual() {
  const secciones = within(screen.getByRole('navigation', { name: 'Secciones de tu cuenta' }));
  expect(secciones.getByRole('link', { name: 'WhatsApp' })).toHaveAttribute('aria-current', 'page');
}
