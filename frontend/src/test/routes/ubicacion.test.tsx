import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { delay, http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { axe } from 'vitest-axe';
import { DIRECCIONES_DE_PRUEBA } from '@/mocks/db/ubicacion';
import { handlersPara } from '@/mocks/handlers';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import { estado } from '@/mocks/db/estado';
import {
  ESPERA,
  setupServerError,
  setupServerResponse,
  setupUsuarioConSesion,
} from '@/test/testHelpers';

type Usuario = ReturnType<typeof userEvent.setup>;

async function abrirUbicacion() {
  setupUsuarioConSesion();
  const router = renderApp('/ubicacion');
  await screen.findByRole('heading', { level: 1, name: 'Ubicación y rubro' }, ESPERA);
  await waitFor(() => expect(screen.getByLabelText('Nombre del comercio')).toBeEnabled(), ESPERA);
  return { user: userEvent.setup(), router };
}

// Va a un paso tocándolo en el camino de arriba.
async function irAlPaso(user: Usuario, nombre: 'Tu comercio' | 'Ubicación' | 'Rubro') {
  const camino = await screen.findByRole(
    'navigation',
    { name: 'Pasos de la configuración' },
    ESPERA,
  );
  const boton = () => within(camino).getByRole('button', { name: new RegExp(nombre) });
  // No se puede saltar a un paso si los anteriores no están completos: espera a que se habilite.
  await waitFor(() => expect(boton()).toBeEnabled(), ESPERA);
  await user.click(boton());
}

function botonDelPaso(nombre: 'Tu comercio' | 'Ubicación' | 'Rubro') {
  const camino = screen.getByRole('navigation', { name: 'Pasos de la configuración' });
  return within(camino).getByRole('button', { name: new RegExp(nombre) });
}

// Escribe calle, altura y localidad (borrando lo que hubiera) sin buscar todavía.
async function escribirDireccion(user: Usuario, calle: string, altura: string, localidad: string) {
  for (const [etiqueta, valor] of [
    ['Calle', calle],
    ['Altura', altura],
    ['Localidad', localidad],
  ] as const) {
    await user.clear(screen.getByLabelText(etiqueta));
    if (valor) await user.type(screen.getByLabelText(etiqueta), valor);
  }
}

async function irARubro(user: Usuario) {
  await irAlPaso(user, 'Rubro');
  await screen.findByRole('option', { name: 'Almacén' }, ESPERA);
}

async function elegirRubro(user: Usuario, codigo: string) {
  await irARubro(user);
  await user.selectOptions(screen.getByLabelText('Rubro del comercio'), codigo);
}

async function buscarDireccion(user: Usuario, localidad = 'San Justo') {
  await irAlPaso(user, 'Ubicación');
  await escribirDireccion(user, 'Florencio Varela', '1903', localidad);
  await user.click(screen.getByRole('button', { name: 'Buscar dirección' }));
  await screen.findByRole('radio', { name: DIRECCIONES_DE_PRUEBA[0].direccion }, ESPERA);
}

async function elegirDireccion(user: Usuario) {
  await buscarDireccion(user);
  await user.click(screen.getByRole('radio', { name: DIRECCIONES_DE_PRUEBA[0].direccion }));
}

// La localidad queda verificada cuando el mapa respondió y el campo muestra la del mapa.
async function esperarLocalidad(localidad: string) {
  await waitFor(() => expect(screen.getByLabelText('Localidad')).toHaveValue(localidad), ESPERA);
  await waitFor(
    () =>
      expect(screen.getByText(/Según el mapa:/)).toHaveTextContent(`Según el mapa: ${localidad}`),
    ESPERA,
  );
}

describe('Ubicación y rubro del negocio', () => {
  it('muestra un paso por vez y el camino de arriba marca dónde estás', async () => {
    const { user } = await abrirUbicacion();
    const camino = screen.getByRole('navigation', { name: 'Pasos de la configuración' });
    expect(within(camino).getByRole('button', { name: /Tu comercio/ })).toHaveAttribute(
      'aria-current',
      'step',
    );
    expect(screen.queryByLabelText('Calle')).toBeNull();
    expect(screen.queryByText(/^PASO 1$/i)).toBeNull();

    await user.click(screen.getByRole('button', { name: /Siguiente/ }));
    expect(screen.getByLabelText('Calle')).toBeVisible();
    expect(screen.getByLabelText('Altura')).toBeVisible();
    expect(screen.getByLabelText('Localidad')).toBeVisible();
    expect(screen.queryByLabelText('Nombre del comercio')).toBeNull();
    expect(within(camino).getByRole('button', { name: /Ubicación/ })).toHaveAttribute(
      'aria-current',
      'step',
    );
    expect(
      within(camino)
        .getByRole('button', { name: /Tu comercio/ })
        .closest('li'),
    ).toHaveClass('hecho');
    expect(screen.getByRole('button', { name: /Siguiente/ })).toBeDisabled();
    expect(within(camino).getByRole('button', { name: /Rubro/ })).toBeDisabled();

    await elegirDireccion(user);
    await esperarLocalidad('San Justo');
    await user.click(screen.getByRole('button', { name: /Siguiente/ }));
    expect(screen.getByLabelText('Rubro del comercio')).toBeVisible();
    expect(screen.queryByRole('button', { name: /Siguiente/ })).toBeNull();
    expect(screen.getByRole('button', { name: 'Guardar configuración' })).toBeVisible();

    await user.click(screen.getByRole('button', { name: /Atrás/ }));
    await user.click(screen.getByRole('button', { name: /Atrás/ }));
    expect(screen.getByLabelText('Nombre del comercio')).toBeVisible();
  });

  it('conserva etiquetas y nombres accesibles con dirección, mapa y revisión', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await elegirRubro(user, 'ALMACEN');
    await user.click(screen.getByRole('button', { name: 'Revisar configuración' }));
    const resultado = await axe(document.body, {
      rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
    });
    expect(resultado.violations.map((violacion) => violacion.id)).toEqual([]);
  });

  it('protege la pantalla con el guard de sesión', async () => {
    const router = renderApp('/ubicacion');
    await screen.findByRole('heading', { name: 'Ingresá a tu cuenta' }, ESPERA);
    expect(router.state.location.pathname).toBe('/login');
    expect(screen.queryByRole('heading', { name: 'Ubicación y rubro' })).toBeNull();
  });

  it('busca sólo al enviar el formulario y nunca mientras se escribe', async () => {
    let consultas = 0;
    server.use(
      http.post('/api/ubicacion/direcciones', async ({ request }) => {
        consultas++;
        expect(await request.json()).toEqual({ consulta: 'Florencio Varela 1903, San Justo' });
        expect(request.headers.get('X-CSRF-TOKEN')).toBe('csrf-token-de-prueba');
        return HttpResponse.json({ resultados: DIRECCIONES_DE_PRUEBA });
      }),
    );
    const { user } = await abrirUbicacion();
    await irAlPaso(user, 'Ubicación');
    await escribirDireccion(user, '  Florencio  Varela ', ' 1903', ' San Justo ');
    expect(consultas).toBe(0);
    await user.click(screen.getByRole('button', { name: 'Buscar dirección' }));
    await screen.findAllByRole('radio', {}, ESPERA);
    expect(consultas).toBe(1);
    expect(screen.queryByRole('region', { name: 'Mapa de tu comercio' })).toBeNull();
  });

  it('rechaza una consulta vacía sin llamar al geocodificador', async () => {
    const { user } = await abrirUbicacion();
    await irAlPaso(user, 'Ubicación');
    await user.click(screen.getByRole('button', { name: 'Buscar dirección' }));
    expect(screen.getByText(/Escribí calle, altura y localidad/)).toBeVisible();
    expect(screen.queryByRole('radio')).toBeNull();
  });

  it('permite elegir entre direcciones y revisar punto y rubro sin guardar nada', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    expect(screen.getByRole('region', { name: 'Mapa de tu comercio' })).toBeVisible();
    expect(screen.queryByLabelText('Latitud')).toBeNull();
    await esperarLocalidad('San Justo');
    await elegirRubro(user, 'ALMACEN');
    await user.click(screen.getByRole('button', { name: 'Revisar configuración' }));
    const resumen = screen.getByRole('status', { name: 'Configuración revisada' });
    expect(within(resumen).getByText('Almacén')).toBeVisible();
    expect(resumen).toHaveTextContent('-34.6701, -58.5625');
    expect(resumen).toHaveTextContent('todavía no se guardaron');
    expect(screen.getByRole('button', { name: 'Guardar configuración' })).toBeEnabled();
  });

  it('descarta la revisión cuando cambia un dato', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await elegirRubro(user, 'KIOSCO');
    await user.click(screen.getByRole('button', { name: 'Revisar configuración' }));
    expect(screen.getByRole('status', { name: 'Configuración revisada' })).toHaveTextContent(
      '-34.6701, -58.5625',
    );
    await user.selectOptions(screen.getByLabelText('Rubro del comercio'), 'OTRO');
    expect(screen.queryByRole('status', { name: 'Configuración revisada' })).toBeNull();
  });

  it('exige dirección y rubro', async () => {
    const { user } = await abrirUbicacion();
    await irAlPaso(user, 'Ubicación');
    expect(screen.getByRole('button', { name: /Siguiente/ })).toBeDisabled();
    expect(botonDelPaso('Rubro')).toBeDisabled();
    expect(screen.getByText(/Buscá y elegí tu dirección/)).toBeVisible();
    await elegirDireccion(user);
    await irARubro(user);
    await user.click(screen.getByRole('button', { name: 'Revisar configuración' }));
    expect(screen.getByText('Elegí el rubro de tu comercio.')).toBeVisible();
    await user.selectOptions(screen.getByLabelText('Rubro del comercio'), 'ALMACEN');
    await user.click(screen.getByRole('button', { name: 'Revisar configuración' }));
    expect(screen.getByRole('status', { name: 'Configuración revisada' })).toBeVisible();
  });

  it('descarta el punto anterior cuando se edita el texto buscado', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await user.type(screen.getByLabelText('Calle'), ' otra');
    expect(screen.queryByRole('radio')).toBeNull();
    expect(screen.queryByRole('region', { name: 'Mapa de tu comercio' })).toBeNull();
  });

  it('explica una búsqueda sin resultados y permite volver a buscar', async () => {
    const { user } = await abrirUbicacion();
    await irAlPaso(user, 'Ubicación');
    await escribirDireccion(user, 'Sin coincidencias', '1', 'Ninguna');
    await user.click(screen.getByRole('button', { name: 'Buscar dirección' }));
    expect(await screen.findByText(/No encontramos esa dirección/, {}, ESPERA)).toBeVisible();
    await buscarDireccion(user);
  });

  it('muestra el error del proveedor y permite reintentar sin un punto inventado', async () => {
    setupServerError(
      'post',
      '/ubicacion/direcciones',
      503,
      'Esperá un segundo antes de buscar otra dirección.',
    );
    const { user } = await abrirUbicacion();
    await irAlPaso(user, 'Ubicación');
    await escribirDireccion(user, 'Varela', '1903', 'San Justo');
    await user.click(screen.getByRole('button', { name: 'Buscar dirección' }));
    expect(await screen.findByText(/Esperá un segundo antes de buscar/, {}, ESPERA)).toBeVisible();
    expect(screen.queryByRole('region', { name: 'Mapa de tu comercio' })).toBeNull();
    setupServerResponse('post', '/ubicacion/direcciones', 200, {
      resultados: DIRECCIONES_DE_PRUEBA,
    });
    await user.click(screen.getByRole('button', { name: 'Buscar dirección' }));
    await screen.findAllByRole('radio', {}, ESPERA);
  });

  it('permite recuperar el catálogo de rubros después de un error', async () => {
    setupServerError('get', '/ubicacion/rubros', 503, 'Sin servicio');
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await irAlPaso(user, 'Rubro');
    expect(await screen.findByText('No pudimos cargar los rubros.', {}, ESPERA)).toBeVisible();
    expect(screen.getByRole('button', { name: 'Revisar configuración' })).toBeDisabled();
    server.resetHandlers();
    await user.click(screen.getByRole('button', { name: 'Reintentar rubros' }));
    await screen.findByRole('option', { name: 'Almacén' }, ESPERA);
    expect(screen.getByRole('button', { name: 'Revisar configuración' })).toBeEnabled();
  });

  it('ignora una respuesta tardía si el usuario cambió la dirección', async () => {
    server.use(
      http.post('/api/ubicacion/direcciones', async () => {
        await delay(500);
        return HttpResponse.json({ resultados: DIRECCIONES_DE_PRUEBA });
      }),
    );
    const { user } = await abrirUbicacion();
    await irAlPaso(user, 'Ubicación');
    await escribirDireccion(user, 'Varela', '1903', 'San Justo');
    await user.click(screen.getByRole('button', { name: 'Buscar dirección' }));
    expect(screen.getByRole('button', { name: 'Buscando…' })).toBeDisabled();
    await user.clear(screen.getByLabelText('Calle'));
    await user.type(screen.getByLabelText('Calle'), 'otra calle');
    await delay(550);
    expect(screen.queryByRole('radio')).toBeNull();
    expect(screen.getByRole('button', { name: 'Buscar dirección' })).toBeEnabled();
  });

  it('borra el borrador al salir y no lo recupera al volver', async () => {
    const { user, router } = await abrirUbicacion();
    await elegirDireccion(user);
    await elegirRubro(user, 'ALMACEN');
    await user.click(screen.getByRole('link', { name: /Volver a Mi zona/ }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/zona'), ESPERA);
    await user.click(await screen.findByRole('link', { name: /Probar ubicación y rubro/ }, ESPERA));
    await screen.findByRole('heading', { name: 'Ubicación y rubro' }, ESPERA);
    await irAlPaso(user, 'Ubicación');
    expect(screen.getByLabelText('Calle')).toHaveValue('');
    expect(screen.queryByRole('region', { name: 'Mapa de tu comercio' })).toBeNull();
    expect(botonDelPaso('Rubro')).toBeDisabled();
  });

  it('deja el geocodificador y el catálogo fuera de los mocks parciales', () => {
    const sonDeUbicacion = (path: unknown) => String(path).includes('/ubicacion/');
    expect(handlersPara('parcial').some((handler) => sonDeUbicacion(handler.info.path))).toBe(
      false,
    );
    expect(
      handlersPara('completo').filter((handler) => sonDeUbicacion(handler.info.path)),
    ).toHaveLength(3);
    expect(
      handlersPara('parcial').some((handler) => String(handler.info.path).endsWith('/business')),
    ).toBe(false);
  });

  it('recupera la configuración guardada sin consultar al geocodificador', async () => {
    const usuario = setupUsuarioConSesion();
    estado.negocios.set(usuario.email, {
      nombre: 'Almacén guardado',
      rubro: 'KIOSCO',
      comparteDatosZona: false,
      zona: 'San Justo - UNLaM',
      ubicacion: {
        direccion: 'Dirección guardada',
        localidad: 'San Justo',
        latitud: -34.68,
        longitud: -58.56,
      },
    });
    renderApp('/ubicacion');
    const user = userEvent.setup();
    await waitFor(
      () => expect(screen.getByLabelText('Nombre del comercio')).toHaveValue('Almacén guardado'),
      ESPERA,
    );
    expect(screen.getByRole('checkbox', { name: /Compartir datos agregados/ })).not.toBeChecked();
    expect(screen.getByText('Tu zona: San Justo - UNLaM')).toBeVisible();
    await irAlPaso(user, 'Ubicación');
    expect(screen.getByText('Dirección elegida: Dirección guardada')).toBeVisible();
    expect(screen.getByLabelText('Calle')).toHaveValue('');
    expect(screen.getByLabelText('Localidad')).toHaveValue('San Justo');
    expect(screen.getByRole('region', { name: 'Mapa de tu comercio' })).toBeVisible();
    expect(screen.queryByRole('radio')).toBeNull();
    await irAlPaso(user, 'Rubro');
    expect(screen.getByLabelText('Rubro del comercio')).toHaveValue('KIOSCO');
  });

  it('avisa que ninguna zona cubre la ubicación guardada', async () => {
    const usuario = setupUsuarioConSesion();
    estado.negocios.set(usuario.email, {
      nombre: 'Almacén lejano',
      rubro: 'KIOSCO',
      comparteDatosZona: true,
      zona: null,
      ubicacion: {
        direccion: 'Dirección lejana',
        localidad: 'CABA',
        latitud: -34.6037,
        longitud: -58.3816,
      },
    });
    renderApp('/ubicacion');
    expect(await screen.findByText(/Todavía no hay una zona que cubra/, {}, ESPERA)).toBeVisible();
  });

  it('no muestra zona mientras el comercio no tiene ubicación guardada', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await irAlPaso(user, 'Tu comercio');
    expect(screen.queryByText(/Tu zona:/)).toBeNull();
    expect(screen.queryByText(/Todavía no hay una zona/)).toBeNull();
  });

  it('guarda los datos y los recupera al salir y volver a entrar', async () => {
    const { user, router } = await abrirUbicacion();
    await user.click(screen.getByRole('checkbox', { name: /Compartir datos agregados/ }));
    await elegirDireccion(user);
    await esperarLocalidad('San Justo');
    await elegirRubro(user, 'ALMACEN');
    await user.click(screen.getByRole('button', { name: 'Guardar configuración' }));
    expect(await screen.findByText(/se guardó correctamente/, {}, ESPERA)).toBeVisible();
    await irAlPaso(user, 'Tu comercio');
    expect(screen.getByText('Tu zona: San Justo - UNLaM')).toBeVisible();
    await user.click(screen.getByRole('link', { name: /Volver a Mi zona/ }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/zona'), ESPERA);
    await user.click(await screen.findByRole('link', { name: /Probar ubicación y rubro/ }, ESPERA));
    expect(await screen.findByText('Tu zona: San Justo - UNLaM', {}, ESPERA)).toBeVisible();
    expect(screen.getByRole('checkbox', { name: /Compartir datos agregados/ })).not.toBeChecked();
    await irAlPaso(user, 'Ubicación');
    await waitFor(
      () => expect(screen.getByRole('region', { name: 'Mapa de tu comercio' })).toBeVisible(),
      ESPERA,
    );
    expect(screen.getByLabelText('Localidad')).toHaveValue('San Justo');
    await irAlPaso(user, 'Rubro');
    expect(screen.getByLabelText('Rubro del comercio')).toHaveValue('ALMACEN');
  });

  it('conserva el borrador si falla el guardado y permite reintentar', async () => {
    setupServerError('patch', '/business', 503, 'No pudimos guardar tu comercio.');
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await esperarLocalidad('San Justo');
    await elegirRubro(user, 'ALMACEN');
    await user.click(screen.getByRole('button', { name: 'Guardar configuración' }));
    expect(await screen.findByText('No pudimos guardar tu comercio.', {}, ESPERA)).toBeVisible();
    expect(screen.queryByText(/se guardó correctamente/)).toBeNull();
    await irAlPaso(user, 'Ubicación');
    expect(screen.getByRole('region', { name: 'Mapa de tu comercio' })).toBeVisible();
    server.resetHandlers();
    await irAlPaso(user, 'Rubro');
    await user.click(screen.getByRole('button', { name: 'Guardar configuración' }));
    expect(await screen.findByText(/se guardó correctamente/, {}, ESPERA)).toBeVisible();
  });

  it('exige nombre y localidad antes de enviar el guardado', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await esperarLocalidad('San Justo');
    await elegirRubro(user, 'ALMACEN');
    await irAlPaso(user, 'Tu comercio');
    await user.clear(screen.getByLabelText('Nombre del comercio'));
    expect(screen.getByText('Escribí el nombre del comercio para seguir.')).toBeVisible();
    expect(screen.getByRole('button', { name: /Siguiente/ })).toBeDisabled();
    expect(botonDelPaso('Ubicación')).toBeDisabled();
    expect(botonDelPaso('Rubro')).toBeDisabled();
    expect(estado.negocios.size).toBe(0);
  });

  it('completa la localidad con la que el mapa reconoce al elegir la dirección', async () => {
    const { user } = await abrirUbicacion();
    await irAlPaso(user, 'Ubicación');
    expect(screen.getByLabelText('Localidad')).toHaveValue('');
    await buscarDireccion(user, 'san justo');
    await user.click(screen.getByRole('radio', { name: DIRECCIONES_DE_PRUEBA[0].direccion }));
    await esperarLocalidad('San Justo');
    expect(screen.queryByText(/no coincide con la del mapa/)).toBeNull();
  });

  it('permite escribir la localidad pero no guardar una que no coincide con el mapa', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await esperarLocalidad('San Justo');
    await user.clear(screen.getByLabelText('Localidad'));
    await user.type(screen.getByLabelText('Localidad'), 'Ramos Mejía');
    expect(screen.getByText('La localidad no coincide con la del mapa (San Justo).')).toBeVisible();
    expect(screen.getByLabelText('Localidad')).toBeInvalid();
    expect(screen.getByRole('button', { name: /Siguiente/ })).toBeDisabled();
    expect(botonDelPaso('Rubro')).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Usar San Justo' }));
    expect(screen.getByLabelText('Localidad')).toHaveValue('San Justo');
    expect(screen.queryByText(/no coincide con la del mapa/)).toBeNull();
    await elegirRubro(user, 'ALMACEN');
    expect(screen.getByRole('button', { name: 'Guardar configuración' })).toBeEnabled();
  });

  it('acepta la localidad escrita sin importar mayúsculas, tildes ni espacios de más', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await esperarLocalidad('San Justo');
    await user.clear(screen.getByLabelText('Localidad'));
    await user.type(screen.getByLabelText('Localidad'), '  sAn   jústo ');
    expect(screen.queryByText(/no coincide con la del mapa/)).toBeNull();
    await irARubro(user);
    expect(screen.getByRole('button', { name: 'Guardar configuración' })).toBeEnabled();
  });

  it('no permite guardar mientras no se pudo verificar la localidad y ofrece reintentar', async () => {
    setupServerError('post', '/ubicacion/localidad', 503, 'No pudimos verificar la localidad.');
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    expect(await screen.findByText('No pudimos verificar la localidad.', {}, ESPERA)).toBeVisible();
    expect(botonDelPaso('Rubro')).toBeDisabled();
    server.resetHandlers();
    await user.click(screen.getByRole('button', { name: 'Reintentar verificación' }));
    await esperarLocalidad('San Justo');
    expect(screen.queryByText('No pudimos verificar la localidad.')).toBeNull();
    await irAlPaso(user, 'Rubro');
    expect(screen.getByRole('button', { name: 'Guardar configuración' })).toBeEnabled();
  });

  it('no pisa la localidad guardada al cargar, pero avisa si no coincide con el mapa', async () => {
    const usuario = setupUsuarioConSesion();
    estado.negocios.set(usuario.email, {
      nombre: 'Almacén viejo',
      rubro: 'KIOSCO',
      comparteDatosZona: true,
      zona: 'San Justo - UNLaM',
      ubicacion: {
        direccion: 'Dirección guardada',
        localidad: 'Ciudad Vieja',
        latitud: -34.6701,
        longitud: -58.5625,
      },
    });
    renderApp('/ubicacion');
    const user = userEvent.setup();
    await waitFor(
      () => expect(screen.getByLabelText('Nombre del comercio')).toHaveValue('Almacén viejo'),
      ESPERA,
    );
    await irAlPaso(user, 'Ubicación');
    expect(
      await screen.findByText('La localidad no coincide con la del mapa (San Justo).', {}, ESPERA),
    ).toBeVisible();
    expect(screen.getByLabelText('Localidad')).toHaveValue('Ciudad Vieja');
    expect(botonDelPaso('Rubro')).toBeDisabled();
  });

  it('no permite guardar una configuración que no pudo cargar y ofrece reintentar', async () => {
    setupServerError('get', '/business', 503, 'Sin servicio');
    setupUsuarioConSesion();
    renderApp('/ubicacion');
    const user = userEvent.setup();
    expect(
      await screen.findByText('No pudimos cargar la configuración de tu comercio.', {}, ESPERA),
    ).toBeVisible();
    expect(botonDelPaso('Rubro')).toBeDisabled();
    expect(screen.getByRole('button', { name: /Siguiente/ })).toBeDisabled();
    server.resetHandlers();
    await user.click(screen.getByRole('button', { name: 'Reintentar configuración' }));
    await waitFor(() => expect(screen.getByLabelText('Nombre del comercio')).toBeEnabled(), ESPERA);
    expect(screen.getByLabelText('Nombre del comercio')).not.toHaveValue('');
  });
});
