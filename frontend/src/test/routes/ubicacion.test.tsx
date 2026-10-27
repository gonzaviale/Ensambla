import { screen, waitFor } from '@testing-library/react';
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
type Solapa = 'Tu comercio' | 'Ubicación' | 'Rubro';

async function abrirUbicacion() {
  const usuario = setupUsuarioConSesion();
  const router = renderApp('/ubicacion');
  await screen.findByRole('heading', { level: 1, name: 'Ubicación y rubro' }, ESPERA);
  await waitFor(() => expect(screen.getByLabelText('Nombre del comercio')).toBeEnabled(), ESPERA);
  return { user: userEvent.setup(), router, usuario };
}

function solapa(nombre: Solapa) {
  return screen.getByRole('tab', { name: new RegExp(nombre) });
}

async function irASolapa(user: Usuario, nombre: Solapa) {
  await user.click(await screen.findByRole('tab', { name: new RegExp(nombre) }, ESPERA));
}

// Escribe la dirección completa (borrando lo que hubiera) sin buscar todavía.
async function escribirDireccion(user: Usuario, texto: string) {
  await user.clear(screen.getByLabelText('Dirección'));
  if (texto) await user.type(screen.getByLabelText('Dirección'), texto);
}

async function irARubro(user: Usuario) {
  await irASolapa(user, 'Rubro');
  await screen.findByRole('option', { name: 'Almacén' }, ESPERA);
}

async function elegirRubro(user: Usuario, codigo: string) {
  await irARubro(user);
  await user.selectOptions(screen.getByLabelText('Rubro del comercio'), codigo);
}

async function buscarDireccion(user: Usuario, texto = 'Florencio Varela 1903, San Justo') {
  await irASolapa(user, 'Ubicación');
  await escribirDireccion(user, texto);
  await user.click(screen.getByRole('button', { name: 'Buscar dirección' }));
  await screen.findByRole('radio', { name: DIRECCIONES_DE_PRUEBA[0].direccion }, ESPERA);
}

async function elegirDireccion(user: Usuario) {
  await buscarDireccion(user);
  await user.click(screen.getByRole('radio', { name: DIRECCIONES_DE_PRUEBA[0].direccion }));
}

const localidadEnPantalla = () => screen.getByText(/^Localidad:/);

// La localidad queda verificada cuando el mapa respondió y ya se puede guardar la ubicación.
async function esperarLocalidad(localidad: string) {
  await waitFor(
    () => expect(localidadEnPantalla()).toHaveTextContent(`Localidad: ${localidad}`),
    ESPERA,
  );
  await waitFor(
    () => expect(screen.getByRole('button', { name: 'Guardar ubicación' })).toBeEnabled(),
    ESPERA,
  );
}

