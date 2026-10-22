import { describe, expect, it } from 'vitest';
import { analizar, parecido, responder, type Aviso, type TipoDeAviso } from '@/lib/sistema/alertas';
import { allBlocks, makeSystem } from '@/lib/sistema/core';
import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import type { Sistema } from '@/lib/sistema/tipos';
import { MINIMO_DE_COMERCIOS, sinDatosInsuficientes, type Zona } from '@/lib/sistema/zona';
import { zonaDeLaPersona, zonaParaTexto } from '@/mocks/db/zona';

const HOY = '2026-10-16';
const zona = zonaDeLaPersona();

const tipos = (avisos: Aviso[]) => avisos.map((aviso) => aviso.tipo);
const de = (avisos: Aviso[], tipo: TipoDeAviso) => avisos.filter((aviso) => aviso.tipo === tipo);

// Un sistema de un solo producto, para llevar cada regla a su umbral.
function conProducto(
  valores: Record<string, string | number>,
  ventas: [dias: number, cantidad: number][] = [],
) {
  const sistema = makeSystem('Prueba', ['inventory', 'sales'], false);
  const [inventario, bloqueDeVentas] = allBlocks(sistema);
  if (!inventario || !bloqueDeVentas) throw new Error('Faltan bloques');
  inventario.rows = [
    {
      id: 'p1',
      name: 'Yerba Playadito 1 kg',
      quantity: 50,
      price: 3900,
      cost: 2700,
      minimum: 10,
      ...valores,
    },
  ];
  bloqueDeVentas.rows = ventas.map(([dias, cantidad], indice) => {
    const fecha = new Date(`${HOY}T12:00:00`);
    fecha.setDate(fecha.getDate() - dias);
    return {
      id: `v${indice}`,
      product: 'p1',
      quantity: cantidad,
      price: 3900,
      date: fecha.toISOString().slice(0, 10),
      customer: '',
      credit: false,
    };
  });
  return sistema;
}

const avisosDe = (sistema: Sistema, otraZona: Zona = zona, grupos: Record<string, boolean> = {}) =>
  analizar({ sistemas: [sistema], zona: otraZona, hoy: HOY, grupos });

describe('el almacén de ejemplo', () => {
  const sistema = crearAlmacenDeEjemplo(HOY);
  const avisos = analizar({ sistemas: [sistema], zona, hoy: HOY });

  it('dispara las seis reglas', () => {
    expect(new Set(tipos(avisos))).toEqual(
      new Set(['stock', 'precio_zona', 'estancado', 'muy_vendido', 'combo', 'consumo_zona']),
    );
  });

  it('cada aviso explica por qué y trae acciones', () => {
    for (const aviso of avisos) {
      expect(aviso.porque.length, aviso.titulo).toBeGreaterThan(0);
      expect(
        aviso.porque.every((motivo) => motivo.trim().length > 0),
        aviso.titulo,
      ).toBe(true);
      expect(aviso.acciones.length, aviso.titulo).toBeGreaterThan(0);
      expect(aviso.sistemaId).toBe(sistema.id);
    }
  });

  it('ordena por urgencia: primero lo agotado', () => {
    expect(avisos[0]?.titulo).toBe('Te quedaste sin Aceite Natura 900 ml');
    expect(avisos[0]?.prioridad).toBe(3);
    const prioridades = avisos.map((aviso) => aviso.prioridad);
    expect(prioridades).toEqual([...prioridades].sort((a, b) => b - a));
  });

  it('los ids no se repiten y son iguales si los datos no cambian', () => {
    const otra = analizar({ sistemas: [sistema], zona, hoy: HOY });
    expect(new Set(avisos.map((aviso) => aviso.id)).size).toBe(avisos.length);
    expect(otra.map((aviso) => aviso.id)).toEqual(avisos.map((aviso) => aviso.id));
  });

  it('agrupa en «Tu stock», «Precios y ventas» y «Tu zona»', () => {
    const grupos = new Set(avisos.map((aviso) => aviso.grupo));
    expect(grupos).toEqual(new Set(['stock', 'ventas', 'zona']));
    expect(avisosDe(sistema, zona, { stock: false }).some((aviso) => aviso.grupo === 'stock')).toBe(
      false,
    );
    expect(
      avisosDe(sistema, zona, { zona: false }).some((aviso) => aviso.tipo === 'consumo_zona'),
    ).toBe(false);
  });

  it('sólo ofrece sumar lo que se vende cerca, sin nombrar comercios', () => {
    const nuevos = de(avisos, 'consumo_zona');
    expect(nuevos.map((aviso) => aviso.titulo)).toEqual([
      'En tu zona se vende Leche La Serenísima 1 L y no lo tenés',
      'En tu zona se vende Agua Villavicencio 2 L y no lo tenés',
    ]);
    const texto = JSON.stringify(avisos);
    expect(texto).not.toMatch(/Fernet/);
  });
});

