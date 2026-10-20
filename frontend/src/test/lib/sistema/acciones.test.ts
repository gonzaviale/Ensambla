import { beforeEach, describe, expect, it } from 'vitest';
import {
  aplicarAcciones,
  mismoSistema,
  resumirSistema,
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
              Fecha: '20/10',
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
      expect(llamada('Turnos').rows.map((fila) => fila.clienta)).toEqual([carla?.id, '']);
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
});
