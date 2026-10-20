import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { makeSystem } from '@/lib/sistema/core';
import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import type { Sistema } from '@/lib/sistema/tipos';
import { estado } from '@/mocks/db/estado';
import { useSistemaStore } from '@/stores/sistemaStore';
import { avisoDelServidor } from '@/test/avisosData';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  clickButton,
  setupServerError,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyToastAsync,
} from '@/test/testHelpers';

const abrirConDemo = async (ruta: 'usar' | 'construir' = 'usar') => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  const sistema = crearAlmacenDeEjemplo(hoy());
  estado.sistemas.set(usuario.email, [sistema]);
  renderApp(`/sistemas/${sistema.id}?modo=${ruta}`);
  await screen.findByRole('heading', { level: 1, name: /Almacén de ejemplo/ }, ESPERA);
  await waitFor(() => expect(useSistemaStore.getState().avisos.length).toBeGreaterThan(0), ESPERA);
  return { user: userEvent.setup(), sistema, usuario };
};

const productoDe = (sistema: Sistema | undefined, nombre: string) =>
  sistema?.sections[0]?.blocks[0]?.rows.find((fila) => fila.name === nombre);

const sistemaActual = () => useSistemaStore.getState().sistemas[0];

const abrirPanel = async (user: ReturnType<typeof userEvent.setup>) => {
  await user.click(screen.getByRole('button', { name: /Avisos de Sami/ }));
  return screen.findByRole('dialog', { name: /Lo que encontré en tus datos/ });
};

const tarjeta = (titulo: RegExp | string) => {
  const nodo = screen.getByRole('heading', { level: 3, name: titulo }).closest('article');
  if (!nodo) throw new Error(`Falta el aviso ${titulo}`);
  return nodo;
};

