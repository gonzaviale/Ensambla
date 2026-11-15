import { describe, expect, it } from 'vitest';
import { conSigno, diapositivasDelAviso } from '@/lib/sistema/rentabilidad';
import type { AvisoEconomico } from '@/types/avisoEconomico';

const aviso = (datos: Partial<AvisoEconomico> = {}): AvisoEconomico => ({
  prioridad: 'ALTA',
  tendencia: 'EMPEORA',
  categoria: 'Rentabilidad',
  titulo: 'Las ventas subieron 12 %, pero la ganancia bajó 4 %',
  explicacion: 'Vendiste más, pero te quedó menos ganancia.',
  variacionVentas: 12,
  variacionResultadoBruto: -4,
  productos: ['Aceite', 'Leche'],
  cambios: [
    { nombre: 'Aceite', monto: -4200 },
    { nombre: 'Leche', monto: -2600 },
  ],
  confianza: 'MEDIA',
  ...datos,
});

describe('conSigno', () => {
  it('pone el signo y el porcentaje', () => {
    expect(conSigno(12)).toBe('+12 %');
    expect(conSigno(-4)).toBe('−4 %');
    expect(conSigno(0)).toBe('0 %');
  });
});

describe('diapositivasDelAviso', () => {
  it('separa ventas, ganancia y productos', () => {
    const diapositivas = diapositivasDelAviso(aviso(), 'n1');

    expect(diapositivas.map((item) => item.tipo)).toEqual(['ventas', 'ganancia', 'productos']);
    expect(diapositivas[0]).toMatchObject({ cambio: 12 });
    expect(diapositivas[1]).toMatchObject({ cambio: -4 });
  });

  it('sin costos avisa que faltan y sin productos no arma esa tarjeta', () => {
    const diapositivas = diapositivasDelAviso(
      aviso({ variacionResultadoBruto: null, productos: [], cambios: [] }),
      'n1',
    );

    expect(diapositivas.map((item) => item.tipo)).toEqual(['ventas', 'costos']);
  });

  it('la clave cambia con el negocio y con el aviso', () => {
    const uno = diapositivasDelAviso(aviso(), 'n1')[0]?.clave;
    expect(diapositivasDelAviso(aviso(), 'n2')[0]?.clave).not.toBe(uno);
    expect(diapositivasDelAviso(aviso({ titulo: 'Otro' }), 'n1')[0]?.clave).not.toBe(uno);
  });
});
