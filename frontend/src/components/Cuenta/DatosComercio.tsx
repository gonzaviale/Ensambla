import { useRef, useState, type FormEvent } from 'react';
import { Button, InputField, Panel, SelectField } from '@/components/ui';
import { useAccionesRed, useNegocio } from '@/hooks/red/useRed';
import { RUBROS } from '@/lib/red/relato';
import type { NegocioRed } from '@/types/red';
import { enfocarPrimerError } from './enfocarPrimerError';

type Valores = Pick<NegocioRed, 'nombre' | 'rubro' | 'zona' | 'telefono'>;
type Errores = Partial<Record<keyof Valores, string>>;

const desdeNegocio = (negocio: NegocioRed): Valores => ({
  nombre: negocio.nombre,
  rubro: negocio.rubro,
  zona: negocio.zona,
  telefono: negocio.telefono,
});

// Un teléfono es opcional; si se escribe, tiene que parecer uno (al menos 6 números).
const TELEFONO_VALIDO = /^[\d\s()+-]+$/;

function validarDatosComercio(valores: Valores): Errores {
  const errores: Errores = {};
  const nombre = valores.nombre.trim();
  if (!nombre) errores.nombre = 'Escribí cómo se llama tu comercio.';
  else if (nombre.length > 120) errores.nombre = 'Usá como máximo 120 letras.';
  const telefono = valores.telefono.trim();
  if (telefono) {
    const digitos = telefono.replace(/\D/g, '');
    if (!TELEFONO_VALIDO.test(telefono) || digitos.length < 6) {
      errores.telefono = 'Escribí un teléfono con números, por ejemplo 11 5555-0142.';
    }
  }
  return errores;
}

type DatosComercioProps = { onGuardado: (mensaje: string) => void };

// Datos del comercio (T-041). Se guardan en el navegador; el nombre es obligatorio.
export function DatosComercio({ onGuardado }: DatosComercioProps) {
  const negocio = useNegocio();
  const { guardarNegocio } = useAccionesRed();
  const formulario = useRef<HTMLFormElement>(null);
  const [valores, setValores] = useState<Valores>(() => desdeNegocio(negocio));
  const [errores, setErrores] = useState<Errores>({});

  // Si los datos cambian desde afuera (por ejemplo al volver a los datos de ejemplo), el formulario
  // los muestra. Se ajusta durante el render, sin efectos.
  const [visto, setVisto] = useState(negocio);
  if (visto !== negocio) {
    setVisto(negocio);
    setValores(desdeNegocio(negocio));
    setErrores({});
  }

  const rubros: string[] = [...RUBROS];
  if (valores.rubro && !rubros.includes(valores.rubro)) rubros.unshift(valores.rubro);

  function cambiar<K extends keyof Valores>(campo: K, valor: Valores[K]) {
    setValores((actuales) => ({ ...actuales, [campo]: valor }));
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }));
  }

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    const nuevos = validarDatosComercio(valores);
    setErrores(nuevos);
    if (Object.keys(nuevos).length > 0) {
      enfocarPrimerError(formulario.current);
      return;
    }
    guardarNegocio({
      nombre: valores.nombre.trim(),
      rubro: valores.rubro,
      zona: valores.zona.trim(),
      telefono: valores.telefono.trim(),
    });
    onGuardado('Guardamos los datos de tu comercio.');
  }

  return (
    <Panel
      aria-labelledby="cuenta-comercio"
      data-recorrido="cuenta-datos"
      className="flex flex-col gap-4"
    >
      <h2 id="cuenta-comercio" className="m-0 text-h3">
        Datos de tu comercio
      </h2>
      <form ref={formulario} noValidate onSubmit={enviar} className="flex flex-col gap-4">
        <InputField
          label="Nombre del comercio"
          required
          autoComplete="organization"
          maxLength={120}
          value={valores.nombre}
          error={errores.nombre}
          onChange={(evento) => cambiar('nombre', evento.target.value)}
        />
        <SelectField
          label="Rubro"
          value={valores.rubro}
          onChange={(evento) => cambiar('rubro', evento.target.value)}
        >
          {rubros.map((rubro) => (
            <option key={rubro} value={rubro}>
              {rubro}
            </option>
          ))}
        </SelectField>
        <InputField
          label="Zona o barrio"
          autoComplete="address-level2"
          maxLength={80}
          value={valores.zona}
          onChange={(evento) => cambiar('zona', evento.target.value)}
        />
        <InputField
          label="Teléfono"
          type="tel"
          autoComplete="tel"
          maxLength={30}
          hint="Es opcional. Lo usamos sólo para que tus proveedores te encuentren."
          value={valores.telefono}
          error={errores.telefono}
          onChange={(evento) => cambiar('telefono', evento.target.value)}
        />
        <Button type="submit" variant="solid" size="lg" fullWidth>
          Guardar los datos
        </Button>
      </form>
    </Panel>
  );
}
