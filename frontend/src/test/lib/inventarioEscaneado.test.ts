import { describe, expect, it } from 'vitest';
import { allBlocks, clone, makeTemplate } from '@/lib/sistema/core';
import { codigoGtin, identidadGtin } from '@/lib/escaneo/codigo';
import {
  buscarProductoPorCodigo,
  guardarProductoEscaneado,
  prepararProducto,
  sumarStockProductoEscaneado,
} from '@/lib/escaneo/inventario';
import {
  cuandoSeCalcula,
  cuandoSeIntentaCalcular,
  entoncesElCalculoFalla,
  entoncesElResultadoEs,
} from '@/test/calculo';
import type { Bloque, Fila, Sistema } from '@/lib/sistema/tipos';

type Guardado = ReturnType<typeof guardarProductoEscaneado>;
type Borrador = ReturnType<typeof prepararProducto>;

const CODIGO_NUEVO = '3017620422003';

let sistema: Sistema;
let inventario: Bloque;
let borrador: Borrador;
let guardado: Guardado;
let copiaDelSistema: Sistema;

function requerido<T>(dato: T | undefined): T {
  if (dato === undefined) throw new Error('Falta un dato del escenario');
  return dato;
}

const alfajor = (): Fila => requerido(inventario.rows[0]);
const filasDelInventarioDe = (resultado: Guardado) =>
  allBlocks(requerido(resultado.sistema)).find((bloque) => bloque.id === inventario.id)?.rows;

