import { beforeEach, describe, expect, it } from 'vitest';
import {
  aplicarAcciones,
  entiendeElNegocio,
  esOrdenSimple,
  mismoSistema,
  resumirSistema,
  type AccionDeSami,
} from '@/lib/sistema/acciones';
import { allBlocks, clone, makeTemplate } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';

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

    it('no suma una pieza repetida, inventada ni de un tipo que no es del catálogo', () => {
      const resultado = aplicarAcciones(sistema, [
        accion('agregar_pieza', { tipo: 'inventory' }),
        accion('agregar_pieza', { tipo: 'turnos' }),
        accion('agregar_pieza', { tipo: 'note' }),
      ]);

      expect(resultado.aplicadas).toEqual([]);
      expect(resultado.omitidas).toHaveLength(3);
      expect(allBlocks(sistema)).toHaveLength(3);
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

  it('una herramienta desconocida se omite sin romper el resto', () => {
    const resultado = aplicarAcciones(sistema, [
      accion('borrar_todo', {}),
      accion('agregar_pieza', { tipo: 'expenses' }),
    ]);

    expect(resultado.omitidas).toEqual(['No sé hacer «borrar_todo».']);
    expect(resultado.aplicadas).toEqual(['Sumé gastos.']);
  });

  describe('entiendeElNegocio', () => {
    it('reconoce descripciones de un negocio, con o sin tildes', () => {
      expect(entiendeElNegocio('Tengo un almacén y vendo productos')).toBe(true);
      expect(entiendeElNegocio('TENGO UN KIOSCO')).toBe(true);
      expect(entiendeElNegocio('anoto mis pedidos')).toBe(true);
    });

    it('no reconoce lo que no dice nada del negocio', () => {
      expect(entiendeElNegocio('no sé por dónde empezar')).toBe(false);
      expect(entiendeElNegocio('quiero ordenar todo')).toBe(false);
    });
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

describe('esOrdenSimple', () => {
  it('reconoce un verbo y una pieza, con tildes, artículos y signos', () => {
    expect(esOrdenSimple('sumá gastos')).toBe(true);
    expect(esOrdenSimple('Sacá los clientes!')).toBe(true);
    expect(esOrdenSimple('  poné el stock  ')).toBe(true);
    expect(esOrdenSimple('separá las ventas.')).toBe(true);
  });

  it('no reconoce lo que podría confundir un dato con una pieza ni lo que es una charla', () => {
    expect(esOrdenSimple('agregá un campo de proveedor')).toBe(false);
    expect(esOrdenSimple('agregá proveedores')).toBe(false);
    expect(esOrdenSimple('quiero dos columnas')).toBe(false);
    expect(esOrdenSimple('¿por qué me pusiste clientes?')).toBe(false);
    expect(esOrdenSimple('sumá gastos y clientes')).toBe(false);
  });
});
