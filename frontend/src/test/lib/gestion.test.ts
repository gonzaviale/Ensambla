import { describe, expect, it } from 'vitest';
import { cambiosDeCosto, faltantes, planDeCompras, proveedorDe } from '@/lib/gestion/compras';
import { HOJA_DEL_DIA, type Renglon } from '@/lib/gestion/datosDeEjemplo';
import { destinosDelCuaderno, pendientesDeLaHoja } from '@/lib/gestion/hoja';
import {
  interpretarPregunta,
  proyectar,
  supuestoDePrecio,
  type Supuesto,
} from '@/lib/gestion/proyeccion';
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

  it('un pedido enviado no cambia con la plata y descuenta lo que ya se comprometió', () => {
    const lista = faltantes(almacen(), HOY);
    const [primera] = planDeCompras(lista, 150000).ordenes;
    if (!primera) throw new Error('El almacén de la demo tiene que tener algo para pedir');

    const plan = planDeCompras(lista, 1, [primera]);
    const enviada = plan.ordenes.find((orden) => orden.proveedor.id === primera.proveedor.id);

    expect(enviada?.enviada).toBe(true);
    expect(enviada?.total).toBe(primera.total);
    expect(plan.enviado).toBe(primera.total);
    expect(plan.quedan).toBe(1 - primera.total - plan.total);
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
  const ayudante = (mensual: number): Supuesto[] => [
    { id: 'g', tipo: 'gastoFijo', concepto: 'Un ayudante', mensual, origen: 'vos' },
    {
      id: 'v',
      tipo: 'ventas',
      etiqueta: 'Ventas de más por día',
      monto: 0,
      vecesPorMes: 30,
      cuando: 'por día',
      incognita: true,
      origen: 'supuesto',
    },
  ];

  it('sumar un gasto fijo sin vender más baja la ganancia', () => {
    const proyeccion = proyectar(almacen(), HOY, ayudante(60000));

    expect(proyeccion.diferencia).toBeCloseTo(-60000);
    expect(proyeccion.veredicto).toBe('no-conviene');
    expect(proyeccion.serie).toHaveLength(14);
  });

  it('lo que no se sabe se vuelve un umbral: vendiendo eso de más, el gasto se paga solo', () => {
    const datos = almacen();
    const { umbral } = proyectar(datos, HOY, ayudante(60000));
    if (!umbral) throw new Error('Con ventas desconocidas tiene que haber un umbral');

    const conElUmbral = ayudante(60000).map((supuesto) =>
      supuesto.tipo === 'ventas' ? { ...supuesto, monto: umbral.valor } : supuesto,
    );

    expect(umbral.supuestoId).toBe('v');
    expect(umbral.frase).toMatch(/más que hoy/);
    expect(proyectar(datos, HOY, conElUmbral).diferencia).toBeCloseTo(0, 0);
  });

  it('una compra grande se mira contra lo que deja en un año', () => {
    const proyeccion = proyectar(almacen(), HOY, [
      { id: 'i', tipo: 'inversion', concepto: 'Heladera', monto: 120000, origen: 'vos' },
      {
        id: 'v',
        tipo: 'ventas',
        etiqueta: 'Ventas de más por día',
        monto: 0,
        vecesPorMes: 30,
        cuando: 'por día',
        incognita: true,
        origen: 'supuesto',
      },
    ]);

    expect(proyeccion.serie[0]?.despues).toBe(-120000);
    expect(proyeccion.veredicto).toBe('no-conviene');
    expect(proyeccion.umbral?.valor).toBeGreaterThan(0);
  });

  it('subir un básico vende menos unidades pero puede ganar más', () => {
    const datos = almacen();
    const yerba = datos.productos.find((item) => item.nombre.startsWith('Yerba'));
    if (!yerba) throw new Error('El almacén de la demo tiene yerba');

    const proyeccion = proyectar(datos, HOY, [supuestoDePrecio(yerba, 0.1)]);

    expect(proyeccion.despues.ventas).not.toBe(proyeccion.antes.ventas);
    expect(proyeccion.veredicto).toBe('conviene');
  });

  it('traduce una pregunta escrita en supuestos', () => {
    const datos = almacen();
    const tipos = (pregunta: string) =>
      interpretarPregunta(pregunta, datos, HOY).supuestos.map((supuesto) => supuesto.tipo);

    expect(interpretarPregunta('¿Qué pasa si subo la yerba 15%?', datos, HOY).supuestos).toEqual([
      expect.objectContaining({ tipo: 'precio', cambio: 0.15 }),
    ]);
    expect(
      interpretarPregunta('¿Puedo tomar un ayudante por 350 mil?', datos, HOY).supuestos[0],
    ).toMatchObject({ tipo: 'gastoFijo', mensual: 350000, origen: 'vos' });
    expect(tipos('¿Me conviene abrir los domingos?')).toEqual(['ventas', 'gastoFijo', 'aparte']);
    expect(tipos('¿Me conviene comprar una heladera de 450 mil?')).toEqual(['inversion', 'ventas']);
    expect(
      interpretarPregunta('¿Me conviene empezar a vender pan casero?', datos, HOY).supuestos[0],
    ).toMatchObject({ tipo: 'producto', nombre: 'pan casero' });
  });

  it('una pregunta que no reconoce igual arma la cuenta con lo básico', () => {
    const decision = interpretarPregunta('¿Me conviene pintar el frente?', almacen(), HOY);

    expect(decision.entendida).toBe(false);
    expect(decision.supuestos.map((supuesto) => supuesto.tipo)).toEqual(['ventas', 'gastoFijo']);
  });
});

describe('la hoja del día en el cuaderno', () => {
  it('manda cada renglón a un bloque del cuaderno y avisa cuál falta', () => {
    const destinos = destinosDelCuaderno(crearAlmacenCargado(HOY));

    expect(destinos.fiado).toMatchObject({ bloque: 'Ventas', falta: false });
    expect(destinos.gasto).toMatchObject({ bloque: 'Gastos', falta: true });
    expect(destinos.merma.tipoDeBloque).toBe('inventory');
    expect(pendientesDeLaHoja(HOJA_DEL_DIA, destinos).sinBloque).toEqual(['Gastos']);
  });

  it('con el bloque creado y cada fiado con su cliente, se puede cerrar', () => {
    const destinos = destinosDelCuaderno(crearAlmacenCargado(HOY), ['expenses']);
    const sinCliente: Renglon = {
      id: 'x',
      escrito: 'fiado 500',
      tipo: 'fiado',
      importe: 500,
      detalle: 'Fiado',
    };

    expect(pendientesDeLaHoja(HOJA_DEL_DIA, destinos).listo).toBe(true);
    expect(pendientesDeLaHoja([...HOJA_DEL_DIA, sinCliente], destinos)).toMatchObject({
      sinCliente: 1,
      listo: false,
    });
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
