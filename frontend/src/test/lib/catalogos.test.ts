import { describe, it } from 'vitest';
import { sugerirProducto } from '@/lib/escaneo/catalogos';
import { elegirCamaraAutomatica } from '@/lib/escaneo/camaras';
import {
  cuandoSeCalcula,
  entoncesElResultadoCoincideCon,
  entoncesElResultadoEs,
  entoncesElResultadoNoExiste,
} from '@/test/calculo';
import type { CoincidenciaCatalogo, ProductoDeCatalogo } from '@/types/escaneo';

const IMAGEN_DE_OPEN_FOOD_FACTS = 'https://images.openfoodfacts.org/foto.jpg';

const CAMARAS = [
  { id: 'ultra', nombre: 'camera 2, facing back ultra wide' },
  { id: 'frente', nombre: 'camera 1, facing front' },
  { id: 'principal', nombre: 'camera 0, facing back' },
];

let sepa: CoincidenciaCatalogo;
let openFoodFacts: CoincidenciaCatalogo;

function encontrado(
  fuente: CoincidenciaCatalogo['fuente'],
  datos: Partial<ProductoDeCatalogo>,
): CoincidenciaCatalogo {
  return {
    fuente,
    estado: 'encontrado',
    producto: { codigo: '77903754', nombre: '', marca: '', presentacion: '', ...datos },
  };
}

const sugerir = (...fuentes: CoincidenciaCatalogo[]) =>
  sugerirProducto({ codigo: '77903754', fuentes });

describe('descripción unificada del producto', () => {
  it('elige la descripción completa de una fuente y recupera la foto de la otra', () => {
    dadoQueSepaDescribeElProductoYOpenFoodFactsTieneLaFoto();

    cuandoSeCalcula(() => sugerir(sepa, openFoodFacts));

    entoncesElResultadoCoincideCon({
      nombre: 'Alfajor de chocolate relleno con mousse · Jorgito · 45 g',
      fuenteDescripcion: 'sepa',
      fuenteImagen: 'openfoodfacts',
      imagen: IMAGEN_DE_OPEN_FOOD_FACTS,
    });
  });

  it('elige la descripción más completa aunque venga de la otra fuente', () => {
    dadoQueOpenFoodFactsDescribeMejorQueSepa();

    cuandoSeCalcula(() => sugerir(sepa, openFoodFacts));

    entoncesElResultadoCoincideCon({ fuenteDescripcion: 'openfoodfacts' });
  });

  it('evita repetir marca y presentación con espacios o acentos distintos', () => {
    cuandoSeCalcula(
      () =>
        sugerir(
          encontrado('openfoodfacts', {
            nombre: 'Alfajor Guaymallén de oro 48g',
            marca: 'Guaymallen',
            presentacion: '48 g',
          }),
        ).nombre,
    );

    entoncesElResultadoEs('Alfajor Guaymallén de oro 48g');
  });

  it('funciona con una sola coincidencia y no confunde caída con ausencia', () => {
    cuandoSeCalcula(
      () =>
        sugerir(
          { fuente: 'sepa', estado: 'no_disponible' },
          encontrado('openfoodfacts', { nombre: 'Leche entera', marca: 'Yatasto' }),
        ).nombre,
    );

    entoncesElResultadoEs('Leche entera · Yatasto');
  });

  it('sin coincidencias no sugiere nombre, fuente ni imagen', () => {
    cuandoSeCalcula(() =>
      sugerir(
        { fuente: 'sepa', estado: 'no_encontrado' },
        { fuente: 'openfoodfacts', estado: 'no_encontrado' },
      ),
    );

    entoncesElResultadoCoincideCon({ nombre: '', fuenteDescripcion: undefined, imagen: undefined });
  });

  it('descarta nombres genéricos y códigos solos, incluso cuando la ficha tiene foto', () => {
    cuandoSeCalcula(() =>
      sugerir(
        encontrado('sepa', { nombre: '77903754' }),
        encontrado('openfoodfacts', { nombre: 'Producto', imagen: IMAGEN_DE_OPEN_FOOD_FACTS }),
      ),
    );

    entoncesElResultadoCoincideCon({
      nombre: '',
      fuenteDescripcion: undefined,
      fuenteImagen: 'openfoodfacts',
    });
  });
});

describe('selección de lente', () => {
  it('prioriza la trasera principal sobre lentes especiales y la frontal', () => {
    cuandoSeCalcula(() => elegirCamaraAutomatica(CAMARAS)?.id);

    entoncesElResultadoEs('principal');
  });

  it('prefiere la trasera normal sobre la ultra gran angular', () => {
    cuandoSeCalcula(
      () =>
        elegirCamaraAutomatica([
          { id: 'ultra', nombre: 'Back Ultra Wide Camera' },
          { id: 'normal', nombre: 'Back Camera' },
        ])?.id,
    );

    entoncesElResultadoEs('normal');
  });

  it('una lectura automática aislada no desplaza a la principal', () => {
    cuandoSeCalcula(() => elegirCamaraAutomatica(CAMARAS, 'ultra')?.id);

    entoncesElResultadoEs('principal');
  });

  it('omite identificadores ausentes', () => {
    cuandoSeCalcula(() => elegirCamaraAutomatica(CAMARAS, 'ausente')?.id);

    entoncesElResultadoEs('principal');
  });

  it('la elección manual gana incluso sobre la principal', () => {
    cuandoSeCalcula(() => elegirCamaraAutomatica(CAMARAS, 'principal', CAMARAS[0])?.id);

    entoncesElResultadoEs('ultra');
  });

  it('la cámara elegida antes puede recuperarse por etiqueta', () => {
    cuandoSeCalcula(
      () =>
        elegirCamaraAutomatica(CAMARAS, '', { id: 'anterior', nombre: 'camera 0, facing back' })
          ?.id,
    );

    entoncesElResultadoEs('principal');
  });

  it('deja decidir al navegador cuando no puede identificar una trasera', () => {
    cuandoSeCalcula(() => elegirCamaraAutomatica([{ id: 'a', nombre: 'Cámara 1' }]));

    entoncesElResultadoNoExiste();
  });
});

function dadoQueSepaDescribeElProductoYOpenFoodFactsTieneLaFoto() {
  sepa = encontrado('sepa', {
    nombre: 'Alfajor de chocolate relleno con mousse',
    marca: 'Jorgito',
    presentacion: '45 g',
  });
  openFoodFacts = encontrado('openfoodfacts', {
    nombre: 'Alfajor Jorgito',
    imagen: IMAGEN_DE_OPEN_FOOD_FACTS,
  });
}

function dadoQueOpenFoodFactsDescribeMejorQueSepa() {
  sepa = encontrado('sepa', { nombre: 'Alfajor' });
  openFoodFacts = encontrado('openfoodfacts', {
    nombre: 'Alfajor Jorgito de chocolate con mousse',
  });
}
