import { beforeEach, describe, expect, it } from 'vitest';
import {
  ajusteParaGuardar,
  aplicarAcciones,
  mismoSistema,
  resumirSistema,
  sistemaParaCrear,
  type AccionDeSami,
} from '@/lib/sistema/acciones';
import { allBlocks, clone, makeSystem, makeTemplate, upsertRow } from '@/lib/sistema/core';
import type { Fila, Sistema } from '@/lib/sistema/tipos';

const accion = (herramienta: string, argumentos: Record<string, unknown>): AccionDeSami => ({
  herramienta,
  argumentos,
});

describe('las acciones de Sami sobre el taller', () => {
  let sistema: Sistema;
  const pieza = (tipo: string) => {
    const encontrada = allBlocks(sistema).find((bloque) => bloque.type === tipo);
    if (!encontrada) throw new Error(`Falta la pieza ${tipo}`);
    return encontrada;
  };

  beforeEach(() => {
    sistema = makeTemplate('shop', {}, true);
  });

  describe('resumirSistema', () => {
    it('resume nombre, distribución, piezas y datos sin filas', () => {
      const resumen = resumirSistema(sistema);

      expect(resumen.nombre).toBe(sistema.name);
      expect(resumen.distribucion).toBe('mosaic');
      expect(resumen.piezas.map((item) => item.tipo)).toEqual(['inventory', 'sales', 'clients']);
      const inventario = resumen.piezas[0];
      expect(inventario?.id).toBe(pieza('inventory').id);
      expect(inventario?.campos[0]).toEqual({ etiqueta: 'Producto', tipo: 'text' });
      expect(JSON.stringify(resumen)).not.toContain('Yerba');
    });

    it('lleva la distribución elegida y la sección de cada pieza', () => {
      sistema.layout = 'single';

      const resumen = resumirSistema(sistema);

      expect(resumen.distribucion).toBe('single');
      expect(resumen.piezas.every((item) => item.seccion === sistema.sections[0]?.name)).toBe(true);
    });
  });

  describe('agregar_pieza y quitar_pieza', () => {
    it('suma una pieza del catálogo con sus datos de ejemplo', () => {
      const resultado = aplicarAcciones(sistema, [accion('agregar_pieza', { tipo: 'expenses' })]);

      expect(resultado.aplicadas).toEqual(['Sumé gastos.']);
      expect(pieza('expenses').rows.length).toBeGreaterThan(0);
    });

    it('no suma una pieza repetida ni de un tipo que no es del catálogo', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('agregar_pieza', { tipo: 'inventory' }),
        accion('agregar_pieza', { tipo: 'turnos' }),
      ]);

      expect(resultado.aplicadas).toEqual([]);
      expect(resultado.omitidas).toEqual(['Esa pieza ya está.', 'No existe una pieza «turnos».']);
      expect(allBlocks(sistema)).toHaveLength(3);
    });

    it('suma cualquiera de los componentes del catálogo, también la nota y el resumen', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('agregar_pieza', { tipo: 'note', nombre: 'Para recordar' }),
        accion('agregar_pieza', { tipo: 'summary', campos: [{ etiqueta: 'X', tipo: 'text' }] }),
      ]);

      expect(resultado.aplicadas).toEqual(['Sumé para recordar.', 'Sumé resumen.']);
      expect(pieza('summary').fields).toEqual([]);
    });

    it('no puede sumar nada si no hay secciones', () => {
      sistema.sections = [];

      const resultado = aplicarAcciones(sistema, [accion('agregar_pieza', { tipo: 'expenses' })]);

      expect(resultado.omitidas).toEqual(['No hay dónde sumar la pieza.']);
    });

    it('quita una pieza por id, por tipo o por nombre', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('quitar_pieza', { pieza: pieza('clients').id }),
        accion('quitar_pieza', { pieza: 'sales' }),
        accion('quitar_pieza', { pieza: 'inventario' }),
      ]);

      expect(resultado.aplicadas).toHaveLength(3);
      expect(allBlocks(sistema)).toHaveLength(0);
    });

    it('no quita una pieza que no existe', () => {
      const resultado = aplicarAcciones(sistema, [accion('quitar_pieza', { pieza: 'fantasma' })]);

      expect(resultado.omitidas).toEqual(['No encontré esa pieza.']);
    });

    it('al quitar una pieza, los vínculos que apuntaban a ella no quedan colgados', () => {
      aplicarAcciones(sistema, [
        accion('agregar_pieza', { tipo: 'form', nombre: 'Turnos' }),
        accion('conectar', { desde: 'Turnos', hacia: 'clients', etiqueta: 'Clienta' }),
      ]);
      expect(pieza('sales').fields.find((campo) => campo.key === 'customer')?.target).toBe(
        pieza('clients').id,
      );

      aplicarAcciones(sistema, [accion('quitar_pieza', { pieza: 'clients' })]);

      const cliente = pieza('sales').fields.find((campo) => campo.key === 'customer');
      expect(cliente).toMatchObject({ type: 'text' });
      expect(cliente?.target).toBeUndefined();
      expect(pieza('sales').rows.every((fila) => !('customer' in fila))).toBe(true);
      const clienta = pieza('form').fields.find((campo) => campo.label === 'Clienta');
      expect(clienta).toMatchObject({ type: 'text' });
      expect(clienta?.target).toBeUndefined();
    });

    it('al quitar el inventario, el producto de las ventas sigue siendo un vínculo, sin destino', () => {
      aplicarAcciones(sistema, [accion('quitar_pieza', { pieza: 'inventory' })]);

      const producto = pieza('sales').fields.find((campo) => campo.key === 'product');
      expect(producto).toMatchObject({ type: 'relation' });
      expect(producto?.target).toBeUndefined();
    });
  });

  describe('renombrar', () => {
    it('renombra una pieza y el sistema, recortando los espacios', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('renombrar_pieza', { pieza: 'sales', nombre: '  Caja  ' }),
        accion('renombrar_sistema', { nombre: '  Velas de Ana ' }),
      ]);

      expect(resultado.aplicadas).toHaveLength(2);
      expect(pieza('sales').name).toBe('Caja');
      expect(sistema.name).toBe('Velas de Ana');
    });

    it('recorta los nombres demasiado largos', () => {
      aplicarAcciones(sistema, [
        accion('renombrar_pieza', { pieza: 'sales', nombre: 'x'.repeat(80) }),
        accion('renombrar_sistema', { nombre: 'y'.repeat(150) }),
      ]);

      expect(pieza('sales').name).toHaveLength(60);
      expect(sistema.name).toHaveLength(100);
    });

    it('omite un nombre vacío o una pieza que no existe', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('renombrar_pieza', { pieza: 'sales', nombre: ' ' }),
        accion('renombrar_pieza', { pieza: 'nada', nombre: 'X' }),
        accion('renombrar_sistema', { nombre: '' }),
      ]);

      expect(resultado.omitidas).toHaveLength(3);
    });
  });

  describe('agregar_campo y quitar_campo', () => {
    it('suma un dato con una clave propia y única', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('agregar_campo', {
          pieza: 'inventory',
          etiqueta: 'Fecha de vencimiento',
          tipo: 'date',
        }),
      ]);

      expect(resultado.aplicadas).toEqual(['Sumé Fecha de vencimiento a inventario.']);
      const campo = pieza('inventory').fields.at(-1);
      expect(campo).toMatchObject({ label: 'Fecha de vencimiento', type: 'date', required: false });
      expect(campo?.key).toBe('fecha_de_vencimiento');
    });

    it('una clave que ya existe recibe un sufijo', () => {
      pieza('inventory').fields.push({
        key: 'rubro',
        label: 'Otro',
        type: 'text',
        required: false,
      });

      aplicarAcciones(sistema, [
        accion('agregar_campo', { pieza: 'inventory', etiqueta: 'Rubro', tipo: 'text' }),
      ]);

      expect(pieza('inventory').fields.at(-1)?.key).toBe('rubro_2');
    });

    it('una etiqueta sólo de signos usa una clave genérica', () => {
      aplicarAcciones(sistema, [
        accion('agregar_campo', { pieza: 'inventory', etiqueta: '???', tipo: 'text' }),
      ]);

      expect(pieza('inventory').fields.at(-1)?.key).toBe('dato');
    });

    it('suma un dato de lista con sus opciones', () => {
      aplicarAcciones(sistema, [
        accion('agregar_campo', {
          pieza: 'sales',
          etiqueta: 'Forma de pago',
          tipo: 'select',
          opciones: ['Efectivo', ' Tarjeta ', ''],
        }),
      ]);

      expect(pieza('sales').fields.at(-1)).toMatchObject({
        type: 'select',
        options: ['Efectivo', 'Tarjeta'],
      });
    });

    it('omite datos repetidos, de tipo inválido, de lista sin opciones o de una pieza inexistente', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('agregar_campo', { pieza: 'inventory', etiqueta: 'producto', tipo: 'text' }),
        accion('agregar_campo', { pieza: 'inventory', etiqueta: 'Color', tipo: 'color' }),
        accion('agregar_campo', {
          pieza: 'inventory',
          etiqueta: 'Rubro',
          tipo: 'select',
          opciones: ['Una'],
        }),
        accion('agregar_campo', { pieza: 'inventory', etiqueta: 'Rubro', tipo: 'select' }),
        accion('agregar_campo', { pieza: 'nada', etiqueta: 'X', tipo: 'text' }),
      ]);

      expect(resultado.aplicadas).toEqual([]);
      expect(resultado.omitidas).toHaveLength(5);
    });

    it('quita un dato y también su valor en las filas', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('quitar_campo', { pieza: 'inventory', campo: 'costo' }),
      ]);

      expect(resultado.aplicadas).toEqual(['Quité Costo de inventario.']);
      expect(pieza('inventory').fields.some((campo) => campo.key === 'cost')).toBe(false);
      expect(pieza('inventory').rows.every((fila) => !('cost' in fila))).toBe(true);
    });

    it('no quita un dato obligatorio, una conexión ni uno que no existe', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('quitar_campo', { pieza: 'inventory', campo: 'Producto' }),
        accion('quitar_campo', { pieza: 'sales', campo: 'Cliente' }),
        accion('quitar_campo', { pieza: 'inventory', campo: 'Color' }),
        accion('quitar_campo', { pieza: 'nada', campo: 'Producto' }),
      ]);

      expect(resultado.aplicadas).toEqual([]);
      expect(resultado.omitidas).toEqual([
        '«Producto» no se puede quitar.',
        '«Cliente» no se puede quitar.',
        'No encontré ese dato.',
        'No encontré ese dato.',
      ]);
    });
  });

  describe('separar_en_seccion y cambiar_distribucion', () => {
    it('crea la sección y pasa la pieza', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('separar_en_seccion', { pieza: 'clients', seccion: 'Mis clientes' }),
      ]);

      expect(resultado.aplicadas).toHaveLength(1);
      const seccion = sistema.sections.find((item) => item.name === 'Mis clientes');
      expect(seccion?.blocks.map((bloque) => bloque.type)).toEqual(['clients']);
    });

    it('reutiliza una sección que ya existe sin importar mayúsculas', () => {
      aplicarAcciones(sistema, [
        accion('separar_en_seccion', { pieza: 'clients', seccion: 'Aparte' }),
        accion('separar_en_seccion', { pieza: 'sales', seccion: 'aparte' }),
      ]);

      expect(sistema.sections.filter((item) => item.name.toLowerCase() === 'aparte')).toHaveLength(
        1,
      );
      expect(sistema.sections.find((item) => item.name === 'Aparte')?.blocks).toHaveLength(2);
    });

    it('omite una pieza inexistente o una sección sin nombre', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('separar_en_seccion', { pieza: 'nada', seccion: 'X' }),
        accion('separar_en_seccion', { pieza: 'clients', seccion: ' ' }),
      ]);

      expect(resultado.omitidas).toHaveLength(2);
    });

    it('cambia la distribución de todas las piezas', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('cambiar_distribucion', { layout: 'single' }),
      ]);

      expect(resultado.aplicadas).toEqual(['Cambié la distribución.']);
      expect(sistema.layout).toBe('single');
      expect(allBlocks(sistema).every((bloque) => bloque.width === 'full')).toBe(true);
    });

    it('omite una distribución que no existe', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('cambiar_distribucion', { layout: 'diagonal' }),
      ]);

      expect(resultado.omitidas).toEqual(['No conozco esa distribución.']);
    });
  });

  describe('editar_campo', () => {
    const campo = (tipo: string, etiqueta: string) =>
      pieza(tipo).fields.find((item) => item.label === etiqueta);
    const etiquetas = (tipo: string) => pieza(tipo).fields.map((item) => item.label);

    it('renombra un dato buscándolo sin importar mayúsculas ni tildes; la clave no cambia', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('editar_campo', { pieza: 'Clientes', campo: 'telefono', etiqueta: ' Celular ' }),
      ]);

      expect(resultado).toEqual({
        aplicadas: ['Renombré Teléfono a Celular en clientes.'],
        omitidas: [],
      });
      expect(campo('clients', 'Celular')).toMatchObject({ key: 'phone', type: 'tel' });
      expect(pieza('clients').rows[0]?.phone).toBe('11 5555-0101');
    });

    it('recorta el nombre nuevo y permite cambiar sólo mayúsculas', () => {
      aplicarAcciones(sistema, [
        accion('editar_campo', { pieza: 'clients', campo: 'Notas', etiqueta: 'x'.repeat(80) }),
        accion('editar_campo', { pieza: 'clients', campo: 'Correo', etiqueta: 'CORREO' }),
      ]);

      expect(pieza('clients').fields.find((item) => item.key === 'notes')?.label).toHaveLength(60);
      expect(pieza('clients').fields.find((item) => item.key === 'email')?.label).toBe('CORREO');
    });

    it('no repite el nombre de otro dato de la pieza', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('editar_campo', { pieza: 'clients', campo: 'Correo', etiqueta: 'teléfono' }),
      ]);

      expect(resultado.omitidas).toEqual(['Ya hay un dato «teléfono» en clientes.']);
      expect(campo('clients', 'Correo')).toBeDefined();
    });

    it('cambia el tipo y convierte los valores de las filas; lo que no sirve se borra', () => {
      const inventario = pieza('inventory');
      const [primera, segunda] = inventario.rows as [Fila, Fila];
      primera.minimum = 5;
      segunda.minimum = 'mucho';
      const real: Fila = { id: 'real', name: 'Arroz', minimum: '7' };
      inventario.rows.push(real);

      const resultado = aplicarAcciones(sistema, [
        accion('editar_campo', { pieza: 'inventory', campo: 'Stock mínimo', tipo: 'text' }),
        accion('editar_campo', { pieza: 'inventory', campo: 'Stock mínimo', tipo: 'number' }),
      ]);

      expect(resultado.aplicadas).toEqual([
        'Cambié Stock mínimo a texto en inventario.',
        'Cambié Stock mínimo a número en inventario.',
      ]);
      expect(campo('inventory', 'Stock mínimo')?.type).toBe('number');
      expect(primera.minimum).toBe(5);
      expect('minimum' in segunda).toBe(false);
      expect(real.minimum).toBe(7);
    });

    it('al pasar a lista usa las opciones (hasta seis, sin repetir) y descarta los valores que no están', () => {
      const clientes = pieza('clients');
      const [primera, segunda] = clientes.rows as [Fila, Fila];
      primera.notes = 'mayorista';
      segunda.notes = 'Otra cosa';

      const resultado = aplicarAcciones(sistema, [
        accion('editar_campo', {
          pieza: 'clients',
          campo: 'Notas',
          tipo: 'select',
          opciones: ['Mayorista', 'MAYORISTA', 'Minorista', 'A', 'B', 'C', 'D', 'E', ''],
        }),
      ]);

      expect(resultado.aplicadas).toEqual(['Cambié Notas a lista de opciones en clientes.']);
      expect(campo('clients', 'Notas')).toMatchObject({
        type: 'select',
        options: ['Mayorista', 'Minorista', 'A', 'B', 'C', 'D'],
      });
      expect(primera.notes).toBe('Mayorista');
      expect('notes' in segunda).toBe(false);
    });

    it('cambia las opciones de una lista y al dejar de ser lista las saca', () => {
      aplicarAcciones(sistema, [accion('agregar_pieza', { tipo: 'expenses' })]);

      const resultado = aplicarAcciones(sistema, [
        accion('editar_campo', {
          pieza: 'expenses',
          campo: 'Categoría',
          opciones: ['Mercadería', 'Impuestos'],
        }),
      ]);
      expect(resultado.aplicadas).toEqual(['Cambié las opciones de Categoría en gastos.']);
      expect(campo('expenses', 'Categoría')?.options).toEqual(['Mercadería', 'Impuestos']);
      expect(pieza('expenses').rows.map((fila) => fila.category)).toEqual([
        'Mercadería',
        undefined,
      ]);

      aplicarAcciones(sistema, [
        accion('editar_campo', { pieza: 'expenses', campo: 'Categoría', tipo: 'text' }),
      ]);
      expect(campo('expenses', 'Categoría')?.type).toBe('text');
      expect(campo('expenses', 'Categoría')?.options).toBeUndefined();
    });

    it('no pasa a lista sin al menos dos opciones válidas', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('editar_campo', { pieza: 'clients', campo: 'Notas', tipo: 'select' }),
        accion('editar_campo', {
          pieza: 'clients',
          campo: 'Notas',
          tipo: 'select',
          opciones: ['Sola', ' ', 'sola'],
        }),
      ]);

      expect(resultado.omitidas).toEqual([
        'Una lista necesita al menos dos opciones.',
        'Una lista necesita al menos dos opciones.',
      ]);
      expect(campo('clients', 'Notas')?.type).toBe('textarea');
    });

    it('no cambia el tipo de un vínculo, del dato principal ni a un tipo desconocido', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('editar_campo', { pieza: 'sales', campo: 'Cliente', tipo: 'text' }),
        accion('editar_campo', { pieza: 'clients', campo: 'Nombre', tipo: 'number' }),
        accion('editar_campo', { pieza: 'clients', campo: 'Notas', tipo: 'relation' }),
        accion('editar_campo', { pieza: 'clients', campo: 'Notas', tipo: 'color' }),
      ]);

      expect(resultado.aplicadas).toEqual([]);
      expect(resultado.omitidas).toEqual([
        '«Cliente» conecta con otra pieza: no le puedo cambiar el tipo.',
        '«Nombre» es el dato principal de clientes: no le puedo cambiar el tipo.',
        'No conozco el tipo «relation».',
        'No conozco el tipo «color».',
      ]);
      expect(campo('sales', 'Cliente')?.type).toBe('relation');
      expect(campo('clients', 'Nombre')?.type).toBe('text');
    });

    it('un vínculo y el dato principal sí se pueden renombrar', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('editar_campo', { pieza: 'Ventas', campo: 'Cliente', etiqueta: 'Clienta' }),
        accion('editar_campo', {
          pieza: 'clients',
          campo: 'Nombre',
          etiqueta: 'Nombre y apellido',
        }),
      ]);

      expect(resultado.aplicadas).toEqual([
        'Renombré Cliente a Clienta en ventas.',
        'Renombré Nombre a Nombre y apellido en clientes.',
      ]);
      expect(campo('sales', 'Clienta')).toMatchObject({ key: 'customer', type: 'relation' });
    });

    it('lo hace obligatorio o no', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('editar_campo', { pieza: 'clients', campo: 'Teléfono', obligatorio: true }),
        accion('editar_campo', { pieza: 'clients', campo: 'Nombre', obligatorio: false }),
      ]);

      expect(resultado.aplicadas).toEqual([
        'Hice obligatorio Teléfono en clientes.',
        'Nombre ya no es obligatorio en clientes.',
      ]);
      expect(campo('clients', 'Teléfono')?.required).toBe(true);
      expect(campo('clients', 'Nombre')?.required).toBe(false);
    });

    it('mueve el dato al lugar pedido, acotado a los que hay', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('editar_campo', { pieza: 'Ventas', campo: 'Cliente', posicion: 1 }),
        accion('editar_campo', { pieza: 'clients', campo: 'Nombre', posicion: 99 }),
        accion('editar_campo', { pieza: 'clients', campo: 'Correo', posicion: '0' }),
      ]);

      expect(resultado.aplicadas).toEqual([
        'Moví Cliente al lugar 1 en ventas.',
        'Moví Nombre al lugar 4 en clientes.',
        'Moví Correo al lugar 1 en clientes.',
      ]);
      expect(etiquetas('sales')).toEqual([
        'Cliente',
        'Producto',
        'Cantidad',
        'Precio',
        'Fecha',
        'Fiado',
      ]);
      expect(etiquetas('clients')).toEqual(['Correo', 'Teléfono', 'Notas', 'Nombre']);
    });

    it('renombra y mueve en la misma acción', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('editar_campo', {
          pieza: 'sales',
          campo: 'Cliente',
          etiqueta: 'Clienta',
          posicion: 1,
        }),
      ]);

      expect(resultado.aplicadas).toEqual([
        'Renombré Cliente a Clienta en ventas.',
        'Moví Clienta al lugar 1 en ventas.',
      ]);
      expect(etiquetas('sales')[0]).toBe('Clienta');
    });

    it('omite un lugar que no es un número', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('editar_campo', { pieza: 'clients', campo: 'Notas', posicion: 'arriba' }),
      ]);

      expect(resultado.omitidas).toEqual(['No entendí a qué lugar mover ese dato.']);
    });

    it('si no cambia nada, o si falta la pieza o el dato, se omite', () => {
      const antes = clone(sistema);

      const resultado = aplicarAcciones(sistema, [
        accion('editar_campo', {
          pieza: 'clients',
          campo: 'Teléfono',
          etiqueta: 'Teléfono',
          tipo: 'tel',
          obligatorio: false,
          posicion: 2,
        }),
        accion('editar_campo', { pieza: 'clients', campo: 'Teléfono' }),
        accion('editar_campo', { pieza: 'clients', campo: 'Teléfono', obligatorio: 'sí' }),
        accion('editar_campo', { pieza: 'clients', campo: 'Edad', etiqueta: 'Años' }),
        accion('editar_campo', { pieza: 'proveedores', campo: 'Nombre', etiqueta: 'Razón' }),
      ]);

      expect(resultado.aplicadas).toEqual([]);
      expect(resultado.omitidas).toEqual([
        '«Teléfono» ya estaba así.',
        '«Teléfono» ya estaba así.',
        'No entendí si «Teléfono» es obligatorio.',
        'No encontré ese dato.',
        'No encontré ese dato.',
      ]);
      expect(mismoSistema(sistema, antes)).toBe(true);
    });

    it('es todo o nada: si una parte no vale, no cambia el resto', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('editar_campo', {
          pieza: 'clients',
          campo: 'Notas',
          etiqueta: 'Comentarios',
          tipo: 'color',
          posicion: 1,
        }),
        accion('editar_campo', {
          pieza: 'clients',
          campo: 'Notas',
          etiqueta: 'Comentarios',
          obligatorio: 'sí',
        }),
      ]);

      expect(resultado.aplicadas).toEqual([]);
      expect(etiquetas('clients')).toEqual(['Nombre', 'Teléfono', 'Correo', 'Notas']);
    });

    it('el tipo de un vínculo o del dato principal se ignora y lo demás se aplica', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('editar_campo', {
          pieza: 'clients',
          campo: 'Nombre',
          etiqueta: 'Razón social',
          tipo: 'number',
        }),
      ]);

      expect(resultado).toEqual({
        aplicadas: ['Renombré Nombre a Razón social en clientes.'],
        omitidas: [],
      });
      expect(campo('clients', 'Razón social')?.type).toBe('text');
    });

    it('lista vacía, nulos o textos en blanco cuentan como no pedidos', () => {
      aplicarAcciones(sistema, [accion('agregar_pieza', { tipo: 'expenses' })]);

      const resultado = aplicarAcciones(sistema, [
        accion('editar_campo', {
          pieza: 'expenses',
          campo: 'Categoría',
          etiqueta: ' ',
          tipo: null,
          opciones: [],
          obligatorio: null,
          posicion: '',
        }),
      ]);

      expect(resultado.omitidas).toEqual(['«Categoría» ya estaba así.']);
    });
  });

  it('una herramienta desconocida se omite sin romper el resto', () => {
    const resultado = aplicarAcciones(sistema, [
      accion('borrar_todo', {}),
      accion('agregar_pieza', { tipo: 'expenses' }),
    ]);

    expect(resultado.omitidas).toEqual(['No sé hacer «borrar_todo».']);
    expect(resultado.aplicadas).toEqual(['Sumé gastos.']);
  });

  describe('mismoSistema', () => {
    it('ignora la fecha de actualización pero detecta cualquier otro cambio', () => {
      const copia = clone(sistema);
      copia.updatedAt = 'otra fecha';
      expect(mismoSistema(sistema, copia)).toBe(true);

      copia.name = 'Otro nombre';
      expect(mismoSistema(sistema, copia)).toBe(false);
    });
  });
});

