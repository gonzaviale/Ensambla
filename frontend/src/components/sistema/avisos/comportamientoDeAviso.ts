import { pesos, type Aviso, type TipoDeAviso } from '@/lib/sistema/alertas';

export type DatoDelAviso = { etiqueta: string; valor: string; tono?: 'bueno' | 'malo' };

type Frase = readonly [uno: string, varios: string];

const numero = (valor: unknown) => {
  const convertido = typeof valor === 'string' ? Number(valor) : valor;
  return typeof convertido === 'number' && Number.isFinite(convertido) ? convertido : undefined;
};

const datosDe = (aviso: Aviso) => aviso.datos ?? {};

/** Lo que cambia de un tipo de aviso a otro: cómo se titula un grupo, qué números muestra y qué propone. */
abstract class ComportamientoDeAviso {
  protected abstract readonly frase: Frase;
  protected readonly frasePorNivel: Partial<Record<number, Frase>> = {};

  titulo(cantidad: number, prioridad: number) {
    const [uno, varios] = this.frasePorNivel[prioridad] ?? this.frase;
    return `${cantidad} ${cantidad === 1 ? uno : varios}`;
  }

  datos(aviso: Aviso): DatoDelAviso[] {
    void aviso;
    return [];
  }

  propuesta(aviso: Aviso): string | undefined {
    return aviso.acciones[0]?.texto;
  }
}

class AvisoDeStock extends ComportamientoDeAviso {
  protected readonly frase: Frase = ['producto bajo el mínimo', 'productos bajo el mínimo'];
  protected override readonly frasePorNivel = {
    3: ['producto sin stock', 'productos sin stock'] as Frase,
  };

  override datos(aviso: Aviso): DatoDelAviso[] {
    const cantidad = numero(datosDe(aviso).cantidad);
    const minimo = numero(datosDe(aviso).minimo);
    if (cantidad === undefined) return [];
    const tuMinimo: DatoDelAviso[] = minimo
      ? [{ etiqueta: 'Tu mínimo', valor: `${minimo} u.` }]
      : [];
    return [{ etiqueta: 'Te quedan', valor: `${cantidad} u.`, tono: 'malo' }, ...tuMinimo];
  }

  override propuesta(aviso: Aviso) {
    const pedir = /(?:pedí|pedir) ([^.]+?)(?: para [^.]+)?\./.exec(aviso.detalle)?.[1];
    return pedir ? `Te conviene pedir ${pedir}` : super.propuesta(aviso);
  }
}

class AvisoDePrecioDeZona extends ComportamientoDeAviso {
  protected readonly frase: Frase = [
    'producto con precio distinto al de la zona',
    'productos con precio distinto al de la zona',
  ];
  protected override readonly frasePorNivel = {
    2: ['producto casi al costo', 'productos casi al costo'] as Frase,
  };
}

class AvisoEstancado extends ComportamientoDeAviso {
  protected readonly frase: Frase = [
    'producto caro que no se mueve',
    'productos caros que no se mueven',
  ];

  override datos(aviso: Aviso): DatoDelAviso[] {
    const precio = numero(datosDe(aviso).precio);
    const sugerido = numero(datosDe(aviso).sugerido);
    if (precio === undefined || sugerido === undefined) return [];
    const baja = this.porcentajeDeRebaja(precio, sugerido);
    const rebaja: DatoDelAviso[] = baja > 0 ? [{ etiqueta: 'Rebaja', valor: `${baja} %` }] : [];
    return [
      { etiqueta: 'Lo tenés a', valor: pesos(precio), tono: 'malo' },
      { etiqueta: 'Precio sugerido', valor: pesos(sugerido), tono: 'bueno' },
      ...rebaja,
    ];
  }

  override propuesta(aviso: Aviso) {
    const precio = numero(datosDe(aviso).precio);
    const sugerido = numero(datosDe(aviso).sugerido);
    if (precio === undefined || sugerido === undefined) return super.propuesta(aviso);
    return `Bajar el precio de ${pesos(precio)} a ${pesos(sugerido)}`;
  }

  private porcentajeDeRebaja(precio: number, sugerido: number) {
    return precio > 0 ? Math.round(((precio - sugerido) / precio) * 100) : 0;
  }
}

class AvisoMuyVendido extends ComportamientoDeAviso {
  protected readonly frase: Frase = [
    'producto muy vendido con precio bajo',
    'productos muy vendidos con precio bajo',
  ];
}

class AvisoDeCombo extends ComportamientoDeAviso {
  protected readonly frase: Frase = ['combo para armar', 'combos para armar'];
}

class AvisoDeConsumoDeZona extends ComportamientoDeAviso {
  protected readonly frase: Frase = [
    'producto de la zona que no tenés',
    'productos de la zona que no tenés',
  ];
}

class AvisoDeRevisarPrecio extends ComportamientoDeAviso {
  protected readonly frase: Frase = ['precio para revisar', 'precios para revisar'];

  override datos(aviso: Aviso): DatoDelAviso[] {
    const anterior = numero(datosDe(aviso).precioAnterior);
    const nuevo = numero(datosDe(aviso).precioNuevo);
    if (anterior === undefined || nuevo === undefined) return [];
    return [
      { etiqueta: 'Precio anterior', valor: pesos(anterior), tono: 'bueno' },
      { etiqueta: 'Precio actual', valor: pesos(nuevo), tono: 'malo' },
    ];
  }
}

const COMPORTAMIENTO_DE_AVISO: Record<TipoDeAviso, ComportamientoDeAviso> = {
  stock: new AvisoDeStock(),
  precio_zona: new AvisoDePrecioDeZona(),
  estancado: new AvisoEstancado(),
  muy_vendido: new AvisoMuyVendido(),
  combo: new AvisoDeCombo(),
  consumo_zona: new AvisoDeConsumoDeZona(),
  revisar_precio: new AvisoDeRevisarPrecio(),
};

export const comportamientoDeAviso = (tipo: TipoDeAviso) => COMPORTAMIENTO_DE_AVISO[tipo];
