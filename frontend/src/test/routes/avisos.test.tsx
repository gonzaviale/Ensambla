import type { QueryClient } from '@tanstack/react-query';
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
  setupUsuarioConSesion as setupUsuarioBase,
  verifyHeadingPresentAsync,
  verifyToastAsync,
} from '@/test/testHelpers';

// Estos casos revisan todas las herramientas; los límites gratuitos tienen pruebas propias.
const setupUsuarioConSesion = (...args: Parameters<typeof setupUsuarioBase>) => {
  const usuario = setupUsuarioBase(...args);
  estado.pruebasPremium.set(usuario.email, {
    inicioEn: new Date(Date.now() - 1000).toISOString(),
    finEn: new Date(Date.now() + 7 * 86400000).toISOString(),
    paginasIaUtilizadas: 0,
    limitePaginasIa: 10,
  });
  return usuario;
};

const abrirConDemo = async (ruta: 'usar' | 'construir' = 'usar', configurado = false) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  if (configurado)
    estado.negocios.set(usuario.email, {
      nombre: 'Mi almacén',
      rubro: 'ALMACEN',
      comparteDatosZona: true,
      zona: null,
      ubicacion: {
        direccion: 'Calle 123',
        localidad: 'San Justo',
        latitud: -34.67,
        longitud: -58.56,
      },
    });
  const sistema = crearAlmacenDeEjemplo(hoy());
  estado.sistemas.set(usuario.email, [sistema]);
  const router = renderApp(`/sistemas/${sistema.id}?modo=${ruta}`);
  await screen.findByRole('heading', { level: 1, name: /Almacén de ejemplo/ }, ESPERA);
  await waitFor(() => expect(useSistemaStore.getState().avisos.length).toBeGreaterThan(0), ESPERA);
  return {
    user: userEvent.setup(),
    sistema,
    usuario,
    cliente: router.options.context.queryClient as QueryClient,
  };
};

const productoDe = (sistema: Sistema | undefined, nombre: string) =>
  sistema?.sections[0]?.blocks[0]?.rows.find((fila) => fila.name === nombre);

const sistemaActual = () => useSistemaStore.getState().sistemas[0];

// La campana despliega un resumen; «Ver todos los avisos» lleva a la pantalla completa.
const verTodosDesdeLaCampana = async (user: ReturnType<typeof userEvent.setup>) => {
  await user.click(screen.getByRole('button', { name: /Avisos de Sami/ }));
  const menu = await screen.findByRole('dialog', { name: 'Avisos recientes' }, ESPERA);
  await user.click(within(menu).getByRole('button', { name: /Ver todos los avisos/ }));
};

const abrirPanel = async (user: ReturnType<typeof userEvent.setup>) => {
  await verTodosDesdeLaCampana(user);
  return screen.findByRole('region', { name: /Lo que encontré en tus datos/ }, ESPERA);
};

const tarjeta = (titulo: RegExp | string) => {
  const nodo = screen.getByRole('heading', { level: 3, name: titulo }).closest('article');
  if (!nodo) throw new Error(`Falta el aviso ${titulo}`);
  return nodo;
};

