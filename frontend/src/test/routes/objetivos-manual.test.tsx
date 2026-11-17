import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { sembrarDemoCargada } from '@/mocks/db/demoCargada';
import { estado } from '@/mocks/db/estado';
import { ID_OBJETIVO_SIN_DATOS_DE_EJEMPLO } from '@/mocks/db/objetivosAutogestionados';
import {
  givenQueHoyEsLunes,
  givenQueHoyEsMiercoles,
  seccionDeSami,
  thenNoHayPreguntasDeSami,
  thenSePreguntaPor,
  volverAlTiempoReal,
  whenSeDejaParaMasTarde,
  whenSeVenLasOtrasPreguntas,
} from '@/test/preguntasDeSami';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';

const DETALLE = `/objetivos/${ID_OBJETIVO_SIN_DATOS_DE_EJEMPLO}`;
const MINI_SIN_CONTAR = `${ID_OBJETIVO_SIN_DATOS_DE_EJEMPLO}-sin-contar`;
const MINI_PENDIENTE = `${ID_OBJETIVO_SIN_DATOS_DE_EJEMPLO}-pendiente`;
const MINI_RESPONDIDA = `${ID_OBJETIVO_SIN_DATOS_DE_EJEMPLO}-respondida`;

const conObjetivoSinDatos = () => {
  const usuario = setupUsuarioConSesion();
  sembrarDemoCargada(usuario.email);
  return usuario;
};

const formulario = (titulo: string) =>
  within(screen.getByRole('form', { name: `Contale a Sami cómo te fue con «${titulo}»` }));

describe('mini-objetivos manuales', () => {
  afterEach(volverAlTiempoReal);

  it('Sami pregunta una cosa por vez, sin contador ni alarma, y ofrece ver las otras', async () => {
    givenQueHoyEsMiercoles();
    conObjetivoSinDatos();
    renderApp('/objetivos');

    const seccion = await seccionDeSami();

    thenSePreguntaPor(seccion, ['Anotá qué te piden y no tenés']);
    expect(seccion.getByRole('button', { name: 'Ver las otras 1' })).toBeVisible();
    expect(seccion.queryByText(/\(\d+\)/)).not.toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.queryByText(/Lo lograste/)).not.toBeInTheDocument();
  });

  it('al ver las otras aparecen del más viejo al más nuevo, cada una con su objetivo', async () => {
    givenQueHoyEsMiercoles();
    conObjetivoSinDatos();
    renderApp('/objetivos');
    await seccionDeSami();

    await whenSeVenLasOtrasPreguntas();

    const seccion = await seccionDeSami();
    thenSePreguntaPor(seccion, [
      'Anotá qué te piden y no tenés',
      'Poné un cartel en la vereda con lo que ofrecés',
    ]);
    expect(seccion.getAllByText('Que más gente conozca mi local')).toHaveLength(2);
  });

  it('el lunes no pregunta por lo de esta semana, solo por lo que quedó sin contar', async () => {
    givenQueHoyEsLunes();
    conObjetivoSinDatos();
    renderApp('/objetivos');

    const seccion = await seccionDeSami();

    thenSePreguntaPor(seccion, ['Anotá qué te piden y no tenés']);
    expect(seccion.queryByRole('button', { name: /Ver las otras/ })).not.toBeInTheDocument();
  });

  it('«Más tarde» deja la pregunta para mañana', async () => {
    givenQueHoyEsLunes();
    conObjetivoSinDatos();
    renderApp('/objetivos');
    await seccionDeSami();

    await whenSeDejaParaMasTarde('Anotá qué te piden y no tenés');

    await thenNoHayPreguntasDeSami();
  });

  it('se responde en dos toques: ¿lo hiciste? y ¿qué pasó?, y la respuesta queda guardada', async () => {
    givenQueHoyEsMiercoles();
    conObjetivoSinDatos();
    renderApp('/objetivos');
    const user = userEvent.setup();
    await seccionDeSami();
    await whenSeVenLasOtrasPreguntas();
    const tarjeta = formulario('Poné un cartel en la vereda con lo que ofrecés');

    expect(tarjeta.queryByText('¿Qué pasó?')).not.toBeInTheDocument();
    expect(tarjeta.getByRole('button', { name: 'Contarle a Sami' })).toBeDisabled();
    await user.click(tarjeta.getByRole('button', { name: 'Sí' }));
    expect(tarjeta.getByRole('button', { name: 'Sí' })).toHaveAttribute('aria-pressed', 'true');
    // Si lo hiciste, tuviste tiempo.
    expect(tarjeta.getByRole('button', { name: 'No tuve tiempo' })).toBeDisabled();
    await user.click(tarjeta.getByRole('button', { name: 'Funcionó' }));
    await user.type(tarjeta.getByLabelText('Contale un poco más (opcional)'), 'Vino gente nueva');
    await user.click(tarjeta.getByRole('button', { name: 'Contarle a Sami' }));

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
    await waitFor(
      () => expect(screen.queryByRole('button', { name: /Ver las otras/ })).not.toBeInTheDocument(),
      ESPERA,
    );
  });

  it('en parte o no, deja elegir «no tuve tiempo» y la persona puede cambiar de idea antes de enviar', async () => {
    givenQueHoyEsMiercoles();
    conObjetivoSinDatos();
    renderApp('/objetivos');
    const user = userEvent.setup();
    await screen.findByRole('region', { name: /Sami quiere saber cómo te fue/ }, ESPERA);
    const tarjeta = formulario('Anotá qué te piden y no tenés');

    await user.click(tarjeta.getByRole('button', { name: 'No' }));
    await user.click(tarjeta.getByRole('button', { name: 'No tuve tiempo' }));
    expect(tarjeta.getByRole('button', { name: 'No tuve tiempo' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    // Cambia a «sí»: «no tuve tiempo» ya no vale y hay que elegir de nuevo.
    await user.click(tarjeta.getByRole('button', { name: 'Sí' }));
    expect(tarjeta.getByRole('button', { name: 'No tuve tiempo' })).toBeDisabled();
    expect(tarjeta.getByRole('button', { name: 'No tuve tiempo' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
    expect(tarjeta.getByRole('button', { name: 'Contarle a Sami' })).toBeDisabled();
  });

  it('si el servidor ya tenía la respuesta, lo dice y no pisa la primera', async () => {
    givenQueHoyEsMiercoles();
    conObjetivoSinDatos();
    renderApp('/objetivos');
    const user = userEvent.setup();
    await screen.findByRole('region', { name: /Sami quiere saber cómo te fue/ }, ESPERA);
    setupServerError(
      'post',
      `/objetivos/${ID_OBJETIVO_SIN_DATOS_DE_EJEMPLO}/mini-objetivos/${MINI_SIN_CONTAR}/respuesta`,
      409,
      'Ya le contaste a Sami cómo te fue esta semana.',
    );
    const tarjeta = formulario('Anotá qué te piden y no tenés');

    await user.click(tarjeta.getByRole('button', { name: 'Sí' }));
    await user.click(tarjeta.getByRole('button', { name: 'Funcionó' }));
    await user.click(tarjeta.getByRole('button', { name: 'Contarle a Sami' }));

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
