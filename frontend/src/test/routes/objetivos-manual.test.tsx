import { screen, waitFor, within } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { sembrarDemoCargada } from '@/mocks/db/demoCargada';
import { estado } from '@/mocks/db/estado';
import { ID_OBJETIVO_SIN_DATOS_DE_EJEMPLO } from '@/mocks/db/objetivosAutogestionados';
import {
  givenQueHoyEsLunes,
  givenQueHoyEsMiercoles,
  seccionDeSami,
  thenElPaginadorDice,
  thenLaOpcionEsta,
  thenLaOpcionNoSePuedeElegir,
  thenNoHayBotonDeEnviar,
  thenNoHayPaginador,
  thenNoHayPreguntasDeSami,
  thenNoSePideTodavia,
  thenSePideElPaso,
  thenSePreguntaPor,
  volverAlTiempoReal,
  whenSeDejaParaMasTarde,
  whenSeElige,
  whenSePasaALaSiguientePregunta,
  whenSeResponde,
  whenSeVuelveALaPreguntaAnterior,
  whenSeVuelveUnPaso,
} from '@/test/preguntasDeSami';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';

const DETALLE = `/objetivos/${ID_OBJETIVO_SIN_DATOS_DE_EJEMPLO}`;
const MINI_SIN_CONTAR = `${ID_OBJETIVO_SIN_DATOS_DE_EJEMPLO}-sin-contar`;
const MINI_PENDIENTE = `${ID_OBJETIVO_SIN_DATOS_DE_EJEMPLO}-pendiente`;
const MINI_RESPONDIDA = `${ID_OBJETIVO_SIN_DATOS_DE_EJEMPLO}-respondida`;
const PRIMERA = 'Anotá qué te piden y no tenés';
const SEGUNDA = 'Poné un cartel en la vereda con lo que ofrecés';

const conObjetivoSinDatos = () => {
  const usuario = setupUsuarioConSesion();
  sembrarDemoCargada(usuario.email);
  return usuario;
};

const givenQueSamiPreguntaUnMiercoles = async () => {
  givenQueHoyEsMiercoles();
  conObjetivoSinDatos();
  renderApp('/objetivos');
  return seccionDeSami();
};

const givenQueSamiPreguntaUnLunes = async () => {
  givenQueHoyEsLunes();
  conObjetivoSinDatos();
  renderApp('/objetivos');
  return seccionDeSami();
};

const givenQueElServidorYaTeniaLaRespuesta = () =>
  setupServerError(
    'post',
    `/objetivos/${ID_OBJETIVO_SIN_DATOS_DE_EJEMPLO}/mini-objetivos/${MINI_SIN_CONTAR}/respuesta`,
    409,
    'Ya le contaste a Sami cómo te fue esta semana.',
  );

