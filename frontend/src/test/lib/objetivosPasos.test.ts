import { describe, expect, it } from 'vitest';
import { objetivoDeEjemplo } from '@/lib/objetivos';
import { negocioDe } from '@/lib/objetivosNegocio';
import {
  comparacionConZona,
  escenarioDe,
  gananciaExtraPorAumento,
  montoDe,
  pasosConcretos,
  ventasConAumento,
} from '@/lib/objetivosPasos';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { cuandoSeCalcula, entoncesElResultadoEs } from '@/test/calculo';
import type { ProductoParaPaso } from '@/types/objetivos';

const HOY = '2026-11-08';
const PRECIO_DE_ZONA = { minimo: 3500, promedio: 3900, maximo: 4300 };

const PRODUCTOS: ProductoParaPaso[] = [
  {
    tipo: 'SUBIR_PRECIO',
    nombre: 'Yerba Playadito 1 kg',
    precio: 3600,
    costo: 2700,
    stock: 4,
    vendidas30Dias: 60,
    precioZona: PRECIO_DE_ZONA,
  },
  {
    tipo: 'MOVER_PARADO',
    nombre: 'Cacao amargo',
    precio: 2500,
    costo: 1800,
    stock: 20,
    vendidas30Dias: 0,
    precioZona: null,
  },
  {
    tipo: 'PONER_A_LA_VISTA',
    nombre: 'Alfajor',
    precio: 1000,
    costo: 400,
    stock: 8,
    vendidas30Dias: 10,
    precioZona: null,
  },
];

const yerba = {
  nombre: 'Yerba',
  precio: 3600,
  costo: 2700,
  stock: 4,
  vendidas: 60,
  precioZona: null,
};

type Paso = ReturnType<typeof pasosConcretos>[number];

const pasos = () => pasosConcretos(PRODUCTOS);
const pasoDeSubirPrecio = (): Paso => pasos()[0] as Paso;
const pasoDeMoverParado = (): Paso => pasos()[1] as Paso;
const hastaDelAjuste = (paso: Paso) => paso.ajuste?.hasta ?? 0;

