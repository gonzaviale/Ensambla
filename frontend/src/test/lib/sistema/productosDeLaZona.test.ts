import { describe, it } from 'vitest';
import {
  cuandoSeBuscaDondeSumar,
  cuandoSeBuscaEnLaZona,
  cuandoSeCalculaElPorcentaje,
  cuandoSeCalculaElPrecioParaSumar,
  cuandoSeEscribeLaFraseDeComercios,
  cuandoSeListanLosNombresDeInventario,
  cuandoSeOrdenanPorLoMasVendido,
  cuandoSePreguntaSiYaLoTiene,
  dadoUnProductoDeZona,
  dadoUnSistemaConInventarioDe,
  dadoUnSistemaSinInventario,
  dadoUnaZonaCon,
  entoncesNoHayDondeSumar,
  entoncesSale,
  entoncesSeSumaAlPrimerInventarioDe,
} from '@/test/productosDeLaZona';

describe('Los productos de la zona que se buscan', () => {
  it('se buscan sin distinguir mayúsculas ni tildes', () => {
    const zona = dadoUnaZonaCon([dadoUnProductoDeZona('Azúcar'), dadoUnProductoDeZona('Yerba')]);
    const sistema = dadoUnSistemaSinInventario();

    const nombres = cuandoSeBuscaEnLaZona(zona, sistema, 'AZUCAR');

    entoncesSale(nombres, ['Azúcar']);
  });

  it('muestran primero lo que ya tenés y después el resto por nombre', () => {
    const zona = dadoUnaZonaCon([
      dadoUnProductoDeZona('Arroz'),
      dadoUnProductoDeZona('Fideos'),
      dadoUnProductoDeZona('Yerba'),
    ]);
    const sistema = dadoUnSistemaConInventarioDe(['Yerba']);

    const nombres = cuandoSeBuscaEnLaZona(zona, sistema, '');

    entoncesSale(nombres, ['Yerba', 'Arroz', 'Fideos']);
  });
});

describe('Lo que se vende cerca', () => {
  it('muestra primero lo que más comercios venden y desempata por unidades', () => {
    const productos = [
      dadoUnProductoDeZona('Sal', 3, 10),
      dadoUnProductoDeZona('Yerba', 8, 5),
      dadoUnProductoDeZona('Arroz', 3, 20),
    ];

    const nombres = cuandoSeOrdenanPorLoMasVendido(productos);

    entoncesSale(nombres, ['Yerba', 'Arroz', 'Sal']);
  });

  it('el porcentaje es la parte de los comercios de la zona que lo venden', () => {
    const zona = dadoUnaZonaCon([], 8);
    const producto = dadoUnProductoDeZona('Yerba', 6);

    const porcentaje = cuandoSeCalculaElPorcentaje(producto, zona);

    entoncesSale(porcentaje, 75);
  });

  it('se suma con el precio promedio redondeado a la decena', () => {
    const producto = dadoUnProductoDeZona('Yerba', 5, 10, 1234);

    const precio = cuandoSeCalculaElPrecioParaSumar(producto);

    entoncesSale(precio, 1230);
  });
});

describe('Dónde se suman los productos nuevos', () => {
  it('van al primer sistema que tenga un inventario', () => {
    const sinInventario = dadoUnSistemaSinInventario();
    const conInventario = dadoUnSistemaConInventarioDe([]);

    const destino = cuandoSeBuscaDondeSumar([sinInventario, conInventario]);

    entoncesSeSumaAlPrimerInventarioDe(destino, conInventario);
  });

  it('sin ningún inventario no hay dónde sumarlos', () => {
    const sistemas = [dadoUnSistemaSinInventario()];

    const destino = cuandoSeBuscaDondeSumar(sistemas);

    entoncesNoHayDondeSumar(destino);
  });

  it('un producto con nombre parecido a uno del inventario ya lo tenés', () => {
    const nombres = cuandoSeListanLosNombresDeInventario([dadoUnSistemaConInventarioDe(['Yerba'])]);

    const loTiene = cuandoSePreguntaSiYaLoTiene(dadoUnProductoDeZona('Yerba'), nombres);

    entoncesSale(loTiene, true);
  });

  it('un producto que no está en el inventario no lo tenés', () => {
    const nombres = cuandoSeListanLosNombresDeInventario([dadoUnSistemaConInventarioDe(['Yerba'])]);

    const loTiene = cuandoSePreguntaSiYaLoTiene(dadoUnProductoDeZona('Fideos'), nombres);

    entoncesSale(loTiene, false);
  });
});

describe('Cuántos comercios hay en la zona', () => {
  it.each([
    [0, 'No encontramos comercios en esta zona.'],
    [1, 'Hay 1 comercio en esta zona.'],
    [2, 'Hay 2 comercios en esta zona.'],
  ])('con %s se dice «%s»', (cantidad, frase) => {
    const texto = cuandoSeEscribeLaFraseDeComercios(cantidad);

    entoncesSale(texto, frase);
  });
});
