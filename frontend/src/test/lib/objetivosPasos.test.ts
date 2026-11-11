import { describe, expect, it } from 'vitest';
import { objetivoDeEjemplo } from '@/lib/objetivos';
import { conResultadoReal, negocioDe } from '@/lib/objetivosNegocio';
import {
  comparacionConZona,
  escenarioDe,
  gananciaExtraPorAumento,
  montoDe,
  pasosConcretos,
  ventasConAumento,
} from '@/lib/objetivosPasos';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import type { ProductoParaPaso } from '@/types/objetivos';

const HOY = '2026-11-08';

const PRODUCTOS: ProductoParaPaso[] = [
  {
    tipo: 'SUBIR_PRECIO',
    nombre: 'Yerba Playadito 1 kg',
    precio: 3600,
    costo: 2700,
    stock: 4,
    vendidas30Dias: 60,
    precioZona: { minimo: 3500, promedio: 3900, maximo: 4300 },
  },
  {
    tipo: 'MOVER_PARADO',
    nombre: 'Cacao amargo',
    precio: 2500,
    costo: 1800,
    stock: 20,
    vendidas30Dias: 0,
    precioZona: null,
  },
  {
    tipo: 'PONER_A_LA_VISTA',
    nombre: 'Alfajor',
    precio: 1000,
    costo: 400,
    stock: 8,
    vendidas30Dias: 10,
    precioZona: null,
  },
];

const almacen = () => pasosConcretos(PRODUCTOS);
const yerba = {
  nombre: 'Yerba',
  precio: 3600,
  costo: 2700,
  stock: 4,
  vendidas: 60,
  precioZona: null,
};