describe('stock', () => {
  it('avisa por debajo del mínimo y cuando se agota', () => {
    const bajo = avisosDe(conProducto({ quantity: 8 }));
    expect(de(bajo, 'stock')[0]?.prioridad).toBe(2);
    const agotado = avisosDe(conProducto({ quantity: 0 }));
    expect(de(agotado, 'stock')[0]?.prioridad).toBe(3);
    expect(de(avisosDe(conProducto({ quantity: 11 })), 'stock')).toHaveLength(0);
  });

  it('con ventas, calcula para cuántos días alcanza y cuánto pedir', () => {
    const [aviso] = de(
      avisosDe(
        conProducto({ quantity: 6 }, [
          [1, 30],
          [10, 30],
        ]),
      ),
      'stock',
    );
    expect(aviso?.porque[0]).toContain('vendiste 60 unidades');
    expect(aviso?.porque[0]).toContain('te alcanza para 3 días');
    expect(aviso?.porque[1]).toContain('pedí 22');
  });

  it('sugiere registrar una entrada si hay bloque de entradas', () => {
    const sistema = makeSystem('Con entradas', ['inventory', 'entries'], false);
    const inventario = allBlocks(sistema)[0];
    if (inventario) inventario.rows = [{ id: 'p1', name: 'Fideos', quantity: 0, minimum: 5 }];
    const [aviso] = de(avisosDe(sistema), 'stock');
    expect(aviso?.acciones[0]?.tipo).toBe('registrar');
  });
});

describe('precio frente a la zona', () => {
  it('más barato: puede perder margen y propone subir sin pasar el máximo de la zona', () => {
    const [aviso] = de(avisosDe(conProducto({ price: 3500 })), 'precio_zona');
    expect(aviso?.titulo).toBe('Yerba Playadito 1 kg está más barato que en tu zona');
    const accion = aviso?.acciones[0];
    expect(accion?.tipo === 'aplicar' && accion.valores.price).toBe(3780);
    expect(aviso?.porque[1]).toContain('ganarías');
  });

  it('casi al costo sube la prioridad', () => {
    const [aviso] = de(avisosDe(conProducto({ price: 3000, cost: 2900 })), 'precio_zona');
    expect(aviso?.prioridad).toBe(2);
    expect(aviso?.titulo).toContain('casi al costo');
  });

  it('más caro: bajarlo ayuda a vender más', () => {
    const [aviso] = de(avisosDe(conProducto({ price: 4600 }, [[3, 10]])), 'precio_zona');
    expect(aviso?.titulo).toContain('más caro que en tu zona');
    expect(aviso?.detalle).toContain('vender más');
    const accion = aviso?.acciones[0];
    expect(accion?.tipo === 'aplicar' && accion.valores.price).toBe(4020);
  });

  it('en el umbral no avisa', () => {
    // 3900 es el promedio: ni 8 % más barato (3588) ni 12 % más caro (4368) cambian nada dentro del margen.
    expect(de(avisosDe(conProducto({ price: 3600 })), 'precio_zona')).toHaveLength(0);
    expect(de(avisosDe(conProducto({ price: 4300 })), 'precio_zona')).toHaveLength(0);
    expect(de(avisosDe(conProducto({ price: 3587 })), 'precio_zona')).toHaveLength(1);
    expect(de(avisosDe(conProducto({ price: 4369 })), 'precio_zona')).toHaveLength(1);
  });

  it('no compara con una zona sin datos suficientes', () => {
    expect(
      avisosDe(conProducto({ price: 3000 }), { nombre: 'Chica', comercios: 2, productos: [] }),
    ).toEqual([]);
  });
});

