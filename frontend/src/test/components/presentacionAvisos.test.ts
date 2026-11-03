import { describe, expect, it } from 'vitest';
import type { Aviso } from '@/lib/sistema/alertas';
import { datosDelAviso, NOMBRE_DE_TIPO } from '@/components/sistema/avisos/presentacion';

const avisoDeRevisarPrecio = (datos?: Record<string, unknown>): Aviso => ({
  id: 'servidor:1',
  tipo: 'revisar_precio',
  grupo: 'ventas',
  prioridad: 2,
  sistemaId: 'c0000000-0000-4000-8000-000000000001',
  sistema: 'Mi almacén',
  titulo: 'Yerba vende menos desde que subiste el precio',
  detalle: 'Revisá si el nuevo precio te conviene.',
  porque: [],
  acciones: [],
  datos,
});

describe('datosDelAviso · revisar el precio', () => {
  it('muestra el precio anterior y el actual', () => {
    const datos = datosDelAviso(avisoDeRevisarPrecio({ precioAnterior: 1000, precioNuevo: 1200 }));

    expect(datos.map((dato) => dato.etiqueta)).toEqual(['Precio anterior', 'Precio actual']);
  });

  it('no muestra nada si el aviso no trae los precios', () => {
    expect(datosDelAviso(avisoDeRevisarPrecio())).toEqual([]);
    expect(datosDelAviso(avisoDeRevisarPrecio({ precioAnterior: 1000 }))).toEqual([]);
  });

  it('tiene su nombre de tipo', () => {
    expect(NOMBRE_DE_TIPO.revisar_precio).toBe('Revisar el precio');
  });
});
