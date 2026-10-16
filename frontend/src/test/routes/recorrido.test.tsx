import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { beforeEach, describe, expect, it } from 'vitest';
import { crearProyecto } from '@/api/proyectos';
import { InfoTip } from '@/components/ui';
import { PASOS_RECORRIDO, TOTAL_PASOS } from '@/lib/recorrido/pasos';
import { useRecorridoStore } from '@/stores/recorrido';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
} from '@/test/testHelpers';

async function sinViolaciones() {
  const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });
  const resumen = resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`);
  expect(resumen).toEqual([]);
}

const store = () => useRecorridoStore.getState();

function paso(indice: number) {
  const encontrado = PASOS_RECORRIDO[indice];
  if (!encontrado) throw new Error(`No hay un paso ${indice} en el recorrido`);
  return encontrado;
}

const NOMBRE_OFERTA = '¿Querés que te mostremos cómo usar Ensambla?';
const ofertaDelRecorrido = () => screen.queryByRole('region', { name: NOMBRE_OFERTA });

const tarjetaDe = (titulo: string) => screen.findByRole('dialog', { name: titulo }, ESPERA);

const botonEmpezar = () => screen.findByRole('button', { name: 'Empezar el recorrido' }, ESPERA);

// Con un sistema armado, /inicio no redirige a la bienvenida.
async function abrirInicio() {
  setupUsuarioConSesion();
  await crearProyecto({ nombre: 'Almacén Don Carlos' });
  const router = renderApp('/inicio');
  await verifyPathAsync(router, '/inicio');
  return router;
}

// En el resto de la suite el recorrido ya está «visto»: acá se prueba la primera vez.
beforeEach(() => {
  useRecorridoStore.setState({ visto: false, activo: false, paso: 0 });
});

describe('Recorrido guiado: la lista de pasos', () => {
  it('cubre la propuesta con textos completos, anclas y rutas del contrato', () => {
    expect(TOTAL_PASOS).toBeGreaterThanOrEqual(7);
    expect(TOTAL_PASOS).toBeLessThanOrEqual(8);

    expect(PASOS_RECORRIDO.map((p) => p.ruta)).toEqual(
      expect.arrayContaining(['/inicio', '/subir', '/bloques', '/respuestas', '/plan', '/cuenta']),
    );
    expect(paso(0).ruta).toBe('/inicio');
    expect(PASOS_RECORRIDO.at(-1)?.ancla).toBe('cuenta-demo');

    const ids = PASOS_RECORRIDO.map((p) => p.id);
    expect(new Set(ids).size).toBe(ids.length);
    for (const unPaso of PASOS_RECORRIDO) {
      expect(unPaso.titulo.trim()).not.toBe('');
      expect(unPaso.texto.trim()).not.toBe('');
      expect(unPaso.ancla.trim()).not.toBe('');
    }
  });
});

describe('Recorrido guiado: la oferta del primer ingreso', () => {
  it('se ofrece la primera vez en Inicio y «Ahora no» la oculta y la da por vista', async () => {
    const user = userEvent.setup();
    await abrirInicio();

    expect(await screen.findByRole('region', { name: NOMBRE_OFERTA }, ESPERA)).toBeInTheDocument();
    expect(screen.getByText(/Dura unos 3 minutos/)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Ahora no' }));

    expect(ofertaDelRecorrido()).toBeNull();
    expect(store().visto).toBe(true);
    expect(store().activo).toBe(false);
  });

  it('no se ofrece si ya se vio', async () => {
    useRecorridoStore.setState({ visto: true });
    await abrirInicio();

    expect(ofertaDelRecorrido()).toBeNull();
  });

  it('sólo se ofrece en Inicio, no en el resto de las pantallas', async () => {
    setupUsuarioConSesion();
    renderApp('/cuenta');
    await verifyHeadingPresentAsync('Mi cuenta');

    expect(ofertaDelRecorrido()).toBeNull();
  });

  it('«Empezar el recorrido» abre el primer paso con «Paso 1 de N»', async () => {
    const user = userEvent.setup();
    await abrirInicio();

    await user.click(await botonEmpezar());

    const tarjeta = await tarjetaDe(paso(0).titulo);
    expect(tarjeta).toHaveTextContent(`Paso 1 de ${TOTAL_PASOS}`);
    expect(tarjeta).toHaveAccessibleDescription(expect.stringContaining(paso(0).texto));
    expect(tarjeta).toHaveAttribute('aria-modal', 'false');
    await waitFor(() => expect(tarjeta).toHaveFocus());
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled();
    expect(ofertaDelRecorrido()).toBeNull();
  });
});

describe('Recorrido guiado: avanzar y cambiar de pantalla', () => {
  it('«Siguiente» avanza y, al llegar a un paso de otra ruta, navega a esa ruta', async () => {
    const user = userEvent.setup();
    const router = await abrirInicio();
    await user.click(await botonEmpezar());
    await tarjetaDe(paso(0).titulo);

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    const segundo = await tarjetaDe(paso(1).titulo);
    expect(segundo).toHaveTextContent(`Paso 2 de ${TOTAL_PASOS}`);
    expect(router.state.location.pathname).toBe('/inicio');
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeEnabled();

    // Los pasos que quedan en Inicio no cambian de ruta; el siguiente es de otra pantalla.
    const primeroDeOtraRuta = PASOS_RECORRIDO.findIndex((p) => p.ruta !== '/inicio');
    for (let i = 2; i <= primeroDeOtraRuta; i += 1) {
      await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    }

    const destino = paso(primeroDeOtraRuta);
    await tarjetaDe(destino.titulo);
    await verifyPathAsync(router, destino.ruta);
    expect(destino.ruta).toBe('/subir');

    // «Anterior» vuelve al paso de Inicio y a su pantalla.
    await user.click(screen.getByRole('button', { name: 'Anterior' }));
    await tarjetaDe(paso(primeroDeOtraRuta - 1).titulo);
    await verifyPathAsync(router, '/inicio');
  });

  it('en el último paso se llega a Mi cuenta, se termina y no se ofrece de nuevo', async () => {
    const user = userEvent.setup();
    const router = await abrirInicio();
    const ultimo = TOTAL_PASOS - 1;
    useRecorridoStore.setState({ activo: true, paso: ultimo });

    const tarjeta = await tarjetaDe(paso(ultimo).titulo);
    await verifyPathAsync(router, '/cuenta');
    expect(tarjeta).toHaveTextContent(`Paso ${TOTAL_PASOS} de ${TOTAL_PASOS}`);

    expect(screen.queryByRole('button', { name: 'Saltar el recorrido' })).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Terminar' }));

    await waitFor(() =>
      expect(screen.queryByRole('dialog', { name: paso(ultimo).titulo })).toBeNull(),
    );
    expect(store().activo).toBe(false);
    expect(store().visto).toBe(true);
  });
});

describe('Recorrido guiado: cerrar y volver a abrir', () => {
  it('Esc cierra el recorrido y no se vuelve a ofrecer', async () => {
    const user = userEvent.setup();
    await abrirInicio();
    await user.click(await botonEmpezar());
    await tarjetaDe(paso(0).titulo);

    await user.keyboard('{Escape}');

    await waitFor(() => expect(store().activo).toBe(false));
    expect(screen.queryByRole('dialog', { name: paso(0).titulo })).toBeNull();
    expect(store().visto).toBe(true);
    expect(ofertaDelRecorrido()).toBeNull();
  });

  it('«Saltar el recorrido» lo cierra y lo da por visto', async () => {
    const user = userEvent.setup();
    await abrirInicio();
    await user.click(await botonEmpezar());
    await tarjetaDe(paso(0).titulo);

    await user.click(screen.getByRole('button', { name: 'Saltar el recorrido' }));

    expect(screen.queryByRole('dialog', { name: paso(0).titulo })).toBeNull();
    expect(store()).toMatchObject({ visto: true, activo: false, paso: 0 });
    expect(ofertaDelRecorrido()).toBeNull();
  });

  it('«Volver a empezar» en Mi cuenta lo reabre desde el principio', async () => {
    const user = userEvent.setup();
    useRecorridoStore.setState({ visto: true, activo: false, paso: 3 });
    setupUsuarioConSesion();
    const router = renderApp('/cuenta');
    await verifyHeadingPresentAsync('Mi cuenta');

    await user.click(screen.getByRole('button', { name: 'Volver a empezar el recorrido guiado' }));

    const tarjeta = await tarjetaDe(paso(0).titulo);
    expect(tarjeta).toHaveTextContent(`Paso 1 de ${TOTAL_PASOS}`);
    await verifyPathAsync(router, paso(0).ruta);
  });

  it('el botón «Recorrido guiado» de la barra superior también lo abre', async () => {
    const user = userEvent.setup();
    useRecorridoStore.setState({ visto: true });
    setupUsuarioConSesion();
    renderApp('/cuenta');
    await verifyHeadingPresentAsync('Mi cuenta');

    const [boton] = screen.getAllByRole('button', { name: 'Recorrido guiado' });
    if (!boton) throw new Error('Falta el botón del recorrido');
    await user.click(boton);

    await tarjetaDe(paso(0).titulo);
    expect(store().activo).toBe(true);
  });
});

describe('Recorrido guiado: accesibilidad', () => {
  it('con el recorrido abierto no hay violaciones de axe', async () => {
    const user = userEvent.setup();
    await abrirInicio();
    await user.click(await botonEmpezar());
    await tarjetaDe(paso(0).titulo);

    await sinViolaciones();
  });

  it('la oferta del primer ingreso no tiene violaciones de axe', async () => {
    await abrirInicio();
    expect(await screen.findByRole('region', { name: NOMBRE_OFERTA }, ESPERA)).toBeInTheDocument();

    await sinViolaciones();
  });
});

describe('InfoTip', () => {
  it('se abre y se cierra con el teclado y se asocia al botón con aria-describedby', async () => {
    const user = userEvent.setup();
    render(
      <p>
        Ganancia{' '}
        <InfoTip tema="ganancia sobre el costo">
          Si te cuesta $ 100 y lo vendés a $ 130, ganás 30 %.
        </InfoTip>
      </p>,
    );
    const boton = screen.getByRole('button', { name: '¿Qué es esto? ganancia sobre el costo' });
    expect(boton).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('tooltip')).toBeNull();

    // Tab enfoca el botón y abre la ayuda.
    await user.tab();
    expect(boton).toHaveFocus();
    expect(boton).toHaveAttribute('aria-expanded', 'true');
    const globo = screen.getByRole('tooltip');
    expect(globo).toHaveTextContent('Si te cuesta $ 100 y lo vendés a $ 130, ganás 30 %.');
    expect(boton).toHaveAttribute('aria-describedby', globo.id);
    expect(boton).toHaveAccessibleDescription(/ganás 30 %/);

    // Esc la cierra y el foco se queda en el botón.
    await user.keyboard('{Escape}');
    expect(boton).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('tooltip')).toBeNull();
    expect(boton).toHaveFocus();

    // Enter la abre de nuevo y otro Enter la cierra.
    await user.keyboard('{Enter}');
    expect(boton).toHaveAttribute('aria-expanded', 'true');
    await user.keyboard('{Enter}');
    expect(boton).toHaveAttribute('aria-expanded', 'false');
  });

  it('se cierra al tocar afuera y al perder el foco', async () => {
    const user = userEvent.setup();
    render(
      <div>
        <InfoTip tema="redondeo">El redondeo deja el precio en un número cómodo.</InfoTip>
        <button type="button">Otro botón</button>
      </div>,
    );
    const boton = screen.getByRole('button', { name: '¿Qué es esto? redondeo' });

    await user.click(boton);
    expect(boton).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('tooltip')).toBeInTheDocument();

    await user.click(document.body);
    expect(boton).toHaveAttribute('aria-expanded', 'false');

    await user.tab();
    expect(boton).toHaveAttribute('aria-expanded', 'true');
    await user.tab();
    expect(screen.getByRole('button', { name: 'Otro botón' })).toHaveFocus();
    expect(boton).toHaveAttribute('aria-expanded', 'false');
  });
});
