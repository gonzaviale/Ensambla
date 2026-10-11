import { useEffect, useRef, useState, type FormEvent } from 'react';
import { FloppyDiskIcon } from '@phosphor-icons/react';
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

// Un control con ese `name` puede ser un input suelto o un grupo de radios (Sí / No).
function primerControl(formulario: HTMLFormElement | null, nombre: string) {
  const control = formulario?.elements.namedItem(nombre);
  const primero = control instanceof RadioNodeList ? control[0] : control;
  return primero instanceof HTMLElement ? primero : undefined;
}

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
    primerControl(formulario.current, campoAEnfocar.current)?.focus();
    campoAEnfocar.current = null;
  }, [enviando, errores, valores]);

  async function guardar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (bloqueo.current) return;
    setAviso(null);
    const nuevosErrores = validarFormulario(campos, valores);
    for (const campo of campos) {
      const control = event.currentTarget.elements.namedItem(campo.id);
      if (control instanceof HTMLInputElement && control.validity.badInput)
        nuevosErrores[campo.id] = 'Tiene que ser una fecha válida';
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
        // Listo para cargar otra fila: el foco vuelve al primer campo.
        campoAEnfocar.current = camposOrdenados[0]?.id ?? null;
        setAviso({ tipo: 'exito', mensaje: 'Fila guardada. Ya podés cargar otra.' });
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
    return (
      <p className="m-0 text-body text-soft">
        Esta tabla todavía no tiene columnas para completar.
      </p>
    );
  }

  return (
    <form
      ref={formulario}
      noValidate
      onSubmit={guardar}
      aria-busy={enviando}
      className="flex flex-col gap-5"
    >
      <p className="m-0 text-hint text-muted">Completá las columnas marcadas como obligatorias.</p>
      {Object.keys(errores).length > 0 && (
        <p role={aviso ? undefined : 'alert'} className="m-0 text-base font-semibold text-bad">
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
        {/* En el celular el botón queda fijo sobre la barra inferior: siempre a mano al cargar. */}
        <div className="sticky bottom-[calc(4.5rem+env(safe-area-inset-bottom))] z-10 -mx-5 -mb-5 border-t border-line bg-surface px-5 py-3 md:static md:mx-0 md:mb-0 md:border-0 md:bg-transparent md:p-0">
          <Button
            type="submit"
            variant="solid"
            size="lg"
            loading={enviando}
            className="max-md:w-full"
          >
            {!enviando && <FloppyDiskIcon aria-hidden size={24} />}
            {enviando ? 'Guardando…' : 'Guardar fila'}
          </Button>
        </div>
      </fieldset>
      {aviso && <Toast {...aviso} onClose={() => setAviso(null)} />}
    </form>
  );
}
