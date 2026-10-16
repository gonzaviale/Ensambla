import { describe, expect, it } from 'vitest';
import { piezaDeNombre } from '@/lib/catalogo/piezas';
import { detectarRubro, ensamblar } from '@/lib/ensamblador';
import { planEfectivo, importeACobrar } from '@/lib/planes/planes';
import * as suscripcion from '@/lib/planes/suscripcion';
import { entenderRelato, leerImporte } from '@/lib/relato/parser';
import {
  periodosDelMes,
  respuestaDeuda,
  respuestaMargen,
  respuestaPerdida,
  respuestaTeQuedo,
} from '@/lib/respuestas/calculos';
import { faltan, respuestasQueDesbloquea } from '@/lib/respuestas/definiciones';

describe('ensamblador', () => {
  it('detecta el rubro y arma la base más lo que el relato pide', () => {
    const { rubro, piezas } = ensamblar(
      'Hago viandas por encargo, cobro una seña y le fío a algunos clientes',
    );
    expect(rubro).toBe('gastronomia');
    expect(piezas.map((p) => p.id)).toEqual(['productos', 'gastos', 'pedidos', 'fiados']);
    expect(piezas.find((p) => p.id === 'productos')?.nombre).toBe('Platos');
    expect(piezas.find((p) => p.id === 'fiados')?.motivo).toMatch(/fio/i);
  });

  it('un almacén arma Productos, Ventas y Gastos; con el rubro elegido gana el chip', () => {
    expect(detectarRubro('Tengo un kiosco en el barrio')).toBe('comercio');
    expect(ensamblar('', 'comercio').piezas.map((p) => p.id)).toEqual([
      'productos',
      'ventas',
      'gastos',
    ]);
  });

  it('reconoce una tabla por nombre o alias', () => {
    expect(piezaDeNombre('Platos')?.id).toBe('productos');
    expect(piezaDeNombre('gastos')?.id).toBe('gastos');
    expect(piezaDeNombre('Algo raro')).toBeUndefined();
  });
});

describe('respuestas', () => {
  it('sumar una pieza desbloquea las respuestas que dependían de ella', () => {
    expect(faltan(['ventas', 'gastos'], ['ventas'])).toEqual(['gastos']);
    expect(respuestasQueDesbloquea('gastos', ['ventas'])).toContain('te-quedo');
    expect(respuestasQueDesbloquea('fiados', ['ventas'])).toEqual(['quien-te-debe']);
  });

  it('calcula lo que te quedó y compara con el mes anterior', () => {
    const r = respuestaTeQuedo(
      { total: 300000, cantidad: 40 },
      { total: 120000, cantidad: 10 },
      { total: 200000, cantidad: 30 },
      { total: 100000, cantidad: 9 },
    );
    expect(r).toMatchObject({ estado: 'lista' });
    if (r.estado === 'lista') {
      expect(r.titular.replace(/\s/g, ' ')).toContain('180.000');
      expect(r.tendencia?.mejor).toBe(true);
    }
  });

  it('marca los productos que se venden con pérdida y el que más deja', () => {
    const productos = [
      { id: '1', nombre: 'Vela', costo: 500, precio: 1200, stock: 3 },
      { id: '2', nombre: 'Jabón', costo: 900, precio: 800, stock: 10 },
    ];
    const perdida = respuestaPerdida(productos);
    expect(perdida.estado === 'lista' && perdida.items?.[0]?.nombre).toBe('Jabón');
    const margen = respuestaMargen(productos);
    expect(margen.estado === 'lista' && margen.titular).toBe('Vela');
  });

  it('suma lo que te deben por cliente', () => {
    const r = respuestaDeuda(
      [
        { cliente: 'Marta', monto: 3000 },
        { cliente: 'Marta', monto: 1000 },
        { cliente: 'Luis', monto: 500 },
      ],
      3,
    );
    expect(r.estado === 'lista' && r.items?.[0]).toMatchObject({ nombre: 'Marta' });
  });

  it('arma los períodos del mes y del mes anterior', () => {
    const { actual, anterior } = periodosDelMes(new Date(2026, 9, 15));
    expect(actual).toEqual({ desde: '2026-10-01', hasta: '2026-10-15' });
    expect(anterior).toEqual({ desde: '2026-09-01', hasta: '2026-09-30' });
  });
});

describe('contá qué pasó', () => {
  it('entiende ventas, gastos y fiados', () => {
    const { filas, sinEntender } = entenderRelato(
      'Vendí 3 velas a 4500. Gasté 12000 en cera; le fié a Marta 3000\nHoy llovió mucho',
    );
    expect(filas.map((f) => f.pieza)).toEqual(['ventas', 'gastos', 'fiados']);
    expect(filas[0]?.valores).toMatchObject({ cantidad: 3, producto: 'Velas', monto: 4500 });
    expect(filas[1]?.valores).toMatchObject({ concepto: 'Cera', monto: 12000 });
    expect(filas[2]?.valores).toMatchObject({ cliente: 'Marta', monto: 3000 });
    expect(sinEntender).toEqual(['Hoy llovió mucho']);
  });

  it('lee importes con puntos de miles y coma decimal', () => {
    expect(leerImporte('4.500')).toBe(4500);
    expect(leerImporte('$ 4500')).toBe(4500);
    expect(leerImporte('12,5')).toBe(12.5);
    expect(leerImporte('nada')).toBeNull();
  });
});

describe('planes', () => {
  const AHORA = new Date('2026-10-15T12:00:00Z');

  it('la prueba da todo y al vencer vuelve al plan Gratis', () => {
    const inicial = suscripcion.suscripcionInicial(AHORA);
    expect(planEfectivo(inicial, AHORA)).toMatchObject({ plan: 'negocio-plus', enPrueba: true });
    const vencida = suscripcion.simularFinDePrueba(inicial, AHORA);
    expect(planEfectivo(vencida, AHORA)).toMatchObject({ plan: 'gratis', estado: 'vencida' });
  });

  it('suscribirse registra el pago y cancelar conserva lo ya pagado', () => {
    const activa = suscripcion.suscribirse(
      suscripcion.suscripcionInicial(AHORA),
      { plan: 'negocio', periodo: 'anual', medioPago: { tipo: 'mercado-pago', detalle: 'MP' } },
      AHORA,
    );
    expect(activa.pagos[0]?.importe).toBe(importeACobrar('negocio', 'anual'));
    const cancelada = suscripcion.cancelarSuscripcion(activa, AHORA);
    expect(planEfectivo(cancelada, AHORA)).toMatchObject({ plan: 'negocio', estado: 'cancelada' });
  });
});
