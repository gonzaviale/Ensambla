import { describe, expect, it } from 'vitest';
import { crearRegistro } from '@/api/registros';
import { crearProyectoDesdePlantilla } from '@/api/proyectos';
import { estado, plantillasMock } from '@/mocks/db/estado';
import { setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';

async function bloqueInventario() {
  setupUsuarioConSesion();
  const plantilla = plantillasMock.find((item) => item.nombre === 'Inventario');
  if (!plantilla) throw new Error('Falta Inventario');
  const proyecto = await crearProyectoDesdePlantilla(plantilla.id);
  const bloque = proyecto.bloques[0];
  if (!bloque) throw new Error('Falta el bloque');
  const nombre = bloque.campos.find((campo) => campo.nombre === 'Nombre');
  const stock = bloque.campos.find((campo) => campo.nombre === 'Stock');
  if (!nombre || !stock) throw new Error('Faltan campos');
  return { bloque, nombre, stock };
}

describe('registros: contrato de creación', () => {
  it('envía valores por UUID con CSRF y recibe el registro creado', async () => {
    const { bloque, nombre, stock } = await bloqueInventario();
    const valores = { [nombre.id]: 'Harina', [stock.id]: 0 };
    const registro = await crearRegistro(bloque.id, valores);
    expect(registro).toMatchObject({ bloqueId: bloque.id, valores });
    expect(registro.id).toBeTruthy();
    expect(registro.creadoEn).toBeTruthy();
    expect(estado.registros.size).toBe(1);
  });

  it('conserva los errores del servidor identificados por campo', async () => {
    const { bloque, nombre, stock } = await bloqueInventario();
    setupServerError('post', `/bloques/${bloque.id}/registros`, 400, 'Revisá los datos', {
      [nombre.id]: 'Nombre rechazado por el servidor',
    });
    await expect(
      crearRegistro(bloque.id, { [nombre.id]: 'Harina', [stock.id]: 1 }),
    ).rejects.toMatchObject({
      status: 400,
      fieldErrors: { [nombre.id]: 'Nombre rechazado por el servidor' },
    });
    expect(estado.registros.size).toBe(0);
  });

  it('no permite crear registros en un bloque ajeno', async () => {
    const { bloque, nombre, stock } = await bloqueInventario();
    setupUsuarioConSesion({ email: 'otro@example.com' });
    await expect(
      crearRegistro(bloque.id, { [nombre.id]: 'Harina', [stock.id]: 1 }),
    ).rejects.toMatchObject({ status: 404 });
    expect(estado.registros.size).toBe(0);
  });

  it('exige sesión y rechaza un bloque inexistente', async () => {
    await expect(crearRegistro(crypto.randomUUID(), {})).rejects.toMatchObject({ status: 401 });
    setupUsuarioConSesion();
    await expect(crearRegistro(crypto.randomUUID(), {})).rejects.toMatchObject({ status: 404 });
  });
});