describe('alta por código en el inventario existente', () => {
  it('valida un GTIN y le quita los espacios', () => {
    cuandoSeCalcula(() => codigoGtin(' 77903754 '));

    entoncesElResultadoEs('77903754');
  });

  it('conserva las equivalencias con ceros a la izquierda', () => {
    cuandoSeCalcula(() => identidadGtin('77903754') === identidadGtin('0000077903754'));

    entoncesElResultadoEs(true);
  });

  it.each(['77903755', '00000000', '7790a754'])('rechaza el código inválido %s', (codigo) => {
    cuandoSeIntentaCalcular(() => codigoGtin(codigo));

    entoncesElCalculoFalla();
  });

  it('guarda costos, precios y campos propios del producto escaneado', () => {
    dadoUnComercioConInventario();
    dadoUnCampoPropioObligatorio();
    dadoUnBorradorCompletoDeUnProductoNuevo();

    cuandoSeGuardaElBorrador();

    entoncesSeGuardoElProductoConTodosSusDatos();
  });

  it('el alta no crea ventas ni movimientos ni modifica el inventario original', () => {
    dadoUnComercioConInventario();
    dadoUnCampoPropioObligatorio();
    dadoUnBorradorCompletoDeUnProductoNuevo();

    cuandoSeGuardaElBorrador();

    entoncesNoSeCrearonVentasNiMovimientosNiSeTocoElOriginal();
  });

  it('exige un conteo explícito: no inventa stock', () => {
    dadoUnComercioConInventario();
    dadoUnBorradorDeUnProductoNuevoSinConteo();

    cuandoSeGuardaElBorrador();

    entoncesElErrorEn('quantity');
  });

  it('acepta un conteo cero explícito y precios opcionales', () => {
    dadoUnComercioConInventario();
    dadoUnBorradorDeUnProductoNuevoSinConteo();
    dadoUnConteoDe('0');

    cuandoSeGuardaElBorrador();

    entoncesElProductoGuardadoCoincideCon({ quantity: 0, price: '', cost: '' });
  });

  it('rechaza un duplicado equivalente', () => {
    dadoUnComercioConInventario();
    dadoUnBorradorParaElCodigo('0000077903754');

    cuandoSeGuardaElBorrador();

    entoncesElErrorDelFormularioDice(/ya está cargado/);
  });

  it('encuentra el producto equivalente y no duplica el inventario', () => {
    dadoUnComercioConInventario();
    dadoUnBorradorParaElCodigo('0000077903754');

    cuandoSeCalcula(() => buscarProductoPorCodigo(inventario, '0000077903754')?.id);

    entoncesElResultadoEs('alfajor');
    entoncesElInventarioTieneUnProducto();
  });

  it('vincula un producto sin código y conserva su cantidad actual si otro flujo la modificó', () => {
    dadoUnComercioConInventario();
    dadoUnProductoSinCodigoEnRevisionConCostoDe('350');
    dadoQueOtroFlujoCambioLaCantidadA(6);

    cuandoSeGuardaElBorrador();

    entoncesElProductoGuardadoCoincideCon({
      id: 'alfajor',
      barcode: '77903754',
      quantity: 6,
      cost: 350,
      price: 500.25,
    });
    entoncesElInventarioGuardadoTieneUnProducto();
  });

  it('un cambio concurrente en un dato editado requiere revisar', () => {
    dadoUnComercioConInventario();
    dadoUnBorradorQueEditaCantidadYPrecioDelAlfajor();
    dadoQueOtroFlujoCambioLaCantidadA(8);

    cuandoSeGuardaElBorrador();

    entoncesElErrorEnDice('quantity', /cambió/);
  });

  it('un cambio concurrente no aplica cambios parciales', () => {
    dadoUnComercioConInventario();
    dadoUnBorradorQueEditaCantidadYPrecioDelAlfajor();
    dadoQueOtroFlujoCambioLaCantidadA(8);
    dadoUnaCopiaDelSistema();

    cuandoSeGuardaElBorrador();

    entoncesElSistemaNoCambio();
  });

  it('respeta los campos obligatorios', () => {
    dadoUnComercioConInventario();
    dadoUnCampoObligatorioDeRubro();
    dadoUnBorradorSinElRubro();

    cuandoSeGuardaElBorrador();

    entoncesElErrorEn('rubro');
  });

  it.each(['quantity', 'cost', 'price', 'minimum'])('rechaza %s negativo', (clave) => {
    dadoUnComercioConInventario();
    dadoUnCampoObligatorioDeRubro();
    dadoUnBorradorValidoConElRubro();
    dadoQueElValorDeEsNegativo(clave);

    cuandoSeGuardaElBorrador();

    entoncesElErrorEn(clave);
    entoncesElInventarioTieneUnProducto();
  });

  it('no reemplaza el código de un producto que ya tiene otro', () => {
    dadoUnComercioConInventario();
    dadoUnBorradorParaElCodigoDelProducto(CODIGO_NUEVO);

    cuandoSeGuardaElBorrador();

    entoncesElErrorDelFormularioDice(/otro código/);
  });

  it('no usa una tabla que no existe', () => {
    dadoUnComercioConInventario();
    dadoUnBorradorParaElCodigoDelProducto(CODIGO_NUEVO);

    cuandoSeGuardaEnLaTabla('ausente');

    entoncesElErrorDelFormularioDice(/no está disponible/);
  });

  it('no usa una tabla cuyo código no es de texto', () => {
    dadoUnComercioConInventario();
    dadoUnCampoDeCodigoNumerico();
    dadoUnBorradorParaElCodigo(CODIGO_NUEVO);

    cuandoSeGuardaElBorrador();

    entoncesElErrorDelFormularioDice(/de texto/);
  });
});

