import { useEffect, useRef, useState, type ChangeEvent } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { CameraIcon, ImagesIcon } from '@phosphor-icons/react';
import { ConsejosFoto } from '@/components/Cuaderno/ConsejosFoto';
import { DestinoSubida } from '@/components/Cuaderno/DestinoSubida';
import { MiniaturaPagina, type EstadoHoja } from '@/components/Cuaderno/MiniaturaPagina';
import { Button, PageHeading, StickyActionBar, buttonClasses } from '@/components/ui';
import { useSubirLote } from '@/hooks/lotes/mutations';
import { girar, prepararFoto, type Rotacion } from '@/lib/imagen';
import { MAX_FOTOS_POR_LOTE } from '@/types/lote';

type Pagina = { id: string; archivo: File; url: string; rotacion: Rotacion; estado: EstadoHoja };

// Los dos botones grandes son <label> sobre un <input type=file> oculto: el anillo de foco se dibuja
// en el label cuando el input recibe foco por teclado, y el deshabilitado se ve igual que un botón.
const botonGrande =
  'w-full cursor-pointer py-4 text-lg has-[:focus-visible]:outline-3 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-accent-700 has-[:disabled]:cursor-not-allowed has-[:disabled]:border-transparent has-[:disabled]:bg-fg/10 has-[:disabled]:text-muted has-[:disabled]:shadow-none';

const textoHojas = (n: number) => (n === 1 ? '1 hoja' : `${n} hojas`);

