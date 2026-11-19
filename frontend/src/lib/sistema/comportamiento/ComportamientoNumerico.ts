import { normalize } from '../core';
import { formatoPesos } from '../formatoDeValores';
import type { Campo, Valor } from '../tipos';
import { ComportamientoDeCampo } from './ComportamientoDeCampo';

export class ComportamientoNumerico extends ComportamientoDeCampo {
  coercionar(valor: Valor): Valor {
    return this.estaVacio(valor) ? (valor ?? '') : Number(valor);
  }

  interpretar(_campo: Campo, crudo: unknown): Valor {
    const numero = typeof crudo === 'number' ? crudo : Number(String(crudo).replace(',', '.'));
    return crudo !== '' && Number.isFinite(numero) ? numero : undefined;
  }

  protected valorPorDefecto(_campo: Campo, indice: number): Valor {
    return indice + 1;
  }

  protected errorDeTipo(_campo: Campo, valor: Valor): string | undefined {
    return Number.isFinite(Number(valor)) ? undefined : 'Ingresá un número válido.';
  }
}

export class ComportamientoDeMoneda extends ComportamientoNumerico {
  enTabla(_campo: Campo, valor: Valor): string {
    return formatoPesos(valor);
  }
}

export class ComportamientoDeSiNo extends ComportamientoDeCampo {
  private static readonly AFIRMATIVOS = ['si', 'true', '1', 'x'];

  falta(valor: Valor): boolean {
    return valor !== true;
  }

  ocupaTodoElAncho(): boolean {
    return true;
  }

  coercionar(valor: Valor): Valor {
    return Boolean(valor);
  }

  interpretar(_campo: Campo, crudo: unknown): Valor {
    return crudo === true || ComportamientoDeSiNo.AFIRMATIVOS.includes(normalize(crudo));
  }

  protected valorPorDefecto(): Valor {
    return false;
  }

  protected describir(_campo: Campo, valor: Valor): string {
    return valor ? 'Sí' : 'No';
  }
}
