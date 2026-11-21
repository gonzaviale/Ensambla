import { describe, it } from 'vitest';
import type { TipoBloque, Valor } from '@/lib/sistema/tipos';
import {
  cuandoSeArmanDosSistemasConClientes,
  cuandoSeArmaUnSistemaConEjemplosDe,
  cuandoSeCalculanLosTotales,
  cuandoSeCargaUnProductoSinPrecio,
  cuandoSeCopiaElSistema,
  cuandoSeImportanYerbaRepetidaYAceite,
  cuandoSeIntentaPonerLosPedidosAntesDeSiMismos,
  cuandoSeMuevenLosPedidosALaOtraPestana,
  cuandoSeRenombraElPrimerProducto,
  cuandoSeValidaEn,
  cuandoSeVendeFiadoSinCliente,
  cuandoSeVuelvenAPonerLosPedidosAntesDeLosClientes,
  dadosClientesYPedidosDeEjemploYOtraPestana,
  dadoUnInventarioConYerba,
  dadoUnSistemaCon,
  entoncesCadaSistemaTieneSusPropiosClientes,
  entoncesElErrorDe,
  entoncesElPrecioQuedaVacio,
  entoncesElPrimerPedidoEsDelPrimerClienteYEsValido,
  entoncesElProductoEditadoYaNoEsEjemplo,
  entoncesHayUnErrorEn,
  entoncesLaCopiaEsIgualAlOriginal,
  entoncesLaFilaImportadaVieneDeLaLectura,
  entoncesLosPedidosEstanEnLaOtraPestanaConSusDatos,
  entoncesLosPedidosQuedanPrimeros,
  entoncesLosPedidosYLasVentasApuntanALosClientes,
  entoncesLosTotalesSon,
  entoncesNoHayErrores,
  entoncesNoSeMovio,
  entoncesSeAgregoUnaYSeOmitioLaRepetidaSinPisarla,
} from '@/test/motorDelSistema';

const NEGOCIO_COMPLETO: TipoBloque[] = ['orders', 'clients', 'inventory', 'sales'];
const PARA_VALIDAR: TipoBloque[] = ['inventory', 'clients', 'orders'];

type DatoInvalido = {
  descripcion: string;
  tipo: TipoBloque;
  valores: Record<string, Valor>;
  clave: string;
};

const DATOS_INVALIDOS: DatoInvalido[] = [
  {
    descripcion: 'un nombre vacío',
    tipo: 'inventory',
    valores: { name: '  ', quantity: '2' },
    clave: 'name',
  },
  {
    descripcion: 'una cantidad que no es un número',
    tipo: 'inventory',
    valores: { name: 'Yerba', quantity: 'cero' },
    clave: 'quantity',
  },
  {
    descripcion: 'un correo inválido',
    tipo: 'clients',
    valores: { name: 'Ana', email: 'incorrecto' },
    clave: 'email',
  },
  {
    descripcion: 'un cliente que no existe',
    tipo: 'orders',
    valores: { name: 'Pedido', customer: 'no-existe' },
    clave: 'customer',
  },
  {
    descripcion: 'un estado inventado',
    tipo: 'orders',
    valores: { name: 'Pedido', status: 'Inventado' },
    clave: 'status',
  },
];

describe('motor: creación del sistema', () => {
  it('vincula los pedidos y las ventas con los clientes del mismo sistema', () => {
    cuandoSeArmaUnSistemaConEjemplosDe(NEGOCIO_COMPLETO);

    entoncesLosPedidosYLasVentasApuntanALosClientes();
  });

  it('el primer pedido de ejemplo queda a nombre del primer cliente y es válido', () => {
    cuandoSeArmaUnSistemaConEjemplosDe(NEGOCIO_COMPLETO);

    entoncesElPrimerPedidoEsDelPrimerClienteYEsValido();
  });

  it('cada sistema tiene sus propios bloques', () => {
    cuandoSeArmanDosSistemasConClientes();

    entoncesCadaSistemaTieneSusPropiosClientes();
  });
});

describe('motor: validación', () => {
  it.each(DATOS_INVALIDOS)('rechaza $descripcion', ({ tipo, valores, clave }) => {
    dadoUnSistemaCon(PARA_VALIDAR);

    cuandoSeValidaEn(tipo, valores);

    entoncesHayUnErrorEn(clave);
  });

  it('acepta un producto con cantidad cero', () => {
    dadoUnSistemaCon(PARA_VALIDAR);

    cuandoSeValidaEn('inventory', { name: 'Yerba', quantity: 0 });

    entoncesNoHayErrores();
  });

  it('pide a quién se le fía cuando una venta es fiada', () => {
    dadoUnSistemaCon(['inventory', 'sales', 'clients'], true);

    cuandoSeVendeFiadoSinCliente();

    entoncesElErrorDe('customer', 'Elegí a quién le fiás.');
  });
});

describe('motor: registros', () => {
  it('un ejemplo editado deja de ser ejemplo', () => {
    dadoUnSistemaCon(['inventory'], true);

    cuandoSeRenombraElPrimerProducto();

    entoncesElProductoEditadoYaNoEsEjemplo();
  });

  it('un producto nuevo sin precio queda con el precio vacío', () => {
    dadoUnSistemaCon(['inventory'], true);

    cuandoSeCargaUnProductoSinPrecio();

    entoncesElPrecioQuedaVacio();
  });

  it('la carga revisada omite los repetidos sin pisar los datos', () => {
    dadoUnInventarioConYerba(12);

    cuandoSeImportanYerbaRepetidaYAceite();

    entoncesSeAgregoUnaYSeOmitioLaRepetidaSinPisarla(12);
  });

  it('lo importado queda marcado como lectura', () => {
    dadoUnInventarioConYerba(12);

    cuandoSeImportanYerbaRepetidaYAceite();

    entoncesLaFilaImportadaVieneDeLaLectura();
  });
});

describe('motor: bloques', () => {
  it('mover un bloque a otra pestaña conserva sus registros y vínculos', () => {
    dadosClientesYPedidosDeEjemploYOtraPestana();

    cuandoSeMuevenLosPedidosALaOtraPestana();

    entoncesLosPedidosEstanEnLaOtraPestanaConSusDatos();
  });

  it('un bloque movido se puede volver a poner antes de otro', () => {
    dadosClientesYPedidosDeEjemploYOtraPestana();

    cuandoSeVuelvenAPonerLosPedidosAntesDeLosClientes();

    entoncesLosPedidosQuedanPrimeros();
  });

  it('un bloque no se puede poner antes de sí mismo', () => {
    dadosClientesYPedidosDeEjemploYOtraPestana();

    cuandoSeIntentaPonerLosPedidosAntesDeSiMismos();

    entoncesNoSeMovio();
  });
});

describe('motor: totales y copias', () => {
  it('los totales suman el stock, los pedidos pendientes, los clientes y los ingresos', () => {
    dadoUnSistemaCon(['inventory', 'orders', 'clients'], true);

    cuandoSeCalculanLosTotales();

    entoncesLosTotalesSon({ stock: 72, pending: 2, clients: 3, income: 48700 });
  });

  it('una copia del sistema es igual al original', () => {
    dadoUnSistemaCon(['inventory', 'orders', 'clients'], true);

    cuandoSeCopiaElSistema();

    entoncesLaCopiaEsIgualAlOriginal();
  });
});
