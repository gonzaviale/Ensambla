import { describe, expect, it } from 'vitest';
import type {
  FormatoDeImagen} from '@/lib/promociones/formatosDeImagen';
import {
  Cuadrado,
  FORMATOS_DE_IMAGEN,
  Historia,
  ListaDePrecios,
  formatoPara,
  partirEnRenglones,
  type Lienzo,
  type TarjetaDePromocion,
} from '@/lib/promociones/formatosDeImagen';
import type { Promocion } from '@/types/promociones';

/** Un lienzo que anota lo que se le pide: sin navegador se puede ver qué se dibujó. */
class LienzoFalso implements Lienzo {
  fillStyle: string | CanvasGradient | CanvasPattern = '';
  font = '';
  textAlign: CanvasTextAlign = 'left';
  textBaseline: CanvasTextBaseline = 'alphabetic';
  readonly rectangulos: { color: unknown; x: number; y: number; ancho: number; alto: number }[] = [];
  readonly textos: { texto: string; x: number; y: number; alineacion: CanvasTextAlign }[] = [];

  fillRect(x: number, y: number, ancho: number, alto: number) {
    this.rectangulos.push({ color: this.fillStyle, x, y, ancho, alto });
  }

  fillText(texto: string, x: number, y: number) {
    this.textos.push({ texto, x, y, alineacion: this.textAlign });
  }

  // Cada carácter mide 10: un renglón de 20 caracteres mide 200.
  measureText(texto: string) {
    return { width: texto.length * 10 };
  }

  get escrito() {
    return this.textos.map((anotado) => anotado.texto);
  }
}

const promocionCon = (cantidad: number, extra: Partial<Promocion> = {}): Promocion => ({
  clave: 'estrella',
  titulo: 'Tu producto estrella',
  gancho: 'Lo que más se vendió',
  productos: Array.from({ length: cantidad }, (_, posicion) => ({
    nombre: `Producto ${posicion + 1}`,
    precio: 1000 * (posicion + 1),
  })),
  ...extra,
});

const tarjetaCon = (promocion: Promocion): TarjetaDePromocion => ({ negocio: 'Almacén de Ana', promocion });

const dibujar = (formato: FormatoDeImagen, promocion: Promocion) => {
  const lienzo = new LienzoFalso();
  formato.dibujar(lienzo, tarjetaCon(promocion));
  return lienzo;
};

describe('los formatos de imagen', () => {
  describe.each([
    ['cuadrado', new Cuadrado(), 1080, 1080],
    ['historia', new Historia(), 1080, 1920],
    ['lista', new ListaDePrecios(), 1080, 1350],
  ])('el formato %s', (clave, formato, ancho, alto) => {
    it('conoce su clave y su tamaño', () => {
      expect(formato.clave).toBe(clave);
      expect(formato.ancho).toBe(ancho);
      expect(formato.alto).toBe(alto);
      expect(formato.nombre).not.toBe('');
    });

    it('pinta primero el fondo de toda la imagen y después el resto', () => {
      const lienzo = dibujar(formato, promocionCon(1));

      expect(lienzo.rectangulos[0]).toMatchObject({ x: 0, y: 0, ancho, alto });
      expect(lienzo.rectangulos.length).toBeGreaterThan(1);
    });

    it('escribe el título, el gancho, cada producto con su precio y el negocio', () => {
      const lienzo = dibujar(formato, promocionCon(2));

      expect(lienzo.escrito).toEqual(
        expect.arrayContaining([
          'Tu producto estrella',
          'Lo que más se vendió',
          'Producto 1',
          'Producto 2',
          'Almacén de Ana',
        ]),
      );
      expect(lienzo.escrito.some((texto) => /\$\s?1\.000/.test(texto))).toBe(true);
    });

    it('escribe el pie al final, centrado, con el nombre del negocio', () => {
      const lienzo = dibujar(formato, promocionCon(1));

      expect(lienzo.textos.at(-1)).toMatchObject({ texto: 'Almacén de Ana', x: ancho / 2, alineacion: 'center' });
    });
  });

  it('el cuadrado muestra cinco productos y cuenta cuántos más hay', () => {
    const lienzo = dibujar(new Cuadrado(), promocionCon(8));

    expect(lienzo.escrito).toContain('Producto 5');
    expect(lienzo.escrito).not.toContain('Producto 6');
    expect(lienzo.escrito).toContain('y 3 más en el local');
  });

  it('la lista de precios muestra hasta doce productos', () => {
    const lienzo = dibujar(new ListaDePrecios(), promocionCon(12));

    expect(lienzo.escrito).toContain('Producto 12');
    expect(lienzo.escrito.some((texto) => texto.includes('más en el local'))).toBe(false);
  });

  it('un título largo se parte en renglones', () => {
    const tituloLargo =
      'Una promoción con un título muy largo que no entra en una sola línea de la imagen porque ' +
      'tiene muchísimas más palabras de las que caben en el ancho disponible del cuadrado';

    const lienzo = dibujar(new Cuadrado(), promocionCon(1, { titulo: tituloLargo }));

    const renglonesDelTitulo = lienzo.escrito.filter((texto) => tituloLargo.includes(texto) && texto !== '');
    expect(renglonesDelTitulo.length).toBeGreaterThan(1);
    expect(renglonesDelTitulo.join(' ')).toBe(tituloLargo);
  });

  it('hay una instancia de cada formato', () => {
    expect(Object.keys(FORMATOS_DE_IMAGEN)).toEqual(['cuadrado', 'historia', 'lista']);
  });
});

describe('formatoPara', () => {
  it('cada canal tiene el formato que mejor le queda', () => {
    expect(formatoPara('instagram', promocionCon(1)).clave).toBe('cuadrado');
    expect(formatoPara('estado', promocionCon(1)).clave).toBe('historia');
    expect(formatoPara('clientes', promocionCon(1)).clave).toBe('cuadrado');
  });

  it('un canal desconocido usa el cuadrado', () => {
    expect(formatoPara('telegram', promocionCon(1)).clave).toBe('cuadrado');
  });

  it('la lista de precios siempre es la lista, en cualquier canal', () => {
    expect(formatoPara('estado', promocionCon(1, { clave: 'lista' })).clave).toBe('lista');
  });
});

describe('partirEnRenglones', () => {
  const lienzo = new LienzoFalso();

  it('junta las palabras que entran en el ancho', () => {
    expect(partirEnRenglones(lienzo, 'uno dos tres', 200)).toEqual(['uno dos tres']);
  });

  it('corta entre palabras cuando no entra más', () => {
    expect(partirEnRenglones(lienzo, 'uno dos tres cuatro', 100)).toEqual(['uno dos', 'tres', 'cuatro']);
  });

  it('una palabra más larga que el ancho queda sola en su renglón', () => {
    expect(partirEnRenglones(lienzo, 'a abecedario b', 50)).toEqual(['a', 'abecedario', 'b']);
  });

  it('un texto vacío no deja renglones y los espacios de más se ignoran', () => {
    expect(partirEnRenglones(lienzo, '', 100)).toEqual([]);
    expect(partirEnRenglones(lienzo, '  uno   dos ', 200)).toEqual(['uno dos']);
  });
});
