import { describe, expect, it } from 'vitest';
import { crearProyectoDesdePlantilla } from '@/api/proyectos';
import { eliminarRegistro, listarRegistros } from '@/api/registros';
import { estado, plantillasMock } from '@/mocks/db/estado';
import { agregarRegistro } from '@/mocks/db/registros';
import { setupUsuarioConSesion } from '@/test/testHelpers';

function elemento<T>(items: readonly T[], indice = 0): T {
  const item = items[indice];
  if (item === undefined) throw new Error(`Falta el elemento de prueba ${indice}`);
  return item;
}

// Inventario: un bloque Productos con Nombre (texto), Stock (número) y Rubro (opción).
async function inventario() {
  setupUsuarioConSesion();
  const proyecto = await crearProyectoDesdePlantilla(elemento(plantillasMock).id);
  const bloque = elemento(proyecto.bloques);
  return {
    bloqueId: bloque.id,
    nombre: elemento(bloque.campos).id,
    stock: elemento(bloque.campos, 1).id,
    rubro: elemento(bloque.campos, 2).id,
  };
}

const minutos = (n: number) => new Date(Date.UTC(2026, 9, 10, 12, n)).toISOString();

describe('registros: contrato del listado', () => {
  it('devuelve una página vacía cuando el bloque no tiene datos', async () => {
    const { bloqueId } = await inventario();

    const pagina = await listarRegistros(bloqueId, {});

    expect(pagina).toMatchObject({
      content: [],
      pageNumber: 0,
      totalElements: 0,
      totalPages: 1,
      first: true,
      last: true,
    });
  });

  it('pagina 120 registros e informa el total y los límites de cada página', async () => {
    const { bloqueId, stock } = await inventario();
    for (let n = 0; n < 120; n++) agregarRegistro(bloqueId, { [stock]: n }, minutos(n % 60));

    const primera = await listarRegistros(bloqueId, { tamanio: 50 });
    const ultima = await listarRegistros(bloqueId, { tamanio: 50, pagina: 2 });

    expect(primera).toMatchObject({ pageSize: 50, totalElements: 120, totalPages: 3, first: true });
    expect(primera.content).toHaveLength(50);
    expect(primera.last).toBe(false);
    expect(ultima.content).toHaveLength(20);
    expect(ultima).toMatchObject({ pageNumber: 2, first: false, last: true });
  });

  it('acota el tamaño de página entre 1 y 100', async () => {
    const { bloqueId, stock } = await inventario();
    for (let n = 0; n < 120; n++) agregarRegistro(bloqueId, { [stock]: n });

    expect((await listarRegistros(bloqueId, { tamanio: 500 })).pageSize).toBe(100);
    expect((await listarRegistros(bloqueId, {})).pageSize).toBe(20);
  });

  it('ordena los números por valor y deja los registros sin el dato al final', async () => {
    const { bloqueId, stock } = await inventario();
    agregarRegistro(bloqueId, {}, minutos(0));
    agregarRegistro(bloqueId, { [stock]: 100 }, minutos(1));
    agregarRegistro(bloqueId, { [stock]: 9 }, minutos(2));
    agregarRegistro(bloqueId, { [stock]: 10 }, minutos(3));
    const valores = (direccion: 'asc' | 'desc') =>
      listarRegistros(bloqueId, { orden: stock, direccion }).then((pagina) =>
        pagina.content.map((registro) => registro.valores[stock]),
      );

    expect(await valores('asc')).toEqual([9, 10, 100, undefined]);
    expect(await valores('desc')).toEqual([100, 10, 9, undefined]);
  });

  it('ordena los textos sin distinguir mayúsculas', async () => {
    const { bloqueId, nombre } = await inventario();
    for (const texto of ['Zeta', 'banana', 'Apple']) agregarRegistro(bloqueId, { [nombre]: texto });

    const pagina = await listarRegistros(bloqueId, { orden: nombre, direccion: 'asc' });

    expect(pagina.content.map((registro) => registro.valores[nombre])).toEqual([
      'Apple',
      'banana',
      'Zeta',
    ]);
  });

  it('por defecto lista los últimos cargados primero', async () => {
    const { bloqueId, nombre } = await inventario();
    agregarRegistro(bloqueId, { [nombre]: 'Viejo' }, minutos(0));
    agregarRegistro(bloqueId, { [nombre]: 'Nuevo' }, minutos(5));

    const pagina = await listarRegistros(bloqueId, {});

    expect(pagina.content.map((registro) => registro.valores[nombre])).toEqual(['Nuevo', 'Viejo']);
  });

  it('busca el texto entre los valores sin distinguir mayúsculas', async () => {
    const { bloqueId, nombre, stock } = await inventario();
    agregarRegistro(bloqueId, { [nombre]: 'Harina 000', [stock]: 4500 });
    agregarRegistro(bloqueId, { [nombre]: 'Azúcar', [stock]: 12 });

    const porTexto = await listarRegistros(bloqueId, { q: 'HARINA' });
    const porNumero = await listarRegistros(bloqueId, { q: '450' });

    expect(porTexto.content.map((registro) => registro.valores[nombre])).toEqual(['Harina 000']);
    expect(porTexto.totalElements).toBe(1);
    expect(porNumero.content).toHaveLength(1);
  });

  it('rechaza una dirección inválida y un campo que no es del bloque', async () => {
    const { bloqueId } = await inventario();

    await expect(listarRegistros(bloqueId, { direccion: 'arriba' as 'asc' })).rejects.toMatchObject(
      {
        status: 400,
      },
    );
    await expect(listarRegistros(bloqueId, { orden: crypto.randomUUID() })).rejects.toMatchObject({
      status: 400,
      message: 'No se puede ordenar por ese campo',
    });
  });

  it('exige sesión y no expone los bloques de otro usuario', async () => {
    const { bloqueId } = await inventario();
    setupUsuarioConSesion({ email: 'otro@example.com' });

    await expect(listarRegistros(bloqueId, {})).rejects.toMatchObject({ status: 404 });
    estado.sesionUsuarioId = null;
    await expect(listarRegistros(bloqueId, {})).rejects.toMatchObject({ status: 401 });
  });
});

describe('registros: eliminación', () => {
  it('elimina el registro y deja de listarlo', async () => {
    const { bloqueId, nombre } = await inventario();
    const registro = agregarRegistro(bloqueId, { [nombre]: 'Temporal' });

    await eliminarRegistro(registro.id);

    expect((await listarRegistros(bloqueId, {})).content).toEqual([]);
  });

  it('no elimina registros de otro usuario ni inexistentes', async () => {
    const { bloqueId, nombre } = await inventario();
    const registro = agregarRegistro(bloqueId, { [nombre]: 'Privado' });
    setupUsuarioConSesion({ email: 'otro@example.com' });

    await expect(eliminarRegistro(registro.id)).rejects.toMatchObject({ status: 404 });
    await expect(eliminarRegistro(crypto.randomUUID())).rejects.toMatchObject({ status: 404 });
    expect(estado.registros.has(registro.id)).toBe(true);
  });
});
