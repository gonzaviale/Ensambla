import { useState, type FormEvent } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { Button, InputField, TextareaField } from '@/components/ui';
import { useCrearProyecto } from '@/hooks/proyectos/mutations';
import { crearProyectoSchema } from '@/types/proyecto';

type Props = { onCancelar: () => void };
export function CrearProyectoForm({ onCancelar }: Props) {
  const [nombre, setNombre] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [errorNombre, setErrorNombre] = useState<string>();
  const crear = useCrearProyecto();
  const navigate = useNavigate();
  async function enviar(event: FormEvent) {
    event.preventDefault();
    const datos = crearProyectoSchema.safeParse({ nombre, descripcion: descripcion || null });
    if (!datos.success) {
      setErrorNombre(datos.error.issues[0]?.message);
      return;
    }
    setErrorNombre(undefined);
    try {
      const proyecto = await crear.mutateAsync(datos.data);
      await navigate({
        to: '/proyectos/$proyectoId/construir',
        params: { proyectoId: proyecto.id },
      });
    } catch {
      /* La mutación muestra el error y conserva lo escrito. */
    }
  }
  return (
    <form onSubmit={(event) => void enviar(event)} className="flex flex-col gap-4">
      <InputField
        label="Nombre del sistema"
        hint="Por ejemplo: Mi almacén, Pedidos del taller."
        value={nombre}
        onChange={(e) => setNombre(e.target.value)}
        maxLength={120}
        error={errorNombre}
        required
        disabled={crear.isPending}
      />
      <TextareaField
        label="Descripción (opcional)"
        value={descripcion}
        onChange={(e) => setDescripcion(e.target.value)}
        maxLength={2000}
        disabled={crear.isPending}
      />
      {crear.error && (
        <p role="alert" className="m-0 font-semibold text-bad">
          {crear.error.message}
        </p>
      )}
      <div className="flex flex-wrap justify-end gap-3">
        <Button onClick={onCancelar} disabled={crear.isPending}>
          Cancelar
        </Button>
        <Button type="submit" variant="solid" loading={crear.isPending}>
          {crear.isPending ? 'Creando…' : 'Crear sistema'}
        </Button>
      </div>
    </form>
  );
}
