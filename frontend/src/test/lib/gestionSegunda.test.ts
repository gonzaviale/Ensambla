import { describe, expect, it } from 'vitest';
import { faltantes, planDeCompras } from '@/lib/gestion/compras';
import { PEDIDOS_DE_HOY, type CategoriaMonotributo } from '@/lib/gestion/datosDeEjemplo';
import { conLoDeHoy, DIA_VACIO, pedidosDeOferta, renglonesDePedidos } from '@/lib/gestion/elDia';
import { categoriaPara, estadoMonotributo, facturaSimulada } from '@/lib/gestion/monotributo';
import {
  conDescuento,
  descuentoMaximo,
  descuentoSugerido,
  MAXIMO_DE_OFERTAS,
  ofertasPosibles,
} from '@/lib/gestion/ofertas';
import { armarPedido, respuestaAlCliente, type MensajeDePedido } from '@/lib/gestion/pedidos';
import { datosDelNegocio, type DatosDelNegocio } from '@/lib/gestion/resultado';
import { crearAlmacenCargado } from '@/lib/sistema/demo';

const HOY = '2026-11-05';

const almacen = () => datosDelNegocio(crearAlmacenCargado(HOY));

const negocio = (datos: Partial<DatosDelNegocio>): DatosDelNegocio => ({
  nombre: 'Prueba',
  productos: [],
  ventas: [],
  clientes: [],
  ...datos,
});

const mensaje = (lineas: MensajeDePedido['lineas']): MensajeDePedido => ({
  id: 'm1',
  cliente: 'Doña Rosa',
  hora: '10:00',
  texto: '',
  lineas,
  entrega: 'retira',
  pago: 'cobrado',
});

describe('los pedidos por WhatsApp', () => {
  it('arma el pedido contra el stock y marca lo que falta', () => {
    const datos = negocio({
      productos: [
        {
          id: 'p1',
          nombre: 'Fernet Branca 750 ml',
          stock: 5,
          precio: 9500,
          costo: 7200,
          minimo: 3,
        },
        {
          id: 'p2',
          nombre: 'Aceite Natura 900 ml',
          stock: 0,
          precio: 2600,
          costo: 1900,
          minimo: 6,
        },
      ],
    });

    const pedido = armarPedido(
      mensaje([
        { texto: '8 fernet', cantidad: 8, busca: 'fernet' },
        { texto: '1 aceite', cantidad: 1, busca: 'aceite' },
        { texto: '2 leches', cantidad: 2, busca: 'leche entera' },
      ]),
      datos,
    );

    expect(pedido.lineas.map((linea) => linea.estado)).toEqual(['poco', 'agotado', 'no-trabajas']);
    expect(pedido.lineas[0]?.entrega).toBe(5);
    expect(pedido.total).toBe(5 * 9500);
    expect(pedido.incompleto).toBe(true);
  });

  it('resuelve «la de siempre» con lo que más compró el cliente', () => {
    const datos = negocio({
      productos: [
        {
          id: 'y1',
          nombre: 'Yerba Playadito 1 kg',
          stock: 4,
          precio: 3600,
          costo: 2700,
          minimo: 0,
        },
        { id: 'y2', nombre: 'Yerba Taragüí 1 kg', stock: 4, precio: 3500, costo: 2600, minimo: 0 },
      ],
      clientes: [{ id: 'c1', nombre: 'Doña Rosa', telefono: '' }],
      ventas: [
        {
          productoId: 'y2',
          cantidad: 3,
          precio: 3500,
          fecha: '2026-10-01',
          clienteId: 'c1',
          fiado: false,
        },
        {
          productoId: 'y1',
          cantidad: 1,
          precio: 3600,
          fecha: '2026-10-02',
          clienteId: 'c1',
          fiado: false,
        },
      ],
    });

    const pedido = armarPedido(
      mensaje([{ texto: 'la yerba de siempre', cantidad: 1, busca: 'yerba', deSiempre: true }]),
      datos,
    );

    expect(pedido.lineas[0]?.producto?.id).toBe('y2');
  });

  it('la respuesta al cliente cuenta lo que se separa y lo que no hay', () => {
    const pedido = armarPedido(PEDIDOS_DE_HOY[0] as MensajeDePedido, almacen());

    const respuesta = respuestaAlCliente(pedido, 'Almacén de barrio');

    expect(respuesta).toContain('Yerba Playadito');
    expect(respuesta).toContain('2 leches no tengo');
  });
});

describe('las ofertas', () => {
  it('el descuento máximo deja la venta en cero y el sugerido es la mitad, de a 5 %', () => {
    const maximo = descuentoMaximo(1000, 600);

    expect(maximo).toBeGreaterThan(0.35);
    expect(maximo).toBeLessThan(0.4);
    expect(descuentoSugerido(maximo)).toBe(0.15);
    expect(descuentoSugerido(0.02)).toBe(0);
  });

  it('propone pocas ofertas, con ganancia y sin a quienes pidieron no recibirlas', () => {
    const ofertas = ofertasPosibles(almacen(), HOY, ['Doña Rosa']);

    expect(ofertas.length).toBeGreaterThan(0);
    expect(ofertas.length).toBeLessThanOrEqual(MAXIMO_DE_OFERTAS);
    for (const oferta of ofertas) {
      expect(oferta.dejaPorUnidad).toBeGreaterThan(0);
      expect(oferta.destinatarios.some((cliente) => cliente.nombre === 'Doña Rosa')).toBe(false);
    }
  });

  it('con un descuento más grande que el máximo, cada venta pierde plata', () => {
    const [oferta] = ofertasPosibles(almacen(), HOY);
    if (!oferta) throw new Error('El almacén de la demo tiene que tener algo para ofertar');

    expect(conDescuento(oferta, oferta.descuentoMaximo + 0.05).dejaPorUnidad).toBeLessThan(0);
  });
});

