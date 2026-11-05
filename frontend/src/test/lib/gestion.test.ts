import { describe, expect, it } from 'vitest';
import { cambiosDeCosto, faltantes, planDeCompras, proveedorDe } from '@/lib/gestion/compras';
import { HOJA_DEL_DIA, type Renglon } from '@/lib/gestion/datosDeEjemplo';
import { interpretarPregunta, proyectar } from '@/lib/gestion/proyeccion';
import {
  cierreDelDia,
  cuentaDelMes,
  cuentasCorrientes,
  datosDelNegocio,
  fiadoConLaHoja,
  resultadoDelMes,
  type DatosDelNegocio,
  type Producto,
} from '@/lib/gestion/resultado';
import { crearAlmacenCargado } from '@/lib/sistema/demo';

const HOY = '2026-11-05';

const almacen = () => datosDelNegocio(crearAlmacenCargado(HOY));

const producto = (datos: Partial<Producto>): Producto => ({
  id: 'p1',
  nombre: 'Yerba Playadito 1 kg',
  stock: 10,
  precio: 1000,
  costo: 600,
  minimo: 0,
  ...datos,
});

const negocioDe = (productos: Producto[], ventas: DatosDelNegocio['ventas']): DatosDelNegocio => ({
  nombre: 'Prueba',
  productos,
  ventas,
  clientes: [{ id: 'c1', nombre: 'Doña Marta', telefono: '' }],
});

describe('la ganancia real', () => {
  it('resta comisiones, mermas y gastos fijos al margen de lo vendido', () => {
    const cuenta = cuentaDelMes(100000, 70000, 20000);

    expect(cuenta.margenBruto).toBe(30000);
    expect(cuenta.ganancia).toBeLessThan(cuenta.margenBruto - cuenta.gastosFijos);
    expect(cuenta.ganancia).toBeCloseTo(
      cuenta.margenBruto - cuenta.comisiones - cuenta.mermas - cuenta.gastosFijos,
    );
  });

  it('da el punto de equilibrio con lo que queda de cada peso vendido', () => {
    const cuenta = cuentaDelMes(100000, 70000, 20000);

    expect(cuenta.equilibrioMensual).toBeCloseTo(20000 / cuenta.contribucion);
    expect(cuenta.diaDeEquilibrio).toBeGreaterThan(0);
  });

  it('no tiene punto de equilibrio cuando lo vendido no deja margen', () => {
    const cuenta = cuentaDelMes(100000, 100000, 20000);

    expect(cuenta.equilibrioMensual).toBeNull();
    expect(cuenta.diaDeEquilibrio).toBeNull();
  });

  it('en el almacén de la demo la ganancia real es menor que «ventas menos costos»', () => {
    const resultado = resultadoDelMes(almacen(), HOY);

    expect(resultado.ventas).toBeGreaterThan(0);
    expect(resultado.ganancia).toBeLessThan(resultado.cuentaVieja);
    expect(resultado.vendidoAFiado).toBeGreaterThan(0);
  });
});

describe('el cierre del día', () => {
  it('separa la ganancia de la plata del cajón', () => {
    const cierre = cierreDelDia(HOJA_DEL_DIA, 0.25);

    expect(cierre.vendido).toBe(24600 + 8500 + 4300);
    expect(cierre.fiadoNuevo).toBe(4300);
    // Cambio + efectivo vendido + cobro de fiado − pago al proveedor − bolsas.
    expect(cierre.cajon).toBe(10000 + 24600 + 6000 - 15000 - 1200);
    expect(cierre.ganancia).toBeLessThan(cierre.margenBruto);
  });

  it('pagarle al proveedor no cambia la ganancia del día', () => {
    const sinPago = HOJA_DEL_DIA.filter((renglon) => renglon.tipo !== 'pago');
    const pago: Renglon = {
      id: 'otro',
      escrito: 'Dist. Norte 15.000',
      tipo: 'pago',
      importe: 15000,
      detalle: 'Otro pago',
      medio: 'efectivo',
    };
    const pagoDoble = [...HOJA_DEL_DIA, pago];

    expect(cierreDelDia(sinPago, 0.25).ganancia).toBeCloseTo(
      cierreDelDia(pagoDoble, 0.25).ganancia,
    );
  });
});

describe('el fiado', () => {
  it('aplica los cobros a lo más viejo y deja lo que falta', () => {
    const datos = negocioDe(
      [producto({})],
      [
        {
          productoId: 'p1',
          cantidad: 2,
          precio: 1000,
          fecha: '2026-09-01',
          clienteId: 'c1',
          fiado: true,
        },
        {
          productoId: 'p1',
          cantidad: 1,
          precio: 1000,
          fecha: '2026-11-01',
          clienteId: 'c1',
          fiado: true,
        },
      ],
    );

    const [cuenta] = cuentasCorrientes(datos, HOY, { 'Doña Marta': 2000 });

    expect(cuenta?.debe).toBe(1000);
    expect(cuenta?.dias).toBe(4);
    expect(cuenta?.riesgo).toBe('bajo');
  });

  it('no lista a quien no debe nada', () => {
    const datos = negocioDe(
      [producto({})],
      [
        {
          productoId: 'p1',
          cantidad: 1,
          precio: 1000,
          fecha: '2026-11-01',
          clienteId: 'c1',
          fiado: true,
        },
      ],
    );

    expect(cuentasCorrientes(datos, HOY, { 'Doña Marta': 1000 })).toEqual([]);
  });
});

