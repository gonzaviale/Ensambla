import { formatoPesos } from '@/lib/sistema/formato';
import type { ProductoPromocionado, Promocion } from '@/types/promociones';

export type ClaveDeFormato = 'cuadrado' | 'historia' | 'lista';

/** Lo que se dibuja: el negocio que promociona y la promoción. */
export type TarjetaDePromocion = { negocio: string; promocion: Promocion };

/** Lo mínimo del canvas que usan los formatos; así se pueden probar sin un navegador. */
export interface Lienzo {
  fillStyle: string | CanvasGradient | CanvasPattern;
  font: string;
  textAlign: CanvasTextAlign;
  textBaseline: CanvasTextBaseline;
  fillRect(x: number, y: number, ancho: number, alto: number): void;
  fillText(texto: string, x: number, y: number, anchoMaximo?: number): void;
  measureText(texto: string): { width: number };
}

// Los colores del manual de marca (docs/marca.md).
const PETROLEO = '#00506A';
const NARANJA = '#EE8000';
const CREMA = '#FFF7EB';
const PIZARRA = '#263443';
const BLANCO = '#FFFFFF';

const MARGEN = 80;
const ALTO_DE_LA_FRANJA = 28;
const ALTO_DEL_PIE = 120;
const FUENTE = 'Plus Jakarta Sans, system-ui, sans-serif';

type OpcionesDeTexto = {
  tamano: number;
  color: string;
  peso?: '400' | '700' | '800';
  alineacion?: CanvasTextAlign;
};

/** Cuántos renglones de producto entran en cada formato y cómo se ve cada uno. */
type DisenoDeProductos = { y: number; tamano: number; separacion: number; maximo: number };

/**
 * Una imagen para compartir una promoción. La base fija el orden en que se dibuja (fondo, encabezado, cuerpo y pie) y
 * cada formato pone su tamaño, su encabezado y su cuerpo.
 */
export abstract class FormatoDeImagen {
  abstract readonly clave: ClaveDeFormato;
  abstract readonly nombre: string;
  abstract readonly ancho: number;
  abstract readonly alto: number;

  dibujar(lienzo: Lienzo, tarjeta: TarjetaDePromocion) {
    this.pintarFondo(lienzo);
    this.escribirEncabezado(lienzo, tarjeta);
    this.escribirCuerpo(lienzo, tarjeta);
    this.escribirPie(lienzo, tarjeta);
  }

  protected abstract escribirEncabezado(lienzo: Lienzo, tarjeta: TarjetaDePromocion): void;

  protected abstract escribirCuerpo(lienzo: Lienzo, tarjeta: TarjetaDePromocion): void;

  protected pintarFondo(lienzo: Lienzo) {
    lienzo.fillStyle = CREMA;
    lienzo.fillRect(0, 0, this.ancho, this.alto);
    lienzo.fillStyle = NARANJA;
    lienzo.fillRect(0, 0, this.ancho, ALTO_DE_LA_FRANJA);
  }

  protected escribirPie(lienzo: Lienzo, tarjeta: TarjetaDePromocion) {
    lienzo.fillStyle = PETROLEO;
    lienzo.fillRect(0, this.alto - ALTO_DEL_PIE, this.ancho, ALTO_DEL_PIE);
    this.escribir(lienzo, tarjeta.negocio, this.ancho / 2, this.alto - ALTO_DEL_PIE / 2 + 12, {
      tamano: 44,
      color: BLANCO,
      peso: '800',
      alineacion: 'center',
    });
  }

  /** El título (y el gancho debajo) a partir de `y`; devuelve dónde termina. */
  protected escribirTitulo(lienzo: Lienzo, promocion: Promocion, y: number, tamano: number): number {
    const alTitulo = this.escribirRenglones(lienzo, promocion.titulo, y, {
      tamano,
      color: PETROLEO,
      peso: '800',
    });
    return this.escribirRenglones(lienzo, promocion.gancho, alTitulo + 10, {
      tamano: Math.round(tamano * 0.5),
      color: PIZARRA,
    });
  }

  /** Un renglón por producto con el precio a la derecha; si no entran todos, dice cuántos más hay. */
  protected escribirProductos(lienzo: Lienzo, productos: ProductoPromocionado[], diseno: DisenoDeProductos) {
    const visibles = productos.slice(0, diseno.maximo);
    visibles.forEach((producto, posicion) => {
      const y = diseno.y + posicion * diseno.separacion;
      this.escribir(lienzo, producto.nombre, MARGEN, y, { tamano: diseno.tamano, color: PIZARRA });
      this.escribir(lienzo, formatoPesos(producto.precio), this.ancho - MARGEN, y, {
        tamano: diseno.tamano,
        color: PETROLEO,
        peso: '800',
        alineacion: 'right',
      });
    });
    const sobrantes = productos.length - visibles.length;
    if (sobrantes > 0) {
      const y = diseno.y + visibles.length * diseno.separacion;
      this.escribir(lienzo, `y ${sobrantes} más en el local`, MARGEN, y, { tamano: diseno.tamano, color: NARANJA });
    }
  }

