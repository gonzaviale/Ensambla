import { describe, expect, it } from 'vitest';
import {
  colorDelNegocio,
  detalleDelNegocio,
  inicialDelNegocio,
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

describe('paraElegir', () => {
  it('toma del resumen lo justo para elegir, sin montos', () => {
    expect(paraElegir(KIOSCO)).toEqual({
      id: 'kiosco',
      nombre: 'Kiosco de la esquina',
      avisosPendientes: 2,
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

describe('colorDelNegocio', () => {
  it('le da a cada negocio un color de la paleta según su lugar, y vuelve a empezar después del tercero', () => {
    expect([0, 1, 2].map(colorDelNegocio)).toEqual(['petrol', 'orange', 'coral']);
    expect(colorDelNegocio(3)).toBe('petrol');
  });
});

describe('inicialDelNegocio', () => {
  it('usa la primera letra en mayúscula, y una N si no hay nombre', () => {
    expect(inicialDelNegocio(' almacén')).toBe('A');
    expect(inicialDelNegocio('')).toBe('N');
  });
});

describe('detalleDelNegocio', () => {
  it('junta el rubro y las tablas, y sin rubro dice sólo las tablas', () => {
    expect(detalleDelNegocio('Kiosco', 4)).toBe('Kiosco · 4 tablas');
    expect(detalleDelNegocio(undefined, 1)).toBe('1 tabla');
  });
});
