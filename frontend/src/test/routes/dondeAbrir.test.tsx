import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { estado } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';

const TITULO = 'Tu próximo negocio';

const prepararNegocio = (localidad = 'Villa Crespo') => {
  const usuario = setupUsuarioConSesion();
  estado.negocios.set(usuario.email, {
    nombre: 'Almacén de barrio',
    rubro: 'ALMACEN',
    ubicacion: {
      direccion: 'Av. Corrientes 5000',
      localidad,
      latitud: -34.5996,
      longitud: -58.4403,
    },
    comparteDatosZona: true,
    zona: localidad,
  });
  return usuario;
};

type Respuestas = {
  alcance?: RegExp;
  prioridad?: RegExp;
  publico?: RegExp;
  tamano?: RegExp;
};

const abrir = async () => {
  prepararNegocio();
  const router = renderApp('/zona/donde-abrir');
  await screen.findByRole('heading', { name: TITULO }, ESPERA);
  return { router, user: userEvent.setup() };
};

/** Confirma el negocio y contesta las cuatro preguntas hasta llegar a las zonas. */
const llegarALasZonas = async (
  user: ReturnType<typeof userEvent.setup>,
  respuestas: Respuestas = {},
) => {
  await user.click(screen.getByRole('button', { name: /Continuar con estos datos/ }));
  for (const respuesta of [
    respuestas.alcance ?? /Cerca de mi local actual/,
    respuestas.prioridad ?? /Un equilibrio/,
    respuestas.publico ?? /Sin priorizar un entorno/,
    respuestas.tamano ?? /^Mediano/,
  ]) {
    await user.click(await screen.findByRole('button', { name: respuesta }));
  }
  await screen.findByRole('list', { name: /Zonas recomendadas/ });
};

const zonas = () =>
  within(screen.getByRole('list', { name: /Zonas recomendadas/ })).getAllByRole('button');
const mover = (nombre: RegExp, valor: number) =>
  fireEvent.change(screen.getByRole('slider', { name: nombre }), {
    target: { value: String(valor) },
  });
const irATramites = async (user: ReturnType<typeof userEvent.setup>) =>
  user.click(screen.getByRole('tab', { name: 'Qué tenés que tramitar' }));