describe('estancado', () => {
  it('caro y sin ventas en 30 días propone la rebaja al promedio', () => {
    const sistema = conProducto({ price: 4400 }, [[45, 3]]);
    const [aviso] = de(avisosDe(sistema), 'estancado');
    expect(aviso?.prioridad).toBe(2);
    expect(aviso?.porque.join(' ')).toContain('La última venta que anotaste fue el');
    const accion = aviso?.acciones[0];
    expect(accion?.tipo === 'aplicar' && accion.valores.price).toBe(3900);
    expect(de(avisosDe(sistema), 'precio_zona')).toHaveLength(0);
  });

  it('con una venta reciente ya no está estancado', () => {
    expect(
      de(
        avisosDe(
          conProducto({ price: 4400 }, [
            [45, 3],
            [5, 1],
          ]),
        ),
        'estancado',
      ),
    ).toHaveLength(0);
  });

  it('sin historial de ventas no se afirma que no se mueve', () => {
    expect(de(avisosDe(conProducto({ price: 4400 })), 'estancado')).toHaveLength(0);
  });
});

describe('muy vendido', () => {
  it('mucha rotación y precio bajo propone un aumento y cuenta lo que se gana', () => {
    const [aviso] = de(avisosDe(conProducto({ price: 3600 }, [[2, 20]])), 'muy_vendido');
    const accion = aviso?.acciones[0];
    expect(accion?.tipo === 'aplicar' && accion.valores.price).toBe(3820);
    expect(aviso?.porque[1]).toContain('$ 4.400 más por mes');
    expect(de(avisosDe(conProducto({ price: 3600 }, [[2, 20]])), 'precio_zona')).toHaveLength(0);
  });

  it('poca rotación no alcanza', () => {
    expect(de(avisosDe(conProducto({ price: 3600 }, [[2, 14]])), 'muy_vendido')).toHaveLength(0);
  });
});

describe('combos', () => {
  function conCanastas(veces: number) {
    const sistema = makeSystem('Canastas', ['inventory', 'sales', 'clients'], false);
    const [inventario, ventas, clientes] = allBlocks(sistema);
    if (!inventario || !ventas || !clientes) throw new Error('Faltan bloques');
    inventario.rows = [
      { id: 'a', name: 'Alfajor Jorgito', quantity: 30, price: 800, cost: 500 },
      { id: 'b', name: 'Galletitas Criollitas 3 U', quantity: 30, price: 1250, cost: 880 },
    ];
    clientes.rows = [{ id: 'c', name: 'Rosa' }];
    ventas.rows = Array.from({ length: veces }, (_, indice) => indice).flatMap((indice) => {
      const fecha = `2026-10-${String(10 - indice).padStart(2, '0')}`;
      return ['a', 'b'].map((producto) => ({
        id: `v${indice}${producto}`,
        product: producto,
        quantity: 1,
        price: 800,
        date: fecha,
        customer: 'c',
        credit: false,
      }));
    });
    return sistema;
  }

  it('dos productos de poca salida vendidos juntos tres veces sugieren un combo con 10 % menos', () => {
    const [aviso] = de(avisosDe(conCanastas(3)), 'combo');
    expect(aviso?.titulo).toBe('Alfajor Jorgito y Galletitas Criollitas 3 U se venden juntos');
    const accion = aviso?.acciones[0];
    expect(accion?.tipo === 'crear' && accion.valores).toMatchObject({
      name: 'Combo Alfajor Jorgito + Galletitas Criollitas 3 U',
      price: 1850,
      cost: 1380,
    });
  });

  it('con dos veces todavía no', () => {
    expect(de(avisosDe(conCanastas(2)), 'combo')).toHaveLength(0);
  });
});