  protected escribir(lienzo: Lienzo, texto: string, x: number, y: number, opciones: OpcionesDeTexto) {
    lienzo.fillStyle = opciones.color;
    lienzo.font = `${opciones.peso ?? '700'} ${opciones.tamano}px ${FUENTE}`;
    lienzo.textAlign = opciones.alineacion ?? 'left';
    lienzo.textBaseline = 'alphabetic';
    lienzo.fillText(texto, x, y);
  }

  /** Escribe el texto partido en renglones que entran en el ancho; devuelve el `y` del último renglón. */
  protected escribirRenglones(lienzo: Lienzo, texto: string, y: number, opciones: OpcionesDeTexto): number {
    lienzo.font = `${opciones.peso ?? '700'} ${opciones.tamano}px ${FUENTE}`;
    const renglones = partirEnRenglones(lienzo, texto, this.ancho - 2 * MARGEN);
    renglones.forEach((renglon, posicion) =>
      this.escribir(lienzo, renglon, MARGEN, y + posicion * opciones.tamano * 1.25, opciones),
    );
    return y + (renglones.length - 1) * opciones.tamano * 1.25 + opciones.tamano * 0.5;
  }
}

/** Parte el texto en renglones que no pasan del ancho, cortando entre palabras. */
export function partirEnRenglones(lienzo: Lienzo, texto: string, anchoMaximo: number): string[] {
  const renglones: string[] = [];
  let actual = '';
  texto
    .split(' ')
    .filter((palabra) => palabra !== '')
    .forEach((palabra) => {
      const candidato = actual === '' ? palabra : `${actual} ${palabra}`;
      if (actual !== '' && lienzo.measureText(candidato).width > anchoMaximo) {
        renglones.push(actual);
        actual = palabra;
      } else {
        actual = candidato;
      }
    });
  return actual === '' ? renglones : [...renglones, actual];
}

/** La publicación cuadrada de Instagram. */
export class Cuadrado extends FormatoDeImagen {
  readonly clave = 'cuadrado';
  readonly nombre = 'Cuadrada';
  readonly ancho = 1080;
  readonly alto = 1080;

  protected escribirEncabezado(lienzo: Lienzo, { promocion }: TarjetaDePromocion) {
    this.escribirTitulo(lienzo, promocion, 190, 76);
  }

  protected escribirCuerpo(lienzo: Lienzo, { promocion }: TarjetaDePromocion) {
    this.escribirProductos(lienzo, promocion.productos, { y: 520, tamano: 50, separacion: 90, maximo: 5 });
  }
}

/** La historia vertical de Instagram o el estado de WhatsApp. */
export class Historia extends FormatoDeImagen {
  readonly clave = 'historia';
  readonly nombre = 'Historia';
  readonly ancho = 1080;
  readonly alto = 1920;

  protected escribirEncabezado(lienzo: Lienzo, { promocion }: TarjetaDePromocion) {
    this.escribirTitulo(lienzo, promocion, 420, 96);
  }

  protected escribirCuerpo(lienzo: Lienzo, { promocion }: TarjetaDePromocion) {
    this.escribirProductos(lienzo, promocion.productos, { y: 940, tamano: 58, separacion: 120, maximo: 7 });
  }
}

/** La lista de precios para mandarles a los clientes. */
export class ListaDePrecios extends FormatoDeImagen {
  readonly clave = 'lista';
  readonly nombre = 'Lista de precios';
  readonly ancho = 1080;
  readonly alto = 1350;

  protected escribirEncabezado(lienzo: Lienzo, { promocion }: TarjetaDePromocion) {
    this.escribirTitulo(lienzo, promocion, 170, 72);
  }

  protected escribirCuerpo(lienzo: Lienzo, { promocion }: TarjetaDePromocion) {
    this.escribirProductos(lienzo, promocion.productos, { y: 400, tamano: 42, separacion: 72, maximo: 12 });
  }
}

export const FORMATOS_DE_IMAGEN: Record<ClaveDeFormato, FormatoDeImagen> = {
  cuadrado: new Cuadrado(),
  historia: new Historia(),
  lista: new ListaDePrecios(),
};

const FORMATO_DE_CADA_CANAL: Record<string, ClaveDeFormato> = {
  instagram: 'cuadrado',
  estado: 'historia',
  clientes: 'cuadrado',
};

/** El formato que mejor le queda a la promoción en ese canal; la lista de precios siempre es la lista. */
export function formatoPara(canal: string, promocion: Promocion): FormatoDeImagen {
  const clave = promocion.clave === 'lista' ? 'lista' : (FORMATO_DE_CADA_CANAL[canal] ?? 'cuadrado');
  return FORMATOS_DE_IMAGEN[clave];
}
