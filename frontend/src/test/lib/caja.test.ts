import { describe, expect, it } from 'vitest';
import { allBlocks, makeTemplate, removeRow, upsertRow } from '@/lib/sistema/core';
import { agregarLinea, cambiarCantidad, crearLinea, guardarArticuloCaja, idCaja, leerPrecio, prepararCaja } from '@/lib/caja';
import type { Bloque, Fila } from '@/lib/sistema/tipos';

const pan: Fila = { id: 'pan', name: 'Pan francés', saleMode: 'peso', quantity: 2, price: 4000 };
const coca: Fila = { id: 'coca', name: 'Coca-Cola', quantity: 10, price: 2700 };

function escenario() {
  const sistema = makeTemplate('shop', {}, false);
  const inventario = allBlocks(sistema).find(b => b.type === 'inventory') as Bloque;
  inventario.rows = [{ ...pan }, { ...coca }];
  return { sistema, inventario };
}

describe('caja con precios por unidad y kilo', () => {
  it('calcula gramos, precio por kilo y redondeo por renglón', () => {
    expect(crearLinea(pan, '500')).toMatchObject({ cantidad: 0.5, importeCentavos: 200000 });
    expect(crearLinea({ ...pan, price: 10999 }, '125').importeCentavos).toBe(137488);
    expect(crearLinea(coca, '2').importeCentavos).toBe(540000);
  });

  it('coincide con el cobro del servidor al redondear exactamente medio centavo', () => {
    expect(crearLinea({ ...pan, price: 1 }, '145').importeCentavos).toBe(15);
    expect(crearLinea({ ...pan, price: 1 }, '144').importeCentavos).toBe(14);
  });

  it.each(['', '-1', '0', '1.5', 'NaN'])('rechaza cantidad inválida %s', cantidad => {
    expect(() => crearLinea(pan, cantidad)).toThrow();
  });

  it.each(['', '-1', '0', '1.234,50', '1,2,3', '3.001'])('rechaza precio ambiguo %s', precio => {
    expect(() => leerPrecio(precio)).toThrow();
  });

  it('acepta coma decimal sin separador de miles', () => {
    expect(leerPrecio('1250,50')).toBe(125050);
  });

  it('agrupa unidades y conserva pesadas independientes', () => {
    const primera = agregarLinea([], crearLinea(coca));
    const segunda = agregarLinea(primera, crearLinea(coca, '2'));
    expect(segunda).toHaveLength(1);
    expect(segunda[0]).toMatchObject({ cantidad: 3, importeCentavos: 810000 });
    expect(agregarLinea([crearLinea(pan,'100')], crearLinea(pan,'100'))).toHaveLength(2);
    expect(primera[0]?.cantidad).toBe(1);
  });

  it('corrige los gramos sin cambiar el precio histórico ni el identificador de línea', () => {
    const linea = crearLinea(pan, '100');
    expect(cambiarCantidad(linea, 0.108)).toMatchObject({ id: linea.id, cantidad: 0.108, importeCentavos: 43200 });
  });

  it('prepara las tablas conectadas sin duplicar productos ni repetir configuración', () => {
    const { sistema, inventario } = escenario();
    const ventas = prepararCaja(sistema, inventario.id);
    expect(prepararCaja(sistema, inventario.id)).toBe(ventas);
    expect(inventario.rows).toHaveLength(2);
    expect(inventario.rows[0]).toMatchObject({ quantity: 2, saleMode: 'peso', stockControl: true, favorite: false });
    expect(inventario.rows[1]?.saleMode).toBe('unidad');
    expect(inventario.fields.filter(f => f.key === 'saleMode')).toHaveLength(1);
  });

  it('el alta rápida guarda una ficha reutilizable sin inventar costo ni existencias', () => {
    const { sistema, inventario } = escenario();
    prepararCaja(sistema,inventario.id);
    const producto = guardarArticuloCaja(sistema,inventario,{ nombre:'Jamón', precio:'12000', modo:'peso',
      favorito:true, controlarStock:false, cantidad:'100', extras:{} }, '77903754');
    expect(producto).toMatchObject({ name:'Jamón', price:12000, saleMode:'peso', favorite:true, stockControl:false, quantity:0, barcode:'77903754' });
    expect(producto.cost).toBe('');
  });

  it('conserva campos propios y conteo al modificar la ficha', () => {
    const { sistema, inventario } = escenario();
    prepararCaja(sistema,inventario.id);
    const producto = guardarArticuloCaja(sistema,inventario,{ nombre:'Pan francés', precio:'4500', modo:'peso',
      favorito:true, controlarStock:true, cantidad:'100', extras:{} }, '', 'pan');
    expect(producto.quantity).toBe(2);
    expect(producto.price).toBe(4500);
  });

  it('no permite modificar o eliminar una venta ya confirmada desde el editor', () => {
    const { sistema, inventario } = escenario();
    const ventas = allBlocks(sistema).find(b => b.type === 'sales') as Bloque;
    ventas.rows.push({ id:'venta',product:inventario.rows[0]?.id, quantity:1,price:4000,__source:'Caja operación' });
    expect(upsertRow(ventas,{},'venta',sistema).errors).toBeDefined();
    expect(removeRow(ventas,'venta',sistema).errors).toBeDefined();
  });

  it('envía los ids UUID del backend sin el prefijo local de las tablas', () => {
    const uuid=crypto.randomUUID();
    expect(idCaja(`e${uuid}`)).toBe(uuid);
    expect(idCaja(uuid)).toBe(uuid);
  });
});