describe('la campana y la pantalla de avisos', () => {
  it('invita a completar el comercio y lleva a la pantalla de configuración', async () => {
    const { user } = await abrirConDemo();
    await abrirPanel(user);
    await user.click(await screen.findByRole('link', { name: 'Configurar mi comercio' }, ESPERA));
    expect(await screen.findByRole('heading', { name: 'Ubicación y rubro' }, ESPERA)).toBeVisible();
  });

  it('no invita a completar una ubicación que ya está guardada', async () => {
    const { user } = await abrirConDemo('usar', true);
    const panel = await abrirPanel(user);
    expect(within(panel).queryByRole('link', { name: 'Configurar mi comercio' })).toBeNull();
  });

  it('la campana cuenta los avisos y se pone urgente si hay algo para hoy', async () => {
    await abrirConDemo();

    const campana = screen.getByRole('button', { name: 'Avisos de Sami: 9' });
    expect(campana).toHaveClass('urgente');
    expect(within(campana).getByText('9')).toBeVisible();
  });

  it('la campana abre a la derecha un resumen de los avisos y se cierra con Escape o la ✕', async () => {
    const { user } = await abrirConDemo();
    const campana = screen.getByRole('button', { name: 'Avisos de Sami: 9' });
    expect(campana).toHaveAttribute('aria-expanded', 'false');

    await user.click(campana);
    const menu = screen.getByRole('dialog', { name: 'Avisos recientes' });
    expect(campana).toHaveAttribute('aria-expanded', 'true');
    expect(within(menu).getAllByRole('listitem')).toHaveLength(9);
    expect(within(menu).getByText(/Deslizá un aviso/)).toBeVisible();

    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog', { name: 'Avisos recientes' })).toBeNull();
    expect(campana).toHaveFocus();

    await user.click(campana);
    await user.click(screen.getByRole('button', { name: 'Cerrar los avisos' }));
    expect(screen.queryByRole('dialog', { name: 'Avisos recientes' })).toBeNull();
  });

  it('sacar un aviso de la campana no lo borra de la pantalla de avisos', async () => {
    const { user } = await abrirConDemo();
    await user.click(screen.getByRole('button', { name: 'Avisos de Sami: 9' }));
    const menu = screen.getByRole('dialog', { name: 'Avisos recientes' });
    const primero = within(menu).getAllByRole('listitem')[0];
    if (!primero) throw new Error('Falta un aviso en la campana');
    const titulo = primero.querySelector('strong')?.textContent ?? '';

    await user.click(within(primero).getByRole('button', { name: /Sacar de acá/ }));
    expect(within(menu).getAllByRole('listitem')).toHaveLength(8);
    expect(within(menu).queryByText(titulo)).toBeNull();
    expect(screen.getByRole('button', { name: 'Avisos de Sami: 8' })).toBeVisible();

    await user.click(within(menu).getByRole('button', { name: /Ver todos los avisos/ }));
    const panel = await screen.findByRole(
      'region',
      { name: /Lo que encontré en tus datos/ },
      ESPERA,
    );
    expect(within(panel).getByText(/9 en total/)).toBeVisible();
    expect(within(panel).getByRole('heading', { level: 3, name: titulo })).toBeVisible();
  });

  it('tocar un aviso de la campana lo abre resaltado en la pantalla de avisos', async () => {
    const { user } = await abrirConDemo();
    await user.click(screen.getByRole('button', { name: 'Avisos de Sami: 9' }));
    const menu = screen.getByRole('dialog', { name: 'Avisos recientes' });
    const primero = within(menu).getAllByRole('listitem')[0];
    if (!primero) throw new Error('Falta un aviso en la campana');
    const nombre = primero.querySelector('strong')?.textContent ?? '';

    await user.click(
      within(primero).getByRole('button', {
        name: (texto) => !texto.startsWith('Sacar') && texto.includes(nombre),
      }),
    );
    await screen.findByRole('region', { name: /Lo que encontré en tus datos/ }, ESPERA);
    expect(screen.queryByRole('dialog', { name: 'Avisos recientes' })).toBeNull();
    expect(tarjeta(nombre)).toHaveClass('foco');
  });

  it('la campana lleva a la pantalla de avisos, que agrupa y filtra', async () => {
    const { user } = await abrirConDemo();

    const panel = await abrirPanel(user);
    expect(
      screen.getByRole('heading', { level: 1, name: 'Lo que encontré en tus datos' }),
    ).toBeVisible();
    expect(within(panel).getByText(/9 en total/)).toBeVisible();
    expect(within(panel).getByText(/12 comercios, sin nombres/)).toBeVisible();
    await user.click(within(panel).getByRole('button', { name: /Tu zona/ }));
    expect(panel.querySelectorAll('.alerta')).toHaveLength(2);
    await user.click(within(panel).getByRole('button', { name: /Urgentes/ }));
    expect(panel.querySelectorAll('.alerta.p1')).toHaveLength(0);
  });

  it('la pestaña «Avisos» de arriba abre la misma pantalla', async () => {
    setupUsuarioConSesion();
    renderApp('/inicio');
    await verifyHeadingPresentAsync(/En qué puedo/);
    const user = userEvent.setup();

    await user.click(
      within(screen.getByRole('navigation', { name: 'Principal' })).getByRole('button', {
        name: 'Avisos',
      }),
    );

    expect(await screen.findByRole('region', { name: /Tus avisos/ }, ESPERA)).toBeVisible();
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

  it('descartar oculta el aviso y se guarda en las preferencias', async () => {
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

    await user.click(within(panel).getByRole('button', { name: /Elegir qué avisos recibo/ }));
    await screen.findByRole('heading', { name: 'Qué me avisa Sami' });
    await user.click(screen.getByRole('button', { name: 'Volver a mostrar 1 aviso oculto' }));
    await waitFor(() => expect(useSistemaStore.getState().avisos).toHaveLength(antes));
  });

  it('apagar un grupo saca sus avisos', async () => {
    const { user } = await abrirConDemo();
    const panel = await abrirPanel(user);

    await user.click(within(panel).getByRole('button', { name: /Elegir qué avisos recibo/ }));
    await screen.findByRole('heading', { name: 'Qué me avisa Sami' });
    await user.click(screen.getByRole('checkbox', { name: 'Tu stock' }));

    await waitFor(() =>
      expect(useSistemaStore.getState().avisos.some((aviso) => aviso.grupo === 'stock')).toBe(
        false,
      ),
    );
    expect(screen.getByRole('checkbox', { name: 'Tu stock' })).not.toBeChecked();
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
        name: 'Sumar a mi inventario',
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
          name: 'Crear el combo',
        },
      ),
    );

    expect(
      productoDe(sistemaActual(), 'Combo Alfajor Jorgito + Galletitas Criollitas 3 U'),
    ).toMatchObject({ price: 1850 });
  });

  it('«Preguntale a Sami» lleva al taller de Sami', async () => {
    const { user } = await abrirConDemo();
    const panel = await abrirPanel(user);

    await user.click(within(panel).getByRole('button', { name: /Preguntale a Sami/ }));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Lo armamos juntos.' }, ESPERA),
    ).toBeVisible();
  });
});

