import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  clickButton,
  setupServerError,
  setupServerResponse,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
  verifyToastAsync,
} from '@/test/testHelpers';

const abrirSami = async () => {
  setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  const router = renderApp('/sami');
  await verifyHeadingPresentAsync('Armemos tu proyecto.');
  return { router, user: userEvent.setup() };
};

const contar = async (user: ReturnType<typeof userEvent.setup>, texto: string) => {
  await user.type(screen.getByLabelText('Contame un poco de tu negocio'), texto);
  await clickButton(user, /Pensalo conmigo/);
};

const nombreDelProyecto = () => screen.findByLabelText('Nombre de tu proyecto', {}, ESPERA);

describe('«Armalo con Sami» (crear un proyecto real)', () => {
  it('empieza con el saludo de Sami y sin propuesta', async () => {
    await abrirSami();

    expect(screen.getByText(/Hola, Ana Pérez\. Contame qué hacés/)).toBeVisible();
    expect(screen.getByText('TU PROPUESTA VA A APARECER ACÁ')).toBeVisible();
    expect(screen.queryByLabelText('Nombre de tu proyecto')).toBeNull();
  });

  it('«vendo velas y anoto quién me debe» propone Ventas con la columna de saldo pendiente', async () => {
    const { user } = await abrirSami();

    await contar(user, 'Vendo velas y anoto quién me debe');

    expect(await nombreDelProyecto()).toHaveValue('Ventas');
    expect(
      screen.getByText(/Te propongo empezar con «Ventas» y sumarle la columna Saldo pendiente/),
    ).toBeVisible();
    const vista = within(screen.getByRole('region', { name: 'Tu proyecto' }));
    expect(await vista.findByText('Producto', {}, ESPERA)).toBeVisible();
    expect(vista.getByText('Saldo pendiente')).toBeVisible();
    expect(vista.getByText('Armada por Sami')).toBeVisible();
  });

  it('con una descripción vaga hace una pregunta con respuestas rápidas y después propone', async () => {
    const { user } = await abrirSami();

    await contar(user, 'quiero ordenar mi negocio');
    const rapidas = await screen.findByRole('group', { name: 'Respuestas rápidas' }, ESPERA);
    expect(screen.getByText('¿Qué es lo principal que querés anotar?')).toBeVisible();
    await user.click(within(rapidas).getByRole('button', { name: 'Mis gastos' }));

    expect(await nombreDelProyecto()).toHaveValue('Gastos');
    expect(screen.queryByRole('group', { name: 'Respuestas rápidas' })).toBeNull();
  });

  it('si la persona elige «otra cosa», Sami lo piensa con la IA y lo dice en la propuesta', async () => {
    const { user } = await abrirSami();
    await contar(user, 'quiero ordenar mi negocio');
    await user.click(await screen.findByRole('button', { name: 'Otra cosa' }, ESPERA));

    expect(await nombreDelProyecto()).toBeVisible();
    expect(
      within(screen.getByRole('region', { name: 'Tu proyecto' })).getByText(
        'Armada con ayuda de IA',
      ),
    ).toBeVisible();
  });

  it('si el servidor no tiene propuesta, Sami pide más detalle y deja volver a escribir', async () => {
    const { user } = await abrirSami();
    setupServerResponse('post', '/sami/conversacion', 200, {
      tipo: 'RESPALDO_IA',
      pregunta: null,
      propuesta: null,
      preguntasHechas: 3,
    });

    await contar(user, 'quiero ordenar mi negocio');

    expect(
      await screen.findByText(/todavía no encuentro un punto de partida/, {}, ESPERA),
    ).toBeVisible();
    expect(screen.getByLabelText('Contame un poco de tu negocio')).toBeEnabled();
    expect(screen.queryByLabelText('Nombre de tu proyecto')).toBeNull();
  });

  it('pide un poco más cuando lo escrito es muy corto y no borra lo escrito', async () => {
    const { user } = await abrirSami();

    await contar(user, 'hola');

    await verifyToastAsync(/Contame un poquito más/);
    expect(screen.getByLabelText('Contame un poco de tu negocio')).toHaveValue('hola');
  });

  it('se puede quitar una columna sugerida, volver a sumarla y crear el proyecto con las que quedan', async () => {
    const { user } = await abrirSami();
    await contar(user, 'Vendo velas y anoto quién me debe');
    await nombreDelProyecto();
    await user.click(
      await screen.findByRole('button', { name: /Quitar la columna Saldo pendiente/ }, ESPERA),
    );
    expect(screen.getByText('sin esta')).toBeVisible();
    await user.click(
      screen.getByRole('button', { name: /Volver a sumar la columna Saldo pendiente/ }),
    );
    await user.click(screen.getByRole('button', { name: /Quitar la columna Saldo pendiente/ }));

    await clickButton(user, /Sí, crear mi proyecto/);

    expect(
      await screen.findByText('Tu proyecto «Ventas» ya está creado.', {}, ESPERA),
    ).toBeVisible();
    expect(screen.getByText('5 columnas')).toBeVisible();
  });

  it('crea el proyecto con la columna sugerida y el nombre elegido', async () => {
    const { user } = await abrirSami();
    await contar(user, 'Vendo velas y anoto quién me debe');
    const nombre = await nombreDelProyecto();
    await user.clear(nombre);
    await user.type(nombre, 'Velas de Ana');

    await clickButton(user, /Sí, crear mi proyecto/);

    expect(
      await screen.findByText('Tu proyecto «Velas de Ana» ya está creado.', {}, ESPERA),
    ).toBeVisible();
    expect(screen.getByText('6 columnas')).toBeVisible();
  });

  it('si el servidor rechaza la creación, avisa y deja la propuesta para reintentar', async () => {
    const { user } = await abrirSami();
    await contar(user, 'Vendo velas y anoto quién me debe');
    await nombreDelProyecto();
    setupServerError('post', '/sami/proyectos', 400, 'Revisá la configuración del campo');

    await clickButton(user, /Sí, crear mi proyecto/);

    await verifyToastAsync(/Revisá la configuración del campo/);
    expect(screen.getByRole('button', { name: /Sí, crear mi proyecto/ })).toBeEnabled();
  });

  it('si Sami no puede pensar, avisa con el mensaje del servidor y conserva lo escrito', async () => {
    const { user } = await abrirSami();
    setupServerError(
      'post',
      '/sami/conversacion',
      400,
      'Hoy ya usaste todas las consultas con IA.',
    );

    await contar(user, 'quiero ordenar mi negocio');

    await verifyToastAsync(/ya usaste todas las consultas con IA/);
    expect(screen.getByLabelText('Contame un poco de tu negocio')).toHaveValue(
      'quiero ordenar mi negocio',
    );
  });

  it('«Empezar de nuevo» vuelve al saludo', async () => {
    const { user } = await abrirSami();
    await contar(user, 'Vendo velas y anoto quién me debe');
    await nombreDelProyecto();

    await clickButton(user, 'Empezar de nuevo');

    expect(screen.getByLabelText('Contame un poco de tu negocio')).toHaveValue('');
    expect(screen.queryByLabelText('Nombre de tu proyecto')).toBeNull();
    expect(screen.queryByText('Vendo velas y anoto quién me debe')).toBeNull();
  });

  it('se llega desde el inicio', async () => {
    setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    const router = renderApp('/inicio');
    await verifyHeadingPresentAsync(/En qué puedo/);

    await clickButton(userEvent.setup(), 'Armalo con Sami');

    await verifyPathAsync(router, '/sami');
  });

  it('es accesible', async () => {
    await abrirSami();

    const resultado = await axe(document.body, {
      rules: {
        'color-contrast': { enabled: false },
        region: { enabled: false },
        'heading-order': { enabled: false },
      },
    });

    expect(resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html}`)).toEqual([]);
  });
});
