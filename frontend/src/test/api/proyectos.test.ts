import { describe, expect, it } from 'vitest';
import {
  crearProyecto,
  crearProyectoDesdePlantilla,
  eliminarProyecto,
  listarProyectos,
  obtenerProyecto,
  renombrarProyecto,
} from '@/api/proyectos';
import { estado, plantillasMock } from '@/mocks/db/estado';
import { setupUsuarioConSesion } from '@/test/testHelpers';

function elemento<T>(items: readonly T[], indice = 0): T {
  const item = items[indice];
  if (item === undefined) throw new Error(`Falta el elemento de prueba ${indice}`);
  return item;
}

describe('proyectos: contrato y aislamiento', () => {
  it('crea un proyecto en blanco en la sesión y normaliza su nombre', async () => {
    setupUsuarioConSesion();
    const proyecto = await crearProyecto({ nombre: '  Mi negocio  ' });

    expect(proyecto.nombre).toBe('Mi negocio');
    expect(proyecto.bloques).toEqual([]);
    expect(proyecto.plantillaId).toBeNull();
    expect((await listarProyectos()).map((item) => item.id)).toEqual([proyecto.id]);
  });

  it('copia una plantilla con ids distintos en cada proyecto y conserva sus valores', async () => {
    setupUsuarioConSesion();
    const plantilla = elemento(plantillasMock);
    const original = structuredClone(plantilla);

    const primero = await crearProyectoDesdePlantilla(plantilla.id);
    const segundo = await crearProyectoDesdePlantilla(plantilla.id);
    const primerBloque = elemento(primero.bloques);
    const segundoBloque = elemento(segundo.bloques);

    expect(primero.id).not.toBe(segundo.id);
    expect(primerBloque.id).not.toBe(segundoBloque.id);
    expect(elemento(primerBloque.campos).id).not.toBe(elemento(segundoBloque.campos).id);
    expect(elemento(primerBloque.campos, 1).valorInicial).toBe('0');
    expect(elemento(primerBloque.campos, 2).opciones).toEqual(['Insumos', 'Terminados']);
    expect(await obtenerProyecto(primero.id)).toEqual(primero);
    expect(plantilla).toEqual(original);
  });

  it('renombra y permite releer el nombre guardado', async () => {
    setupUsuarioConSesion();
    const proyecto = await crearProyecto({ nombre: 'Original' });

    await renombrarProyecto({ id: proyecto.id, nombre: '  Nuevo  ' });

    expect((await obtenerProyecto(proyecto.id)).nombre).toBe('Nuevo');
  });

  it('elimina el proyecto y deja de devolverlo', async () => {
    setupUsuarioConSesion();
    const proyecto = await crearProyecto({ nombre: 'Temporal' });

    await eliminarProyecto(proyecto.id);

    expect(await listarProyectos()).toEqual([]);
    await expect(obtenerProyecto(proyecto.id)).rejects.toMatchObject({ status: 404 });
  });

  it('no lista proyectos ajenos', async () => {
    setupUsuarioConSesion();
    await crearProyecto({ nombre: 'Privado' });
    setupUsuarioConSesion({ email: 'otro@example.com' });

    expect(await listarProyectos()).toEqual([]);
  });

  it('no permite consultar, renombrar ni borrar proyectos ajenos', async () => {
    setupUsuarioConSesion();
    const proyecto = await crearProyecto({ nombre: 'Privado' });
    setupUsuarioConSesion({ email: 'otro@example.com' });

    await expect(obtenerProyecto(proyecto.id)).rejects.toMatchObject({ status: 404 });
    await expect(renombrarProyecto({ id: proyecto.id, nombre: 'Cambio' })).rejects.toMatchObject({
      status: 404,
    });
    await expect(eliminarProyecto(proyecto.id)).rejects.toMatchObject({ status: 404 });
    expect(estado.proyectos.get(proyecto.id)?.proyecto.nombre).toBe('Privado');
  });

  it('rechaza una plantilla inexistente sin crear proyectos', async () => {
    setupUsuarioConSesion();

    await expect(crearProyectoDesdePlantilla(crypto.randomUUID())).rejects.toMatchObject({
      status: 404,
    });
    expect(await listarProyectos()).toEqual([]);
  });

  it('exige sesión para crear proyectos', async () => {
    await expect(crearProyecto({ nombre: 'Nuevo' })).rejects.toMatchObject({ status: 401 });
  });
});
