import { describe, it } from 'vitest';
import {
  cuandoSeAnalizaElAlmacenCargado,
  cuandoSeArmaElAlmacenCargado,
  entoncesHayAlMenosAvisos,
  entoncesHayAvisosDeLosTipos,
  entoncesLasVentasApuntanAProductosQueExisten,
  entoncesTieneAlMenos,
} from '@/test/almacenesDeDemo';

const CANTIDADES = [
  { tipo: 'inventory', nombre: 'productos', cantidad: 25 },
  { tipo: 'sales', nombre: 'ventas', cantidad: 501 },
  { tipo: 'clients', nombre: 'clientes', cantidad: 10 },
] as const;

describe('el almacén cargado de la demo', () => {
  it.each(CANTIDADES)('trae al menos $cantidad $nombre', ({ tipo, cantidad }) => {
    cuandoSeArmaElAlmacenCargado();

    entoncesTieneAlMenos(tipo, cantidad);
  });

  it('las ventas apuntan a productos que existen', () => {
    cuandoSeArmaElAlmacenCargado();

    entoncesLasVentasApuntanAProductosQueExisten();
  });

  it('genera muchos avisos', () => {
    cuandoSeAnalizaElAlmacenCargado();

    entoncesHayAlMenosAvisos(12);
  });

  it('genera avisos de todos los tipos', () => {
    cuandoSeAnalizaElAlmacenCargado();

    entoncesHayAvisosDeLosTipos([
      'stock',
      'estancado',
      'precio_zona',
      'muy_vendido',
      'combo',
      'consumo_zona',
    ]);
  });
});
