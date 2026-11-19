import type { Campo, Valor } from '../tipos';
import { ComportamientoDeCampo } from './ComportamientoDeCampo';

export class ComportamientoDeTexto extends ComportamientoDeCampo {
  protected valorPorDefecto(campo: Campo, indice: number): Valor {
    return campo.required ? `Registro ${indice + 1}` : '';
  }
}

export class ComportamientoDeTextoLargo extends ComportamientoDeTexto {
  ocupaTodoElAncho(): boolean {
    return true;
  }
}

export class ComportamientoDeHora extends ComportamientoDeTexto {
  private static readonly FORMATO = /^([01]\d|2[0-3]):[0-5]\d$/;

  interpretar(_campo: Campo, crudo: unknown): Valor {
    const [, horas = '', minutos = ''] = /^(\d{1,2}):(\d{2})$/.exec(String(crudo)) ?? [];
    return this.esHorario(horas, minutos) ? `${horas.padStart(2, '0')}:${minutos}` : undefined;
  }

  protected errorDeTipo(_campo: Campo, valor: Valor): string | undefined {
    return ComportamientoDeHora.FORMATO.test(String(valor))
      ? undefined
      : 'Elegí un horario válido.';
  }

  private esHorario(horas: string, minutos: string): boolean {
    return Boolean(horas) && Number(horas) < 24 && Number(minutos) < 60;
  }
}

export class ComportamientoDeEmail extends ComportamientoDeTexto {
  private static readonly FORMATO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  protected valorPorDefecto(): Valor {
    return 'contacto@example.com';
  }

  protected errorDeTipo(_campo: Campo, valor: Valor): string | undefined {
    return ComportamientoDeEmail.FORMATO.test(String(valor)) ? undefined : 'Revisá el correo.';
  }
}
