import { describe, expect, it } from 'vitest';
import {
  estadoDelNegocio,
  paraElegir,
  rubroLegible,
  textoDeAvisos,
} from '@/lib/negocios/presentacion';
import type { ResumenDeNegocio } from '@/types/negocios';

const KIOSCO: ResumenDeNegocio = {
  id: 'kiosco',
  nombre: 'Kiosco de la esquina',
  activo: false,
  hayDatos: true,
  vendiste: 280000,
  gastos: 52000,
  queda: -72000,
  teDeben: 15000,
  avisosPendientes: 2,
};

describe('estadoDelNegocio', () => {
  it('sin ventas en el período no lo juzga', () => {
    expect(estadoDelNegocio({ hayDatos: false, queda: null })).toBe('SIN_DATOS');
  });

  it('pide atención si lo que queda es negativo y si no, viene bien', () => {
    expect(estadoDelNegocio({ hayDatos: true, queda: -1 })).toBe('ATENCION');
    expect(estadoDelNegocio({ hayDatos: true, queda: 0 })).toBe('BIEN');
    expect(estadoDelNegocio({ hayDatos: true, queda: null })).toBe('BIEN');
  });
});

describe('paraElegir', () => {
  it('toma del resumen lo justo para elegir, sin montos', () => {
    expect(paraElegir(KIOSCO)).toEqual({
      id: 'kiosco',
      nombre: 'Kiosco de la esquina',
      avisosPendientes: 2,
      estado: 'ATENCION',
    });
  });
});

describe('textoDeAvisos', () => {
  it('dice cuántos avisos pendientes hay, en singular o plural, o que no hay', () => {
    expect(textoDeAvisos(0)).toBe('Sin avisos pendientes');
    expect(textoDeAvisos(1)).toBe('1 aviso pendiente');
    expect(textoDeAvisos(4)).toBe('4 avisos pendientes');
  });
});

describe('rubroLegible', () => {
  it('pasa el código del rubro a un texto con mayúscula inicial', () => {
    expect(rubroLegible('KIOSCO')).toBe('Kiosco');
    expect(rubroLegible('VENTA_ONLINE')).toBe('Venta online');
  });
});