describe('mini-objetivos manuales', () => {
  afterEach(volverAlTiempoReal);

  it('Sami pregunta una cosa por vez, sin contador ni alarma', async () => {
    const seccion = await givenQueSamiPreguntaUnMiercoles();

    thenSePreguntaPor(seccion, [PRIMERA]);
    thenElPaginadorDice(seccion, '1 de 2');
    expect(seccion.queryByText(/\(\d+\)/)).not.toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.queryByText(/Lo lograste/)).not.toBeInTheDocument();
  });

  it('con las flechas se pasa de una pregunta a la otra, del más viejo al más nuevo', async () => {
    await givenQueSamiPreguntaUnMiercoles();

    await whenSePasaALaSiguientePregunta();

    const seccion = await seccionDeSami();
    thenSePreguntaPor(seccion, [SEGUNDA]);
    thenElPaginadorDice(seccion, '2 de 2');
    expect(seccion.getByText('Que más gente conozca mi local')).toBeVisible();
  });

  it('se puede volver a la pregunta anterior', async () => {
    await givenQueSamiPreguntaUnMiercoles();
    await whenSePasaALaSiguientePregunta();

    await whenSeVuelveALaPreguntaAnterior();

    thenSePreguntaPor(await seccionDeSami(), [PRIMERA]);
  });

  it('el lunes no pregunta por lo de esta semana, solo por lo que quedó sin contar', async () => {
    const seccion = await givenQueSamiPreguntaUnLunes();

    thenSePreguntaPor(seccion, [PRIMERA]);
    thenNoHayPaginador(seccion);
  });

  it('«Más tarde» deja la pregunta para mañana', async () => {
    await givenQueSamiPreguntaUnLunes();

    await whenSeDejaParaMasTarde(PRIMERA);

    await thenNoHayPreguntasDeSami();
  });

  it('cada pregunta aparece en su propia página, siempre en el mismo lugar', async () => {
    await givenQueSamiPreguntaUnMiercoles();
    thenSePideElPaso(PRIMERA, '¿Lo hiciste?');
    thenNoSePideTodavia(PRIMERA, '¿Qué pasó?');
    thenNoHayBotonDeEnviar(PRIMERA);

    await whenSeElige(PRIMERA, 'Sí');

    thenSePideElPaso(PRIMERA, '¿Qué pasó?');
    thenNoSePideTodavia(PRIMERA, '¿Lo hiciste?');
    // Si lo hiciste, tuviste tiempo.
    thenLaOpcionNoSePuedeElegir(PRIMERA, 'No tuve tiempo');
  });

  it('se responde en dos toques y la respuesta queda guardada', async () => {
    await givenQueSamiPreguntaUnMiercoles();
    await whenSePasaALaSiguientePregunta();

    await whenSeResponde(SEGUNDA, { hizo: 'Sí', quePaso: 'Funcionó', comentario: 'Vino gente nueva' });

    await waitFor(() => expect(estado.respuestasDeMinis.get(MINI_PENDIENTE)).toBeDefined(), ESPERA);
    expect(estado.respuestasDeMinis.get(MINI_PENDIENTE)).toMatchObject({
      hizo: 'SI',
      quePaso: 'FUNCIONO',
      comentario: 'Vino gente nueva',
    });
    // Queda anotada junto a las demás decisiones de la semana.
    expect(estado.autogestion.get(ID_OBJETIVO_SIN_DATOS_DE_EJEMPLO)?.decisiones).toContainEqual(
      expect.objectContaining({ tipo: 'MINI_HECHO', miniObjetivoId: MINI_PENDIENTE }),
    );
  });

  it('al responder una aparece la otra que faltaba', async () => {
    await givenQueSamiPreguntaUnMiercoles();

    await whenSeResponde(PRIMERA, { hizo: 'Sí', quePaso: 'Funcionó' });

    const seccion = await seccionDeSami();
    await waitFor(() => thenSePreguntaPor(seccion, [SEGUNDA]), ESPERA);
    thenNoHayPaginador(seccion);
  });

  it('en parte o no, deja elegir «no tuve tiempo» y la persona puede volver y cambiar de idea', async () => {
    await givenQueSamiPreguntaUnMiercoles();
    await whenSeElige(PRIMERA, 'No');
    await whenSeElige(PRIMERA, 'No tuve tiempo');

    await whenSeVuelveUnPaso(PRIMERA);
    thenLaOpcionEsta(PRIMERA, 'No tuve tiempo', 'elegida');
    await whenSeVuelveUnPaso(PRIMERA);
    thenLaOpcionEsta(PRIMERA, 'No', 'elegida');
    // Cambia a «sí»: «no tuve tiempo» ya no vale y hay que elegir de nuevo.
    await whenSeElige(PRIMERA, 'Sí');

    thenLaOpcionNoSePuedeElegir(PRIMERA, 'No tuve tiempo');
    thenLaOpcionEsta(PRIMERA, 'No tuve tiempo', 'sinElegir');
    thenNoHayBotonDeEnviar(PRIMERA);
  });

  it('si el servidor ya tenía la respuesta, lo dice y no pisa la primera', async () => {
    await givenQueSamiPreguntaUnMiercoles();
    givenQueElServidorYaTeniaLaRespuesta();

    await whenSeResponde(PRIMERA, { hizo: 'Sí', quePaso: 'Funcionó' });

    expect(await screen.findByRole('alert', {}, ESPERA)).toHaveTextContent(
      'Ya le contaste a Sami cómo te fue esta semana.',
    );
    expect(estado.respuestasDeMinis.has(MINI_SIN_CONTAR)).toBe(false);
  });

  it('el objetivo muestra sus manuales: el que falta contar con las respuestas rápidas y el que ya se contó', async () => {
    conObjetivoSinDatos();
    renderApp(DETALLE);

    const minis = within(await screen.findByRole('region', { name: 'Tus mini-objetivos' }, ESPERA));
    expect(await minis.findAllByRole('article')).toHaveLength(3);
    expect(minis.getByRole('form', { name: /Poné un cartel en la vereda/ })).toBeVisible();
    expect(minis.getByRole('form', { name: /Anotá qué te piden/ })).toBeVisible();
    // El que ya se contó no vuelve a pedir nada.
    const respondida = within(
      minis
        .getByRole('heading', { name: 'Pedile a 3 clientes que te recomienden con un vecino' })
        .closest('article') as HTMLElement,
    );
    expect(respondida.getByText(/Ya se lo contaste a Sami/)).toBeVisible();
    expect(respondida.queryByRole('button', { name: 'Contarle a Sami' })).not.toBeInTheDocument();
    expect(estado.respuestasDeMinis.has(MINI_RESPONDIDA)).toBe(true);
    // Sin meta en pesos no hay franja de avance ni gráfico.
    expect(screen.queryByRole('region', { name: 'Avance hacia tu meta' })).not.toBeInTheDocument();
    expect(screen.queryByRole('img', { name: /meta de/ })).not.toBeInTheDocument();
  });

  it('el resumen del domingo cuenta lo que se cumplió, lo que no y lo nuevo que propone Sami', async () => {
    conObjetivoSinDatos();
    renderApp(DETALLE);

    const resumen = within(
      await screen.findByRole('region', { name: /Resumen del domingo/ }, ESPERA),
    );
    const cumplido = within(
      resumen.getByRole('heading', { name: 'Lo que cumpliste' }).parentElement as HTMLElement,
    );
    expect(cumplido.getByText('Hablá con 3 clientes sobre lo que querés lograr')).toBeVisible();
    const noCumplido = within(
      resumen.getByRole('heading', { name: 'Lo que no se dio' }).parentElement as HTMLElement,
    );
    expect(noCumplido.getByText('Preguntale a 5 clientes qué les falta en el local')).toBeVisible();
    expect(noCumplido.getByText('· en parte')).toBeVisible();
    expect(noCumplido.getByText('Saludá por el nombre a los clientes de siempre')).toBeVisible();
    expect(noCumplido.getByText('· no se pudo')).toBeVisible();
    // Un manual que quedó sin respuesta no se renueva solo: se cuenta como «sin contar».
    expect(noCumplido.getByText('Anotá qué te piden y no tenés')).toBeVisible();
    expect(noCumplido.getByText('· sin contar')).toBeVisible();
    const nuevo = within(
      resumen.getByRole('heading', { name: 'Lo nuevo que propone Sami' })
        .parentElement as HTMLElement,
    );
    expect(nuevo.getByText('Poné un cartel en la vereda con lo que ofrecés')).toBeVisible();
    expect(nuevo.getByText('Pedile a 3 clientes que te recomienden con un vecino')).toBeVisible();
  });
});
