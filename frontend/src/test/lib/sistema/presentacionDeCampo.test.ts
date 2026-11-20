import { describe, it } from 'vitest';
import {
  cuandoSeMuestraEnLaVistaPrevia,
  cuandoSeMuestraEnUnaTabla,
  cuandoSePideLaClaseDeLaCelda,
  cuandoSePideLaMarcaDeLaColumna,
  cuandoSePreguntaSiOcupaTodoElAncho,
  dadoUnCampoDeTipo,
  dadoUnCampoVinculadoAUnaTablaConUnRegistro,
  dadoUnInventarioConElStockEn,
  dadoUnaTablaDeClientesConLaCantidad,
  entoncesLaCeldaEsDeStockBajo,
  entoncesLaCeldaNoTieneClase,
  entoncesNoOcupaTodoElAncho,
  entoncesNoTieneUnaVistaPropia,
  entoncesOcupaTodoElAncho,
  entoncesSeVe,
} from '@/test/presentacionDeCampo';

describe('cómo se ve un valor en una tabla', () => {
  it('el dinero se muestra en pesos', () => {
    dadoUnCampoDeTipo('currency');

    cuandoSeMuestraEnUnaTabla(1500);

    entoncesSeVe('$ 1.500');
  });

  it('la fecha se muestra como día, mes y año', () => {
    dadoUnCampoDeTipo('date');

    cuandoSeMuestraEnUnaTabla('2026-10-16');

    entoncesSeVe('16/10/2026');
  });

  it('un texto se muestra tal como se cargó', () => {
    dadoUnCampoDeTipo('text');

    cuandoSeMuestraEnUnaTabla('Harina');

    entoncesSeVe('Harina');
  });

  it('un vínculo se muestra con el nombre del registro al que apunta', () => {
    dadoUnCampoVinculadoAUnaTablaConUnRegistro('Marta', 'c1');

    cuandoSeMuestraEnUnaTabla('c1');

    entoncesSeVe('Marta');
  });

  it('un vínculo roto se avisa en la tabla', () => {
    dadoUnCampoVinculadoAUnaTablaConUnRegistro('Marta', 'c1');

    cuandoSeMuestraEnUnaTabla('otro');

    entoncesSeVe('Sin vínculo');
  });
});

describe('cómo se ve un valor en una vista previa', () => {
  it('un vínculo muestra el nombre del registro al que apunta', () => {
    dadoUnCampoVinculadoAUnaTablaConUnRegistro('Marta', 'c1');

    cuandoSeMuestraEnLaVistaPrevia('c1');

    entoncesSeVe('Marta');
  });

  it('un vínculo roto se muestra con un guion', () => {
    dadoUnCampoVinculadoAUnaTablaConUnRegistro('Marta', 'c1');

    cuandoSeMuestraEnLaVistaPrevia('otro');

    entoncesSeVe('—');
  });

  it('los demás tipos no tienen una vista previa propia', () => {
    dadoUnCampoDeTipo('currency');

    cuandoSeMuestraEnLaVistaPrevia(1500);

    entoncesNoTieneUnaVistaPropia();
  });
});

describe('el título de la columna', () => {
  it('un vínculo lleva una flecha', () => {
    dadoUnCampoDeTipo('relation');

    cuandoSePideLaMarcaDeLaColumna();

    entoncesSeVe(' ↗');
  });

  it('los demás tipos no llevan marca', () => {
    dadoUnCampoDeTipo('text');

    cuandoSePideLaMarcaDeLaColumna();

    entoncesSeVe('');
  });
});

describe('el ancho de un campo en el formulario', () => {
  it.each(['textarea', 'checkbox'] as const)('un campo %s ocupa todo el ancho', (tipo) => {
    dadoUnCampoDeTipo(tipo);

    cuandoSePreguntaSiOcupaTodoElAncho();

    entoncesOcupaTodoElAncho();
  });

  it.each(['text', 'number', 'select', 'relation'] as const)(
    'un campo %s comparte la fila con otro',
    (tipo) => {
      dadoUnCampoDeTipo(tipo);

      cuandoSePreguntaSiOcupaTodoElAncho();

      entoncesNoOcupaTodoElAncho();
    },
  );
});

describe('la celda de la cantidad del inventario', () => {
  it('se marca cuando el stock llegó al mínimo', () => {
    dadoUnInventarioConElStockEn(3, 5);

    cuandoSePideLaClaseDeLaCelda();

    entoncesLaCeldaEsDeStockBajo();
  });

  it('no se marca mientras el stock supera el mínimo', () => {
    dadoUnInventarioConElStockEn(9, 5);

    cuandoSePideLaClaseDeLaCelda();

    entoncesLaCeldaNoTieneClase();
  });

  it('no se marca en una tabla que no es de inventario', () => {
    dadoUnaTablaDeClientesConLaCantidad(1);

    cuandoSePideLaClaseDeLaCelda();

    entoncesLaCeldaNoTieneClase();
  });
});
