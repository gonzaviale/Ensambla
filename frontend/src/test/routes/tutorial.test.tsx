import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { estado } from '@/mocks/db/estado';
import { useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
  verifyToastAsync,
} from '@/test/testHelpers';

type Usuario = ReturnType<typeof userEvent.setup>;

const abrirConBienvenida = async () => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' }, { bienvenidaVista: false });
  const router = renderApp('/inicio');
  await screen.findByRole('dialog', { name: /Hola, Ana Pérez/ }, ESPERA);
  return { router, usuario, user: userEvent.setup() };
};

const empezarLaGuia = async (user: Usuario) => {
  await user.click(screen.getByRole('button', { name: /Hacer mi primer sistema con guía/ }));
  await esperarPaso('Tres maneras de empezar.');
};

const cartel = () => screen.getByLabelText('Guía interactiva de Sami');

const esperarPaso = async (titulo: string) => {
  await waitFor(
    () => expect(within(cartel()).getByRole('heading', { level: 2 })).toHaveTextContent(titulo),
    ESPERA,
  );
};

const pasoActual = () => useSistemaStore.getState().tutorial.paso;

const bloqueDelTutorial = () => {
  const id = useSistemaStore.getState().tutorial.bloqueId;
  const nodo = document.querySelector<HTMLElement>(`[data-block="${id}"]`);
  if (!nodo) throw new Error('Falta el bloque del tutorial');
  return nodo;
};

describe('la bienvenida', () => {
  it('aparece la primera vez con el nombre y no vuelve una vez vista', async () => {
    const { user, usuario } = await abrirConBienvenida();

    expect(screen.getByRole('button', { name: /Hacer mi primer sistema con guía/ })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: /Explorar por mi cuenta/ }));

    expect(screen.queryByRole('dialog')).toBeNull();
    expect(useSistemaStore.getState().tutorial.activo).toBe(false);
    await waitFor(
      () => expect(estado.preferencias.get(usuario.email)?.bienvenidaVista).toBe(true),
      ESPERA,
    );
  });

  it('con Esc también se cierra y queda como vista', async () => {
    const { user, usuario } = await abrirConBienvenida();

    await user.keyboard('{Escape}');

    expect(screen.queryByRole('dialog')).toBeNull();
    await waitFor(
      () => expect(estado.preferencias.get(usuario.email)?.bienvenidaVista).toBe(true),
      ESPERA,
    );
  });

  it('si ya fue vista no se muestra', async () => {
    setupUsuarioConSesion({}, { bienvenidaVista: true });
    renderApp('/inicio');
    await verifyHeadingPresentAsync(/En qué puedo/);

    expect(screen.queryByRole('dialog')).toBeNull();
  });
});

