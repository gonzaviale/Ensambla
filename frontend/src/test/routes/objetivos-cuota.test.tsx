import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { estado } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupObjetivoDeEjemplo, setupUsuarioConSesion } from '@/test/testHelpers';

type Router = ReturnType<typeof renderApp>;
type Usuario = ReturnType<typeof setupUsuarioConSesion>;

const elMes = () => new Date().toISOString().slice(0, 7);

let usuario: Usuario;
let router: Router;

describe('la cuota de objetivos con IA', () => {
  it('Objetivos muestra cuántos objetivos con IA quedan en el mes', async () => {
    dadoUnObjetivoConIaUsadas(1);

    cuandoSeAbreObjetivos();

    await entoncesSeDiceQue('Objetivos con IA disponibles este mes: 4 de 5');
  });

  it('un objetivo monetario del formulario clásico no gasta cuota', async () => {
    dadoUnObjetivoConIaUsadas(0);
    cuandoSeAbreObjetivos();

    await cuandoSeDefineUnObjetivoMonetarioClasico();

    await entoncesSeSumoUnObjetivoSinGastarCuota();
  });

  it('al agotarse explica qué hacer', async () => {
    dadoUnObjetivoConIaUsadas(5);

    cuandoSeAbreObjetivos();

    await entoncesElAvisoExplicaQueSeAgotoYQueHayPremium();
  });

  it('al agotarse lleva a Planes', async () => {
    dadoUnObjetivoConIaUsadas(5);
    cuandoSeAbreObjetivos();

    await cuandoSePideVerLosPlanes();

    await entoncesSeAbrenLosPlanes();
  });

  it('con Premium dice que se renuevan el mes que viene', async () => {
    dadoUnPremiumConIaUsadas(20);

    cuandoSeAbreObjetivos();

    await entoncesElAvisoDiceQueSeRenuevanElMesQueViene();
  });

  it('con Premium no ofrece cambiar de plan', async () => {
    dadoUnPremiumConIaUsadas(20);

    cuandoSeAbreObjetivos();

    await entoncesNoSeOfreceCambiarDePlan();
  });

  it('Planes dice cuántos objetivos con IA incluye cada plan', async () => {
    dadoUnUsuarioSinObjetivos();

    await cuandoSeAbrenLosPlanes();

    entoncesCadaPlanDiceSusObjetivosConIa('5', '20');
  });
});

function dadoUnObjetivoConIaUsadas(usadas: number) {
  usuario = setupUsuarioConSesion();
  setupObjetivoDeEjemplo(usuario.email);
  if (usadas > 0) estado.objetivosConIa.set(`${usuario.email}|${elMes()}`, usadas);
}

function dadoUnPremiumConIaUsadas(usadas: number) {
  dadoUnObjetivoConIaUsadas(usadas);
  estado.pruebasPremium.set(usuario.email, {
    inicioEn: new Date(Date.now() - 86_400_000).toISOString(),
    finEn: new Date(Date.now() + 5 * 86_400_000).toISOString(),
    paginasIaUtilizadas: 0,
    limitePaginasIa: 10,
  });
}

function dadoUnUsuarioSinObjetivos() {
  usuario = setupUsuarioConSesion();
}

function cuandoSeAbreObjetivos() {
  router = renderApp('/objetivos');
}

async function cuandoSeAbrenLosPlanes() {
  renderApp('/planes');
  await screen.findByRole('article', { name: 'Plan Básico' }, ESPERA);
}

async function cuandoSeDefineUnObjetivoMonetarioClasico() {
  const user = userEvent.setup();
  await screen.findByText('Objetivos con IA disponibles este mes: 5 de 5', {}, ESPERA);
  await user.click(screen.getByRole('button', { name: /Nuevo desafío/ }));
  await user.click(
    await screen.findByRole('button', { name: 'Quiero poner una meta en pesos por mes' }),
  );
  await user.click(screen.getByRole('button', { name: /Quiero ganar \$400\.000/ }));
  await user.click(screen.getByRole('button', { name: 'Definir objetivo' }));
}

async function cuandoSePideVerLosPlanes() {
  const aviso = await elAvisoDeLaCuota();
  await userEvent.click(await aviso.findByRole('button', { name: 'Ver planes' }));
}

const elAvisoDeLaCuota = async () =>
  within(await screen.findByRole('note', { name: 'Objetivos con IA de este mes' }, ESPERA));

async function entoncesSeDiceQue(texto: string) {
  expect(await screen.findByText(texto, {}, ESPERA)).toBeVisible();
}

async function entoncesSeSumoUnObjetivoSinGastarCuota() {
  await waitFor(() => expect(screen.getAllByRole('article')).toHaveLength(2), ESPERA);
  expect(estado.objetivosConIa.has(`${usuario.email}|${elMes()}`)).toBe(false);
  expect(screen.getByText('Objetivos con IA disponibles este mes: 5 de 5')).toBeVisible();
}

async function entoncesElAvisoExplicaQueSeAgotoYQueHayPremium() {
  const aviso = await elAvisoDeLaCuota();
  expect(aviso.getByText('Usaste los 5 objetivos con IA de este mes')).toBeVisible();
  expect(await aviso.findByText(/Con Premium tenés 20 por mes/)).toBeVisible();
}

async function entoncesSeAbrenLosPlanes() {
  await waitFor(() => expect(router.state.location.pathname).toBe('/planes'));
}

async function entoncesElAvisoDiceQueSeRenuevanElMesQueViene() {
  const aviso = await elAvisoDeLaCuota();
  expect(aviso.getByText('Usaste los 20 objetivos con IA de este mes')).toBeVisible();
  expect(await aviso.findByText(/Se renuevan el mes que viene/)).toBeVisible();
}

async function entoncesNoSeOfreceCambiarDePlan() {
  const aviso = await elAvisoDeLaCuota();
  await aviso.findByText(/Se renuevan el mes que viene/);
  expect(aviso.queryByRole('button', { name: 'Ver planes' })).not.toBeInTheDocument();
}

function entoncesCadaPlanDiceSusObjetivosConIa(gratuito: string, premium: string) {
  const delGratuito = within(screen.getByRole('article', { name: 'Plan Básico' }));
  const delPremium = within(screen.getByRole('article', { name: 'Plan Premium' }));
  const etiqueta = 'Objetivos con IA por mes';
  expect(delGratuito.getByText(etiqueta).nextElementSibling).toHaveTextContent(gratuito);
  expect(delPremium.getByText(etiqueta).nextElementSibling).toHaveTextContent(premium);
}
