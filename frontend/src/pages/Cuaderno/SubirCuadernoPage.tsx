import { useEffect, useRef, useState, type ChangeEvent } from 'react';
import { Link } from '@tanstack/react-router';
import { CameraIcon, CheckCircleIcon, ImagesIcon } from '@phosphor-icons/react';
import { ConsejosFoto } from '@/components/Cuaderno/ConsejosFoto';
import { MiniaturaPagina } from '@/components/Cuaderno/MiniaturaPagina';
import { Button, PageHeading, Panel, buttonClasses } from '@/components/ui';
import { useSubirLote } from '@/hooks/lotes/mutations';
import { girar, prepararFoto, type Rotacion } from '@/lib/imagen';
import { MAX_FOTOS_POR_LOTE } from '@/types/lote';

type Pagina = { id: string; archivo: File; url: string; rotacion: Rotacion };

const botonGrande = 'w-full cursor-pointer py-4 text-lg';

// T-091: subir las páginas del cuaderno desde la cámara o la galería, revisarlas y mandarlas a leer.
export function SubirCuadernoPage() {
  const [paginas, setPaginas] = useState<Pagina[]>([]);
  const [aviso, setAviso] = useState<string>();
  const [preparando, setPreparando] = useState(false);
  const subirLote = useSubirLote();
  const paginasActuales = useRef(paginas);

  useEffect(() => {
    paginasActuales.current = paginas;
  }, [paginas]);
  // Al salir de la pantalla se liberan las vistas previas.
  useEffect(() => () => paginasActuales.current.forEach((p) => URL.revokeObjectURL(p.url)), []);

  const enviando = preparando || subirLote.isPending;

  function agregar(evento: ChangeEvent<HTMLInputElement>) {
    const elegidas = Array.from(evento.target.files ?? []).filter((archivo) =>
      archivo.type.startsWith('image/'),
    );
    evento.target.value = '';
    const lugar = MAX_FOTOS_POR_LOTE - paginas.length;
    setAviso(
      elegidas.length > lugar
        ? `Podés subir hasta ${MAX_FOTOS_POR_LOTE} páginas por vez. Mandá estas y después seguí con el resto.`
        : undefined,
    );
    const nuevas = elegidas.slice(0, Math.max(lugar, 0)).map((archivo) => ({
      id: crypto.randomUUID(),
      archivo,
      url: URL.createObjectURL(archivo),
      rotacion: 0 as Rotacion,
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

  async function leerCuaderno() {
    setPreparando(true);
    try {
      const fotos = await Promise.all(paginas.map((p) => prepararFoto(p.archivo, p.rotacion)));
      subirLote.mutate(fotos);
    } finally {
      setPreparando(false);
    }
  }

  function empezarDeNuevo() {
    paginas.forEach((p) => URL.revokeObjectURL(p.url));
    setPaginas([]);
    setAviso(undefined);
    subirLote.reset();
  }

  if (subirLote.isSuccess) {
    const cantidad = subirLote.data.fotos.length;
    return (
      <Panel role="status" className="mx-auto flex max-w-xl flex-col items-start gap-3">
        <CheckCircleIcon aria-hidden size={32} className="text-ok" />
        <h1 className="m-0 text-[24px]">¡Listo! Recibimos tu cuaderno</h1>
        <p className="text-soft">
          {cantidad === 1 ? 'Recibimos 1 página.' : `Recibimos ${cantidad} páginas.`} La estamos
          leyendo; cuando termine vas a poder revisar los datos.
        </p>
        <div className="flex flex-wrap gap-2">
          <Button variant="primary" onClick={empezarDeNuevo}>
            Subir más páginas
          </Button>
          <Link to="/proyectos" className={buttonClasses()}>
            Ir a mis proyectos
          </Link>
        </div>
      </Panel>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeading
        kicker="Cuaderno"
        title="Subir foto de mi cuaderno"
        lead="Sacá una foto de cada página. Revisalas y tocá «Leer mi cuaderno»."
      />

      <div className="grid gap-3 sm:grid-cols-2">
        <label className={buttonClasses({ variant: 'solid', className: botonGrande })}>
          <CameraIcon aria-hidden size={26} />
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
        <label className={buttonClasses({ variant: 'primary', className: botonGrande })}>
          <ImagesIcon aria-hidden size={26} />
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
        <p role="alert" className="m-0 text-warn">
          {aviso}
        </p>
      )}

      {paginas.length === 0 ? (
        <ConsejosFoto />
      ) : (
        <>
          <ol
            aria-label="Páginas para leer"
            className="m-0 grid list-none grid-cols-2 gap-3 p-0 sm:grid-cols-3 lg:grid-cols-4"
          >
            {paginas.map((pagina, i) => (
              <MiniaturaPagina
                key={pagina.id}
                numero={i + 1}
                url={pagina.url}
                rotacion={pagina.rotacion}
                deshabilitada={enviando}
                onGirar={() => girarPagina(pagina.id)}
                onQuitar={() => quitarPagina(pagina)}
              />
            ))}
          </ol>

          {subirLote.isError && (
            <p role="alert" className="m-0 text-bad">
              {subirLote.error.message}
            </p>
          )}

          <Button
            variant="solid"
            className={botonGrande}
            disabled={enviando}
            onClick={() => void leerCuaderno()}
          >
            {enviando
              ? 'Enviando…'
              : paginas.length === 1
                ? 'Leer mi cuaderno (1 página)'
                : `Leer mi cuaderno (${paginas.length} páginas)`}
          </Button>
          <ConsejosFoto />
        </>
      )}
    </div>
  );
}
