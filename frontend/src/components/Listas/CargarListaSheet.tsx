import { useState, type ReactNode } from 'react';
import { useNavigate } from '@tanstack/react-router';
import {
  CameraIcon,
  FileArrowUpIcon,
  FilePdfIcon,
  FileXlsIcon,
  WarningCircleIcon,
  WhatsappLogoIcon,
} from '@phosphor-icons/react';
import { Button, FieldShell, SelectField, Sheet } from '@/components/ui';
import { useAccionesRed, useProveedores } from '@/hooks/red/useRed';
import type { OrigenLista } from '@/types/red';
import { OpcionRadio } from './OpcionRadio';

const ORIGENES: { valor: OrigenLista; titulo: string; icono: ReactNode }[] = [
  { valor: 'whatsapp', titulo: 'WhatsApp', icono: <WhatsappLogoIcon size={26} /> },
  { valor: 'pdf', titulo: 'PDF', icono: <FilePdfIcon size={26} /> },
  { valor: 'excel', titulo: 'Excel', icono: <FileXlsIcon size={26} /> },
  { valor: 'foto', titulo: 'Foto', icono: <CameraIcon size={26} /> },
];

type CargarListaSheetProps = { open: boolean; onClose: () => void };

// Cargar una lista (demostración): se elige el proveedor y cómo llegó; la lectura real la haría la IA.
export function CargarListaSheet({ open, onClose }: CargarListaSheetProps) {
  return (
    <Sheet
      open={open}
      onClose={onClose}
      title="Cargar una lista"
      description="Elegí de qué proveedor es y cómo te llegó. Ensambla la lee y te muestra qué precios revisar."
    >
      <FormularioCarga onClose={onClose} />
    </Sheet>
  );
}

function FormularioCarga({ onClose }: { onClose: () => void }) {
  const proveedores = useProveedores();
  const { subirLista } = useAccionesRed();
  const navigate = useNavigate();
  const [proveedorId, setProveedorId] = useState(proveedores[0]?.id ?? '');
  const [origen, setOrigen] = useState<OrigenLista>(proveedores[0]?.origenHabitual ?? 'whatsapp');
  const [archivo, setArchivo] = useState('');
  const [error, setError] = useState('');

  function elegirProveedor(id: string) {
    setProveedorId(id);
    setError('');
    const proveedor = proveedores.find((p) => p.id === id);
    if (proveedor) setOrigen(proveedor.origenHabitual);
  }

  function cargar() {
    const listaId = subirLista(proveedorId, origen);
    if (!listaId) {
      setError(
        'Ese proveedor todavía no tiene productos en tu sistema, así que no pudimos armar la lista. Probá con otro proveedor.',
      );
      return;
    }
    onClose();
    void navigate({ to: '/listas/$listaId', params: { listaId } });
  }

  return (
    <form
      className="flex flex-col gap-5"
      onSubmit={(event) => {
        event.preventDefault();
        cargar();
      }}
    >
      <SelectField
        label="¿De qué proveedor es?"
        value={proveedorId}
        onChange={(event) => elegirProveedor(event.target.value)}
        data-autofocus
      >
        {proveedores.map((proveedor) => (
          <option key={proveedor.id} value={proveedor.id}>
            {proveedor.nombre}
          </option>
        ))}
      </SelectField>

      <fieldset className="m-0 flex min-w-0 flex-col gap-2 border-0 p-0">
        <legend className="mb-2 p-0 text-base font-semibold">¿Cómo te llegó?</legend>
        <div className="grid grid-cols-2 gap-3">
          {ORIGENES.map((opcion) => (
            <OpcionRadio
              key={opcion.valor}
              name="origen-de-la-lista"
              value={opcion.valor}
              checked={origen === opcion.valor}
              onChange={(valor) => setOrigen(valor as OrigenLista)}
              icono={opcion.icono}
              titulo={opcion.titulo}
            />
          ))}
        </div>
      </fieldset>

      <FieldShell
        label="Archivo de la lista (opcional)"
        hint="En la versión real la lectura la hace la IA. Acá es una demostración: podés elegir cualquier archivo, o ninguno."
      >
        {({ id, describedBy }) => (
          <div className="flex flex-col gap-2">
            <input
              id={id}
              type="file"
              aria-describedby={describedBy}
              onChange={(event) => setArchivo(event.target.files?.[0]?.name ?? '')}
              className="min-h-12 w-full cursor-pointer rounded-md border border-dashed border-fg/55 bg-canvas p-2.5 text-base file:mr-3 file:cursor-pointer file:rounded-md file:border-0 file:bg-sand-100 file:px-4 file:py-2 file:font-semibold file:text-fg"
            />
            {archivo && (
              <p className="m-0 flex items-center gap-2 text-hint text-soft">
                <FileArrowUpIcon aria-hidden size={20} />
                {archivo}
              </p>
            )}
          </div>
        )}
      </FieldShell>

      {error && (
        <p role="alert" className="m-0 flex items-start gap-2 text-body font-semibold text-bad">
          <WarningCircleIcon aria-hidden size={24} weight="fill" className="mt-0.5 shrink-0" />
          {error}
        </p>
      )}

      <div className="flex flex-wrap justify-end gap-3">
        <Button onClick={onClose}>Ahora no</Button>
        <Button type="submit" variant="solid">
          Cargar lista
        </Button>
      </div>
    </form>
  );
}
