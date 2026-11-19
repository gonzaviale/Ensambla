import { findBlock, normalize } from '../core';
import type { Campo, Sistema, Valor } from '../tipos';
import { ComportamientoDeCampo } from './ComportamientoDeCampo';

const SIN_VALOR = '—';

export class ComportamientoDeSeleccion extends ComportamientoDeCampo {
  interpretar(campo: Campo, crudo: unknown): Valor {
    return campo.options?.find((opcion) => normalize(opcion) === normalize(crudo));
  }

  protected valorPorDefecto(campo: Campo): Valor {
    return campo.options?.[0] ?? '';
  }

  protected errorDeTipo(campo: Campo, valor: Valor): string | undefined {
    return campo.options?.includes(String(valor)) ? undefined : 'Elegí una de las opciones.';
  }
}

export class ComportamientoDeRelacion extends ComportamientoDeCampo {
  valorDeEjemplo(_campo: Campo, indice: number, _muestra: Valor, vinculos: string[]): Valor {
    return vinculos.length ? (vinculos[indice % vinculos.length] ?? '') : '';
  }

  marcaDeColumna(): string {
    return ' ↗';
  }

  enVistaPrevia(campo: Campo, valor: Valor, sistema: Sistema | undefined): string {
    return this.nombreDelRegistro(campo, valor, sistema) ?? SIN_VALOR;
  }

  protected valorPorDefecto(): Valor {
    return '';
  }

  protected errorDeTipo(campo: Campo, valor: Valor, sistema: Sistema): string | undefined {
    return this.existeElRegistro(campo, valor, sistema)
      ? undefined
      : 'Elegí un registro del bloque vinculado.';
  }

  protected describir(campo: Campo, valor: Valor, sistema: Sistema | undefined): string {
    return this.nombreDelRegistro(campo, valor, sistema) ?? 'Sin vínculo';
  }

  private nombreDelRegistro(campo: Campo, valor: Valor, sistema: Sistema | undefined): string | undefined {
    const objetivo = this.bloqueVinculado(campo, sistema);
    const registro = objetivo?.rows.find((fila) => fila.id === valor);
    const clave = objetivo?.fields[0]?.key;
    return String((clave && registro?.[clave]) || '') || undefined;
  }

  private bloqueVinculado(campo: Campo, sistema: Sistema | undefined) {
    return findBlock(sistema, campo.target);
  }

  private existeElRegistro(campo: Campo, valor: Valor, sistema: Sistema): boolean {
    const objetivo = this.bloqueVinculado(campo, sistema);
    return Boolean(objetivo?.rows.some((fila) => fila.id === valor));
  }
}