describe('reposición de un producto reconocido', () => {
  it('suma las unidades al stock vigente sin duplicar el producto', () => {
    dadoUnComercioConInventario();
    dadoUnAlfajorReconocidoConStockDe(6);

    cuandoSeSumanUnidades('3');

    entoncesElStockQuedaEnYSinDuplicarse(9);
  });

  it('sumar no modifica otros datos del sistema', () => {
    dadoUnComercioConInventario();
    dadoUnAlfajorReconocidoConStockDe(6);
    dadoUnaCopiaDelSistema();

    cuandoSeSumanUnidades('3');

    entoncesElSistemaNoCambio();
  });

  it('admite cantidades decimales positivas sin reemplazar el stock actual', () => {
    dadoUnComercioConInventario();

    cuandoSeSumanUnidades('0.5');

    entoncesElProductoGuardadoCoincideCon({ quantity: 10.5 });
  });

  it.each([
    '',
    ' ',
    '0',
    '-2',
    'NaN',
    'Infinity',
    '1e309',
    String(Number.MAX_SAFE_INTEGER),
    String(Number.MIN_VALUE),
  ])('rechaza una cantidad inválida %j sin alterar el inventario', (cantidad) => {
    dadoUnComercioConInventario();
    dadoUnaCopiaDelSistema();

    cuandoSeSumanUnidades(cantidad);

    entoncesElErrorEn('quantity');
    entoncesElSistemaNoCambio();
  });

  it('impide reponer un producto eliminado', () => {
    dadoUnComercioConInventario();

    cuandoSeReponeElProducto('eliminado', '77903754');

    entoncesElErrorDelFormularioDice(/eliminado/);
  });

  it('impide reponer un producto cuyo código cambió', () => {
    dadoUnComercioConInventario();

    cuandoSeReponeElProducto('alfajor', CODIGO_NUEVO);

    entoncesElErrorDelFormularioDice(/código.*cambió/);
  });

  it('un producto que no se pudo reponer conserva su stock', () => {
    dadoUnComercioConInventario();

    cuandoSeReponeElProducto('eliminado', '77903754');

    entoncesElStockDelAlfajorEs(10);
  });
});

// --- dado ---

function dadoUnComercioConInventario() {
  sistema = makeTemplate('shop', { movements: true }, false);
  inventario = requerido(allBlocks(sistema).find((bloque) => bloque.type === 'inventory'));
  inventario.rows = [
    {
      id: 'alfajor',
      name: 'Guaymallén',
      barcode: '77903754',
      quantity: 10,
      price: 500.25,
      cost: 300,
      minimum: 2,
    },
  ];
}

function dadoUnCampoPropioObligatorio() {
  inventario.fields.push({
    key: 'categoria',
    label: 'Categoría',
    type: 'select',
    required: true,
    options: ['Kiosco'],
  });
}

function dadoUnCampoObligatorioDeRubro() {
  inventario.fields.push({ key: 'rubro', label: 'Rubro', type: 'text', required: true });
}

function dadoUnCampoDeCodigoNumerico() {
  inventario.fields = inventario.fields.map((campo) =>
    campo.key === 'barcode' ? { ...campo, label: 'Código', type: 'number' } : campo,
  );
}

function dadoUnBorradorCompletoDeUnProductoNuevo() {
  borrador = prepararProducto(inventario, CODIGO_NUEVO);
  borrador.valores = {
    ...borrador.valores,
    name: 'Nuevo producto',
    quantity: '12',
    price: '850.25',
    cost: '500.10',
    minimum: '3',
    categoria: 'Kiosco',
  };
}

function dadoUnBorradorDeUnProductoNuevoSinConteo() {
  borrador = prepararProducto(inventario, CODIGO_NUEVO);
  borrador.valores.name = 'Nuevo';
}

function dadoUnConteoDe(cantidad: string) {
  borrador.valores.quantity = cantidad;
}

function dadoUnBorradorParaElCodigo(codigo: string) {
  borrador = prepararProducto(inventario, codigo);
}

function dadoUnBorradorParaElCodigoDelProducto(codigo: string) {
  borrador = prepararProducto(inventario, codigo, alfajor());
}

function dadoUnProductoSinCodigoEnRevisionConCostoDe(costo: string) {
  const producto = alfajor();
  delete producto.barcode;
  borrador = prepararProducto(inventario, '77903754', producto);
  borrador.valores.cost = costo;
}

function dadoUnBorradorQueEditaCantidadYPrecioDelAlfajor() {
  borrador = prepararProducto(inventario, '77903754', alfajor());
  borrador.valores.quantity = '15';
  borrador.valores.price = '900';
}

