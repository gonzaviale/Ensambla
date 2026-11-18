/* eslint-disable @typescript-eslint/no-unused-vars -- los parámetros son la firma que las subclases sí usan */
import type { Campo, Sistema, Valor } from '../tipos';

export const LARGO_DE_VALOR = 200;

export abstract class ComportamientoDeCampo {
  protected abstract valorPorDefecto(campo: Campo, indice: number): Valor;

  esVinculo(): boolean {
    return false;
  }

  falta(valor: Valor): boolean {
    return valor == null || (typeof valor === 'string' && !valor.trim());
  }

  validar(campo: Campo, valor: Valor, sistema: Sistema): string | undefined {
    return this.errorDeTipo(campo, valor, sistema) ?? this.errorDeMinimo(campo, valor);
  }

  coercionar(valor: Valor): Valor {
    return valor ?? '';
  }

  interpretar(_campo: Campo, crudo: unknown): Valor {
    return String(crudo).slice(0, LARGO_DE_VALOR) || undefined;
  }

  valorDeEjemplo(campo: Campo, indice: number, muestra: Valor, _vinculos: string[]): Valor {
    return muestra ?? this.valorPorDefecto(campo, indice);
  }

  textoPlano(campo: Campo, valor: Valor, sistema: Sistema | undefined): string {
    return this.estaVacio(valor) ? '' : this.describir(campo, valor, sistema);
  }

  protected errorDeTipo(_campo: Campo, _valor: Valor, _sistema: Sistema): string | undefined {
    return undefined;
  }

  protected describir(_campo: Campo, valor: Valor, _sistema: Sistema | undefined): string {
    return String(valor);
  }

  protected estaVacio(valor: Valor): boolean {
    return valor == null || valor === '';
  }

  private errorDeMinimo(campo: Campo, valor: Valor): string | undefined {
    if (this.estaVacio(valor) || campo.min == null) return undefined;
    return Number(valor) < campo.min ? `El mínimo es ${campo.min}.` : undefined;
  }
}