describe('las compras', () => {
  it('pide lo que no llega a la próxima entrega, redondeado al bulto', () => {
    const ventas = Array.from({ length: 30 }, (_, dia) => ({
      productoId: 'p1',
      cantidad: 1,
      precio: 1000,
      fecha: `2026-10-${String(dia + 1).padStart(2, '0')}`,
      clienteId: '',
      fiado: false,
    })).filter((venta) => venta.fecha <= '2026-10-31');
    const datos = negocioDe([producto({ stock: 2 })], ventas);

    const [faltante] = faltantes(datos, '2026-10-31');

    expect(faltante?.pedir).toBe(12);
    expect(faltante?.urgencia).toBe('urgente');
    expect(faltante?.proveedor.nombre).toBe('Distribuidora Norte');
  });

  it('elige el proveedor por el nombre del producto', () => {
    expect(proveedorDe('Coca-Cola 2,25 L').nombre).toBe('Bebidas del Sur');
    expect(proveedorDe('Lavandina Ayudín 1 L').nombre).toBe('Limpieza Brillo');
  });

  it('deja para la próxima vuelta lo que no entra en la plata, salvo lo agotado y lo urgente', () => {
    const lista = faltantes(almacen(), HOY);

    const plan = planDeCompras(lista, 1);

    expect(
      plan.ordenes.flatMap((orden) => orden.items).every((item) => item.urgencia !== 'pronto'),
    ).toBe(true);
    expect(plan.postergados.length).toBeGreaterThan(0);
  });

  it('sugiere el precio que mantiene el margen cuando sube el costo', () => {
    const [cambio] = cambiosDeCosto([producto({ precio: 1000, costo: 600 })], {
      'Yerba Playadito 1 kg': 750,
    });

    expect(cambio?.variacion).toBeCloseTo(0.25);
    expect(cambio?.precioSugerido).toBe(1250);
  });
});

describe('¿me conviene?', () => {
  it('sumar un gasto fijo sin vender más baja la ganancia', () => {
    const proyeccion = proyectar(almacen(), HOY, {
      tipo: 'gastoFijo',
      concepto: 'Un ayudante',
      mensual: 60000,
      ventasExtraPorDia: 0,
    });

    expect(proyeccion.diferencia).toBeCloseTo(-60000);
    expect(proyeccion.veredicto).toBe('no-conviene');
    expect(proyeccion.serie).toHaveLength(14);
  });

  it('subir un básico vende menos unidades pero puede ganar más', () => {
    const datos = almacen();
    const yerba = datos.productos.find((item) => item.nombre.startsWith('Yerba'));

    const proyeccion = proyectar(datos, HOY, {
      tipo: 'precio',
      productoId: yerba?.id ?? '',
      cambio: 0.1,
    });

    expect(proyeccion.despues.ventas).not.toBe(proyeccion.antes.ventas);
    expect(proyeccion.veredicto).toBe('conviene');
  });

  it('entiende una pregunta escrita', () => {
    const productos = almacen().productos;

    expect(interpretarPregunta('¿Qué pasa si subo la yerba 15%?', productos)).toMatchObject({
      tipo: 'precio',
      cambio: 0.15,
    });
    expect(interpretarPregunta('¿Puedo tomar un ayudante por 350 mil?', productos)).toMatchObject({
      tipo: 'gastoFijo',
      mensual: 350000,
    });
    expect(
      interpretarPregunta('¿Me conviene empezar a vender pan casero?', productos),
    ).toMatchObject({
      tipo: 'producto',
      nombre: 'pan casero',
    });
    expect(interpretarPregunta('hola', productos)).toBeNull();
  });
});

describe('el fiado con la hoja del día', () => {
  it('suma al cliente nuevo y descuenta lo cobrado', () => {
    const cuentas = [
      {
        cliente: { id: 'c2', nombre: 'Bar La Esquina', telefono: '' },
        debe: 10000,
        dias: 30,
        riesgo: 'medio' as const,
      },
    ];

    const lista = fiadoConLaHoja(cuentas, HOJA_DEL_DIA);

    expect(lista.find((cuenta) => cuenta.cliente.nombre === 'Bar La Esquina')?.debe).toBe(4000);
    expect(lista.find((cuenta) => cuenta.cliente.nombre === 'Doña Marta')).toMatchObject({
      debe: 4300,
      dias: 0,
    });
  });
});