function dadoQueOtroFlujoCambioLaCantidadA(cantidad: number) {
  alfajor().quantity = cantidad;
}

function dadoUnBorradorSinElRubro() {
  borrador = prepararProducto(inventario, CODIGO_NUEVO);
  borrador.valores = { name: 'Nuevo', quantity: '2', price: '100', cost: '50' };
}

function dadoUnBorradorValidoConElRubro() {
  dadoUnBorradorSinElRubro();
  borrador.valores.rubro = 'Alimentos';
}

function dadoQueElValorDeEsNegativo(clave: string) {
  borrador = { ...borrador, valores: { ...borrador.valores, [clave]: '-1' } };
}

function dadoUnAlfajorReconocidoConStockDe(cantidad: number) {
  const producto = alfajor();
  producto.barcode = '0000077903754';
  producto.quantity = cantidad;
  producto.cost = 350;
}

function dadoUnaCopiaDelSistema() {
  copiaDelSistema = clone(sistema);
}

// --- cuando ---

function cuandoSeGuardaElBorrador() {
  guardado = guardarProductoEscaneado(sistema, inventario.id, borrador);
}

function cuandoSeGuardaEnLaTabla(id: string) {
  guardado = guardarProductoEscaneado(sistema, id, borrador);
}

function cuandoSeSumanUnidades(cantidad: string) {
  cuandoSeReponeElProducto('alfajor', '77903754', cantidad);
}

function cuandoSeReponeElProducto(productoId: string, codigo: string, cantidad = '2') {
  guardado = sumarStockProductoEscaneado(sistema, inventario.id, productoId, codigo, cantidad);
}

// --- entonces ---

function entoncesSeGuardoElProductoConTodosSusDatos() {
  expect(guardado.errores).toBeUndefined();
  expect(guardado.producto).toMatchObject({
    name: 'Nuevo producto',
    barcode: CODIGO_NUEVO,
    quantity: 12,
    price: 850.25,
    cost: 500.1,
    minimum: 3,
    categoria: 'Kiosco',
  });
}

function entoncesNoSeCrearonVentasNiMovimientosNiSeTocoElOriginal() {
  expect(inventario.rows).toHaveLength(1);
  expect(inventario.fields).toHaveLength(10);
  const bloquesDeMovimientos = allBlocks(requerido(guardado.sistema)).filter((bloque) =>
    ['sales', 'entries'].includes(bloque.type),
  );
  expect(bloquesDeMovimientos.every((bloque) => bloque.rows.length === 0)).toBe(true);
}

function entoncesElErrorEn(clave: string) {
  expect(guardado.errores?.[clave]).toBeTruthy();
}

function entoncesElErrorEnDice(clave: string, esperado: RegExp) {
  expect(guardado.errores?.[clave]).toMatch(esperado);
}

function entoncesElErrorDelFormularioDice(esperado: RegExp) {
  expect(guardado.errores?._form).toMatch(esperado);
}

function entoncesElProductoGuardadoCoincideCon(esperado: object) {
  expect(guardado.producto).toMatchObject(esperado);
}

function entoncesElInventarioTieneUnProducto() {
  expect(inventario.rows).toHaveLength(1);
}

function entoncesElInventarioGuardadoTieneUnProducto() {
  expect(filasDelInventarioDe(guardado)).toHaveLength(1);
}

function entoncesElSistemaNoCambio() {
  expect(sistema).toEqual(copiaDelSistema);
}

function entoncesElStockQuedaEnYSinDuplicarse(cantidad: number) {
  expect(guardado.errores).toBeUndefined();
  expect(guardado.producto).toMatchObject({ ...alfajor(), quantity: cantidad });
  expect(filasDelInventarioDe(guardado)).toHaveLength(1);
}

function entoncesElStockDelAlfajorEs(cantidad: number) {
  expect(inventario.rows[0]?.quantity).toBe(cantidad);
}
