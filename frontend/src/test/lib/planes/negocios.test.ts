import { describe, expect, it } from 'vitest';
import { textoDeNegocios } from '@/lib/planes/negocios';

let texto = '';

describe('textoDeNegocios', () => {
  it('con un solo negocio lo dice en singular', () => {
    cuandoSeDescribenLosNegocios(1);

    entoncesDice('1 negocio');
  });

  it('con más de uno cuenta el propio y los que se pueden sumar', () => {
    cuandoSeDescribenLosNegocios(6);

    entoncesDice('Tu negocio y hasta 5 más');
  });

  it('con dos negocios suma uno solo más', () => {
    cuandoSeDescribenLosNegocios(2);

    entoncesDice('Tu negocio y hasta 1 más');
  });

  it('sin límite lo dice', () => {
    cuandoSeDescribenLosNegocios(null);

    entoncesDice('Ilimitado');
  });
});

function cuandoSeDescribenLosNegocios(cantidad: number | null) {
  texto = textoDeNegocios(cantidad);
}

function entoncesDice(esperado: string) {
  expect(texto).toBe(esperado);
}