describe('pasos concretos de un objetivo', () => {
  it('sin secciones no hay nada que recomendar', () => {
    cuandoSeCalcula(() => pasosConcretos([]));

    entoncesElResultadoEs([]);
  });

  it('con el almacén cargado arma un paso por cada tipo de producto', () => {
    cuandoSeCalcula(() => pasos().map((paso) => paso.clave));

    entoncesElResultadoEs(['subir-precio', 'mover-parado', 'poner-a-la-vista']);
  });

  it('el primer paso habla de un producto de verdad', () => {
    cuandoSeCalcula(() => [
      pasoDeSubirPrecio().producto,
      pasoDeSubirPrecio().calcular(200, 'media').titulo,
    ]);

    entoncesElResultadoEs(['Yerba Playadito 1 kg', 'Subile $200 a Yerba Playadito 1 kg']);
  });

  it('mover lo parado no suma ganancia por mes', () => {
    cuandoSeCalcula(() => pasoDeMoverParado().calcular(300, 'media').sumaPorMes);

    entoncesElResultadoEs(undefined);
  });

  it('el monto del consejo se puede cambiar dentro de un rango', () => {
    cuandoSeCalcula(() => [
      pasoDeSubirPrecio().ajuste?.desde,
      hastaDelAjuste(pasoDeSubirPrecio()) > 300,
    ]);

    entoncesElResultadoEs([50, true]);
  });

  it('cada monto tiene su proyección: subir más suma más', () => {
    cuandoSeCalcula(() => {
      const doscientos = pasoDeSubirPrecio().calcular(200, 'media').sumaPorMes ?? 0;
      const trescientos = pasoDeSubirPrecio().calcular(300, 'media').sumaPorMes ?? 0;
      return [doscientos > 0, trescientos > doscientos];
    });

    entoncesElResultadoEs([true, true]);
  });

  it('sin subir el precio se vende lo mismo', () => {
    cuandoSeCalcula(() => ventasConAumento(yerba, 0, 'media'));

    entoncesElResultadoEs(60);
  });

  it('al subir mucho el precio se vende menos', () => {
    cuandoSeCalcula(() => ventasConAumento(yerba, 400, 'media') < 60);

    entoncesElResultadoEs(true);
  });

  it('un aumento moderado deja más ganancia', () => {
    cuandoSeCalcula(() => gananciaExtraPorAumento(yerba, 400, 'media') > 0);

    entoncesElResultadoEs(true);
  });

  it('un aumento enorme termina ganándose menos que uno moderado', () => {
    cuandoSeCalcula(
      () =>
        gananciaExtraPorAumento(yerba, 900, 'media') < gananciaExtraPorAumento(yerba, 450, 'media'),
    );

    entoncesElResultadoEs(true);
  });

  it('con clientes muy sensibles un aumento enorme hace perder plata', () => {
    cuandoSeCalcula(() => gananciaExtraPorAumento(yerba, 900, 'mucha') < 0);

    entoncesElResultadoEs(true);
  });

  it('el paso avisa cuando subir tanto hace ganar menos por mes', () => {
    const paso = pasoDeSubirPrecio();

    cuandoSeCalcula(() => paso.calcular(hastaDelAjuste(paso), 'mucha').efecto);

    entoncesElResultadoEs(expect.stringMatching(/menos por mes/));
  });

  it('cuanto más sensibles son los clientes, menos conviene subir', () => {
    const ajuste = pasoDeSubirPrecio().ajuste;

    cuandoSeCalcula(() => (ajuste?.sugerido('poca') ?? 0) > (ajuste?.sugerido('mucha') ?? 0));

    entoncesElResultadoEs(true);
  });

  it('se usa el monto sugerido si la persona no eligió otro', () => {
    const paso = pasoDeSubirPrecio();

    cuandoSeCalcula(() => montoDe(paso, {}, 'media'));

    entoncesElResultadoEs(paso.ajuste?.sugerido('media'));
  });

  it('se usa el monto que la persona eligió', () => {
    cuandoSeCalcula(() => montoDe(pasoDeSubirPrecio(), { 'subir-precio': 250 }, 'media'));

    entoncesElResultadoEs(250);
  });

  it('el sugerido es el que más ganancia deja', () => {
    cuandoSeCalcula(() => esElSugeridoElQueMasGananciaDeja(pasoDeSubirPrecio()));

    entoncesElResultadoEs(true);
  });

  it('bajar un precio para mover lo parado nunca lo deja por debajo del costo', () => {
    const paso = pasoDeMoverParado();

    cuandoSeCalcula(() => {
      const resultado = paso.calcular(hastaDelAjuste(paso), 'media');
      return [(resultado.precioNuevo ?? 0) > 0, (resultado.liberaPlata ?? 0) > 0];
    });

    entoncesElResultadoEs([true, true]);
  });

  it('no usa porcentajes ni jerga para explicar cada paso', () => {
    cuandoSeCalcula(() => pasos().map((paso) => textoDelPaso(paso)));

    entoncesNingunTextoUsaJerga();
  });

  it('el escenario sin pasos no cambia lo que se gana hoy', () => {
    cuandoSeCalcula(() => escenarioDe(objetivoDeEjemplo(), pasos(), new Set()));

    entoncesElEscenarioSinPasosQuedaIgualQueHoy();
  });

  it('el escenario suma el paso que se elige con su monto sugerido', () => {
    cuandoSeCalcula(() => {
      const hoy = escenarioDe(objetivoDeEjemplo(), pasos(), new Set()).hoy;
      return escenarioDe(objetivoDeEjemplo(), pasos(), new Set(['subir-precio'])).conPasos > hoy;
    });

    entoncesElResultadoEs(true);
  });

  it('el escenario respeta el monto más chico que se elige', () => {
    cuandoSeCalcula(() => {
      const sugerido = escenarioDe(objetivoDeEjemplo(), pasos(), new Set(['subir-precio']));
      const masChico = escenarioDe(objetivoDeEjemplo(), pasos(), new Set(['subir-precio']), {
        'subir-precio': 50,
      });
      return sugerido.conPasos > masChico.conPasos;
    });

    entoncesElResultadoEs(true);
  });

  it('mover lo parado libera plata pero no suma ganancia', () => {
    cuandoSeCalcula(() => {
      const hoy = escenarioDe(objetivoDeEjemplo(), pasos(), new Set()).hoy;
      const parado = escenarioDe(objetivoDeEjemplo(), pasos(), new Set(['mover-parado']));
      return [parado.conPasos === hoy, parado.liberada > 0];
    });

    entoncesElResultadoEs([true, true]);
  });

  it('compara el precio nuevo con lo que cobran en la zona', () => {
    cuandoSeCalcula(() => comparacionConZona(3800, PRECIO_DE_ZONA));

    entoncesElResultadoEs(
      expect.stringMatching(/entre \$3\.500 y \$4\.300.*dentro de lo que cobran/),
    );
  });

  it('avisa cuando el precio nuevo queda más caro que la zona', () => {
    cuandoSeCalcula(() => comparacionConZona(4600, PRECIO_DE_ZONA));

    entoncesElResultadoEs(expect.stringMatching(/más caro/));
  });

  it('calla si hay pocos datos de la zona', () => {
    cuandoSeCalcula(() => comparacionConZona(3800, null));

    entoncesElResultadoEs(null);
  });
});

