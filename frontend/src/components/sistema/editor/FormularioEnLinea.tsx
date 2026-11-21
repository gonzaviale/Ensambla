import { useRef, useState, type FormEvent } from 'react';
import type { Bloque, Sistema } from '@/lib/sistema/tipos';
import { guardarRegistro } from '@/stores/accionesDelEditor';
import { Icon } from '../Icon';
import { CampoDeFormulario } from './CampoDeFormulario';
import { enfocarPrimerError, leerValores } from './leerFormulario';

export function FormularioEnLinea({ bloque, sistema }: { bloque: Bloque; sistema: Sistema }) {
  const formulario = useRef<HTMLFormElement>(null);
  const [errores, setErrores] = useState<Record<string, string>>({});

  const guardar = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    const resultado = guardarRegistro(bloque.id, leerValores(evento.currentTarget, bloque), null);
    if (resultado) {
      setErrores(resultado);
      requestAnimationFrame(() => enfocarPrimerError(formulario.current));
      return;
    }
    setErrores({});
    formulario.current?.reset();
  };

  return (
    <form ref={formulario} className="inline-form" onSubmit={guardar} noValidate>
      {bloque.fields.map((campo) => (
        <CampoDeFormulario
          key={campo.key}
          sistema={sistema}
          campo={campo}
          valor=""
          prefijo={bloque.id}
          error={errores[campo.key]}
        />
      ))}
      <button className="btn primary" type="submit">
        <Icon name="plus" />
        Guardar registro
      </button>
      <div className="form-error" role="alert">
        {errores._form}
      </div>
    </form>
  );
}