describe('lo que se vende en la zona', () => {
  it('sin inventario no hay nada que sugerir', () => {
    expect(analizar({ sistemas: [makeSystem('Vacío', ['note'])], zona, hoy: HOY })).toEqual([]);
  });

  it('un producto que ya tenés (aunque se llame distinto) no se sugiere', () => {
    const sistema = conProducto({ name: 'Leche La Serenísima entera' });
    expect(de(avisosDe(sistema), 'consumo_zona').map((aviso) => aviso.titulo)).not.toContain(
      'En tu zona se vende Leche La Serenísima 1 L y no lo tenés',
    );
  });

  it('un producto de pocos comercios no existe para el motor', () => {
    expect(zona.productos.some((producto) => producto.nombre.startsWith('Fernet'))).toBe(false);
  });
});

describe('zona anónima', () => {
  it('respeta el mínimo de comercios', () => {
    expect(MINIMO_DE_COMERCIOS).toBe(3);
    for (const producto of zona.productos) {
      expect(producto.comercios).toBeGreaterThanOrEqual(MINIMO_DE_COMERCIOS);
      expect(producto.ventas.comercios).toBeGreaterThanOrEqual(MINIMO_DE_COMERCIOS);
    }
  });

  it('un análisis con pocos comercios no devuelve ningún dato', () => {
    const analisis = Array.from({ length: 100 }, (_, indice) => zonaParaTexto(`Barrio ${indice}`));
    const poca = analisis.find((otra) => otra.comercios < MINIMO_DE_COMERCIOS);
    expect(poca?.productos).toEqual([]);
    const suficiente = analisis.find((otra) => otra.comercios >= MINIMO_DE_COMERCIOS);
    expect(suficiente?.productos.length).toBeGreaterThan(0);
    expect(
      sinDatosInsuficientes({ nombre: 'Chica', comercios: 2, productos: zona.productos }).productos,
    ).toEqual([]);
  });

  it('el mismo texto da el mismo análisis', () => {
    expect(zonaParaTexto('Palermo')).toEqual(zonaParaTexto('Palermo'));
  });
});

describe('preguntas a Sami', () => {
  const avisos = analizar({ sistemas: [crearAlmacenDeEjemplo(HOY)], zona, hoy: HOY });

  it('qué reponer', () => {
    const respuesta = responder('¿Qué tengo que reponer?', { zona, avisos });
    expect(respuesta.avisos.every((aviso) => aviso.tipo === 'stock')).toBe(true);
    expect(respuesta.texto).toContain('3 productos para reponer');
  });

  it('el precio de un producto en la zona, agregado', () => {
    const respuesta = responder('¿cuánto cobran la yerba en mi zona?', { zona, avisos });
    expect(respuesta.texto).toContain('va de $ 3.500 a $ 4.300');
    expect(respuesta.precios?.[0]?.nombre).toBe('Yerba Playadito 1 kg');
  });

  it('un producto que casi nadie vende no se informa', () => {
    const respuesta = responder('¿cuánto cobran el fernet?', { zona, avisos });
    expect(respuesta.texto).toContain('al menos 3 comercios');
    expect(respuesta.precios).toEqual([]);
  });

  it('qué se vende cerca y qué no se mueve', () => {
    expect(
      responder('¿qué se vende cerca?', { zona, avisos }).avisos.every(
        (a) => a.tipo === 'consumo_zona',
      ),
    ).toBe(true);
    expect(
      responder('¿qué no se vende?', { zona, avisos }).avisos.every((a) => a.tipo === 'estancado'),
    ).toBe(true);
  });

  it('si no entiende, lo dice y da ejemplos', () => {
    expect(responder('blablá', { zona, avisos }).texto).toContain('Todavía no sé responder eso');
    expect(responder('   ', { zona, avisos }).texto).toContain('pregunta corta');
  });
});

describe('parecido', () => {
  it('comparte la primera palabra y suma por palabra en común', () => {
    expect(parecido('Yerba Playadito 1 kg', 'Yerba Playadito 500 g')).toBe(1);
    expect(parecido('Yerba Playadito', 'Yerba Taragüi')).toBe(0.5);
    expect(parecido('Playadito Yerba', 'Yerba Playadito')).toBe(1);
    expect(parecido('Fideos', 'Yerba')).toBe(0);
  });
});
