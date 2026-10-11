import { cleanup, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { confirmarLectura, obtenerLectura, subirLote } from '@/api/lotes';
import { crearProyecto, crearProyectoDesdePlantilla } from '@/api/proyectos';
import { plantillasMock } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupServerError,
  setupServerResponse,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
} from '@/test/testHelpers';
import type { Lectura } from '@/types/lectura';

const TITULO = 'Esto es lo que leímos de tu cuaderno';

function elemento<T>(items: readonly T[], indice = 0): T {
  const item = items[indice];
  if (item === undefined) throw new Error(`Falta el elemento de prueba ${indice}`);
  return item;
}

const foto = () => new Blob([new Uint8Array([0xff, 0xd8, 0xff, 0xe0])], { type: 'image/jpeg' });

async function crearLote() {
  const lote = await subirLote([foto()]);
  // La primera consulta del mock responde "procesando": se gasta acá para que la pantalla ya vea LISTO.
  await obtenerLectura(lote.id);
  return lote;
}

async function abrirRevision() {
  setupUsuarioConSesion();
  const lote = await crearLote();
  const user = userEvent.setup();
  const router = renderApp(`/cuaderno/${lote.id}`);
  await verifyHeadingPresentAsync(TITULO);
  return { user, router, lote };
}

const celda = (nombre: string) => screen.getByRole('textbox', { name: nombre });

const editar = async (user: ReturnType<typeof userEvent.setup>, nombre: string, texto: string) => {
  await user.clear(celda(nombre));
  await user.type(celda(nombre), texto);
};

beforeEach(() => sessionStorage.clear());

