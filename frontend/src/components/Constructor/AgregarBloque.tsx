import { useRef, useState } from 'react';
import { PlusIcon } from '@phosphor-icons/react';
import { Button, Panel } from '@/components/ui';
import { useCrearBloque } from '@/hooks/bloques/mutations';
import { leerErrorServidor } from '@/hooks/errores';
import type { Bloque } from '@/types/bloque';
import { NombreBloqueForm } from './NombreBloqueForm';

type Props = { proyectoId: string; onCreado: (bloque: Bloque) => void };

export function AgregarBloque({ proyectoId, onCreado }: Props) {
  const [abierto, setAbierto] = useState(false);
  const boton = useRef<HTMLButtonElement>(null);
  const crear = useCrearBloque(proyectoId);

  function cerrar() {
    setAbierto(false);
    crear.reset();
    // Devuelve el foco al botón que abrió el formulario.
    requestAnimationFrame(() => boton.current?.focus());
  }

  if (!abierto) {
    return (
      <Button
        ref={boton}
        variant="primary"
        className="w-full text-base"
        onClick={() => setAbierto(true)}
      >
        <PlusIcon aria-hidden size={20} />
        Agregar bloque
      </Button>
    );
  }

  return (
    <Panel tight aria-label="Agregar bloque">
      <NombreBloqueForm
        etiqueta="Nombre del bloque nuevo"
        textoGuardar="Agregar bloque"
        ocupado={crear.isPending}
        errorServidor={leerErrorServidor(crear.error)}
        onCancelar={cerrar}
        onGuardar={(nombre) =>
          crear.mutate(nombre, {
            onSuccess: (bloque) => {
              setAbierto(false);
              crear.reset();
              onCreado(bloque);
            },
          })
        }
      />
    </Panel>
  );
}
