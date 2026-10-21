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

async function abrirUbicacion() {
  setupUsuarioConSesion();
  const router = renderApp('/ubicacion');
  await screen.findByRole('heading', { level: 1, name: 'Ubicación y rubro' }, ESPERA);
  await screen.findByRole('option', { name: 'Almacén' }, ESPERA);
  await waitFor(() => expect(screen.getByLabelText('Nombre del comercio')).toBeEnabled(), ESPERA);
  return { user: userEvent.setup(), router };
}

async function buscarDireccion(user: ReturnType<typeof userEvent.setup>) {
  await user.type(
    screen.getByLabelText('Dirección del comercio'),
    'Florencio Varela 1903, San Justo',
  );
  await user.click(screen.getByRole('button', { name: 'Buscar dirección' }));
  await screen.findByRole('radio', { name: DIRECCIONES_DE_PRUEBA[0].direccion }, ESPERA);
}

async function elegirDireccion(user: ReturnType<typeof userEvent.setup>) {
  await buscarDireccion(user);
  await user.click(screen.getByRole('radio', { name: DIRECCIONES_DE_PRUEBA[0].direccion }));
}

// La localidad queda verificada cuando el mapa respondió y el campo muestra la del mapa.
async function esperarLocalidad(localidad: string) {
  await waitFor(
    () => expect(screen.getByLabelText('Localidad del comercio')).toHaveValue(localidad),
    ESPERA,
  );
  await waitFor(
    () => expect(screen.getByText(/Según el mapa:/)).toHaveTextContent(`Según el mapa: ${localidad}`),
    ESPERA,
  );
}