describe('Sami ensambla el sistema desde cero', () => {
  let sistema: Sistema;
  const llamada = (nombre: string) => {
    const encontrada = allBlocks(sistema).find((bloque) => bloque.name === nombre);
    if (!encontrada) throw new Error(`Falta la pieza ${nombre}`);
    return encontrada;
  };
  const sinId = (filas: Fila[]) =>
    filas.map((fila) => {
      const resto: Partial<Fila> = { ...fila };
      delete resto.id;
      return resto;
    });

  beforeEach(() => {
    sistema = makeSystem('Mi sistema', [], false);
  });

  describe('agregar_pieza con nombre, datos y ejemplos', () => {
    it('suma el componente con su nombre, los datos nuevos y las filas de ejemplo de Sami', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('agregar_pieza', {
          tipo: 'inventory',
          nombre: 'Velas',
          campos: [
            { etiqueta: 'Aroma', tipo: 'text' },
            { etiqueta: 'producto', tipo: 'text' },
            { etiqueta: 'Color', tipo: 'color' },
          ],
          ejemplos: [
            {
              Producto: 'Vela de lavanda',
              Cantidad: '12',
              Precio: 6500,
              Aroma: 'Lavanda',
              Otra: 1,
            },
            { 'Algo que no existe': 'x' },
          ],
        }),
      ]);

      expect(resultado.aplicadas).toEqual(['Sumé velas.']);
      expect(resultado.omitidas).toEqual(['«producto» ya está.', 'No pude sumar ese dato.']);
      const velas = llamada('Velas');
      expect(velas.type).toBe('inventory');
      expect(velas.fields.map((campo) => campo.label)).toEqual([
        'Producto',
        'Cantidad',
        'Precio',
        'Costo',
        'Stock mínimo',
        'Aroma',
      ]);
      expect(velas.fields.at(-1)).toMatchObject({ key: 'aroma', type: 'text', required: false });
      expect(sinId(velas.rows)).toEqual([
        { __example: true, name: 'Vela de lavanda', quantity: 12, price: 6500, aroma: 'Lavanda' },
      ]);
      expect(JSON.stringify(velas.rows)).not.toContain('Yerba');
    });

    it('convierte cada valor al tipo del dato, descarta lo que no sirve y carga como mucho cinco filas', () => {
      aplicarAcciones(sistema, [
        accion('agregar_pieza', {
          tipo: 'form',
          nombre: 'Turnos',
          campos: [
            { etiqueta: 'Hora', tipo: 'time' },
            { etiqueta: 'Seña', tipo: 'checkbox' },
            { etiqueta: 'Servicio', tipo: 'select', opciones: ['Corte', 'Color'] },
            { etiqueta: 'Precio', tipo: 'currency' },
          ],
          ejemplos: [
            {
              Nombre: 'Sofía',
              Hora: '9:05',
              Seña: 'Sí',
              Servicio: 'color',
              Fecha: 'mañana',
              Precio: 'gratis',
            },
            ...Array.from({ length: 6 }, (_, indice) => ({ Nombre: `Turno ${indice}` })),
          ],
        }),
      ]);

      const turnos = llamada('Turnos');
      expect(turnos.rows).toHaveLength(5);
      expect(sinId(turnos.rows)[0]).toEqual({
        __example: true,
        name: 'Sofía',
        hora: '09:05',
        sena: true,
        servicio: 'Color',
      });
    });

    it('sin ejemplos usa las filas del catálogo', () => {
      aplicarAcciones(sistema, [accion('agregar_pieza', { tipo: 'expenses', nombre: 'Insumos' })]);

      expect(llamada('Insumos').rows.length).toBeGreaterThan(0);
      expect(llamada('Insumos').rows.every((fila) => fila.__example)).toBe(true);
    });

    it('la tabla a medida se puede repetir con otro nombre; las demás piezas y los nombres, no', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('agregar_pieza', { tipo: 'form', nombre: 'Turnos' }),
        accion('agregar_pieza', { tipo: 'form', nombre: 'Encargos', seccion: 'Agenda' }),
        accion('agregar_pieza', { tipo: 'form', nombre: 'turnos' }),
        accion('agregar_pieza', { tipo: 'clients', nombre: 'Encargos' }),
        accion('agregar_pieza', { tipo: 'clients' }),
        accion('agregar_pieza', { tipo: 'clients', nombre: 'Otra' }),
      ]);

      expect(resultado.aplicadas).toEqual([
        'Sumé turnos.',
        'Sumé encargos en la sección Agenda.',
        'Sumé clientes.',
      ]);
      expect(resultado.omitidas).toEqual([
        'Ya hay una pieza «turnos».',
        'Ya hay una pieza «Encargos».',
        'Esa pieza ya está.',
      ]);
      expect(sistema.sections.map((seccion) => seccion.name)).toEqual(['Vista general', 'Agenda']);
      expect(sistema.sections[1]?.blocks.map((bloque) => bloque.name)).toEqual(['Encargos']);
    });
  });

  describe('cargar_ejemplos', () => {
    it('descarta las filas sin el dato principal y, si no queda ninguna, conserva los ejemplos que había', () => {
      aplicarAcciones(sistema, [
        accion('agregar_pieza', {
          tipo: 'inventory',
          nombre: 'Velas',
          ejemplos: [{ Producto: 'Vela lavanda', Cantidad: 8 }, { Cantidad: 3 }],
        }),
      ]);
      expect(llamada('Velas').rows.map((fila) => fila.name)).toEqual(['Vela lavanda']);

      const resultado = aplicarAcciones(sistema, [
        accion('cargar_ejemplos', { pieza: 'Velas', filas: [{ Cantidad: 5 }, { Precio: 900 }] }),
      ]);

      expect(resultado.omitidas).toEqual(['No pude cargar esos ejemplos en velas.']);
      expect(llamada('Velas').rows.map((fila) => fila.name)).toEqual(['Vela lavanda']);
    });

    it('reemplaza las filas de ejemplo de la pieza y deja las reales', () => {
      aplicarAcciones(sistema, [accion('agregar_pieza', { tipo: 'clients' })]);
      const clientes = llamada('Clientes');
      const real = upsertRow(clientes, { name: 'Rosa' }, null, sistema).row;

      const resultado = aplicarAcciones(sistema, [
        accion('cargar_ejemplos', {
          pieza: 'clientes',
          filas: Array.from({ length: 7 }, (_, indice) => ({ Nombre: `Cliente ${indice}` })),
        }),
      ]);

      expect(resultado.aplicadas).toEqual(['Cargué 5 ejemplos en clientes.']);
      expect(clientes.rows[0]).toBe(real);
      expect(clientes.rows.slice(1).map((fila) => fila.name)).toEqual([
        'Cliente 0',
        'Cliente 1',
        'Cliente 2',
        'Cliente 3',
        'Cliente 4',
      ]);
    });

    it('omite una pieza que no existe, filas vacías o una pieza que no lleva filas', () => {
      aplicarAcciones(sistema, [
        accion('agregar_pieza', { tipo: 'clients' }),
        accion('agregar_pieza', { tipo: 'note' }),
      ]);

      const resultado = aplicarAcciones(sistema, [
        accion('cargar_ejemplos', { pieza: 'nada', filas: [{ Nombre: 'X' }] }),
        accion('cargar_ejemplos', { pieza: 'clients', filas: [] }),
        accion('cargar_ejemplos', { pieza: 'clients', filas: [{ Color: 'Rojo' }] }),
        accion('cargar_ejemplos', { pieza: 'note', filas: [{ Nombre: 'X' }] }),
      ]);

      expect(resultado.aplicadas).toEqual([]);
      expect(resultado.omitidas).toEqual([
        'No encontré esa pieza.',
        'No pude cargar esos ejemplos en clientes.',
        'No pude cargar esos ejemplos en clientes.',
        '«Nota» no lleva filas.',
      ]);
    });
  });

  describe('cargar_registros', () => {
    it('entiende las fechas como se anotan en un cuaderno y descarta las que no existen', () => {
      aplicarAcciones(sistema, [accion('agregar_pieza', { tipo: 'expenses', ejemplos: [] })]);
      const anio = new Date().getFullYear();

      aplicarAcciones(sistema, [
        accion('cargar_registros', {
          pieza: 'expenses',
          filas: [
            { Concepto: 'Luz', Fecha: '02/10' },
            { Concepto: 'Gas', Fecha: '5/3/25' },
            { Concepto: 'Agua', Fecha: '09-10-2024' },
            { Concepto: 'Internet', Fecha: '2026-01-31' },
            { Concepto: 'Alquiler', Fecha: '31/02' },
          ],
        }),
      ]);

      expect(
        llamada('Gastos')
          .rows.filter((fila) => !fila.__example)
          .map((fila) => fila.date),
      ).toEqual([`${anio}-10-02`, '2025-03-05', '2024-10-09', '2026-01-31', undefined]);
    });

    it('suma registros reales a lo que ya había y reemplaza los ejemplos de esa pieza', () => {
      aplicarAcciones(sistema, [
        accion('agregar_pieza', { tipo: 'clients', ejemplos: [{ Nombre: 'Ejemplo' }] }),
      ]);
      const clientes = llamada('Clientes');
      const real = upsertRow(clientes, { name: 'Rosa' }, null, sistema).row;

      const resultado = aplicarAcciones(sistema, [
        accion('cargar_registros', {
          pieza: 'Clientes',
          filas: [
            { Nombre: 'Lucía', Teléfono: '11 5555-0101' },
            { Nombre: 'Marta' },
            { Nombre: 'Pedro' },
          ],
        }),
      ]);

      expect(resultado).toEqual({ aplicadas: ['Cargué 3 registros en clientes.'], omitidas: [] });
      expect(clientes.rows.map((fila) => fila.name)).toEqual(['Rosa', 'Lucía', 'Marta', 'Pedro']);
      expect(clientes.rows[0]).toBe(real);
      const nuevas = clientes.rows.slice(1);
      expect(nuevas.every((fila) => !('__example' in fila))).toBe(true);
      expect(sinId(nuevas)[0]).toEqual({ name: 'Lucía', phone: '11 5555-0101' });
    });

    it('convierte los valores como los ejemplos, descarta las filas sin el dato principal y carga hasta 30', () => {
      aplicarAcciones(sistema, [accion('agregar_pieza', { tipo: 'inventory', ejemplos: [] })]);
      llamada('Inventario').rows = [];

      const resultado = aplicarAcciones(sistema, [
        accion('cargar_registros', {
          pieza: 'inventory',
          filas: [
            { Producto: 'Vela lavanda', Cantidad: '8', Precio: '6500,5', Costo: 'mucho' },
            { Cantidad: 3 },
            'no es una fila',
            ...Array.from({ length: 40 }, (_, indice) => ({ Producto: `Vela ${indice}` })),
          ],
        }),
      ]);

      expect(resultado.aplicadas).toEqual(['Cargué 30 registros en inventario.']);
      const filas = llamada('Inventario').rows;
      expect(filas).toHaveLength(30);
      expect(sinId(filas)[0]).toEqual({ name: 'Vela lavanda', quantity: 8, price: 6500.5 });
      expect(filas.at(-1)?.name).toBe('Vela 28');
    });

    it('omite una pieza que no existe, una que no lleva filas o filas sin el dato principal', () => {
      aplicarAcciones(sistema, [
        accion('agregar_pieza', { tipo: 'clients' }),
        accion('agregar_pieza', { tipo: 'note' }),
      ]);
      const antes = clone(sistema);

      const resultado = aplicarAcciones(sistema, [
        accion('cargar_registros', { pieza: 'nada', filas: [{ Nombre: 'X' }] }),
        accion('cargar_registros', { pieza: 'clients', filas: [] }),
        accion('cargar_registros', { pieza: 'clients', filas: [{ Teléfono: '11 5555-0000' }] }),
        accion('cargar_registros', { pieza: 'note', filas: [{ Nombre: 'X' }] }),
      ]);

      expect(resultado.aplicadas).toEqual([]);
      expect(resultado.omitidas).toEqual([
        'No encontré esa pieza.',
        'No pude cargar esos registros en clientes.',
        'No pude cargar esos registros en clientes.',
        '«Nota» no lleva filas.',
      ]);
      expect(mismoSistema(sistema, antes)).toBe(true);
    });

    it('los vínculos por nombre usan un registro real que ya está o crean uno real en la otra pieza', () => {
      aplicarAcciones(sistema, [
        accion('agregar_pieza', {
          tipo: 'inventory',
          nombre: 'Velas',
          ejemplos: [{ Producto: 'Vela lavanda', Cantidad: 5 }],
        }),
        accion('agregar_pieza', { tipo: 'clients', nombre: 'Clientas', ejemplos: [] }),
      ]);
      llamada('Clientas').rows = [];
      const rosa = upsertRow(llamada('Clientas'), { name: 'Rosa' }, null, sistema).row;

      const resultado = aplicarAcciones(sistema, [
        accion('agregar_pieza', { tipo: 'sales', ejemplos: [] }),
        accion('cargar_registros', {
          pieza: 'Ventas',
          filas: [
            { Producto: 'Vela lavanda', Cliente: 'rosa', Cantidad: 2 },
            { Producto: 'Vela vainilla', Cliente: 'Lucía', Cantidad: 1 },
            { Producto: 'vela vainilla', Cliente: 'Lucía', Cantidad: 3 },
          ],
        }),
      ]);

      expect(resultado.aplicadas).toEqual(['Sumé ventas.', 'Cargué 3 registros en ventas.']);
      const velas = llamada('Velas').rows;
      // El ejemplo «Vela lavanda» no se guarda al crear el sistema: el registro real tiene su propia vela, real.
      expect(velas.map((fila) => [fila.name, Boolean(fila.__example)])).toEqual([
        ['Vela lavanda', true],
        ['Vela lavanda', false],
        ['Vela vainilla', false],
      ]);
      const clientas = llamada('Clientas').rows;
      expect(clientas.map((fila) => [fila.name, Boolean(fila.__example)])).toEqual([
        ['Rosa', false],
        ['Lucía', false],
      ]);
      expect(llamada('Ventas').rows.map((fila) => [fila.product, fila.customer])).toEqual([
        [velas[1]?.id, rosa?.id],
        [velas[2]?.id, clientas[1]?.id],
        [velas[2]?.id, clientas[1]?.id],
      ]);
    });
  });

  describe('conectar', () => {
    it('suma un vínculo con el nombre de la otra pieza y no lo duplica', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('agregar_pieza', { tipo: 'inventory', nombre: 'Velas' }),
        accion('agregar_pieza', { tipo: 'orders' }),
        accion('conectar', { desde: 'orders', hacia: 'Velas' }),
        accion('conectar', { desde: 'Pedidos', hacia: 'inventory', etiqueta: 'Otra vela' }),
        accion('conectar', { desde: 'Pedidos', hacia: 'Pedidos' }),
        accion('conectar', { desde: 'Pedidos', hacia: 'nada' }),
      ]);

      expect(resultado.aplicadas).toContain('Conecté pedidos con velas.');
      expect(resultado.omitidas).toEqual([
        'Pedidos ya está conectada con velas.',
        'Una pieza no se conecta consigo misma.',
        'No encontré las piezas para conectar.',
      ]);
      const vinculos = llamada('Pedidos').fields.filter((campo) => campo.type === 'relation');
      expect(vinculos).toEqual([
        {
          key: 'velas',
          label: 'Velas',
          type: 'relation',
          required: false,
          target: llamada('Velas').id,
        },
      ]);
    });

    it('un vínculo que la pieza ya trae de fábrica no se duplica', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('agregar_pieza', { tipo: 'clients' }),
        accion('agregar_pieza', { tipo: 'orders' }),
        accion('conectar', { desde: 'orders', hacia: 'clients' }),
      ]);

      expect(resultado.omitidas).toEqual(['Pedidos ya está conectada con clientes.']);
      expect(llamada('Pedidos').fields.filter((campo) => campo.type === 'relation')).toHaveLength(
        1,
      );
    });

    it('un dato escrito con la etiqueta del vínculo pasa a ser el vínculo; el dato principal, no', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('agregar_pieza', { tipo: 'clients' }),
        accion('agregar_pieza', {
          tipo: 'form',
          nombre: 'Turnos',
          campos: [{ etiqueta: 'Cliente', tipo: 'text' }],
        }),
        accion('conectar', { desde: 'Turnos', hacia: 'Clientes', etiqueta: 'Nombre' }),
        accion('conectar', { desde: 'Turnos', hacia: 'Clientes', etiqueta: 'cliente' }),
      ]);

      expect(resultado.omitidas).toEqual(['«Nombre» ya está en turnos.']);
      expect(llamada('Turnos').fields.filter((campo) => campo.label === 'Cliente')).toEqual([
        {
          key: 'cliente',
          label: 'Cliente',
          type: 'relation',
          required: false,
          target: llamada('Clientes').id,
        },
      ]);
    });
  });

  describe('referencias y vínculos en el mismo turno', () => {
    it('lo que se suma en el turno se referencia por nombre y los ejemplos se vinculan por nombre', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('agregar_pieza', {
          tipo: 'clients',
          nombre: 'Clientas',
          ejemplos: [{ Nombre: 'Carla Gómez' }, { Nombre: 'Juli Sosa' }],
        }),
        accion('agregar_pieza', {
          tipo: 'form',
          nombre: 'Turnos',
          campos: [{ etiqueta: 'Clienta', tipo: 'text' }],
        }),
        accion('conectar', { desde: 'turnos', hacia: 'CLIENTAS', etiqueta: 'Clienta' }),
        accion('cargar_ejemplos', {
          pieza: 'Turnos',
          filas: [
            { Nombre: 'Corte', Clienta: 'carla gomez' },
            { Nombre: 'Color', Clienta: 'Alguien más' },
          ],
        }),
      ]);

      expect(resultado.omitidas).toEqual([]);
      const carla = llamada('Clientas').rows.find((fila) => fila.name === 'Carla Gómez');
      // «Alguien más» no estaba: se suma a Clientas como ejemplo, así el vínculo se ve.
      const otra = llamada('Clientas').rows.find((fila) => fila.name === 'Alguien más');
      expect(otra).toMatchObject({ __example: true });
      expect(llamada('Turnos').rows.map((fila) => fila.clienta)).toEqual([carla?.id, otra?.id]);
    });

    it('si la pieza vinculada está vacía, el ejemplo suma ahí lo que nombra; un id que ya no existe queda vacío', () => {
      aplicarAcciones(sistema, [
        accion('agregar_pieza', { tipo: 'inventory', nombre: 'Velas', ejemplos: [] }),
        accion('agregar_pieza', { tipo: 'clients', nombre: 'Clientas', ejemplos: [] }),
        accion('agregar_pieza', { tipo: 'sales' }),
      ]);
      llamada('Velas').rows = [];
      llamada('Clientas').rows = [];

      aplicarAcciones(sistema, [
        accion('cargar_ejemplos', {
          pieza: 'sales',
          filas: [
            { Producto: 'Vela lavanda', Cliente: 'Lucía', Cantidad: 2 },
            { Producto: 'e1234abcd-0000-4000-8000-000000000000', Cantidad: 1 },
          ],
        }),
      ]);

      const [lavanda] = llamada('Velas').rows;
      const [lucia] = llamada('Clientas').rows;
      expect(lavanda).toMatchObject({ name: 'Vela lavanda', __example: true });
      expect(lucia).toMatchObject({ name: 'Lucía', __example: true });
      expect(llamada('Ventas').rows.map((fila) => [fila.product, fila.customer ?? ''])).toEqual([
        [lavanda?.id, lucia?.id],
        ['', ''],
      ]);
    });

    it('los vínculos del catálogo (producto y cliente de una venta) también se resuelven por nombre', () => {
      aplicarAcciones(sistema, [
        accion('agregar_pieza', {
          tipo: 'inventory',
          ejemplos: [{ Producto: 'Yerba 1 kg', Cantidad: 10 }],
        }),
        accion('agregar_pieza', {
          tipo: 'sales',
          ejemplos: [{ Producto: 'Yerba 1 kg', Cantidad: 2, Cliente: 'Martín', Fiado: true }],
        }),
        accion('agregar_pieza', { tipo: 'clients', ejemplos: [{ Nombre: 'Martín' }] }),
      ]);

      const [venta] = llamada('Ventas').rows;
      expect(venta?.product).toBe(llamada('Inventario').rows[0]?.id);
      expect(venta?.customer).toBe(llamada('Clientes').rows[0]?.id);
      expect(venta?.credit).toBe(true);
    });

    it('si cambian los ejemplos de la pieza vinculada, el vínculo que quedó sin registro se vacía', () => {
      aplicarAcciones(sistema, [
        accion('agregar_pieza', { tipo: 'inventory', ejemplos: [{ Producto: 'Yerba' }] }),
        accion('agregar_pieza', { tipo: 'sales', ejemplos: [{ Producto: 'Yerba', Cantidad: 1 }] }),
      ]);
      expect(llamada('Ventas').rows[0]?.product).toBe(llamada('Inventario').rows[0]?.id);

      aplicarAcciones(sistema, [
        accion('cargar_ejemplos', { pieza: 'inventory', filas: [{ Producto: 'Fideos' }] }),
      ]);

      expect(llamada('Ventas').rows[0]?.product).toBe('');
    });
  });

  describe('borrar_registros', () => {
    beforeEach(() => {
      aplicarAcciones(sistema, [
        accion('agregar_pieza', {
          tipo: 'sales',
          ejemplos: [{ Producto: 'Yerba', Cantidad: 2 }],
        }),
        accion('agregar_pieza', { tipo: 'clients', ejemplos: [{ Nombre: 'Lucía' }] }),
      ]);
    });

    const cargarVentas = () => {
      aplicarAcciones(sistema, [
        accion('cargar_registros', {
          pieza: 'sales',
          filas: [
            { Producto: 'Almohaditas', Cantidad: 0.5, Precio: 3200, Cliente: 'Anto Sdani' },
            { Producto: 'Bolitas', Cantidad: 1, Precio: 2900, Cliente: 'Anto Sdani' },
            { Producto: 'Almohaditas', Cantidad: 1, Precio: 3200, Cliente: 'Vero Susy' },
          ],
        }),
      ]);
      return llamada('Ventas');
    };

    it('borra las filas cuyos datos coinciden, sin importar mayúsculas ni tildes, y sólo esas', () => {
      const ventas = cargarVentas();

      const resultado = aplicarAcciones(sistema, [
        accion('borrar_registros', { pieza: 'sales', donde: { cliente: 'anto sdani' } }),
      ]);

      expect(resultado).toEqual({ aplicadas: ['Borré 2 registros de ventas.'], omitidas: [] });
      expect(ventas.rows).toHaveLength(1);
      expect(ventas.rows[0]?.product).toBeDefined();
    });

    it('exige que coincidan todos los datos de un criterio y acepta una lista de criterios', () => {
      const ventas = cargarVentas();

      aplicarAcciones(sistema, [
        accion('borrar_registros', {
          pieza: 'sales',
          donde: { Cliente: 'Anto Sdani', Cantidad: '0,5' },
        }),
      ]);
      expect(ventas.rows).toHaveLength(2);

      const resultado = aplicarAcciones(sistema, [
        accion('borrar_registros', {
          pieza: 'Ventas',
          donde: [{ Cliente: 'Vero Susy' }, { Cliente: 'Anto Sdani' }],
        }),
      ]);
      expect(resultado.aplicadas).toEqual(['Borré 2 registros de ventas.']);
      expect(ventas.rows).toHaveLength(0);
    });

    it('un solo registro se cuenta en singular y los vínculos que apuntaban a él quedan vacíos', () => {
      cargarVentas();
      const clientes = llamada('Clientes');
      const anto = clientes.rows.find((fila) => fila.name === 'Anto Sdani');

      const resultado = aplicarAcciones(sistema, [
        accion('borrar_registros', { pieza: 'clients', donde: { Nombre: 'Anto Sdani' } }),
      ]);

      expect(resultado.aplicadas).toEqual(['Borré un registro de clientes.']);
      expect(clientes.rows.some((fila) => fila.id === anto?.id)).toBe(false);
      expect(
        llamada('Ventas').rows.filter((fila) => fila.client === anto?.id),
      ).toHaveLength(0);
    });

    it('nunca borra de más: sin criterio, con un dato que no existe o sin coincidencias no se borra nada', () => {
      const ventas = cargarVentas();

      const resultado = aplicarAcciones(sistema, [
        accion('borrar_registros', { pieza: 'sales' }),
        accion('borrar_registros', { pieza: 'sales', donde: {} }),
        accion('borrar_registros', { pieza: 'sales', donde: 'todo' }),
        accion('borrar_registros', { pieza: 'sales', donde: { Color: 'Rojo' } }),
        accion('borrar_registros', { pieza: 'sales', donde: { Cliente: 'Anto Sdani', Color: 'Rojo' } }),
        accion('borrar_registros', { pieza: 'sales', donde: { Cantidad: 'mucho' } }),
        accion('borrar_registros', { pieza: 'sales', donde: { Cliente: 'Nadie' } }),
        accion('borrar_registros', { pieza: 'nada', donde: { Cliente: 'Anto Sdani' } }),
      ]);

      expect(resultado.aplicadas).toEqual([]);
      expect(resultado.omitidas).toHaveLength(8);
      expect(ventas.rows).toHaveLength(3);
    });

    it('usar borrar y cargar en el mismo turno corrige sin duplicar', () => {
      const ventas = cargarVentas();

      aplicarAcciones(sistema, [
        accion('borrar_registros', { pieza: 'sales', donde: { Cliente: 'Anto Sdani' } }),
        accion('cargar_registros', {
          pieza: 'sales',
          filas: [{ Producto: 'Almohaditas', Cantidad: 0.5, Precio: 3200, Cliente: 'Anto Solari' }],
        }),
      ]);

      expect(ventas.rows).toHaveLength(2);
      expect(ventas.rows.filter((fila) => fila.__example)).toHaveLength(0);
    });
  });

  describe('los ejemplos no se mezclan con lo real', () => {
    beforeEach(() => {
      aplicarAcciones(sistema, [
        accion('agregar_pieza', {
          tipo: 'sales',
          ejemplos: [{ Producto: 'Yerba', Cantidad: 2 }],
        }),
        accion('agregar_pieza', { tipo: 'clients', ejemplos: [{ Nombre: 'Lucía' }] }),
      ]);
    });

    it('al cargar registros reales en una pieza desaparecen sus ejemplos, y los de otras piezas siguen marcados', () => {
      const ventas = llamada('Ventas');
      const clientes = llamada('Clientes');
      expect(ventas.rows.every((fila) => fila.__example)).toBe(true);
      expect(clientes.rows.every((fila) => fila.__example)).toBe(true);

      aplicarAcciones(sistema, [
        accion('cargar_registros', { pieza: 'sales', filas: [{ Producto: 'Yerba', Cantidad: 1 }] }),
      ]);

      expect(ventas.rows.some((fila) => fila.__example)).toBe(false);
      expect(ventas.rows).toHaveLength(1);
      expect(clientes.rows.every((fila) => fila.__example)).toBe(true);
    });

    it('lo que se guarda al crear nunca incluye una fila de ejemplo', () => {
      aplicarAcciones(sistema, [
        accion('cargar_registros', { pieza: 'sales', filas: [{ Producto: 'Yerba', Cantidad: 1 }] }),
      ]);

      const creado = sistemaParaCrear(sistema);

      expect(allBlocks(creado).flatMap((bloque) => bloque.rows).some((fila) => fila.__example)).toBe(false);
      expect(allBlocks(creado).find((bloque) => bloque.type === 'sales')?.rows).toHaveLength(1);
    });
  });
});

