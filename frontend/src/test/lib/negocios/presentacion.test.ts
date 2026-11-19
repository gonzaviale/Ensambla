import { describe, it } from 'vitest';
import {
  colorDelNegocio,
  detalleDelNegocio,
  estadoDelNegocio,
  inicialDelNegocio,
  paraElegir,
  rubroLegible,
  textoDeAvisos,
} from '@/lib/negocios/presentacion';
import { cuandoSeCalcula, entoncesElResultadoEs } from '@/test/calculo';
import type { ResumenDeNegocio } from '@/types/negocios';

const KIOSCO: ResumenDeNegocio = {
  id: 'kiosco',
  nombre: 'Kiosco de la esquina',
  activo: false,
  hayDatos: true,
  vendiste: 280000,
  gastos: 52000,
  queda: -72000,
  teDeben: 15000,
  avisosPendientes: 2,
};

describe('estadoDelNegocio', () => {
  it('sin ventas en el período no lo juzga', () => {
    cuandoSeCalcula(() => estadoDelNegocio({ hayDatos: false, queda: null }));

    entoncesElResultadoEs('SIN_DATOS');
  });

  it('pide atención si lo que queda es negativo', () => {
    cuandoSeCalcula(() => estadoDelNegocio({ hayDatos: true, queda: -1 }));

    entoncesElResultadoEs('ATENCION');
  });

  it('viene bien si no queda en negativo', () => {
    cuandoSeCalcula(() => estadoDelNegocio({ hayDatos: true, queda: 0 }));

    entoncesElResultadoEs('BIEN');
  });

  it('viene bien si todavía no se sabe lo que queda', () => {
    cuandoSeCalcula(() => estadoDelNegocio({ hayDatos: true, queda: null }));

    entoncesElResultadoEs('BIEN');
  });
});

describe('paraElegir', () => {
  it('toma del resumen lo justo para elegir, sin montos', () => {
    cuandoSeCalcula(() => paraElegir(KIOSCO));

    entoncesElResultadoEs({
      id: 'kiosco',
      nombre: 'Kiosco de la esquina',
      avisosPendientes: 2,
      estado: 'ATENCION',
    });
  });
});

describe('textoDeAvisos', () => {
  it('dice que no hay avisos pendientes', () => {
    cuandoSeCalcula(() => textoDeAvisos(0));

    entoncesElResultadoEs('Sin avisos pendientes');
  });

  it('dice un aviso pendiente en singular', () => {
    cuandoSeCalcula(() => textoDeAvisos(1));

    entoncesElResultadoEs('1 aviso pendiente');
  });

  it('dice varios avisos pendientes en plural', () => {
    cuandoSeCalcula(() => textoDeAvisos(4));

    entoncesElResultadoEs('4 avisos pendientes');
  });
});

describe('rubroLegible', () => {
  it('pasa el código del rubro a un texto con mayúscula inicial', () => {
    cuandoSeCalcula(() => rubroLegible('KIOSCO'));

    entoncesElResultadoEs('Kiosco');
  });

  it('cambia los guiones bajos por espacios', () => {
    cuandoSeCalcula(() => rubroLegible('VENTA_ONLINE'));

    entoncesElResultadoEs('Venta online');
  });
});

describe('colorDelNegocio', () => {
  it('le da a cada negocio un color de la paleta según su lugar', () => {
    cuandoSeCalcula(() => [0, 1, 2].map(colorDelNegocio));

    entoncesElResultadoEs(['petrol', 'orange', 'coral']);
  });

  it('vuelve a empezar después del tercero', () => {
    cuandoSeCalcula(() => colorDelNegocio(3));

    entoncesElResultadoEs('petrol');
  });
});

describe('inicialDelNegocio', () => {
  it('usa la primera letra en mayúscula', () => {
    cuandoSeCalcula(() => inicialDelNegocio(' almacén'));

    entoncesElResultadoEs('A');
  });

  it('usa una N si no hay nombre', () => {
    cuandoSeCalcula(() => inicialDelNegocio(''));

    entoncesElResultadoEs('N');
  });
});

describe('detalleDelNegocio', () => {
  it('junta el rubro y las tablas', () => {
    cuandoSeCalcula(() => detalleDelNegocio('Kiosco', 4));

    entoncesElResultadoEs('Kiosco · 4 tablas');
  });

  it('sin rubro dice sólo las tablas', () => {
    cuandoSeCalcula(() => detalleDelNegocio(undefined, 1));

    entoncesElResultadoEs('1 tabla');
  });
});
