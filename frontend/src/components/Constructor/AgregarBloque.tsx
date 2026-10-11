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
      <Button ref={boton} variant="primary" fullWidth onClick={() => setAbierto(true)}>
        <PlusIcon aria-hidden size={22} />
        Agregar tabla
      </Button>
    );
  }

  return (
    <Panel tight aria-label="Agregar tabla">
      <NombreBloqueForm
        etiqueta="Nombre de la tabla nueva"
        textoGuardar="Agregar tabla"
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