describe('dónde abrir', () => {
  it('es una sección de la barra de arriba y arranca confirmando el negocio', async () => {
    prepararNegocio();
    const router = renderApp('/inicio');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Zona' }, ESPERA));
    await user.click(await screen.findByRole('link', { name: 'Tu próximo negocio' }, ESPERA));

    expect(await screen.findByRole('heading', { name: TITULO })).toBeVisible();
    expect(router.state.location.pathname).toBe('/zona/donde-abrir');
    expect(screen.getByRole('link', { name: 'Tu próximo negocio' })).toHaveAttribute(
      'aria-current',
      'page',
    );
    expect(screen.queryByText(/Datos trazables/)).not.toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'Progreso de Tu próximo negocio' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Editar ubicación de mi negocio' })).toHaveAttribute(
      'href',
      '/ubicacion',
    );
    expect(await screen.findByText(/Veo que tu negocio es/)).toHaveTextContent(
      /Almacén de barrio.*Villa Crespo.*almacén/,
    );
    expect(screen.getByText(/¿Querés continuar con estos datos\?/)).toBeVisible();
  });

  it('también se entra desde Objetivos', async () => {
    prepararNegocio();
    const router = renderApp('/objetivos');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Ver dónde conviene' }, ESPERA));

    await screen.findByRole('heading', { name: TITULO });
    expect(router.state.location.pathname).toBe('/zona/donde-abrir');
  });

  it('separa la confirmación, el cambio de rubro y la edición de la ubicación', async () => {
    const { user } = await abrir();

    expect(screen.getByRole('link', { name: 'Editar ubicación de mi negocio' })).toHaveAttribute(
      'href',
      '/ubicacion',
    );
    expect(
      screen.queryByRole('button', { name: /No, quiero cambiar algo/ }),
    ).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Cambiar el tipo de local' }));
    await user.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(screen.getByRole('button', { name: 'Continuar con estos datos' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Cambiar el tipo de local' }));
    await user.click(screen.getByRole('radio', { name: 'Una papelería' }));
    await user.click(screen.getByRole('button', { name: 'Usar este tipo de local' }));

    expect(await screen.findByText('¿En qué zona querés llegar a nuevos clientes?')).toBeVisible();
  });

  it('toma la localidad real del negocio para buscar cerca', async () => {
    prepararNegocio('San Justo');
    renderApp('/zona/donde-abrir');
    const user = userEvent.setup();

    await user.click(
      await screen.findByRole('button', { name: /Continuar con estos datos/ }, ESPERA),
    );

    expect(screen.getByRole('button', { name: /Cerca de mi local actual/ })).toHaveTextContent(
      'Hasta 4 km de San Justo',
    );
  });

  it('hace las preguntas de a una, y lo ya respondido queda como una charla', async () => {
    const { user } = await abrir();
    await user.click(screen.getByRole('button', { name: /Continuar con estos datos/ }));

    expect(await screen.findByText('Pregunta 1 de 4')).toBeVisible();
    expect(screen.getByText('¿En qué zona querés llegar a nuevos clientes?')).toBeVisible();
    expect(
      screen.queryByText('¿Qué combinación de lugares puede atraer mejor a tu público?'),
    ).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /En cualquier zona/ }));
    expect(await screen.findByText('Pregunta 2 de 4')).toBeVisible();
    const charla = screen.getByRole('list', { name: 'Lo que ya me contaste' });
    expect(within(charla).getByText('En cualquier zona')).toBeVisible();

    await user.click(screen.getByRole('button', { name: /Volver a la pregunta anterior/ }));
    expect(await screen.findByText('Pregunta 1 de 4')).toBeVisible();
  });

  it('pregunta por entornos comprobables pensando en el público del negocio', async () => {
    const { user } = await abrir();
    await user.click(screen.getByRole('button', { name: /Continuar con estos datos/ }));
    await user.click(screen.getByRole('button', { name: /Cerca de mi local actual/ }));
    expect(
      screen.getByText('¿Qué combinación de lugares puede atraer mejor a tu público?'),
    ).toBeVisible();
    expect(screen.queryByRole('button', { name: /Alquiler bajo/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Mucho movimiento/ })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Escuelas y plazas/ })).toBeVisible();
    await user.click(screen.getByRole('button', { name: /Un equilibrio/ }));

    expect(
      await screen.findByText('¿En qué entorno suele moverse el público al que querés llegar?'),
    ).toBeVisible();
    expect(screen.getByRole('button', { name: /Sin priorizar un entorno/ })).toBeVisible();
    expect(screen.getByRole('button', { name: /Escuelas y plazas/ })).toBeVisible();
    expect(screen.getByRole('button', { name: /Oficinas y transporte/ })).toBeVisible();
    expect(screen.queryByText('¿A quién le vendés más?')).not.toBeInTheDocument();
  });

  it('al terminar las preguntas muestra las zonas, con la leyenda de colores y sin barras', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user);

    expect(zonas().length).toBeGreaterThanOrEqual(3);
    expect(zonas()[0]).toHaveAttribute('aria-pressed', 'true');
    const leyenda = screen.getByRole('list', { name: 'Qué significa cada color' });
    expect(within(leyenda).getByText('Buena coincidencia')).toBeVisible();
    expect(within(leyenda).getByText('Coincidencia intermedia')).toBeVisible();
    expect(within(leyenda).getByText('Mala coincidencia')).toBeVisible();
    expect(document.querySelector('.donde-barra')).toBeNull();
    expect(screen.queryByRole('region', { name: 'Lo que cuenta Sami' })).not.toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Lista de zonas' })).toBeVisible();
  });

  it('primero compara el top tres y después deja ajustar con el mapa siempre al lado', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user);

    const recomendacion = screen.getByRole('region', { name: 'Recomendación de Sami' });
    const comparacion = screen.getByRole('table', { name: 'Comparación de las mejores zonas' });
    const filtros = screen.getByRole('region', { name: 'Ajustes de la recomendación' });
    const lista = screen.getByRole('region', { name: 'Lista de zonas' });
    const mapa = screen.getByRole('region', { name: /Mapa con las zonas/ });
    const antes = (a: HTMLElement, b: HTMLElement) =>
      Boolean(a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_FOLLOWING);
    expect(antes(recomendacion, comparacion)).toBe(true);
    expect(antes(comparacion, filtros)).toBe(true);
    expect(filtros.closest('.donde-explorador')).toBe(mapa.closest('.donde-explorador'));
    expect(antes(filtros, mapa)).toBe(true);
    expect(antes(mapa, lista)).toBe(true);
    expect(antes(recomendacion, mapa)).toBe(true);
    expect(
      within(recomendacion).getByRole('heading', { name: 'Mi recomendación para tu almacén' }),
    ).toBeVisible();
    expect(screen.getByRole('tab', { name: 'Qué tenés que tramitar' })).toBeVisible();
  });

  it('el bloque de Sami deja comparar las tres mejores zonas sin bajar a la lista', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user);
    const recomendacion = screen.getByRole('region', { name: 'Recomendación de Sami' });
    const mejores = within(
      within(recomendacion).getByRole('list', { name: 'Mejores zonas' }),
    ).getAllByRole('button');
    expect(mejores).toHaveLength(3);
    expect(mejores[0]).toHaveAttribute('aria-pressed', 'true');
    const primera = screen.getByRole('article').querySelector('h3')?.textContent;

    await user.click(mejores[1] as HTMLElement);

    expect(mejores[1]).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('article').querySelector('h3')?.textContent).not.toBe(primera);
    expect(
      within(recomendacion).getByRole('heading', { name: 'Lo que sé de la zona que elegiste' }),
    ).toBeVisible();
  });

  it('la comparación muestra diferencias comprensibles y no pesos técnicos', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user);

    const comparacion = screen.getByRole('table', { name: 'Comparación de las mejores zonas' });
    expect(within(comparacion).getByRole('rowheader', { name: 'Puntaje general' })).toBeVisible();
    expect(
      within(comparacion).queryByRole('rowheader', { name: 'Alquiler estimado' }),
    ).not.toBeInTheDocument();
    expect(
      within(comparacion).queryByRole('rowheader', { name: 'Movimiento' }),
    ).not.toBeInTheDocument();
    expect(within(comparacion).getByRole('rowheader', { name: 'Escuelas cerca' })).toBeVisible();
    expect(within(comparacion).getByRole('rowheader', { name: 'Parques y plazas' })).toBeVisible();
    expect(
      within(comparacion).getByRole('rowheader', { name: 'Competencia cercana' }),
    ).toBeVisible();
    expect(within(comparacion).getByRole('rowheader', { name: 'Distancia' })).toBeVisible();
    expect(within(comparacion).queryByText('Peso')).not.toBeInTheDocument();
    expect(within(comparacion).queryByText('Aporte')).not.toBeInTheDocument();
  });

  it('una zona elegida en la lista de abajo también aparece arriba, en el bloque de Sami', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user);

    await user.click(zonas()[5] as HTMLElement);

    const recomendacion = screen.getByRole('region', { name: 'Recomendación de Sami' });
    const mejores = within(
      within(recomendacion).getByRole('list', { name: 'Mejores zonas' }),
    ).getAllByRole('button');
    expect(mejores).toHaveLength(4);
    expect(mejores[3]).toHaveAttribute('aria-pressed', 'true');
  });

  it('elegir una zona no desplaza bruscamente toda la página', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user);
    const desplazarDocumento = vi.spyOn(Element.prototype, 'scrollIntoView');
    desplazarDocumento.mockClear();
    const lista = screen.getByRole('region', { name: 'Lista de zonas' }) as HTMLDivElement;
    const segunda = zonas()[1] as HTMLButtonElement;
    Object.defineProperty(lista, 'clientHeight', { configurable: true, value: 320 });
    Object.defineProperty(segunda, 'offsetTop', { configurable: true, value: 2_000 });
    lista.getBoundingClientRect = () => ({
      x: 0,
      y: 200,
      top: 200,
      right: 400,
      bottom: 520,
      left: 0,
      width: 400,
      height: 320,
      toJSON: () => ({}),
    });
    segunda.getBoundingClientRect = () => ({
      x: 0,
      y: 250,
      top: 250,
      right: 400,
      bottom: 300,
      left: 0,
      width: 400,
      height: 50,
      toJSON: () => ({}),
    });

    await user.click(segunda);

    expect(desplazarDocumento).not.toHaveBeenCalled();
    expect(lista.scrollTop).toBe(0);
    desplazarDocumento.mockRestore();
  });

  it('cada zona dice con un color qué tan bien coincide, también para quien usa lector de pantalla', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user);

    const primera = zonas()[0] as HTMLElement;
    expect(primera.className).toMatch(/\b(buena|intermedia|mala)\b/);
    expect(primera).toHaveTextContent(
      /(Buena|Intermedia|Mala)\s+coincidencia|Coincidencia intermedia/,
    );
  });

  it('muestra todos los factores, los modos rápidos y el radio junto al mapa', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user);

    const modos = screen.getByRole('group', { name: 'Modos rápidos' });
    for (const nombre of ['Equilibrado', 'Escuelas y plazas']) {
      expect(within(modos).getByRole('button', { name: nombre })).toBeVisible();
    }
    for (const nombre of [
      'Poca competencia',
      'Escuelas cerca',
      'Parques cerca',
      'Oficinas cerca',
      'Transporte',
      'Salud cerca',
      'Comercios que atraen gente',
    ]) {
      expect(screen.getByRole('group', { name: nombre })).toBeVisible();
    }
    for (const nombre of [
      'Gente pasando',
      'Vecinos cerca',
      'Demanda observada',
      'Poder de compra',
      'Alquiler accesible',
    ]) {
      expect(screen.queryByRole('group', { name: nombre })).not.toBeInTheDocument();
    }
    expect(screen.queryByText('Agregar cotizaciones reales de alquiler')).not.toBeInTheDocument();
    expect(screen.queryByText('Límites que descartan zonas')).not.toBeInTheDocument();
    expect(
      screen.queryByRole('slider', { name: /Comercios similares cerca/ }),
    ).not.toBeInTheDocument();

    const radio = screen.getByRole('slider', { name: /Radio de búsqueda/ });
    expect(radio).toBeVisible();
    expect(radio.closest('.donde-mapa-columna')).not.toBeNull();
  });

  it('deja personalizar cualquier factor y un cambio manual activa Personalizado', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user);

    const modos = screen.getByRole('group', { name: 'Modos rápidos' });
    expect(within(modos).getByRole('button', { name: 'Equilibrado' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    const competencia = screen.getByRole('group', { name: 'Poca competencia' });
    await user.click(within(competencia).getByRole('radio', { name: 'Mucho' }));

    expect(within(competencia).getByRole('radio', { name: 'Mucho' })).toBeChecked();
    expect(within(modos).getByText('Personalizado')).toHaveAttribute('aria-current', 'true');
  });

  it('los modos rápidos configuran todos los factores y Restablecer vuelve a Equilibrado', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user);
    const modos = screen.getByRole('group', { name: 'Modos rápidos' });

    await user.click(within(modos).getByRole('button', { name: 'Escuelas y plazas' }));
    expect(
      within(screen.getByRole('group', { name: 'Escuelas cerca' })).getByRole('radio', {
        name: 'Mucho',
      }),
    ).toBeChecked();
    expect(
      within(screen.getByRole('group', { name: 'Oficinas cerca' })).getByRole('radio', {
        name: 'Nada',
      }),
    ).toBeChecked();
    expect(within(modos).getByRole('button', { name: 'Escuelas y plazas' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );

    mover(/Radio de búsqueda/, 2);
    await user.click(screen.getByRole('button', { name: 'Restablecer' }));

    expect(within(modos).getByRole('button', { name: 'Equilibrado' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    expect(screen.getByRole('slider', { name: /Radio de búsqueda/ })).toHaveValue('4');
    expect(
      within(screen.getByRole('group', { name: 'Escuelas cerca' })).getByRole('radio', {
        name: 'Importa',
      }),
    ).toBeChecked();
  });

  it('el radio reduce las zonas sin duplicar filtros activos', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user);
    const antes = zonas().length;

    mover(/Radio de búsqueda/, 2);

    await waitFor(() => expect(zonas().length).toBeLessThan(antes));
    expect(screen.queryByRole('list', { name: 'Filtros activos' })).not.toBeInTheDocument();
    expect(screen.getByText(/Descartadas por tus límites/)).toBeVisible();
  });

  it('quién subió o bajó solo se marca cuando se mueve un filtro', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user);
    expect(screen.queryAllByLabelText(/^(Subió|Bajó) \d+ puestos?$|^Zona que antes/)).toHaveLength(
      0,
    );

    await user.click(screen.getByRole('button', { name: 'Escuelas y plazas' }));

    await waitFor(() =>
      expect(screen.getAllByLabelText(/^(Subió|Bajó) \d+ puestos?$/).length).toBeGreaterThan(0),
    );
  });

  it('una zona de la Ciudad con un local mediano se tramita online, sin costo y sin profesional', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user);

    await irATramites(user);

    const guia = screen.getByRole('tabpanel');
    expect(within(guia).getByText('Abrir en la Ciudad de Buenos Aires')).toBeVisible();
    expect(within(guia).getByText(/te sirve la habilitación exprés/)).toBeVisible();
    const sellos = within(guia).getByRole('list', { name: 'Resumen del trámite' });
    expect(within(sellos).getByText('Online')).toBeVisible();
    expect(within(sellos).getByText('Sin costo')).toBeVisible();
    expect(within(sellos).getByText('Sin profesional')).toBeVisible();
    expect(
      within(guia).getByText('Habilitación de actividad económica, modalidad exprés'),
    ).toBeVisible();
    expect(
      within(guia).getByRole('link', { name: /Más información sobre Habilitación de actividad/ }),
    ).toHaveAttribute(
      'href',
      'https://buenosaires.gob.ar/tramites/habilitacion-de-actividad-economica-expres',
    );
  });

  it('el tamaño del local cambia el trámite que corresponde', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user);
    await irATramites(user);

    fireEvent.change(screen.getByRole('slider', { name: /Tamaño del local/ }), {
      target: { value: '300' },
    });

    const guia = screen.getByRole('tabpanel');
    expect(within(guia).getByText(/ya no entrás en el trámite exprés/)).toBeVisible();
    expect(within(guia).getByText('Con profesional')).toBeVisible();
    expect(within(guia).getByText('Anexo Técnico firmado por un profesional')).toBeVisible();
  });

  it('una zona de La Matanza muestra qué se hace online y qué en la oficina', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user, { alcance: /En cualquier zona/ });

    await user.click(
      await within(screen.getByRole('list', { name: /Zonas recomendadas/ })).findByRole('button', {
        name: /Ramos Mejía/,
      }),
    );
    await irATramites(user);

    const guia = screen.getByRole('tabpanel');
    expect(within(guia).getByText('Abrir en La Matanza')).toBeVisible();
    const aprobaciones = within(guia).getByRole('region', { name: 'Aprobaciones que necesitás' });
    const zonificacion = within(aprobaciones)
      .getByText('Consulta de zonificación')
      .closest('li') as HTMLElement;
    expect(within(zonificacion).getByText('En oficina')).toBeVisible();
    const habilitacion = within(aprobaciones)
      .getByText('Habilitación de comercio')
      .closest('li') as HTMLElement;
    expect(within(habilitacion).getByText('Online y en oficina')).toBeVisible();
    expect(within(guia).getByText(/no publica el paso a paso/)).toBeVisible();
    expect(
      within(guia)
        .getAllByRole('link')
        .map((enlace) => enlace.getAttribute('href')),
    ).toContain('https://www.lamatanza.gov.ar/tramites/17/details');
  });

  it('las fuentes son las páginas oficiales y avisa que hay que confirmar antes de empezar', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user);
    await irATramites(user);

    const guia = screen.getByRole('tabpanel');
    expect(within(guia).getByText(/Confirmá los requisitos antes de empezar/)).toBeVisible();
    expect(within(guia).getByText(/no se inicia ningún trámite/)).toBeVisible();
    expect(within(guia).getByRole('link', { name: /Habilitación exprés/ })).toHaveAttribute(
      'href',
      'https://buenosaires.gob.ar/tramites/habilitacion-de-actividad-economica-expres',
    );
  });

  it('en la otra solapa se ve por qué se recomienda la zona y cómo se calculó', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user);

    const panel = screen.getByRole('tabpanel');
    expect(within(panel).getByText('Por qué la recomiendo')).toBeVisible();
    expect(within(panel).getByText('Riesgos')).toBeVisible();
    expect(screen.queryByRole('table', { name: /Detalle del puntaje/ })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Cómo lo calculé/ }));
    const calculo = screen.getByRole('table', { name: /Detalle del puntaje/ });
    expect(within(calculo).getByRole('rowheader', { name: 'Puntaje final' })).toBeVisible();
    expect(
      within(calculo).getByRole('columnheader', { name: 'Importancia en el resultado' }),
    ).toBeVisible();
    expect(within(calculo).queryByRole('columnheader', { name: 'Aporte' })).not.toBeInTheDocument();
    expect(within(calculo).queryByText('OpenStreetMap')).not.toBeInTheDocument();
    await user.click(screen.getByText('Cómo usamos los datos'));
    expect(screen.getAllByText(/OpenStreetMap/).length).toBeGreaterThan(0);
  });

  it('elegir otra zona cambia el detalle sin sacar a la persona de la solapa actual', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user);
    const primera = screen.getByRole('article').querySelector('h3')?.textContent;
    await irATramites(user);

    await user.click(zonas()[1] as HTMLElement);

    expect(screen.getByRole('tab', { name: 'Qué tenés que tramitar' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    expect(screen.getByRole('article').querySelector('h3')?.textContent).not.toBe(primera);
  });

  it('permite cambiar las respuestas y volver a las preguntas', async () => {
    const { user } = await abrir();
    await llegarALasZonas(user);

    await user.click(screen.getByRole('button', { name: 'Cambiar mis respuestas' }));

    expect(await screen.findByText('Pregunta 1 de 4')).toBeVisible();
  });

  it('vuelve a Mi zona', async () => {
    const { router, user } = await abrir();

    await user.click(screen.getByRole('button', { name: 'Volver a Mi zona' }));

    expect(router.state.location.pathname).toBe('/zona');
  });
});
