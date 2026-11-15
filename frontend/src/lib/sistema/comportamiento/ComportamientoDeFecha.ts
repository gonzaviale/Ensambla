import { hoy } from '../formato';
import type { Campo, Valor } from '../tipos';
import { ComportamientoDeCampo } from './ComportamientoDeCampo';
import { fechaDeCuaderno } from './fechaDeCuaderno';

export class ComportamientoDeFecha extends ComportamientoDeCampo {
  private static readonly FORMATO = /^\d{4}-\d{2}-\d{2}$/;

  interpretar(_campo: Campo, crudo: unknown): Valor {
    return typeof crudo === 'string' ? fechaDeCuaderno(crudo) : undefined;
  }

  protected valorPorDefecto(): Valor {
    return hoy();
  }

  protected errorDeTipo(_campo: Campo, valor: Valor): string | undefined {
    const texto = String(valor);
    const valida = ComportamientoDeFecha.FORMATO.test(texto) && !Number.isNaN(Date.parse(texto));
    return valida ? undefined : 'Elegí una fecha válida.';
  }
}
