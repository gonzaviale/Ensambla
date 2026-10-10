import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Button } from '@/components/ui';
import { Toast } from '@/components/ui/Toast';
import { CampoRegistro } from './CampoRegistro';
import {
  validarFormulario,
  valoresIniciales,
  valoresParaGuardar,
  type ErroresFormulario,
} from '@/lib/formularioRegistro';
import type { Campo } from '@/types/proyecto';
import type { ResultadoGuardado, ValoresRegistro } from '@/types/registro';

type FormRendererProps = {
  campos: readonly Campo[];
  onSubmit: (valores: ValoresRegistro) => Promise<ResultadoGuardado>;
  onEnviandoChange?: (enviando: boolean) => void;
};

export function FormRenderer({ campos, onSubmit, onEnviandoChange }: FormRendererProps) {
  const [valores, setValores] = useState(() => valoresIniciales(campos));
  const [errores, setErrores] = useState<ErroresFormulario>({});
  const [enviando, setEnviando] = useState(false);
  const [aviso, setAviso] = useState<{ mensaje: string; tipo: 'exito' | 'error' } | null>(null);
  const bloqueo = useRef(false);
  const formulario = useRef<HTMLFormElement>(null);
  const campoAEnfocar = useRef<string | null>(null);
  const camposOrdenados = [...campos].sort((a, b) => a.posicion - b.posicion);

  useEffect(() => {
    if (enviando || !campoAEnfocar.current) return;
    const control = formulario.current?.elements.namedItem(campoAEnfocar.current);
    if (control instanceof HTMLElement) control.focus();
    campoAEnfocar.current = null;
  }, [enviando, errores]);

  async function guardar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (bloqueo.current) return;
    setAviso(null);
    const nuevosErrores = validarFormulario(campos, valores);
    for (const campo of campos) {
      const control = event.currentTarget.elements.namedItem(campo.id);
      if (control instanceof HTMLInputElement && control.validity.badInput)
        nuevosErrores[campo.id] =
          campo.tipo === 'NUMERO' ? 'Tiene que ser un número' : 'Tiene que ser una fecha válida';
    }
    setErrores(nuevosErrores);
    const primero = camposOrdenados.find((campo) => nuevosErrores[campo.id]);
    if (primero) {
      campoAEnfocar.current = primero.id;
      return;
    }
    bloqueo.current = true;
    setEnviando(true);
    onEnviandoChange?.(true);
    try {
      const resultado = await onSubmit(valoresParaGuardar(campos, valores));
      if (resultado.exito) {
        setValores(valoresIniciales(campos));
        setAviso({ tipo: 'exito', mensaje: 'Registro guardado. Ya podés cargar otro.' });
      } else {
        const erroresVisibles = Object.fromEntries(
          Object.entries(resultado.errores).filter(([id]) =>
            campos.some((campo) => campo.id === id),
          ),
        );
        setErrores(erroresVisibles);
        campoAEnfocar.current =
          camposOrdenados.find((campo) => erroresVisibles[campo.id])?.id ?? null;
        setAviso({ tipo: 'error', mensaje: resultado.mensaje });
      }
    } catch {
      setAviso({
        tipo: 'error',
        mensaje: 'No pudimos confirmar el guardado. Revisá antes de volver a intentar.',
      });
    } finally {
      bloqueo.current = false;
      setEnviando(false);
      onEnviandoChange?.(false);
    }
  }

  if (campos.length === 0) {
    return <p className="m-0 text-soft">Este bloque todavía no tiene campos para cargar datos.</p>;
  }

  return (
    <form
      ref={formulario}
      noValidate
      onSubmit={guardar}
      aria-busy={enviando}
      className="flex flex-col gap-5"
    >
      <p className="m-0 text-sm text-muted">Completá los datos marcados como obligatorios.</p>
      {Object.keys(errores).length > 0 && (
        <p role={aviso ? undefined : 'alert'} className="m-0 text-sm text-bad">
          Revisá los campos marcados para guardar.
        </p>
      )}
      <fieldset disabled={enviando} className="m-0 flex min-w-0 flex-col gap-5 border-0 p-0">
        {camposOrdenados.map((campo) => (
          <CampoRegistro
            key={campo.id}
            campo={campo}
            valor={valores[campo.id] ?? ''}
            error={errores[campo.id]}
            onChange={(valor) => {
              setValores((actuales) => ({ ...actuales, [campo.id]: valor }));
              setErrores((actuales) =>
                Object.fromEntries(Object.entries(actuales).filter(([id]) => id !== campo.id)),
              );
            }}
          />
        ))}
        <Button type="submit" variant="solid" disabled={enviando} className="self-start">
          {enviando ? 'Guardando…' : 'Guardar registro'}
        </Button>
      </fieldset>
      {aviso && <Toast {...aviso} onClose={() => setAviso(null)} />}
    </form>
  );
}