describe('Ubicación y rubro del negocio', () => {
  it('muestra solapas a las que se entra en cualquier orden', async () => {
    const { user } = await abrirUbicacion();
    expect(screen.getByRole('tablist', { name: 'Configuración del comercio' })).toBeVisible();
    expect(solapa('Tu comercio')).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tabpanel', { name: /Tu comercio/ })).toBeVisible();
    expect(screen.queryByLabelText('Dirección')).toBeNull();
    expect(screen.queryByRole('button', { name: /Siguiente/ })).toBeNull();

    // Se puede ir directo al rubro sin completar lo anterior.
    await irARubro(user);
    expect(solapa('Rubro')).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('button', { name: 'Guardar rubro' })).toBeEnabled();
    expect(screen.queryByLabelText('Nombre del comercio')).toBeNull();

    await irASolapa(user, 'Ubicación');
    expect(screen.getByLabelText('Dirección')).toBeVisible();
    expect(screen.queryByLabelText('Calle')).toBeNull();
    expect(screen.queryByLabelText('Altura')).toBeNull();
    expect(screen.queryByLabelText('Localidad')).toBeNull();
    expect(screen.getByRole('button', { name: 'Guardar ubicación' })).toBeVisible();

    await user.keyboard('{ArrowLeft}');
    expect(solapa('Tu comercio')).toHaveAttribute('aria-selected', 'true');
    expect(solapa('Tu comercio')).toHaveFocus();
    expect(screen.getByRole('button', { name: 'Guardar datos del comercio' })).toBeVisible();
  });

  it('conserva etiquetas y nombres accesibles con solapas, dirección y mapa', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await esperarLocalidad('San Justo');
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

  it('busca la dirección completa sólo al enviar el formulario', async () => {
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
    await irASolapa(user, 'Ubicación');
    await escribirDireccion(user, '  Florencio  Varela 1903,  San Justo ');
    expect(consultas).toBe(0);
    await user.click(screen.getByRole('button', { name: 'Buscar dirección' }));
    await screen.findAllByRole('radio', {}, ESPERA);
    expect(consultas).toBe(1);
    expect(screen.queryByRole('region', { name: 'Mapa de tu comercio' })).toBeNull();
  });

  it('rechaza una consulta vacía sin llamar al geocodificador', async () => {
    const { user } = await abrirUbicacion();
    await irASolapa(user, 'Ubicación');
    await user.click(screen.getByRole('button', { name: 'Buscar dirección' }));
    expect(screen.getByText(/Escribí calle, altura y localidad/)).toBeVisible();
    expect(screen.queryByRole('radio')).toBeNull();
  });

  it('muestra el mapa y completa la localidad al elegir la dirección', async () => {
    const { user } = await abrirUbicacion();
    await buscarDireccion(user, 'Florencio Varela 1903, san justo');
    await user.click(screen.getByRole('radio', { name: DIRECCIONES_DE_PRUEBA[0].direccion }));
    expect(screen.getByRole('region', { name: 'Mapa de tu comercio' })).toBeVisible();
    expect(screen.queryByLabelText('Latitud')).toBeNull();
    await esperarLocalidad('San Justo');
    expect(screen.queryByText(/no coincide con la del mapa/)).toBeNull();
    expect(screen.getByRole('button', { name: 'Guardar ubicación' })).toBeEnabled();
  });

  it('conserva lo cargado en cada solapa al pasar de una a otra', async () => {
    const { user } = await abrirUbicacion();
    await user.clear(screen.getByLabelText('Nombre del comercio'));
    await user.type(screen.getByLabelText('Nombre del comercio'), 'Kiosco Nuevo');
    expect(solapa('Tu comercio')).toHaveTextContent('Sin guardar');
    await elegirDireccion(user);
    await elegirRubro(user, 'KIOSCO');
    await irASolapa(user, 'Tu comercio');
    expect(screen.getByLabelText('Nombre del comercio')).toHaveValue('Kiosco Nuevo');
    await irASolapa(user, 'Ubicación');
    expect(screen.getByRole('region', { name: 'Mapa de tu comercio' })).toBeVisible();
    expect(screen.getByRole('radio', { name: DIRECCIONES_DE_PRUEBA[0].direccion })).toBeChecked();
    await irASolapa(user, 'Rubro');
    expect(screen.getByLabelText('Rubro del comercio')).toHaveValue('KIOSCO');
    for (const nombre of ['Tu comercio', 'Ubicación', 'Rubro'] as const)
      expect(solapa(nombre)).toHaveTextContent('Sin guardar');
  });

  it('guarda una solapa sin pisar lo que está cargado en las otras', async () => {
    const { user, usuario } = await abrirUbicacion();
    const campoNombre = screen.getByLabelText<HTMLInputElement>('Nombre del comercio');
    const original = campoNombre.value;
    await user.type(campoNombre, ' editado');
    await elegirRubro(user, 'ALMACEN');
    await user.click(screen.getByRole('button', { name: 'Guardar rubro' }));
    expect(await screen.findByText('Guardamos el rubro de tu comercio.', {}, ESPERA)).toBeVisible();
    expect(estado.negocios.get(usuario.email)?.rubro).toBe('ALMACEN');
    expect(estado.negocios.get(usuario.email)?.nombre).toBe(original);
    expect(solapa('Rubro')).not.toHaveTextContent('Sin guardar');
    expect(solapa('Tu comercio')).toHaveTextContent('Sin guardar');
    await irASolapa(user, 'Tu comercio');
    expect(screen.getByLabelText('Nombre del comercio')).toHaveValue(`${original} editado`);
    await user.click(screen.getByRole('button', { name: 'Guardar datos del comercio' }));
    expect(
      await screen.findByText('Guardamos los datos de tu comercio.', {}, ESPERA),
    ).toBeVisible();
    expect(estado.negocios.get(usuario.email)?.nombre).toBe(`${original} editado`);
    expect(estado.negocios.get(usuario.email)?.rubro).toBe('ALMACEN');
  });

  it('exige elegir una dirección antes de guardar la ubicación', async () => {
    const { user } = await abrirUbicacion();
    await irASolapa(user, 'Ubicación');
    await user.click(screen.getByRole('button', { name: 'Guardar ubicación' }));
    expect(screen.getByText('Buscá y elegí la dirección de tu comercio.')).toBeVisible();
    expect(estado.negocios.size).toBe(0);
  });

  it('exige elegir un rubro antes de guardarlo', async () => {
    const { user } = await abrirUbicacion();
    await irARubro(user);
    await user.click(screen.getByRole('button', { name: 'Guardar rubro' }));
    expect(screen.getByText('Elegí el rubro de tu comercio.')).toBeVisible();
    expect(estado.negocios.size).toBe(0);
  });

  it('descarta el punto anterior cuando se edita la dirección escrita', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await user.type(screen.getByLabelText('Dirección'), ' otra');
    expect(screen.queryByRole('radio')).toBeNull();
    expect(screen.queryByRole('region', { name: 'Mapa de tu comercio' })).toBeNull();
  });

  it('explica una búsqueda sin resultados y permite volver a buscar', async () => {
    const { user } = await abrirUbicacion();
    await irASolapa(user, 'Ubicación');
    await escribirDireccion(user, 'Sin coincidencias 1, Ninguna');
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
    await irASolapa(user, 'Ubicación');
    await escribirDireccion(user, 'Varela 1903, San Justo');
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
    await irASolapa(user, 'Rubro');
    expect(await screen.findByText('No pudimos cargar los rubros.', {}, ESPERA)).toBeVisible();
    expect(screen.getByRole('button', { name: 'Guardar rubro' })).toBeDisabled();
    server.resetHandlers();
    await user.click(screen.getByRole('button', { name: 'Reintentar rubros' }));
    await screen.findByRole('option', { name: 'Almacén' }, ESPERA);
    expect(screen.getByRole('button', { name: 'Guardar rubro' })).toBeEnabled();
  });

  it('ignora una respuesta tardía si el usuario cambió la dirección', async () => {
    server.use(
      http.post('/api/ubicacion/direcciones', async () => {
        await delay(500);
        return HttpResponse.json({ resultados: DIRECCIONES_DE_PRUEBA });
      }),
    );
    const { user } = await abrirUbicacion();
    await irASolapa(user, 'Ubicación');
    await escribirDireccion(user, 'Varela 1903, San Justo');
    await user.click(screen.getByRole('button', { name: 'Buscar dirección' }));
    expect(screen.getByRole('button', { name: 'Buscando…' })).toBeDisabled();
    await escribirDireccion(user, 'otra calle 10, San Justo');
    await delay(550);
    expect(screen.queryByRole('radio')).toBeNull();
    expect(screen.getByRole('button', { name: 'Buscar dirección' })).toBeEnabled();
  });

  it('borra lo que no se guardó al salir y no lo recupera al volver', async () => {
    const { user, router } = await abrirUbicacion();
    await elegirDireccion(user);
    await elegirRubro(user, 'ALMACEN');
    await user.click(screen.getByRole('link', { name: /Volver a Mi zona/ }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/zona'), ESPERA);
    await user.click(await screen.findByRole('link', { name: /Probar ubicación y rubro/ }, ESPERA));
    await screen.findByRole('heading', { name: 'Ubicación y rubro' }, ESPERA);
    await irASolapa(user, 'Ubicación');
    expect(screen.getByLabelText('Dirección')).toHaveValue('');
    expect(screen.queryByRole('region', { name: 'Mapa de tu comercio' })).toBeNull();
    await irARubro(user);
    expect(screen.getByLabelText('Rubro del comercio')).toHaveValue('');
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
    for (const nombre of ['Tu comercio', 'Ubicación', 'Rubro'] as const)
      expect(solapa(nombre)).toHaveTextContent('Guardado');
    await irASolapa(user, 'Ubicación');
    expect(screen.getByText('Dirección elegida: Dirección guardada')).toBeVisible();
    expect(screen.getByLabelText('Dirección')).toHaveValue('');
    expect(localidadEnPantalla()).toHaveTextContent('Localidad: San Justo');
    expect(screen.getByRole('region', { name: 'Mapa de tu comercio' })).toBeVisible();
    expect(screen.queryByRole('radio')).toBeNull();
    await irASolapa(user, 'Rubro');
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
    await irASolapa(user, 'Tu comercio');
    expect(screen.queryByText(/Tu zona:/)).toBeNull();
    expect(screen.queryByText(/Todavía no hay una zona/)).toBeNull();
  });

  it('guarda cada solapa y la recupera al salir y volver a entrar', async () => {
    const { user, router } = await abrirUbicacion();
    await user.click(screen.getByRole('checkbox', { name: /Compartir datos agregados/ }));
    await user.click(screen.getByRole('button', { name: 'Guardar datos del comercio' }));
    expect(
      await screen.findByText('Guardamos los datos de tu comercio.', {}, ESPERA),
    ).toBeVisible();
    await elegirDireccion(user);
    await esperarLocalidad('San Justo');
    await user.click(screen.getByRole('button', { name: 'Guardar ubicación' }));
    expect(
      await screen.findByText(
        'Guardamos la ubicación de tu comercio. Tu zona: San Justo - UNLaM.',
        {},
        ESPERA,
      ),
    ).toBeVisible();
    await elegirRubro(user, 'ALMACEN');
    await user.click(screen.getByRole('button', { name: 'Guardar rubro' }));
    expect(await screen.findByText('Guardamos el rubro de tu comercio.', {}, ESPERA)).toBeVisible();
    await irASolapa(user, 'Tu comercio');
    expect(screen.getByText('Tu zona: San Justo - UNLaM')).toBeVisible();
    await user.click(screen.getByRole('link', { name: /Volver a Mi zona/ }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/zona'), ESPERA);
    await user.click(await screen.findByRole('link', { name: /Probar ubicación y rubro/ }, ESPERA));
    expect(await screen.findByText('Tu zona: San Justo - UNLaM', {}, ESPERA)).toBeVisible();
    expect(screen.getByRole('checkbox', { name: /Compartir datos agregados/ })).not.toBeChecked();
    await irASolapa(user, 'Ubicación');
    await waitFor(
      () => expect(screen.getByRole('region', { name: 'Mapa de tu comercio' })).toBeVisible(),
      ESPERA,
    );
    expect(localidadEnPantalla()).toHaveTextContent('Localidad: San Justo');
    await irASolapa(user, 'Rubro');
    expect(screen.getByLabelText('Rubro del comercio')).toHaveValue('ALMACEN');
  });

  it('conserva lo cargado si falla el guardado y permite reintentar', async () => {
    setupServerError('patch', '/business', 503, 'No pudimos guardar tu comercio.');
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await esperarLocalidad('San Justo');
    await user.click(screen.getByRole('button', { name: 'Guardar ubicación' }));
    expect(await screen.findByText('No pudimos guardar tu comercio.', {}, ESPERA)).toBeVisible();
    expect(screen.queryByText(/Guardamos la ubicación/)).toBeNull();
    expect(screen.getByRole('region', { name: 'Mapa de tu comercio' })).toBeVisible();
    expect(solapa('Ubicación')).toHaveTextContent('Sin guardar');
    server.resetHandlers();
    await user.click(screen.getByRole('button', { name: 'Guardar ubicación' }));
    expect(await screen.findByText(/Guardamos la ubicación/, {}, ESPERA)).toBeVisible();
    expect(solapa('Ubicación')).not.toHaveTextContent('Sin guardar');
  });

  it('exige el nombre antes de guardar los datos del comercio', async () => {
    const { user } = await abrirUbicacion();
    await user.clear(screen.getByLabelText('Nombre del comercio'));
    await user.click(screen.getByRole('button', { name: 'Guardar datos del comercio' }));
    expect(
      screen.getByText('Escribí el nombre del comercio (hasta 120 caracteres).'),
    ).toBeVisible();
    expect(estado.negocios.size).toBe(0);
  });

  it('no permite guardar mientras no se pudo verificar la localidad y ofrece reintentar', async () => {
    setupServerError('post', '/ubicacion/localidad', 503, 'No pudimos verificar la localidad.');
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    expect(await screen.findByText('No pudimos verificar la localidad.', {}, ESPERA)).toBeVisible();
    expect(screen.getByRole('button', { name: 'Guardar ubicación' })).toBeDisabled();
    server.resetHandlers();
    await user.click(screen.getByRole('button', { name: 'Reintentar verificación' }));
    await esperarLocalidad('San Justo');
    expect(screen.queryByText('No pudimos verificar la localidad.')).toBeNull();
    expect(screen.getByRole('button', { name: 'Guardar ubicación' })).toBeEnabled();
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
    await irASolapa(user, 'Ubicación');
    expect(
      await screen.findByText('La localidad no coincide con la del mapa (San Justo).', {}, ESPERA),
    ).toBeVisible();
    expect(localidadEnPantalla()).toHaveTextContent('Localidad: Ciudad Vieja');
    expect(screen.getByRole('button', { name: 'Guardar ubicación' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Usar San Justo' }));
    expect(localidadEnPantalla()).toHaveTextContent('Localidad: San Justo');
    expect(screen.queryByText(/no coincide con la del mapa/)).toBeNull();
    expect(screen.getByRole('button', { name: 'Guardar ubicación' })).toBeEnabled();
  });

  it('no permite guardar una configuración que no pudo cargar y ofrece reintentar', async () => {
    setupServerError('get', '/business', 503, 'Sin servicio');
    setupUsuarioConSesion();
    renderApp('/ubicacion');
    const user = userEvent.setup();
    expect(
      await screen.findByText('No pudimos cargar la configuración de tu comercio.', {}, ESPERA),
    ).toBeVisible();
    expect(screen.getByRole('button', { name: 'Guardar datos del comercio' })).toBeDisabled();
    server.resetHandlers();
    await user.click(screen.getByRole('button', { name: 'Reintentar configuración' }));
    await waitFor(() => expect(screen.getByLabelText('Nombre del comercio')).toBeEnabled(), ESPERA);
    expect(screen.getByLabelText('Nombre del comercio')).not.toHaveValue('');
  });
});