describe('la campana y el panel de avisos', () => {
  it('la campana cuenta los avisos y se pone urgente si hay algo para hoy', async () => {
    await abrirConDemo();

    const campana = screen.getByRole('button', { name: 'Avisos de Sami: 9' });
    expect(campana).toHaveClass('urgente');
    expect(within(campana).getByText('9')).toBeVisible();
  });

  it('el panel agrupa, filtra y cierra con Esc devolviendo el foco a la campana', async () => {
    const { user } = await abrirConDemo();

    const panel = await abrirPanel(user);
    expect(within(panel).getByText(/9 en total/)).toBeVisible();
    expect(within(panel).getByText(/12 comercios, sin nombres/)).toBeVisible();
    await user.click(within(panel).getByRole('button', { name: /Tu zona/ }));
    expect(panel.querySelectorAll('.alerta')).toHaveLength(2);
    await user.click(within(panel).getByRole('button', { name: /Urgentes/ }));
    expect(panel.querySelectorAll('.alerta.p1')).toHaveLength(0);

    await user.keyboard('{Escape}');
    await waitFor(
      () => expect(screen.queryByRole('dialog', { name: /Lo que encontré/ })).toBeNull(),
      ESPERA,
    );
    expect(screen.getByRole('button', { name: /Avisos de Sami/ })).toHaveFocus();
  });

  it('cada aviso dice por qué y ofrece Aplicar y Descartar', async () => {
    const { user } = await abrirConDemo();
    await abrirPanel(user);

    const aviso = tarjeta('Te quedaste sin Aceite Natura 900 ml');
    expect(within(aviso).getByText(/En los últimos 30 días vendiste 10 unidades/)).toBeVisible();
    expect(within(aviso).getByRole('button', { name: 'Registrar entrada' })).toBeVisible();
    expect(within(aviso).getByRole('button', { name: 'Descartar' })).toBeVisible();

    const subida = tarjeta(/Yerba Playadito 1 kg se vende mucho/);
    expect(
      within(subida).getByRole('button', { name: /Aplicar: subir a \$ 3\.820/ }),
    ).toBeVisible();
  });

  it('aplicar un aviso cambia el precio y el aviso desaparece', async () => {
    const { user, usuario } = await abrirConDemo();
    await abrirPanel(user);

    await user.click(
      within(tarjeta(/Yerba Playadito 1 kg se vende mucho/)).getByRole('button', {
        name: /Aplicar: subir a \$ 3\.820/,
      }),
    );

    expect(productoDe(sistemaActual(), 'Yerba Playadito 1 kg')?.price).toBe(3820);
    expect(
      screen.queryByRole('heading', { level: 3, name: /Yerba Playadito 1 kg se vende mucho/ }),
    ).toBeNull();
    await verifyToastAsync('Yerba Playadito 1 kg ahora está a $ 3.820.');
    await waitFor(
      () =>
        expect(
          productoDe(estado.sistemas.get(usuario.email)?.[0], 'Yerba Playadito 1 kg')?.price,
        ).toBe(3820),
      ESPERA,
    );
  });

  it('descartar oculta el aviso, se guarda en las preferencias y se puede restaurar', async () => {
    const { user, usuario } = await abrirConDemo();
    const panel = await abrirPanel(user);
    const antes = useSistemaStore.getState().avisos.length;

    await user.click(
      within(tarjeta('Azúcar Ledesma 1 kg está caro y no se mueve')).getByRole('button', {
        name: 'Descartar',
      }),
    );

    await waitFor(() => expect(useSistemaStore.getState().avisos).toHaveLength(antes - 1));
    await waitFor(
      () => expect(estado.preferencias.get(usuario.email)?.descartadas).toHaveLength(1),
      ESPERA,
    );

    await user.click(within(panel).getByText('Qué me avisa Sami'));
    await user.click(
      within(panel).getByRole('button', { name: 'Volver a mostrar 1 aviso oculto' }),
    );
    await waitFor(() => expect(useSistemaStore.getState().avisos).toHaveLength(antes));
  });

  it('apagar un grupo saca sus avisos', async () => {
    const { user } = await abrirConDemo();
    const panel = await abrirPanel(user);

    await user.click(within(panel).getByText('Qué me avisa Sami'));
    await user.click(within(panel).getByRole('checkbox', { name: 'Tu stock' }));

    await waitFor(() =>
      expect(useSistemaStore.getState().avisos.some((aviso) => aviso.grupo === 'stock')).toBe(
        false,
      ),
    );
    expect(within(panel).getByRole('checkbox', { name: 'Tu stock' })).not.toBeChecked();
  });

  it('registrar entrada abre el formulario del bloque de entradas', async () => {
    const { user } = await abrirConDemo();
    await abrirPanel(user);

    await user.click(
      within(tarjeta('Te quedaste sin Aceite Natura 900 ml')).getByRole('button', {
        name: 'Registrar entrada',
      }),
    );

    const modal = await screen.findByRole('dialog', { name: 'Registrar entrada' }, ESPERA);
    expect(modal).toBeVisible();
  });

  it('sumar a mi inventario agrega lo que se vende en la zona', async () => {
    const { user } = await abrirConDemo();
    await abrirPanel(user);

    await user.click(
      within(tarjeta(/Leche La Serenísima 1 L y no lo tenés/)).getByRole('button', {
        name: /Aplicar: sumar a mi inventario/,
      }),
    );

    const leche = productoDe(sistemaActual(), 'Leche La Serenísima 1 L');
    expect(leche).toMatchObject({ quantity: 0, price: 1650 });
    await waitFor(() =>
      expect(
        useSistemaStore
          .getState()
          .avisos.some((aviso) => aviso.tipo === 'consumo_zona' && aviso.titulo.includes('Leche')),
      ).toBe(false),
    );
  });

  it('crear el combo suma un producto nuevo al inventario', async () => {
    const { user } = await abrirConDemo();
    await abrirPanel(user);

    await user.click(
      within(tarjeta(/Alfajor Jorgito y Galletitas Criollitas 3 U se venden juntos/)).getByRole(
        'button',
        {
          name: /Aplicar: crear el combo/,
        },
      ),
    );

    expect(
      productoDe(sistemaActual(), 'Combo Alfajor Jorgito + Galletitas Criollitas 3 U'),
    ).toMatchObject({ price: 1850 });
  });

  it('se le pueden hacer preguntas a Sami', async () => {
    const { user } = await abrirConDemo();
    const panel = await abrirPanel(user);

    await user.type(
      within(panel).getByLabelText('Preguntale a Sami'),
      '¿Cuánto cobran la yerba en mi zona?',
    );
    await user.click(within(panel).getByRole('button', { name: 'Preguntar' }));

    expect(await within(panel).findByText(/va de \$ 3\.500 a \$ 4\.300/)).toBeVisible();
    expect(within(panel).getByText('11 comercios · de $ 3.500 a $ 4.300')).toBeVisible();
    await user.click(within(panel).getByRole('button', { name: 'Volver a todos los avisos' }));
    expect(panel.querySelectorAll('.alerta').length).toBeGreaterThan(0);

    await user.click(within(panel).getByRole('button', { name: '¿Qué tengo que reponer?' }));
    expect(await within(panel).findByText(/Tenés 3 productos para reponer/)).toBeVisible();
  });
});

