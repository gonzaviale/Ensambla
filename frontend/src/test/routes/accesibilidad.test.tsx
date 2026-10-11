import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { obtenerLectura, subirLote } from '@/api/lotes';
import { crearBloque } from '@/api/bloques';
import { crearCampo } from '@/api/campos';
import { crearProyecto, crearProyectoDesdePlantilla } from '@/api/proyectos';
import { plantillasMock } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion, verifyHeadingPresentAsync } from '@/test/testHelpers';

function plantillaInicial() {
  const plantilla = plantillasMock[0];
  if (!plantilla) throw new Error('Falta una plantilla de prueba');
  return plantilla;
}

// T-056: sin violaciones de accesibilidad detectables por axe en las pantallas del flujo central.
// El contraste de color no se puede medir en jsdom (sin estilos): se verifica a mano con los tokens.
async function sinViolaciones() {
  const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });
  const resumen = resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`);
  expect(resumen).toEqual([]);
}

describe('T-056: accesibilidad (axe)', () => {
  it('Landing', async () => {
    renderApp('/');
    await verifyHeadingPresentAsync('Tu sistema, un bloque a la vez.');
    await sinViolaciones();
  });

  it('Ingresar y Crear cuenta', async () => {
    renderApp('/login');
    await verifyHeadingPresentAsync('Ingresá a tu cuenta');
    await sinViolaciones();
  });

  it('Mis sistemas', async () => {
    setupUsuarioConSesion();
    await crearProyectoDesdePlantilla(plantillaInicial().id);
    renderApp('/proyectos');
    await verifyHeadingPresentAsync('Mis sistemas');
    await screen.findByText(/1 tabla/, {}, ESPERA);
    await sinViolaciones();
  });

  it('Subir foto', async () => {
    setupUsuarioConSesion();
    renderApp('/cuaderno');
    await verifyHeadingPresentAsync('Subir foto de mi cuaderno');
    await sinViolaciones();
  });

  it('Revisar cuaderno, con el editor de columna abierto', async () => {
    setupUsuarioConSesion();
    const lote = await subirLote([new Blob([new Uint8Array([0xff, 0xd8, 0xff, 0xe0])])]);
    await obtenerLectura(lote.id);
    const user = userEvent.setup();
    renderApp(`/cuaderno/${lote.id}`);
    await verifyHeadingPresentAsync('Esto es lo que leímos de tu cuaderno');
    await sinViolaciones();

    await user.click(screen.getByRole('button', { name: 'Editar la columna «Monto»' }));
    await screen.findByRole('dialog', { name: 'Editar columna' });
    await sinViolaciones();
  });

  it('Armar', async () => {
    setupUsuarioConSesion();
    const proyecto = await crearProyecto({ nombre: 'Mi almacén' });
    const bloque = await crearBloque({ proyectoId: proyecto.id, nombre: 'Productos' });
    await crearCampo({
      bloqueId: bloque.id,
      datos: {
        nombre: 'Nombre',
        tipo: 'TEXTO',
        obligatorio: true,
        valorInicial: null,
        opciones: [],
      },
    });
    renderApp(`/proyectos/${proyecto.id}/construir`);
    await verifyHeadingPresentAsync('Tus tablas');
    await screen.findByRole('list', { name: 'Columnas de la tabla' }, ESPERA);
    await sinViolaciones();
  });

  it('Cargar y ver', async () => {
    setupUsuarioConSesion();
    const proyecto = await crearProyectoDesdePlantilla(plantillaInicial().id);
    const user = userEvent.setup();
    renderApp(`/proyectos/${proyecto.id}/usar`);
    await verifyHeadingPresentAsync(proyecto.nombre);
    await screen.findByLabelText('Nombre', {}, ESPERA);
    await sinViolaciones();

    await user.click(screen.getByRole('tab', { name: 'Ver filas' }));
    await screen.findByLabelText('Buscar en Productos', {}, ESPERA);
    await sinViolaciones();
  });
});