describe('ajusteParaGuardar: el sistema ajustado con Sami', () => {
  const deTipo = (sistema: Sistema, tipo: string) => {
    const bloque = allBlocks(sistema).find((item) => item.type === tipo);
    if (!bloque) throw new Error(`Falta la pieza ${tipo}`);
    return bloque;
  };

  it('no guarda los ejemplos que se sumaron en el taller; conserva las filas que ya tenía', () => {
    const original = makeTemplate('shop', {}, true);
    const real: Fila = { id: 'real', name: 'Rosa', phone: '11 4444-0000' };
    deTipo(original, 'clients').rows.push(real);
    deTipo(original, 'sales').rows.push({ id: 'venta', __example: true, quantity: 1 });
    const borrador = clone(original);
    borrador.name = '  ';
    borrador.id = 'otro';
    aplicarAcciones(borrador, [
      accion('agregar_pieza', { tipo: 'expenses' }),
      accion('editar_campo', { pieza: 'clients', campo: 'Teléfono', etiqueta: 'Celular' }),
    ]);
    deTipo(borrador, 'clients').rows.push({ id: 'nueva', __example: true, name: 'Nueva' });
    const venta = deTipo(borrador, 'sales').rows[0] as Fila;
    venta.customer = 'nueva';

    const ajustado = ajusteParaGuardar(original, borrador);

    expect(ajustado).toMatchObject({
      id: original.id,
      createdAt: original.createdAt,
      name: original.name,
    });
    expect(deTipo(ajustado, 'expenses').rows).toEqual([]);
    expect(deTipo(ajustado, 'clients').rows.map((fila) => fila.id)).toEqual(
      deTipo(original, 'clients').rows.map((fila) => fila.id),
    );
    expect(deTipo(ajustado, 'clients').fields[1]?.label).toBe('Celular');
    expect(deTipo(ajustado, 'sales').rows[0]?.customer).toBe('');
    expect(deTipo(borrador, 'expenses').rows.length).toBeGreaterThan(0);
  });

  it('sin cambios queda igual al original', () => {
    const original = makeTemplate('shop', {}, true);

    expect(mismoSistema(ajusteParaGuardar(original, clone(original)), original)).toBe(true);
  });

  it('guarda los registros reales que Sami cargó de una foto, con sus vínculos', () => {
    const original = makeTemplate('shop', {}, false);
    const borrador = clone(original);
    aplicarAcciones(borrador, [
      accion('cargar_registros', {
        pieza: 'sales',
        filas: [{ Producto: 'Vela lavanda', Cliente: 'Lucía', Cantidad: 2, Precio: 13000 }],
      }),
    ]);

    const ajustado = ajusteParaGuardar(original, borrador);

    const [venta] = deTipo(ajustado, 'sales').rows;
    const [vela] = deTipo(ajustado, 'inventory').rows;
    const [lucia] = deTipo(ajustado, 'clients').rows;
    expect(vela).toMatchObject({ name: 'Vela lavanda' });
    expect(lucia).toMatchObject({ name: 'Lucía' });
    expect(venta).toMatchObject({
      product: vela?.id,
      customer: lucia?.id,
      quantity: 2,
      price: 13000,
    });
    expect(
      allBlocks(ajustado)
        .flatMap((bloque) => bloque.rows)
        .some((fila) => fila.__example),
    ).toBe(false);
  });
});