describe('Ubicación y rubro del negocio', () => {
  it('conserva etiquetas y nombres accesibles con dirección, mapa y revisión', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await user.selectOptions(screen.getByLabelText('Rubro del comercio'), 'ALMACEN');
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
    await user.type(
      screen.getByLabelText('Dirección del comercio'),
      '  Florencio  Varela 1903, San Justo  ',
    );
    expect(consultas).toBe(0);
    await user.click(screen.getByRole('button', { name: 'Buscar dirección' }));
    await screen.findAllByRole('radio', {}, ESPERA);
    expect(consultas).toBe(1);
    expect(screen.queryByRole('region', { name: 'Mapa de tu comercio' })).toBeNull();
  });

  it('rechaza una consulta vacía sin llamar al geocodificador', async () => {
    const { user } = await abrirUbicacion();
    await user.click(screen.getByRole('button', { name: 'Buscar dirección' }));
    expect(screen.getByText(/Escribí calle, altura y localidad/)).toBeVisible();
    expect(screen.queryByRole('radio')).toBeNull();
  });

  it('permite elegir entre direcciones y revisar punto y rubro sin guardar nada', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    expect(screen.getByRole('region', { name: 'Mapa de tu comercio' })).toBeVisible();
    expect(screen.getByLabelText('Latitud')).toHaveValue('-34.6701');
    await user.selectOptions(screen.getByLabelText('Rubro del comercio'), 'ALMACEN');
    await user.click(screen.getByRole('button', { name: 'Revisar configuración' }));
    const resumen = screen.getByRole('status', { name: 'Configuración revisada' });
    expect(within(resumen).getByText('Almacén')).toBeVisible();
    expect(resumen).toHaveTextContent('-34.6701, -58.5625');
    expect(resumen).toHaveTextContent('todavía no se guardaron');
    await esperarLocalidad('San Justo');
    expect(screen.getByRole('button', { name: 'Guardar configuración' })).toBeEnabled();
  });

  it('corrige coordenadas con teclado y descarta la revisión cuando cambia un dato', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await user.selectOptions(screen.getByLabelText('Rubro del comercio'), 'KIOSCO');
    await user.clear(screen.getByLabelText('Latitud'));
    await user.type(screen.getByLabelText('Latitud'), '-34,68');
    await user.click(screen.getByRole('button', { name: 'Revisar configuración' }));
    expect(screen.getByRole('status', { name: 'Configuración revisada' })).toHaveTextContent(
      'ajustado manualmente',
    );
    expect(screen.getByRole('status', { name: 'Configuración revisada' })).toHaveTextContent(
      '-34.68, -58.5625',
    );
    await user.selectOptions(screen.getByLabelText('Rubro del comercio'), 'OTRO');
    expect(screen.queryByRole('status', { name: 'Configuración revisada' })).toBeNull();
  });

  it('exige dirección, rubro y coordenadas válidas', async () => {
    const { user } = await abrirUbicacion();
    await user.click(screen.getByRole('button', { name: 'Revisar configuración' }));
    expect(screen.getByText('Buscá y elegí la dirección de tu comercio.')).toBeVisible();
    await elegirDireccion(user);
    await user.click(screen.getByRole('button', { name: 'Revisar configuración' }));
    expect(screen.getByText('Elegí el rubro de tu comercio.')).toBeVisible();
    await user.selectOptions(screen.getByLabelText('Rubro del comercio'), 'ALMACEN');
    await user.clear(screen.getByLabelText('Latitud'));
    await user.type(screen.getByLabelText('Latitud'), '91');
    await user.click(screen.getByRole('button', { name: 'Revisar configuración' }));
    expect(screen.getByText(/Revisá las coordenadas: latitud entre/)).toBeVisible();
    expect(screen.queryByRole('status', { name: 'Configuración revisada' })).toBeNull();
  });

  it('descarta el punto anterior cuando se edita el texto buscado', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await user.type(screen.getByLabelText('Dirección del comercio'), ' otra');
    expect(screen.queryByLabelText('Latitud')).toBeNull();
    expect(screen.queryByRole('radio')).toBeNull();
    expect(screen.queryByRole('region', { name: 'Mapa de tu comercio' })).toBeNull();
  });

  it('explica una búsqueda sin resultados y permite volver a buscar', async () => {
    const { user } = await abrirUbicacion();
    await user.type(screen.getByLabelText('Dirección del comercio'), 'Sin coincidencias');
    await user.click(screen.getByRole('button', { name: 'Buscar dirección' }));
    expect(await screen.findByText(/No encontramos esa dirección/, {}, ESPERA)).toBeVisible();
    await user.clear(screen.getByLabelText('Dirección del comercio'));
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
    await user.type(screen.getByLabelText('Dirección del comercio'), 'Varela 1903, San Justo');
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
    setupUsuarioConSesion();
    renderApp('/ubicacion');
    const user = userEvent.setup();
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
    await user.type(screen.getByLabelText('Dirección del comercio'), 'Varela 1903, San Justo');
    await user.click(screen.getByRole('button', { name: 'Buscar dirección' }));
    expect(screen.getByRole('button', { name: 'Buscando…' })).toBeDisabled();
    await user.clear(screen.getByLabelText('Dirección del comercio'));
    await user.type(screen.getByLabelText('Dirección del comercio'), 'otra dirección');
    await delay(550);
    expect(screen.queryByRole('radio')).toBeNull();
    expect(screen.getByRole('button', { name: 'Buscar dirección' })).toBeEnabled();
  });

  it('borra el borrador al salir y no lo recupera al volver', async () => {
    const { user, router } = await abrirUbicacion();
    await elegirDireccion(user);
    await user.selectOptions(screen.getByLabelText('Rubro del comercio'), 'ALMACEN');
    await user.click(screen.getByRole('link', { name: /Volver a Mi zona/ }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/zona'), ESPERA);
    await user.click(await screen.findByRole('link', { name: /Probar ubicación y rubro/ }, ESPERA));
    await screen.findByRole('heading', { name: 'Ubicación y rubro' }, ESPERA);
    expect(screen.getByLabelText('Dirección del comercio')).toHaveValue('');
    expect(screen.getByLabelText('Rubro del comercio')).toHaveValue('');
    expect(screen.queryByRole('region', { name: 'Mapa de tu comercio' })).toBeNull();
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
    await waitFor(
      () => expect(screen.getByLabelText('Nombre del comercio')).toHaveValue('Almacén guardado'),
      ESPERA,
    );
    expect(screen.getByLabelText('Dirección del comercio')).toHaveValue('Dirección guardada');
    expect(screen.getByLabelText('Localidad del comercio')).toHaveValue('San Justo');
    expect(screen.getByLabelText('Rubro del comercio')).toHaveValue('KIOSCO');
    expect(screen.getByLabelText('Latitud')).toHaveValue('-34.68');
    expect(screen.getByRole('checkbox', { name: /Compartir datos agregados/ })).not.toBeChecked();
    expect(screen.getByText('Tu zona: San Justo - UNLaM')).toBeVisible();
    expect(screen.queryByRole('radio')).toBeNull();
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
    expect(
      await screen.findByText(/Todavía no hay una zona que cubra/, {}, ESPERA),
    ).toBeVisible();
  });

  it('no muestra zona mientras el comercio no tiene ubicación guardada', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    expect(screen.queryByText(/Tu zona:/)).toBeNull();
    expect(screen.queryByText(/Todavía no hay una zona/)).toBeNull();
  });

  it('guarda los datos y los recupera al salir y volver a entrar', async () => {
    const { user, router } = await abrirUbicacion();
    await elegirDireccion(user);
    await esperarLocalidad('San Justo');
    await user.selectOptions(screen.getByLabelText('Rubro del comercio'), 'ALMACEN');
    await user.click(screen.getByRole('checkbox', { name: /Compartir datos agregados/ }));
    await user.click(screen.getByRole('button', { name: 'Guardar configuración' }));
    expect(await screen.findByText(/se guardó correctamente/, {}, ESPERA)).toBeVisible();
    expect(screen.getByText('Tu zona: San Justo - UNLaM')).toBeVisible();
    await user.click(screen.getByRole('link', { name: /Volver a Mi zona/ }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/zona'), ESPERA);
    await user.click(await screen.findByRole('link', { name: /Probar ubicación y rubro/ }, ESPERA));
    await waitFor(() => expect(screen.getByLabelText('Latitud')).toHaveValue('-34.6701'), ESPERA);
    expect(screen.getByText('Tu zona: San Justo - UNLaM')).toBeVisible();
    expect(screen.getByLabelText('Localidad del comercio')).toHaveValue('San Justo');
    expect(screen.getByLabelText('Rubro del comercio')).toHaveValue('ALMACEN');
    expect(screen.getByRole('checkbox', { name: /Compartir datos agregados/ })).not.toBeChecked();
  });

  it('conserva el borrador si falla el guardado y permite reintentar', async () => {
    setupServerError('patch', '/business', 503, 'No pudimos guardar tu comercio.');
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await esperarLocalidad('San Justo');
    await user.selectOptions(screen.getByLabelText('Rubro del comercio'), 'ALMACEN');
    await user.click(screen.getByRole('button', { name: 'Guardar configuración' }));
    expect(await screen.findByText('No pudimos guardar tu comercio.', {}, ESPERA)).toBeVisible();
    expect(screen.queryByText(/se guardó correctamente/)).toBeNull();
    expect(screen.getByLabelText('Latitud')).toHaveValue('-34.6701');
    server.resetHandlers();
    await user.click(screen.getByRole('button', { name: 'Guardar configuración' }));
    expect(await screen.findByText(/se guardó correctamente/, {}, ESPERA)).toBeVisible();
  });

  it('exige nombre y localidad antes de enviar el guardado', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await esperarLocalidad('San Justo');
    await user.selectOptions(screen.getByLabelText('Rubro del comercio'), 'ALMACEN');
    await user.clear(screen.getByLabelText('Nombre del comercio'));
    await user.click(screen.getByRole('button', { name: 'Guardar configuración' }));
    expect(screen.getByText(/Completá el nombre y la localidad/)).toBeVisible();
    expect(estado.negocios.size).toBe(0);
  });

  it('completa la localidad sola con la que el mapa reconoce al elegir la dirección', async () => {
    const { user } = await abrirUbicacion();
    expect(screen.getByLabelText('Localidad del comercio')).toHaveValue('');
    await elegirDireccion(user);
    await esperarLocalidad('San Justo');
    expect(screen.queryByText(/no coincide con la del mapa/)).toBeNull();
  });

  it('actualiza la localidad cuando se mueve el punto a otra localidad', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await esperarLocalidad('San Justo');
    await user.clear(screen.getByLabelText('Latitud'));
    await user.type(screen.getByLabelText('Latitud'), '-34.6037');
    await user.clear(screen.getByLabelText('Longitud'));
    await user.type(screen.getByLabelText('Longitud'), '-58.3816');
    await esperarLocalidad('Otra localidad');
  });

  it('permite escribir la localidad pero no guardar una que no coincide con el mapa', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await esperarLocalidad('San Justo');
    await user.selectOptions(screen.getByLabelText('Rubro del comercio'), 'ALMACEN');
    await user.clear(screen.getByLabelText('Localidad del comercio'));
    await user.type(screen.getByLabelText('Localidad del comercio'), 'Ramos Mejía');
    expect(
      screen.getByText('La localidad no coincide con la del mapa (San Justo).'),
    ).toBeVisible();
    expect(screen.getByLabelText('Localidad del comercio')).toBeInvalid();
    expect(screen.getByRole('button', { name: 'Guardar configuración' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Usar San Justo' }));
    expect(screen.getByLabelText('Localidad del comercio')).toHaveValue('San Justo');
    expect(screen.queryByText(/no coincide con la del mapa/)).toBeNull();
    expect(screen.getByRole('button', { name: 'Guardar configuración' })).toBeEnabled();
  });

  it('acepta la localidad escrita sin importar mayúsculas, tildes ni espacios de más', async () => {
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    await esperarLocalidad('San Justo');
    await user.clear(screen.getByLabelText('Localidad del comercio'));
    await user.type(screen.getByLabelText('Localidad del comercio'), '  sAn   jústo ');
    expect(screen.queryByText(/no coincide con la del mapa/)).toBeNull();
    expect(screen.getByRole('button', { name: 'Guardar configuración' })).toBeEnabled();
  });

  it('no permite guardar mientras no se pudo verificar la localidad y ofrece reintentar', async () => {
    setupServerError('post', '/ubicacion/localidad', 503, 'No pudimos verificar la localidad.');
    const { user } = await abrirUbicacion();
    await elegirDireccion(user);
    expect(await screen.findByText('No pudimos verificar la localidad.', {}, ESPERA)).toBeVisible();
    expect(screen.getByRole('button', { name: 'Guardar configuración' })).toBeDisabled();
    server.resetHandlers();
    await user.click(screen.getByRole('button', { name: 'Reintentar verificación' }));
    await esperarLocalidad('San Justo');
    expect(screen.queryByText('No pudimos verificar la localidad.')).toBeNull();
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
    expect(
      await screen.findByText('La localidad no coincide con la del mapa (San Justo).', {}, ESPERA),
    ).toBeVisible();
    expect(screen.getByLabelText('Localidad del comercio')).toHaveValue('Ciudad Vieja');
    expect(screen.getByRole('button', { name: 'Guardar configuración' })).toBeDisabled();
  });

  it('no permite guardar una configuración que no pudo cargar y ofrece reintentar', async () => {
    setupServerError('get', '/business', 503, 'Sin servicio');
    setupUsuarioConSesion();
    renderApp('/ubicacion');
    expect(
      await screen.findByText('No pudimos cargar la configuración de tu comercio.', {}, ESPERA),
    ).toBeVisible();
    expect(screen.getByRole('button', { name: 'Guardar configuración' })).toBeDisabled();
    server.resetHandlers();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Reintentar configuración' }));
    await waitFor(() => expect(screen.getByLabelText('Nombre del comercio')).toBeEnabled(), ESPERA);
    expect(screen.getByLabelText('Nombre del comercio')).not.toHaveValue('');
  });
});