describe('sin datos y con el almacén de ejemplo', () => {
  it('sin sistemas, el panel lo dice y ofrece probar con un almacén de ejemplo', async () => {
    setupUsuarioConSesion();
    renderApp('/inicio');
    await verifyHeadingPresentAsync(/En qué puedo/);
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: /Avisos de Sami/ }));
    const panel = await screen.findByRole('dialog', { name: /Tus avisos/ });
    expect(within(panel).getByText('Todavía no tengo datos para mirar.')).toBeVisible();
    await user.click(
      within(panel).getByRole('button', { name: 'Probar con un almacén de ejemplo' }),
    );

    await waitFor(() => expect(useSistemaStore.getState().sistemas).toHaveLength(1), ESPERA);
    expect(useSistemaStore.getState().sistemas[0]?.demoAlmacen).toBe(true);
    expect(await screen.findByText(/Encontré 9 cosas para mirar/, {}, ESPERA)).toBeVisible();
  });

  it('la franja de Sami muestra lo principal del sistema en modo usar', async () => {
    const { user } = await abrirConDemo();

    const franja = screen.getByLabelText('Avisos de Sami', { selector: 'section' });
    expect(within(franja).getByText('Encontré 9 cosas para mirar.')).toBeVisible();
    expect(franja.querySelectorAll('.sami-chip')).toHaveLength(3);
    await user.click(within(franja).getAllByRole('button')[0] as HTMLElement);
    expect(await screen.findByRole('dialog', { name: /Lo que encontré/ })).toBeVisible();
  });

  it('en construir no hay franja: sigue la barra de distribución', async () => {
    await abrirConDemo('construir');

    expect(screen.queryByLabelText('Avisos de Sami', { selector: 'section' })).toBeNull();
    expect(screen.getByText('Distribuí tus piezas')).toBeVisible();
  });
});

describe('«Sami vio un cambio»', () => {
  it('no avisa al abrir, pero sí cuando un cambio genera un aviso nuevo', async () => {
    await abrirConDemo();
    expect(document.getElementById('sami-aviso')).toBeNull();

    // Bajar la gaseosa por debajo de su mínimo: aparece un aviso de stock nuevo.
    act(() => {
      useSistemaStore.getState().actualizarSistema(sistemaActual()?.id ?? '', (sistema) => {
        const gaseosa = sistema.sections[0]?.blocks[0]?.rows.find(
          (fila) => fila.name === 'Coca-Cola 2,25 L',
        );
        if (gaseosa) gaseosa.quantity = 3;
      });
    });

    const aviso = await waitFor(() => {
      const nodo = document.getElementById('sami-aviso');
      if (!nodo) throw new Error('Todavía no apareció');
      return nodo;
    }, ESPERA);
    expect(within(aviso).getByText('Sami vio un cambio')).toBeVisible();
    expect(within(aviso).getByText('Coca-Cola 2,25 L está por debajo del mínimo')).toBeVisible();

    await clickButton(userEvent.setup(), 'Ver el aviso');
    expect(await screen.findByRole('dialog', { name: /Lo que encontré/ })).toBeVisible();
    expect(document.querySelector('.alerta.foco')).toHaveTextContent(
      'Coca-Cola 2,25 L está por debajo del mínimo',
    );
  });
});

// El almacén vacío igual recibe los avisos de la zona que Sami calcula en el navegador: los del servidor se distinguen
// por su id.
const delServidor = () => useSistemaStore.getState().avisos.filter((aviso) => aviso.servidorId);