// T-091: subir las hojas del cuaderno desde la cámara o la galería, revisarlas y mandarlas a leer.
export function SubirCuadernoPage() {
  const [paginas, setPaginas] = useState<Pagina[]>([]);
  const [aviso, setAviso] = useState<string>();
  const [preparando, setPreparando] = useState(false);
  const [bloqueDestinoId, setBloqueDestinoId] = useState<string>();
  const subirLote = useSubirLote();
  const navigate = useNavigate();
  const paginasActuales = useRef(paginas);

  useEffect(() => {
    paginasActuales.current = paginas;
  }, [paginas]);
  // Al salir de la pantalla se liberan las vistas previas.
  useEffect(() => () => paginasActuales.current.forEach((p) => URL.revokeObjectURL(p.url)), []);

  const enviando = preparando || subirLote.isPending;
  const hayError = paginas.some((p) => p.estado === 'error');

  function agregar(evento: ChangeEvent<HTMLInputElement>) {
    const elegidas = Array.from(evento.target.files ?? []).filter((archivo) =>
      archivo.type.startsWith('image/'),
    );
    evento.target.value = '';
    const lugar = MAX_FOTOS_POR_LOTE - paginas.length;
    setAviso(
      elegidas.length > lugar
        ? `Podés subir hasta ${MAX_FOTOS_POR_LOTE} hojas por vez. Mandá estas y después seguí con el resto.`
        : undefined,
    );
    const nuevas = elegidas.slice(0, Math.max(lugar, 0)).map<Pagina>((archivo) => ({
      id: crypto.randomUUID(),
      archivo,
      url: URL.createObjectURL(archivo),
      rotacion: 0,
      estado: 'pendiente',
    }));
    setPaginas((actuales) => [...actuales, ...nuevas]);
  }

  function girarPagina(id: string) {
    setPaginas((actuales) =>
      actuales.map((p) => (p.id === id ? { ...p, rotacion: girar(p.rotacion) } : p)),
    );
  }

  function quitarPagina(pagina: Pagina) {
    URL.revokeObjectURL(pagina.url);
    setAviso(undefined);
    setPaginas((actuales) => actuales.filter((p) => p.id !== pagina.id));
  }

  function marcar(id: string, estado: EstadoHoja) {
    setPaginas((actuales) => actuales.map((p) => (p.id === id ? { ...p, estado } : p)));
  }

  // Prepara una hoja por vez (para mostrar el avance y señalar la que falla) y recién entonces sube.
  async function leerCuaderno() {
    setPreparando(true);
    try {
      const fotos: Blob[] = [];
      let fallo = false;
      for (const pagina of paginasActuales.current) {
        marcar(pagina.id, 'preparando');
        try {
          fotos.push(await prepararFoto(pagina.archivo, pagina.rotacion));
          marcar(pagina.id, 'lista');
        } catch {
          marcar(pagina.id, 'error');
          fallo = true;
        }
      }
      if (fallo) return;
      subirLote.mutate(
        { fotos, bloqueDestinoId },
        {
          onSuccess: (lote) =>
            void navigate({ to: '/cuaderno/$loteId', params: { loteId: lote.id } }),
        },
      );
    } finally {
      setPreparando(false);
    }
  }

  const etiquetaLeer = enviando ? 'Enviando…' : `Leer mi cuaderno (${textoHojas(paginas.length)})`;

  return (
    <div className="flex flex-col gap-8">
      <PageHeading
        kicker="Cuaderno"
        title="Subir foto de mi cuaderno"
        lead="Sacá una foto de cada hoja. Revisalas y tocá «Leer mi cuaderno»."
      />

      <ConsejosFoto />

      <DestinoSubida
        bloqueId={bloqueDestinoId}
        onCambiar={setBloqueDestinoId}
        disabled={enviando}
      />

      <section aria-label="Agregar hojas" className="flex flex-col gap-3">
        <p aria-live="polite" className="m-0 text-body font-semibold">
          {paginas.length} de {MAX_FOTOS_POR_LOTE} hojas
        </p>
        <div className="grid gap-3 sm:grid-cols-2">
          <label
            className={buttonClasses({
              variant: paginas.length === 0 ? 'solid' : 'secondary',
              size: 'lg',
              className: botonGrande,
            })}
          >
            <CameraIcon aria-hidden size={28} />
            Sacar foto
            <input
              type="file"
              accept="image/*"
              capture="environment"
              className="sr-only"
              disabled={enviando}
              onChange={agregar}
            />
          </label>
          <label
            className={buttonClasses({ variant: 'secondary', size: 'lg', className: botonGrande })}
          >
            <ImagesIcon aria-hidden size={28} />
            Elegir de la galería
            <input
              type="file"
              accept="image/*"
              multiple
              className="sr-only"
              disabled={enviando}
              onChange={agregar}
            />
          </label>
        </div>
        {aviso && (
          <p role="alert" className="m-0 font-semibold text-warn">
            {aviso}
          </p>
        )}
      </section>

      {paginas.length > 0 && (
        <>
          <ol
            aria-label="Hojas para leer"
            className="m-0 grid list-none grid-cols-2 gap-3 p-0 sm:grid-cols-3 lg:grid-cols-4"
          >
            {paginas.map((pagina, i) => (
              <MiniaturaPagina
                key={pagina.id}
                numero={i + 1}
                url={pagina.url}
                rotacion={pagina.rotacion}
                estado={pagina.estado}
                deshabilitada={enviando}
                onGirar={() => girarPagina(pagina.id)}
                onQuitar={() => quitarPagina(pagina)}
              />
            ))}
          </ol>

          {hayError && (
            <p role="alert" className="m-0 font-semibold text-bad">
              No pudimos preparar alguna hoja. Quitala y sacá la foto de nuevo.
            </p>
          )}
          {subirLote.isError && (
            <p role="alert" className="m-0 font-semibold text-bad">
              {subirLote.error.message}
            </p>
          )}

          <StickyActionBar label="Leer el cuaderno">
            <p className="m-0 text-hint text-soft">
              Revisá que las hojas se vean derechas y nítidas.
            </p>
            <Button
              variant="solid"
              size="lg"
              loading={enviando}
              disabled={hayError}
              onClick={() => void leerCuaderno()}
            >
              {etiquetaLeer}
            </Button>
          </StickyActionBar>
        </>
      )}
    </div>
  );
}