describe('lo que pasa de una pestaña a otra', () => {
  const yerba = {
    id: 'y1',
    nombre: 'Yerba Playadito 1 kg',
    stock: 4,
    precio: 3600,
    costo: 2700,
    minimo: 0,
  };
  const base = () =>
    negocio({
      productos: [yerba],
      clientes: [{ id: 'c1', nombre: 'Doña Rosa', telefono: '' }],
    });

  it('un pedido confirmado es una venta de hoy y descuenta stock', () => {
    const pedido = armarPedido(
      { ...mensaje([{ texto: '3 yerbas', cantidad: 3, busca: 'yerba' }]), pago: 'fiado' },
      base(),
    );

    const datos = conLoDeHoy(base(), HOY, { ...DIA_VACIO, confirmados: [pedido] });

    expect(datos.productos[0]?.stock).toBe(1);
    expect(datos.ventas).toEqual([
      { productoId: 'y1', cantidad: 3, precio: 3600, fecha: HOY, clienteId: 'c1', fiado: true },
    ]);
    expect(renglonesDePedidos([pedido])[0]).toMatchObject({
      tipo: 'fiado',
      importe: 3 * 3600,
      cliente: 'Doña Rosa',
      pedidoId: 'm1',
    });
  });

  it('un remito recibido suma stock y trae el costo nuevo; un precio nuevo queda puesto', () => {
    const cuaderno = almacen();
    const orden = planDeCompras(faltantes(cuaderno, HOY), 150000).ordenes.find((cada) =>
      cada.items.some((cosa) => cosa.producto.nombre.startsWith('Yerba')),
    );
    const item = orden?.items.find((cosa) => cosa.producto.nombre.startsWith('Yerba'));
    if (!orden || !item) throw new Error('La demo tiene que pedir yerba');

    const datos = conLoDeHoy(cuaderno, HOY, {
      confirmados: [],
      recibidos: [orden],
      precios: { [item.producto.id]: 4000 },
    });
    const despues = datos.productos.find((producto) => producto.id === item.producto.id);

    expect(despues?.stock).toBe(item.producto.stock + item.pedir);
    expect(despues?.costo).toBe(3150);
    expect(despues?.precio).toBe(4000);
  });

  it('las respuestas a una oferta son pedidos con el precio de la oferta', () => {
    const [oferta] = ofertasPosibles(almacen(), HOY);
    if (!oferta) throw new Error('El almacén de la demo tiene que tener algo para ofertar');

    const pedidos = pedidosDeOferta(oferta, '12:00');
    const [primero] = pedidos;
    if (!primero) throw new Error('Alguien tiene que responder');
    const armado = armarPedido(primero, almacen());

    expect(pedidos.every((pedido) => pedido.id.startsWith('oferta-'))).toBe(true);
    expect(primero.cliente).toBe(oferta.destinatarios[0]?.nombre);
    expect(armado.total).toBe((armado.lineas[0]?.entrega ?? 0) * oferta.precioOferta);
  });
});

describe('el monotributo', () => {
  const categorias: CategoriaMonotributo[] = [
    { letra: 'A', tope: 1000, cuota: 10 },
    { letra: 'B', tope: 2000, cuota: 20 },
  ];

  it('elige la primera categoría que alcanza', () => {
    expect(categoriaPara(1500, categorias)?.letra).toBe('B');
    expect(categoriaPara(2500, categorias)).toBeNull();
  });

  it('proyecta cuándo se pasa del tope y qué corresponde en las próximas recategorizaciones', () => {
    const estado = estadoMonotributo(almacen(), HOY);

    expect(estado.meses).toHaveLength(12);
    expect(estado.meses.filter((mes) => mes.delCuaderno)).toHaveLength(3);
    expect(estado.superaEn).not.toBeNull();
    expect(estado.recategorizaciones.map((item) => item.mes)).toEqual(['2027-01', '2027-07']);
    expect(estado.vencimiento).toEqual({ fecha: '2026-11-20', dias: 15 });
  });

  it('después del día 20 la cuota vence el mes que viene', () => {
    expect(estadoMonotributo(almacen(), '2026-11-25').vencimiento.fecha).toBe('2026-12-20');
  });

  it('arma una factura simulada con número y CAE', () => {
    const factura = facturaSimulada('Consumidor final', 5700, HOY, 42);

    expect(factura.numero).toBe('00003-00000042');
    expect(factura.cae).toHaveLength(14);
    expect(factura.venceCae).toBe('2026-11-15');
  });
});
