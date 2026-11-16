import { useId, useState } from 'react';
import { DialogoCaja } from './DialogoCaja';
import { ConfirmacionCambios } from './ConfirmacionCambios';
import { ControlDeCampo } from '@/components/sistema/editor/CampoDeFormulario';
import { useConfirmarCambios } from '@/hooks/useConfirmarCambios';
import type { Bloque, ErroresFila, Fila, Sistema, Valor } from '@/lib/sistema/tipos';

export function ClienteCajaForm({
  sistema,
  tabla,
  cliente,
  onGuardar,
  onCerrar,
}: {
  sistema: Sistema;
  tabla: Bloque;
  cliente?: Fila;
  onGuardar: (valores: Record<string, Valor>) => ErroresFila;
  onCerrar: () => void;
}) {
  const prefijo = useId();
  const [valores, setValores] = useState<Record<string, Valor>>({ ...cliente });
  const [errores, setErrores] = useState<ErroresFila>({});
  const [modificado, setModificado] = useState(false);
  const cambios = useConfirmarCambios();
  const cerrar = async () => {
    if (
      !modificado ||
      (await cambios.confirmar('Los cambios de este cliente todavía no se guardaron.'))
    )
      onCerrar();
  };
  return (
    <>
      <DialogoCaja
        titulo={cliente ? 'Editar cliente' : 'Crear cliente'}
        onCerrar={() => void cerrar()}
      >
        <form
          noValidate
          onSubmit={(evento) => {
            evento.preventDefault();
            setErrores(onGuardar(valores));
          }}
        >
          <p className="caja-ayuda">Se guarda en {tabla.name} de Mi Negocio.</p>
          {tabla.fields.map((campo) => {
            const id = `${prefijo}-${campo.key}`;
            return (
              <div key={campo.key} className="field">
                <label htmlFor={id}>
                  {campo.label}
                  {campo.required ? ' *' : ' (opcional)'}
                </label>
                <ControlDeCampo
                  sistema={sistema}
                  campo={campo}
                  valor={valores[campo.key]}
                  id={id}
                  invalido={!!errores[campo.key]}
                  onCambio={(valor) => {
                    setValores((actual) => ({ ...actual, [campo.key]: valor }));
                    setModificado(true);
                  }}
                />
                <span className="field-error" id={`${id}-error`}>
                  {errores[campo.key]}
                </span>
              </div>
            );
          })}
          {errores._form && (
            <p role="alert" className="caja-error">
              {errores._form}
            </p>
          )}
          <div className="caja-acciones">
            <button type="button" className="btn secondary" onClick={() => void cerrar()}>
              Cancelar
            </button>
            <button type="submit" className="btn primary">
              Guardar cliente
            </button>
          </div>
        </form>
      </DialogoCaja>
      {cambios.confirmacion && (
        <ConfirmacionCambios
          mensaje={cambios.confirmacion}
          onConservar={cambios.conservar}
          onDescartar={cambios.aceptar}
        />
      )}
    </>
  );
}
