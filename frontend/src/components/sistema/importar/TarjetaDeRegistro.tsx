import type { FilaRevisada } from '@/lib/sistema/revision';
import type { Campo, ErroresFila, Sistema, Valor } from '@/lib/sistema/tipos';
import { Btn } from '../Btn';
import { ControlDeCampo } from '../editor/CampoDeFormulario';
import { Icon } from '../Icon';

type Props = {
  numero: number;
  fila: FilaRevisada;
  campos: Campo[];
  /** Claves de las columnas nuevas que todavía no se aceptaron. */
  clavesPendientes: Set<string>;
  sistema: Sistema | undefined;
  errores: ErroresFila | undefined;
  onEditar: (clave: string, valor: Valor) => void;
  onEstaBien: (clave: string) => void;
  onQuitar: () => void;
};

// Un registro leído de la foto: cada dato se corrige acá. Los dudosos se marcan con borde, ícono y texto,
// y se resuelven corrigiéndolos o con «Está bien». Quitar el registro lo deja fuera de lo que se incorpora.
export function TarjetaDeRegistro({
  numero,
  fila,
  campos,
  clavesPendientes,
  sistema,
  errores,
  onEditar,
  onEstaBien,
  onQuitar,
}: Props) {
  return (
    <article className="tarjeta-registro" aria-label={`Registro ${numero}`} data-registro={fila.id}>
      <div className="tarjeta-registro-cabecera">
        <h3>Registro {numero}</h3>
        <Btn cls="danger small" onClick={onQuitar} aria-label={`Quitar el registro ${numero}`}>
          <Icon name="trash" /> Quitar
        </Btn>
      </div>
      <div className="tarjeta-registro-campos">
        {campos.map((campo) => {
          const id = `imp-${fila.id}-${campo.key}`;
          const duda = fila.dudas[campo.key];
          const error = errores?.[campo.key];
          const larga = campo.type === 'textarea' || campo.type === 'checkbox';
          const clase = ['field', larga && 'long', duda && 'con-duda', error && 'invalid']
            .filter(Boolean)
            .join(' ');
          return (
            <div className={clase} key={campo.key} data-field-key={campo.key}>
              <label htmlFor={id}>
                {campo.label}
                {clavesPendientes.has(campo.key) && (
                  <span className="tag warm etiqueta-nuevo">Campo nuevo</span>
                )}
              </label>
              <ControlDeCampo
                sistema={sistema}
                campo={{ ...campo, required: false }}
                valor={fila.valores[campo.key]}
                id={id}
                etiqueta={`${campo.label}, registro ${numero}`}
                onCambio={(valor) => onEditar(campo.key, valor)}
              />
              {duda && (
                <p className="aviso-duda">
                  <Icon name="help" />
                  <span>Revisá este dato: {duda}</span>
                  <Btn cls="secondary small" onClick={() => onEstaBien(campo.key)}>
                    Está bien
                    <span className="sr-only">
                      {' '}
                      ({campo.label}, registro {numero})
                    </span>
                  </Btn>
                </p>
              )}
              <span className="field-error" id={`${id}-error`} role={error ? 'alert' : undefined}>
                {error}
              </span>
            </div>
          );
        })}
      </div>
    </article>
  );
}