describe('pasos concretos de un objetivo', () => {
  it('sin cuadernos no hay nada que recomendar', () => {
    expect(pasosConcretos([])).toEqual([]);
  });

  it('con el almacén cargado arma pasos con productos de verdad', () => {
    const pasos = almacen();

    expect(pasos.map((paso) => paso.clave)).toEqual([
      'subir-precio',
      'mover-parado',
      'poner-a-la-vista',
    ]);
    expect(pasos[0]?.producto).toBe('Yerba Playadito 1 kg');
    expect(pasos[0]?.calcular(200, 'media').titulo).toBe('Subile $200 a Yerba Playadito 1 kg');
    expect(pasos[1]?.calcular(300, 'media').sumaPorMes).toBeUndefined();
  });

  it('el monto del consejo se puede cambiar y cada monto tiene su proyección', () => {
    const paso = almacen()[0];
    const doscientos = paso?.calcular(200, 'media').sumaPorMes ?? 0;
    const trescientos = paso?.calcular(300, 'media').sumaPorMes ?? 0;

    expect(paso?.ajuste?.desde).toBe(50);
    expect(paso?.ajuste?.hasta).toBeGreaterThan(300);
    expect(trescientos).toBeGreaterThan(doscientos);
    expect(doscientos).toBeGreaterThan(0);
  });

  it('al subir mucho el precio se vende menos y termina ganándose menos', () => {
    expect(ventasConAumento(yerba, 0, 'media')).toBe(60);
    expect(ventasConAumento(yerba, 400, 'media')).toBeLessThan(60);
    expect(gananciaExtraPorAumento(yerba, 400, 'media')).toBeGreaterThan(0);
    expect(gananciaExtraPorAumento(yerba, 900, 'media')).toBeLessThan(
      gananciaExtraPorAumento(yerba, 450, 'media'),
    );
    expect(gananciaExtraPorAumento(yerba, 900, 'mucha')).toBeLessThan(0);

    const paso = almacen()[0];
    expect(paso?.calcular(paso.ajuste?.hasta ?? 0, 'mucha').efecto).toMatch(/menos por mes/);
  });

  it('cuanto más sensibles son los clientes, menos conviene subir', () => {
    const ajuste = almacen()[0]?.ajuste;

    expect(ajuste?.sugerido('poca')).toBeGreaterThan(ajuste?.sugerido('mucha') ?? 0);
  });

  it('el sugerido es el que más ganancia deja, y se usa si la persona no eligió otro', () => {
    const paso = almacen()[0];
    const sugerido = paso ? montoDe(paso, {}, 'media') : 0;

    expect(sugerido).toBe(paso?.ajuste?.sugerido('media'));
    expect(montoDe(paso as NonNullable<typeof paso>, { 'subir-precio': 250 }, 'media')).toBe(250);
    for (let monto = 50; monto <= (paso?.ajuste?.hasta ?? 0); monto += 50) {
      expect(paso?.curva?.(monto, 'media') ?? 0).toBeLessThanOrEqual(
        (paso?.curva?.(sugerido, 'media') ?? 0) + 1e-6,
      );
    }
  });

  it('bajar un precio para mover lo parado nunca lo deja por debajo del costo', () => {
    const paso = almacen()[1];
    const resultado = paso?.calcular(paso.ajuste?.hasta ?? 0, 'media');

    expect(resultado?.precioNuevo).toBeGreaterThan(0);
    expect(resultado?.liberaPlata).toBeGreaterThan(0);
  });

  it('no usa porcentajes ni jerga para explicar cada paso', () => {
    for (const paso of almacen()) {
      const { titulo, detalle, efecto } = paso.calcular(montoDe(paso, {}, 'media'), 'media');
      expect(`${titulo} ${detalle} ${efecto}`).not.toMatch(/%|margen|elasticidad/i);
    }
  });

  it('el escenario suma sólo los pasos que se eligen, con los montos elegidos', () => {
    const pasos = almacen();
    const objetivo = objetivoDeEjemplo();
    const ninguno = escenarioDe(objetivo, pasos, new Set());
    const sugerido = escenarioDe(objetivo, pasos, new Set(['subir-precio']));
    const masChico = escenarioDe(objetivo, pasos, new Set(['subir-precio']), {
      'subir-precio': 50,
    });
    const parado = escenarioDe(objetivo, pasos, new Set(['mover-parado']));

    expect(ninguno.conPasos).toBe(ninguno.hoy);
    expect(ninguno.faltaria).toBe(objetivo.meta - ninguno.hoy);
    expect(sugerido.conPasos).toBeGreaterThan(masChico.conPasos);
    expect(masChico.conPasos).toBeGreaterThan(ninguno.hoy);
    expect(parado.conPasos).toBe(ninguno.hoy);
    expect(parado.liberada).toBeGreaterThan(0);
  });

  it('compara el precio nuevo con lo que cobran en la zona, y calla si hay pocos datos', () => {
    const precioZona = { minimo: 3500, promedio: 3900, maximo: 4300 };

    expect(comparacionConZona(3800, precioZona)).toMatch(
      /entre \$3\.500 y \$4\.300.*dentro de lo que cobran/,
    );
    expect(comparacionConZona(4600, precioZona)).toMatch(/más caro/);
    expect(comparacionConZona(3800, null)).toBeNull();
  });
});

describe('cómo viene el negocio', () => {
  it('cuenta lo vendido, lo que costó y lo que te deben en los últimos 30 días', () => {
    const negocio = negocioDe([crearAlmacenCargado(HOY)], HOY);

    expect(negocio.hayVentas).toBe(true);
    expect(negocio.queda).toBe(negocio.vendiste - negocio.mercaderia - negocio.gastos);
    expect(negocio.vendiste).toBeGreaterThan(negocio.mercaderia);
  });

  it('sin ventas no hay números y el objetivo sigue con su estimación de ejemplo', () => {
    const negocio = negocioDe([], HOY);
    const objetivo = objetivoDeEjemplo();

    expect(negocio.hayVentas).toBe(false);
    expect(conResultadoReal(objetivo, negocio)).toBe(objetivo);
  });

  it('con ventas, lo que ganás hoy es lo que realmente te queda', () => {
    const negocio = negocioDe([crearAlmacenCargado(HOY)], HOY);
    const real = conResultadoReal(objetivoDeEjemplo(), negocio);

    expect(real.historial.at(-1)?.valor).toBe(Math.round(negocio.queda));
  });
});