describe('los avisos del motor del servidor', () => {
  const abrirConAvisosDelServidor = async (...avisos: ReturnType<typeof avisoDelServidor>[]) => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    const sistema = makeSystem('Mi almacén', ['inventory']);
    for (const bloque of sistema.sections.flatMap((seccion) => seccion.blocks)) bloque.rows = [];
    estado.sistemas.set(usuario.email, [sistema]);
    estado.avisosDelServidor.set(usuario.email, avisos);
    renderApp(`/sistemas/${sistema.id}?modo=usar`);
    await screen.findByRole('heading', { level: 1, name: /Mi almacén/ }, ESPERA);
    await waitFor(() => expect(delServidor()).toHaveLength(avisos.length), ESPERA);
    return { user: userEvent.setup(), usuario };
  };

  it('aparecen en la campana y en el panel junto con los del navegador', async () => {
    const { user } = await abrirConAvisosDelServidor(avisoDelServidor());

    const total = useSistemaStore.getState().avisos.length;
    expect(total).toBeGreaterThan(1);
    expect(screen.getByRole('button', { name: `Avisos de Sami: ${total}` })).toHaveClass('urgente');
    await abrirPanel(user);
    expect(
      screen.getByRole('heading', { level: 3, name: 'Te quedaste sin Yerba Playadito 1 kg' }),
    ).toBeVisible();
  });

  it('descartar lo descarta en el servidor y el aviso no vuelve', async () => {
    const { user, usuario } = await abrirConAvisosDelServidor(avisoDelServidor());
    await abrirPanel(user);

    await user.click(
      within(tarjeta('Te quedaste sin Yerba Playadito 1 kg')).getByRole('button', {
        name: 'Descartar',
      }),
    );

    await waitFor(() => expect(delServidor()).toHaveLength(0), ESPERA);
    await waitFor(
      () => expect(estado.avisosDelServidor.get(usuario.email)?.[0]?.estado).toBe('DESCARTADO'),
      ESPERA,
    );
    expect(estado.preferencias.get(usuario.email)?.descartadas).toEqual([]);
  });

  it('aplicar un cambio de datos lo ejecuta el servidor, que cierra el aviso', async () => {
    const { user, usuario } = await abrirConAvisosDelServidor(
      avisoDelServidor({
        acciones: [{ tipo: 'EDITAR_REGISTRO', texto: 'Subir a $ 4.200', registroId: 'fila-1' }],
      }),
    );
    await abrirPanel(user);

    await user.click(
      within(tarjeta('Te quedaste sin Yerba Playadito 1 kg')).getByRole('button', {
        name: 'Aplicar: subir a $ 4.200',
      }),
    );

    await verifyToastAsync('Listo, aplicamos el cambio.');
    await waitFor(() => expect(delServidor()).toHaveLength(0), ESPERA);
    expect(estado.avisosDelServidor.get(usuario.email)?.[0]?.estado).toBe('APLICADO');
  });

  it('si el servidor no puede aplicar el cambio, el aviso vuelve y se avisa', async () => {
    const { user } = await abrirConAvisosDelServidor(
      avisoDelServidor({
        acciones: [{ tipo: 'EDITAR_REGISTRO', texto: 'Subir a $ 4.200', registroId: 'fila-1' }],
      }),
    );
    await abrirPanel(user);
    setupServerError('post', '/avisos/:id/aplicar', 400, 'El aviso ya fue resuelto');

    await user.click(
      within(tarjeta('Te quedaste sin Yerba Playadito 1 kg')).getByRole('button', {
        name: 'Aplicar: subir a $ 4.200',
      }),
    );

    await verifyToastAsync('No pudimos aplicar el cambio. Probá de nuevo.');
    await waitFor(() => expect(delServidor()).toHaveLength(1), ESPERA);
  });

  it('si el servidor no responde, siguen los avisos que Sami calcula en el navegador', async () => {
    setupServerError('get', '/avisos', 500, 'Error interno');
    await abrirConDemo();

    expect(screen.getByRole('button', { name: 'Avisos de Sami: 9' })).toBeVisible();
  });
});