describe('H-30: revisar lo que leyó la IA', () => {
  it('muestra la foto de cada página junto a los datos para corregir', async () => {
    const { lote } = await abrirRevision();

    const imagen = await screen.findByRole('img', { name: 'Página 1 del cuaderno' }, ESPERA);
    expect(imagen).toHaveAttribute('src', expect.stringContaining(`/lotes/${lote.id}/fotos/`));
    expect(celda('Producto, fila 1')).toHaveValue('Pan');
    expect(celda('Monto, fila 2')).toHaveValue('850');
    expect(screen.getByRole('button', { name: 'Acercar la foto' })).toBeEnabled();
  });

  it('resalta los datos dudosos y corregirlos los da por revisados', async () => {
    const { user } = await abrirRevision();
    expect(screen.getByText('No estamos seguros de este dato.')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Queda 1 dato por revisar.');

    await editar(user, 'Monto, fila 2', '900');

    expect(screen.queryByText('No estamos seguros de este dato.')).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('No quedan datos por revisar.');
  });

  it('«Siguiente duda» lleva el foco al dato dudoso y después lo da por mirado', async () => {
    const { user } = await abrirRevision();

    await user.click(screen.getByRole('button', { name: 'Siguiente duda' }));

    expect(celda('Monto, fila 2')).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Siguiente duda' }));
    expect(screen.getByRole('status')).toHaveTextContent('No quedan datos por revisar.');
    expect(screen.getByRole('button', { name: 'Siguiente duda' })).toBeDisabled();
  });

  it('quita una fila y agrega otra a mano', async () => {
    const { user } = await abrirRevision();

    await user.click(screen.getByRole('button', { name: 'Quitar la fila 2' }));
    expect(screen.queryByRole('textbox', { name: 'Producto, fila 2' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Agregar fila' }));
    expect(celda('Producto, fila 2')).toHaveValue('');
  });

  it('renombra una columna desde su editor', async () => {
    const { user } = await abrirRevision();

    await user.click(screen.getByRole('button', { name: 'Editar la columna «Monto»' }));
    await editar(user, 'Nombre de la columna', 'Importe');

    expect(celda('Importe, fila 1')).toHaveValue('1200');
    await user.click(screen.getByRole('button', { name: 'Listo' }));
    expect(screen.queryByLabelText('Nombre de la columna')).not.toBeInTheDocument();
  });

  it('cambiar el tipo marca los datos que no encajan y frena el guardado', async () => {
    const { user } = await abrirRevision();

    await user.click(screen.getByRole('button', { name: 'Editar la columna «Monto»' }));
    await user.selectOptions(screen.getByLabelText('Qué tipo de dato guarda'), 'FECHA');

    expect(screen.getAllByText('Elegí una fecha válida.')).toHaveLength(2);
    expect(screen.getByText('Hay 2 datos para corregir.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();
  });

  it('una columna de lista pide sus opciones', async () => {
    const { user } = await abrirRevision();

    await user.click(screen.getByRole('button', { name: 'Editar la columna «Producto»' }));
    await user.selectOptions(screen.getByLabelText('Qué tipo de dato guarda'), 'OPCION');
    expect(screen.getByText('Agregá al menos una opción.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();

    await user.type(screen.getByLabelText('Opciones (una por renglón)'), 'Pan{Enter}Leche');

    expect(screen.queryByText('Agregá al menos una opción.')).not.toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: 'Producto, fila 1' })).toHaveValue('Pan');
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeEnabled();
  });

  it('agrega una columna nueva y quita otra pidiendo confirmación', async () => {
    const { user } = await abrirRevision();

    await user.click(screen.getByRole('button', { name: 'Agregar columna' }));
    expect(celda('Columna 3, fila 1')).toHaveValue('');

    await user.click(screen.getByRole('button', { name: 'Editar la columna «Monto»' }));
    await user.click(screen.getByRole('button', { name: 'Quitar columna' }));
    expect(screen.getByRole('alertdialog', { name: '¿Quitar la columna «Monto»?' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Sí, quitar columna' }));

    expect(screen.queryByRole('textbox', { name: 'Monto, fila 1' })).not.toBeInTheDocument();
  });

  it('no deja guardar sin nombre de proyecto', async () => {
    const { user } = await abrirRevision();

    await user.clear(screen.getByLabelText('Nombre del proyecto'));

    expect(screen.getByText('Escribí un nombre para el proyecto.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();
  });

  it('conserva lo corregido si se recarga la página', async () => {
    const { user, lote } = await abrirRevision();
    await editar(user, 'Producto, fila 1', 'Pan lactal');
    cleanup();

    renderApp(`/cuaderno/${lote.id}`);
    await verifyHeadingPresentAsync(TITULO);

    expect(celda('Producto, fila 1')).toHaveValue('Pan lactal');
  });
});

describe('H-30: guardar lo revisado', () => {
  it('crea un proyecto nuevo con los datos corregidos y lleva a verlos', async () => {
    const { user, router } = await abrirRevision();
    await editar(user, 'Producto, fila 1', 'Pan lactal');

    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(router.state.location.pathname).toMatch(/^\/proyectos\/.+\/usar$/), ESPERA);
    expect(await screen.findByText('Pan lactal', {}, ESPERA)).toBeInTheDocument();
    expect(screen.getByText('Leche')).toBeInTheDocument();
    expect(sessionStorage.length).toBe(0);
  });

  it('agrega la tabla a un proyecto que ya existe', async () => {
    setupUsuarioConSesion();
    const existente = await crearProyecto({ nombre: 'Mi negocio' });
    const lote = await crearLote();
    const user = userEvent.setup();
    const router = renderApp(`/cuaderno/${lote.id}`);
    await verifyHeadingPresentAsync(TITULO);

    const opcion = await screen.findByRole('radio', {
      name: 'Agregar una tabla a un proyecto que ya tengo',
    });
    // Se habilita cuando llega la lista de proyectos.
    await waitFor(() => expect(opcion).toBeEnabled(), ESPERA);
    await user.click(opcion);
    expect(screen.getByLabelText('Proyecto')).toHaveValue(existente.id);
    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    await verifyPathAsync(router, `/proyectos/${existente.id}/usar`);
    expect(await screen.findByText('Pan', {}, ESPERA)).toBeInTheDocument();
  });

  it('suma las filas a la tabla que la IA reconoció y no deja tocar sus columnas', async () => {
    setupUsuarioConSesion();
    const proyecto = await crearProyectoDesdePlantilla(elemento(plantillasMock).id);
    const bloque = elemento(proyecto.bloques);
    const lote = await crearLote();
    const lectura: Lectura = {
      loteId: lote.id,
      estado: 'LISTO',
      intentos: 1,
      propuesta: {
        nombre: bloque.nombre,
        bloqueExistenteId: bloque.id,
        mapeoCampos: {
          nombre: elemento(bloque.campos, 0).id,
          stock: elemento(bloque.campos, 1).id,
        },
        campos: [
          { clave: 'nombre', nombre: 'Nombre', tipo: 'TEXTO' },
          { clave: 'stock', nombre: 'Stock', tipo: 'NUMERO' },
        ],
        filas: [
          {
            pagina: 1,
            renglon: 1,
            datos: { nombre: { valor: 'Sal', confianza: 0.99 }, stock: { valor: '20', confianza: 0.99 } },
          },
        ],
      },
    };
    setupServerResponse('get', '/lotes/:id/lectura', 200, lectura);
    const user = userEvent.setup();
    const router = renderApp(`/cuaderno/${lote.id}`);
    await verifyHeadingPresentAsync(TITULO);

    expect(
      await screen.findByRole('radio', { name: new RegExp(`Sumar las filas a tu tabla «${bloque.nombre}»`) }),
    ).toBeChecked();
    expect(screen.queryByLabelText('Nombre de la tabla')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Editar la columna «Stock»' }));
    expect(screen.getByLabelText('Nombre de la columna')).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    await verifyPathAsync(router, `/proyectos/${proyecto.id}/usar`);
    expect(await screen.findByText('Sal', {}, ESPERA)).toBeInTheDocument();
  });

  it('muestra junto al dato el error que devuelve el servidor y deja corregirlo', async () => {
    const { user } = await abrirRevision();
    setupServerError('post', '/lotes/:id/confirmar', 400, 'Revisá los datos marcados', {
      'filas[0].monto': 'Tiene que ser un número',
    });

    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByRole('alert', {}, ESPERA)).toHaveTextContent('Revisá los datos marcados');
    expect(screen.getByText('Tiene que ser un número')).toBeInTheDocument();
    expect(celda('Monto, fila 1')).toHaveAttribute('aria-invalid', 'true');
    await editar(user, 'Monto, fila 1', '1300');
    expect(screen.queryByText('Tiene que ser un número')).not.toBeInTheDocument();
  });

  it('un cuaderno que ya se guardó lo avisa y no vuelve a ofrecer guardar', async () => {
    setupUsuarioConSesion();
    const lote = await crearLote();
    await confirmarLectura({
      loteId: lote.id,
      datos: {
        proyectoId: null,
        nombreProyecto: 'Ventas',
        bloqueId: null,
        nombreBloque: 'Hoja',
        campos: [{ clave: 'a', campoId: null, nombre: 'A', tipo: 'TEXTO', opciones: [] }],
        filas: [{ pagina: 1, valores: { a: 'x' } }],
      },
    });

    renderApp(`/cuaderno/${lote.id}`);

    await verifyHeadingPresentAsync('Este cuaderno ya se guardó');
    expect(screen.queryByRole('button', { name: 'Guardar' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ir a mis proyectos' })).toBeInTheDocument();
  });

  it('descartar vuelve a subir fotos y olvida lo corregido', async () => {
    const { user, router } = await abrirRevision();
    await editar(user, 'Producto, fila 1', 'Otro');

    await user.click(screen.getByRole('button', { name: 'Descartar y subir otras fotos' }));

    await verifyPathAsync(router, '/cuaderno');
    expect(sessionStorage.length).toBe(0);
  });
});
