import { describe, expect, it } from 'vitest';
import { sugerirProducto } from '@/lib/escaneo/catalogos';
import { elegirCamaraAutomatica } from '@/lib/escaneo/camaras';
import type { CoincidenciaCatalogo, ProductoDeCatalogo } from '@/types/escaneo';

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
  it('elige la descripción completa de cualquier fuente y recupera la foto de la otra', () => {
    const sepa = encontrado('sepa', {
      nombre: 'Alfajor de chocolate relleno con mousse',
      marca: 'Jorgito',
      presentacion: '45 g',
    });
    const off = encontrado('openfoodfacts', {
      nombre: 'Alfajor Jorgito',
      imagen: 'https://images.openfoodfacts.org/foto.jpg',
    });
    expect(sugerir(sepa, off)).toMatchObject({
      nombre: 'Alfajor de chocolate relleno con mousse · Jorgito · 45 g',
      fuenteDescripcion: 'sepa',
      fuenteImagen: 'openfoodfacts',
      imagen: off.producto?.imagen,
    });
    expect(
      sugerir(
        encontrado('sepa', { nombre: 'Alfajor' }),
        encontrado('openfoodfacts', { nombre: 'Alfajor Jorgito de chocolate con mousse' }),
      ).fuenteDescripcion,
    ).toBe('openfoodfacts');
  });
  it('evita repetir marca y presentación con espacios o acentos distintos', () => {
    expect(
      sugerir(
        encontrado('openfoodfacts', {
          nombre: 'Alfajor Guaymallén de oro 48g',
          marca: 'Guaymallen',
          presentacion: '48 g',
        }),
      ).nombre,
    ).toBe('Alfajor Guaymallén de oro 48g');
  });
  it('funciona con una sola coincidencia y no confunde caída con ausencia', () => {
    expect(
      sugerir(
        { fuente: 'sepa', estado: 'no_disponible' },
        encontrado('openfoodfacts', { nombre: 'Leche entera', marca: 'Yatasto' }),
      ).nombre,
    ).toBe('Leche entera · Yatasto');
    expect(
      sugerir(
        { fuente: 'sepa', estado: 'no_encontrado' },
        { fuente: 'openfoodfacts', estado: 'no_encontrado' },
      ),
    ).toMatchObject({ nombre: '', fuenteDescripcion: undefined, imagen: undefined });
  });
  it('descarta nombres genéricos y códigos solos, incluso cuando la ficha tiene foto', () => {
    expect(
      sugerir(
        encontrado('sepa', { nombre: '77903754' }),
        encontrado('openfoodfacts', {
          nombre: 'Producto',
          imagen: 'https://images.openfoodfacts.org/foto.jpg',
        }),
      ),
    ).toMatchObject({ nombre: '', fuenteDescripcion: undefined, fuenteImagen: 'openfoodfacts' });
  });
});

describe('selección de lente', () => {
  const camaras = [
    { id: 'ultra', nombre: 'camera 2, facing back ultra wide' },
    { id: 'frente', nombre: 'camera 1, facing front' },
    { id: 'principal', nombre: 'camera 0, facing back' },
  ];
  it('prioriza la trasera principal sobre lentes especiales y la frontal', () => {
    expect(elegirCamaraAutomatica(camaras)?.id).toBe('principal');
    expect(
      elegirCamaraAutomatica([
        { id: 'ultra', nombre: 'Back Ultra Wide Camera' },
        { id: 'normal', nombre: 'Back Camera' },
      ])?.id,
    ).toBe('normal');
  });
  it('una lectura automática aislada no desplaza a la principal y omite identificadores ausentes', () => {
    expect(elegirCamaraAutomatica(camaras, 'ultra')?.id).toBe('principal');
    expect(elegirCamaraAutomatica(camaras, 'ausente')?.id).toBe('principal');
  });
  it('la elección manual gana incluso sobre la principal y puede recuperarse por etiqueta', () => {
    expect(elegirCamaraAutomatica(camaras, 'principal', camaras[0])?.id).toBe('ultra');
    expect(
      elegirCamaraAutomatica(camaras, '', { id: 'anterior', nombre: 'camera 0, facing back' })?.id,
    ).toBe('principal');
  });
  it('deja decidir al navegador cuando no puede identificar una trasera', () => {
    expect(elegirCamaraAutomatica([{ id: 'a', nombre: 'Cámara 1' }])).toBeUndefined();
  });
});