describe('cómo viene el negocio', () => {
  it('cuenta lo vendido, lo que costó y lo que te deben en los últimos 30 días', () => {
    cuandoSeCalcula(() => negocioDe([crearAlmacenCargado(HOY)], HOY));

    entoncesElNegocioTieneVentasYLaCuentaCierra();
  });

  it('sin ventas no hay números', () => {
    cuandoSeCalcula(() => negocioDe([], HOY).hayVentas);

    entoncesElResultadoEs(false);
  });
});

function textoDelPaso(paso: Paso) {
  const { titulo, detalle, efecto } = paso.calcular(montoDe(paso, {}, 'media'), 'media');
  return `${titulo} ${detalle} ${efecto}`;
}

function esElSugeridoElQueMasGananciaDeja(paso: Paso) {
  const sugerido = montoDe(paso, {}, 'media');
  const curva = (monto: number) => paso.curva?.(monto, 'media') ?? 0;
  const montos: number[] = [];
  for (let monto = 50; monto <= hastaDelAjuste(paso); monto += 50) montos.push(monto);
  return montos.every((monto) => curva(monto) <= curva(sugerido) + 1e-6);
}

function entoncesNingunTextoUsaJerga() {
  const textos = pasos().map(textoDelPaso);
  for (const texto of textos) expect(texto).not.toMatch(/%|margen|elasticidad/i);
}

function entoncesElEscenarioSinPasosQuedaIgualQueHoy() {
  const objetivo = objetivoDeEjemplo();
  const ninguno = escenarioDe(objetivo, pasos(), new Set());
  expect(ninguno.conPasos).toBe(ninguno.hoy);
  expect(ninguno.faltaria).toBe(objetivo.meta - ninguno.hoy);
}

function entoncesElNegocioTieneVentasYLaCuentaCierra() {
  const negocio = negocioDe([crearAlmacenCargado(HOY)], HOY);
  expect(negocio.hayVentas).toBe(true);
  expect(negocio.queda).toBe(negocio.vendiste - negocio.mercaderia - negocio.gastos);
  expect(negocio.vendiste).toBeGreaterThan(negocio.mercaderia);
}