describe('el tutorial', () => {
  it('empieza desde la bienvenida con el primer paso y la etapa 1 de 5', async () => {
    const { user } = await abrirConBienvenida();

    await empezarLaGuia(user);

    expect(within(cartel()).getByLabelText('Etapa 1 de 5')).toBeVisible();
    expect(within(cartel()).getByText('Tocá una de las tres opciones.')).toBeVisible();
    expect(screen.queryByText(/Reiniciar la demo/)).toBeNull();
    expect(document.querySelector('.mk-root')).toHaveClass('tutorial-running');
    expect(document.querySelectorAll('.tour-target')).toHaveLength(1);
  });

  it('bloquea lo que no corresponde al paso y recuerda qué hacer', async () => {
    const { user } = await abrirConBienvenida();
    await empezarLaGuia(user);

    await user.click(screen.getByRole('button', { name: 'Plantillas' }));

    await verifyToastAsync('Tocá una de las tres opciones.');
    expect(pasoActual()).toBe('choose');
    expect(screen.getByRole('heading', { name: /En qué puedo/ })).toBeVisible();
  });

  it('sigue el camino de Sami: describir, ajustar el ancho, crear, construir, personalizar, mover, registrar y usar', async () => {
    const { user, router } = await abrirConBienvenida();
    await empezarLaGuia(user);

    await user.click(screen.getByRole('button', { name: /Crear con Sami/ }));
    await esperarPaso('Contame lo que necesitás.');
    await user.type(
      await screen.findByLabelText('Contame un poco de tu negocio'),
      'Tengo un almacén de barrio y quiero anotar mis ventas',
    );
    await user.click(screen.getByRole('button', { name: /Pensalo conmigo/ }));
    await esperarPaso('Tu idea ya tiene forma.');

    await user.click(screen.getAllByRole('button', { name: 'Fila completa' })[1] as HTMLElement);
    await esperarPaso('Cuando te cierre, la creamos.');
    await user.click(screen.getByRole('button', { name: /Sí, crear mi sistema/ }));
    await esperarPaso('Usar y construir, en el mismo lugar.');
    expect(router.state.location.pathname).toMatch(/^\/sistemas\//);

    await user.click(await screen.findByRole('button', { name: /Construir/ }, ESPERA));
    await esperarPaso('De acá se cambia.');
    await user.click(
      within(bloqueDelTutorial()).getByRole('button', { name: 'Personalizar bloque' }),
    );
    await esperarPaso('Ponelo en tus palabras.');

    const nombre = screen.getByLabelText('Nombre');
    expect(nombre).toHaveFocus();
    await user.clear(nombre);
    await user.type(nombre, 'Mercadería');
    await user.tab();
    await esperarPaso('De acá se mueve.');

    await user.click(within(bloqueDelTutorial()).getByRole('button', { name: 'Mover abajo' }));
    await esperarPaso('De acá se llena.');
    await user.click(within(bloqueDelTutorial()).getByRole('button', { name: /Registrar/ }));
    await esperarPaso('Cargá un dato tuyo.');

    const modal = await screen.findByRole('dialog');
    await user.type(within(modal).getByLabelText(/Producto/), 'Yerba mate');
    await user.type(within(modal).getByLabelText(/Cantidad/), '8');
    await user.click(within(modal).getByRole('button', { name: 'Guardar' }));
    await esperarPaso('Ahora, a usarlo.');

    await user.click(screen.getByRole('button', { name: /Usar sistema/ }));
    await esperarPaso('Tu primer sistema ya es tuyo.');
    expect(within(cartel()).getByLabelText('Etapa 5 de 5')).toBeVisible();

    await user.click(within(cartel()).getByRole('button', { name: 'Listo, sigo explorando' }));
    expect(screen.queryByLabelText('Guía interactiva de Sami')).toBeNull();
    await verifyToastAsync('Podés retomar la guía desde Sami en el inicio.');
    expect(document.querySelector('.mk-root')).not.toHaveClass('tutorial-running');
    expect(document.querySelectorAll('.tour-target')).toHaveLength(0);
  });

  it('muestra «Estoy pensando» mientras Sami arma la propuesta', async () => {
    const { user } = await abrirConBienvenida();
    await empezarLaGuia(user);
    await user.click(screen.getByRole('button', { name: /Crear con Sami/ }));
    await esperarPaso('Contame lo que necesitás.');
    await user.type(
      await screen.findByLabelText('Contame un poco de tu negocio'),
      'Tengo un kiosco en la esquina',
    );

    await user.click(screen.getByRole('button', { name: /Pensalo conmigo/ }));

    await esperarPaso('Tu idea ya tiene forma.');
  });

  it('el camino de las plantillas: mirar una, volver y empezar con ella', async () => {
    const { user } = await abrirConBienvenida();
    await empezarLaGuia(user);

    await user.click(screen.getByRole('button', { name: /Elegir una plantilla/ }));
    await esperarPaso('Una plantilla es un conjunto.');
    await screen.findByRole('heading', { name: /Elegí una base/ }, ESPERA);
    const tarjeta = document.querySelector<HTMLElement>(
      '.template-page [data-template="shop"]',
    ) as HTMLElement;
    await user.click(within(tarjeta).getByRole('button', { name: /Ver el sistema/ }));
    await esperarPaso('Mirá las piezas antes de empezar.');

    await user.click(screen.getByRole('button', { name: 'Seguir mirando' }));
    await esperarPaso('Una plantilla es un conjunto.');

    await user.click(within(tarjeta).getByRole('button', { name: /Ver el sistema/ }));
    await esperarPaso('Mirá las piezas antes de empezar.');
    await user.click(screen.getByRole('button', { name: /Empezar con este conjunto/ }));
    await esperarPaso('Usar y construir, en el mismo lugar.');
  });

  it('el camino desde cero: confirmar, agregar una pieza y personalizarla', async () => {
    const { user } = await abrirConBienvenida();
    await empezarLaGuia(user);

    await user.click(screen.getByRole('button', { name: /Empezar de cero/ }));
    await esperarPaso('Tu lienzo, desde cero.');
    await user.click(screen.getByRole('button', { name: /Seguir desde cero/ }));
    await esperarPaso('Traé tu primera pieza.');

    // «Tus piezas» flota cerrada: primero se abre con «Bloques».
    await screen.findByRole('complementary', { name: 'Piezas disponibles' }, ESPERA);
    const abrirPiezas = document.querySelector('[data-accion="mobile-library"]');
    await user.click(abrirPiezas as HTMLElement);
    await user.click(await screen.findByRole('button', { name: 'Agregar Clientes' }, ESPERA));
    await esperarPaso('De acá se cambia.');
  });

  it('cerrar el cartel de «desde cero» vuelve a elegir el camino', async () => {
    const { user } = await abrirConBienvenida();
    await empezarLaGuia(user);
    await user.click(screen.getByRole('button', { name: /Empezar de cero/ }));
    await esperarPaso('Tu lienzo, desde cero.');

    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    await esperarPaso('Tres maneras de empezar.');
  });

  it('Esc sale de la guía y avisa cómo retomarla', async () => {
    const { user } = await abrirConBienvenida();
    await empezarLaGuia(user);

    await user.keyboard('{Escape}');

    expect(screen.queryByLabelText('Guía interactiva de Sami')).toBeNull();
    expect(useSistemaStore.getState().tutorial.activo).toBe(false);
    await verifyToastAsync('Podés retomar la guía desde Sami en el inicio.');
  });

  it('«Explorar sin guía» la cierra y desde el inicio se retoma', async () => {
    const { user } = await abrirConBienvenida();
    await empezarLaGuia(user);

    await user.click(within(cartel()).getByRole('button', { name: 'Explorar sin guía' }));
    expect(screen.queryByLabelText('Guía interactiva de Sami')).toBeNull();

    await user.click(screen.getByRole('button', { name: /Mostrarme cómo empezar/ }));
    await esperarPaso('Tres maneras de empezar.');
  });

  it('ir al inicio durante la guía la termina sin preguntar nada', async () => {
    const { user, router } = await abrirConBienvenida();
    await empezarLaGuia(user);
    await user.click(screen.getByRole('button', { name: /Elegir una plantilla/ }));
    await esperarPaso('Una plantilla es un conjunto.');

    await user.click(screen.getByRole('button', { name: 'Ensambla, ir al inicio' }));

    await verifyPathAsync(router, '/inicio');
    expect(screen.queryByLabelText('Guía interactiva de Sami')).toBeNull();
    expect(screen.queryByText(/¿Volvemos o empezamos de nuevo\?/)).toBeNull();
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('el tutorial no ofrece reiniciar la demo en ningún paso', async () => {
    const { user } = await abrirConBienvenida();
    await empezarLaGuia(user);
    await user.click(screen.getByRole('button', { name: /Elegir una plantilla/ }));

    expect(
      screen.queryByText(/Reiniciar la demo|Borrar todos los sistemas de esta demo/),
    ).toBeNull();
  });
});