describe('sin datos y con el almacén de ejemplo', () => {
  it('sin sistemas, la pantalla lo dice y ofrece probar con un almacén de ejemplo', async () => {
    setupUsuarioConSesion();
    renderApp('/inicio');
    await verifyHeadingPresentAsync(/En qué puedo/);
    const user = userEvent.setup();

    await verTodosDesdeLaCampana(user);
    const panel = await screen.findByRole('region', { name: /Tus avisos/ }, ESPERA);
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
    expect(await screen.findByRole('region', { name: /Lo que encontré/ }, ESPERA)).toBeVisible();
  });

  it('en construir no hay franja: sigue la barra de distribución', async () => {
    await abrirConDemo('construir');

    expect(screen.queryByLabelText('Avisos de Sami', { selector: 'section' })).toBeNull();
    expect(screen.getByRole('group', { name: 'Distribuí tus piezas' })).toBeVisible();
  });
});

describe('«Sami vio un cambio»', () => {
  it('no avisa al abrir, pero sí cuando un cambio genera un aviso nuevo', async () => {
    const { cliente } = await abrirConDemo();
    expect(document.getElementById('sami-aviso')).toBeNull();
    // Mientras cargan los avisos del servidor el motor todavía no tomó su línea base: sin esperarlos, el cambio
    // de abajo quedaría incluido en ella y no se avisaría.
    await waitFor(
      () => expect(cliente.isFetching({ queryKey: ['avisos', 'pendientes'] })).toBe(0),
      ESPERA,
    );
    await waitFor(
      () => expect(cliente.getQueryState(['avisos', 'pendientes'])?.status).toBe('success'),
      ESPERA,
    );
    await act(async () => {
      await new Promise((resolver) => setTimeout(resolver, 50));
    });

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
    expect(await screen.findByRole('region', { name: /Lo que encontré/ }, ESPERA)).toBeVisible();
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

  it('aparecen en la campana y en la pantalla junto con los del navegador', async () => {
    const { user } = await abrirConAvisosDelServidor(avisoDelServidor());

    await waitFor(
      () => expect(useSistemaStore.getState().avisos.length).toBeGreaterThan(1),
      ESPERA,
    );
    const total = useSistemaStore.getState().avisos.length;
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

  it('si el servidor trae un tipo de aviso, no se duplica con el que Sami calcula en el navegador', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    const sistema = crearAlmacenDeEjemplo(hoy());
    estado.sistemas.set(usuario.email, [sistema]);
    estado.avisosDelServidor.set(usuario.email, [
      avisoDelServidor({
        tipo: 'ESTANCADO',
        grupo: 'VENTAS',
        prioridad: 2,
        entidad: 'registro:azucar',
        titulo: 'Azúcar Ledesma 1 kg está caro y no se mueve',
        detalle: 'No vendiste ninguno en los últimos 30 días y lo tenés a $1.500.',
        motivos: ['En tu zona, 8 comercios lo venden entre $1.000 y $1.300.'],
        acciones: [{ tipo: 'EDITAR_REGISTRO', texto: 'Rebajar a $1.150', registroId: 'fila-1' }],
      }),
    ]);
    renderApp(`/sistemas/${sistema.id}?modo=usar`);
    await screen.findByRole('heading', { level: 1, name: /Almacén de ejemplo/ }, ESPERA);
    await waitFor(() => expect(delServidor()).toHaveLength(1), ESPERA);
    // Los del navegador se muestran cuando el plan los habilita: hay que esperar a que lleguen.
    const locales = (tipo: string) =>
      useSistemaStore.getState().avisos.some((aviso) => aviso.tipo === tipo && !aviso.servidorId);
    await waitFor(() => expect(locales('combo')).toBe(true), ESPERA);

    expect(useSistemaStore.getState().avisos.filter((aviso) => aviso.tipo === 'estancado')).toEqual([
      expect.objectContaining({ servidorId: expect.any(String) }),
    ]);
    expect(locales('stock')).toBe(true);
  });

  it('si el servidor no responde, siguen los avisos que Sami calcula en el navegador', async () => {
    setupServerError('get', '/avisos', 500, 'Error interno');
    await abrirConDemo();

    expect(screen.getByRole('button', { name: 'Avisos de Sami: 9' })).toBeVisible();
  });
});