describe('sistemaParaCrear: el sistema que sale del taller', () => {
  const deTipo = (sistema: Sistema, tipo: string) => {
    const bloque = allBlocks(sistema).find((item) => item.type === tipo);
    if (!bloque) throw new Error(`Falta la pieza ${tipo}`);
    return bloque;
  };

  it('descarta los ejemplos, conserva los registros reales y vacía los vínculos a un ejemplo', () => {
    const borrador = makeTemplate('shop', {}, true);
    borrador.name = '  ';
    const ejemploDeCliente = deTipo(borrador, 'clients').rows[0] as Fila;
    aplicarAcciones(borrador, [
      accion('cargar_registros', {
        pieza: 'sales',
        filas: [
          { Producto: 'Vela lavanda', Cliente: 'Lucía', Cantidad: 2 },
          { Producto: 'Vela vainilla', Cantidad: 1 },
        ],
      }),
    ]);
    const [, real] = deTipo(borrador, 'sales').rows.filter((fila) => !fila.__example);
    if (!real) throw new Error('Falta el registro real');
    real.customer = ejemploDeCliente.id;

    const sistema = sistemaParaCrear(borrador);

    expect(sistema.name).toBe('Mi sistema');
    expect(sistema.id).toBe(borrador.id);
    const filas = allBlocks(sistema).flatMap((bloque) => bloque.rows);
    expect(filas.some((fila) => fila.__example)).toBe(false);
    expect(deTipo(sistema, 'inventory').rows.map((fila) => fila.name)).toEqual([
      'Vela lavanda',
      'Vela vainilla',
    ]);
    expect(deTipo(sistema, 'clients').rows.map((fila) => fila.name)).toEqual(['Lucía']);
    const ventas = deTipo(sistema, 'sales').rows;
    expect(ventas.map((fila) => fila.customer)).toEqual([
      deTipo(sistema, 'clients').rows[0]?.id,
      '',
    ]);
    // La propuesta queda como estaba.
    expect(deTipo(borrador, 'clients').rows[0]).toBe(ejemploDeCliente);
  });

  it('sin registros reales, el sistema arranca vacío', () => {
    const sistema = sistemaParaCrear(makeTemplate('shop', {}, true));

    expect(allBlocks(sistema).every((bloque) => bloque.rows.length === 0)).toBe(true);
  });
});
