import { useMemo, useState } from 'react';
import { Link } from '@tanstack/react-router';
import {
  CaretRightIcon,
  MagnifyingGlassIcon,
  PackageIcon,
  WarningIcon,
} from '@phosphor-icons/react';
import {
  Badge,
  EmptyState,
  InfoTip,
  InputField,
  Panel,
  SelectField,
  buttonClasses,
} from '@/components/ui';
import { useProductos, useProveedores, useReglas, useUmbrales } from '@/hooks/red/useRed';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { useEsEscritorio } from '@/hooks/useMediaQuery';
import { diasDeStock, margenSobreCosto } from '@/lib/red/calculos';
import { costo, pesos, plural, porcentaje } from '@/lib/red/formato';
import type { ProductoRed } from '@/types/red';
import { sinTildes, textoDiasDeStock } from './ayudas';
import { GananciaBadge } from './GananciaBadge';

type Orden = 'ganancia' | 'nombre';

type Fila = {
  producto: ProductoRed;
  proveedor: string;
  margen: number;
  dias: number;
  bajo: boolean;
};

type CatalogoPreciosProps = { onAbrir: (productoId: string) => void };

// Catálogo con la ganancia de cada producto: tabla en escritorio, una tarjeta por producto en celular.
// Tocar un producto abre su detalle (costo, historial, proveedores y cambio de precio).
export function CatalogoPrecios({ onAbrir }: CatalogoPreciosProps) {
  const productos = useProductos();
  const proveedores = useProveedores();
  const reglas = useReglas();
  const umbrales = useUmbrales();
  const esEscritorio = useEsEscritorio();

  const [busqueda, setBusqueda] = useState('');
  const buscado = useDebouncedValue(busqueda, 200);
  const [categoria, setCategoria] = useState('');
  const [soloBajas, setSoloBajas] = useState(false);
  const [orden, setOrden] = useState<Orden>('ganancia');

  const filas = useMemo<Fila[]>(
    () =>
      productos.map((producto) => {
        const margen = margenSobreCosto(producto.costo, producto.precio);
        return {
          producto,
          proveedor:
            proveedores.find((p) => p.id === producto.proveedorId)?.nombre ?? 'Sin proveedor',
          margen,
          dias: diasDeStock(producto),
          bajo: margen < reglas.margenMinimo,
        };
      }),
    [productos, proveedores, reglas.margenMinimo],
  );

  const categorias = useMemo(
    () => [...new Set(productos.map((p) => p.categoria))].sort((a, b) => a.localeCompare(b, 'es')),
    [productos],
  );

  const bajos = filas.filter((fila) => fila.bajo).length;
  const promedio =
    filas.length > 0 ? filas.reduce((total, fila) => total + fila.margen, 0) / filas.length : 0;

  const visibles = useMemo(() => {
    const texto = sinTildes(buscado.trim());
    return filas
      .filter((fila) => !texto || sinTildes(fila.producto.nombre).includes(texto))
      .filter((fila) => !categoria || fila.producto.categoria === categoria)
      .filter((fila) => !soloBajas || fila.bajo)
      .sort((a, b) =>
        orden === 'ganancia'
          ? a.margen - b.margen || a.producto.nombre.localeCompare(b.producto.nombre, 'es')
          : a.producto.nombre.localeCompare(b.producto.nombre, 'es'),
      );
  }, [filas, buscado, categoria, soloBajas, orden]);

  const hayFiltros = busqueda !== '' || categoria !== '' || soloBajas;

  function quitarFiltros() {
    setBusqueda('');
    setCategoria('');
    setSoloBajas(false);
  }

  if (productos.length === 0) {
    return (
      <EmptyState icon={<PackageIcon aria-hidden size={32} />} title="Todavía no tenés productos">
        <p className="m-0">
          Cuando cargues la lista de un proveedor, acá vas a ver cuánto ganás con cada producto.
        </p>
        <Link to="/listas" className={buttonClasses({ variant: 'solid', className: 'mt-3' })}>
          Cargar una lista
        </Link>
      </EmptyState>
    );
  }

  return (
    <div className="flex flex-col gap-5">
      <section aria-label="Resumen de tus precios">
        <ul className="m-0 grid list-none grid-cols-1 gap-3 p-0 sm:grid-cols-3">
          <li>
            <Panel tight className="h-full">
              <p className="m-0 text-hint text-muted">Productos</p>
              <p className="m-0 text-h2 font-bold">{filas.length}</p>
            </Panel>
          </li>
          <li>
            <Panel tight className="flex h-full flex-col gap-1">
              <p className="m-0 text-hint text-muted">Con ganancia por debajo del mínimo</p>
              <p className="m-0 flex flex-wrap items-center gap-2 text-h2 font-bold">
                {bajos}
                {bajos > 0 ? (
                  <Badge tone="bad" icon={<WarningIcon size={18} weight="fill" />}>
                    Revisalos
                  </Badge>
                ) : (
                  <Badge tone="ok">Todo en orden</Badge>
                )}
              </p>
              <p className="m-0 text-hint text-muted">
                Tu mínimo es {porcentaje(reglas.margenMinimo)} sobre el costo.
              </p>
            </Panel>
          </li>
          <li>
            <Panel tight className="h-full">
              <p className="m-0 text-hint text-muted">Ganancia promedio</p>
              <p className="m-0 text-h2 font-bold">{porcentaje(promedio)}</p>
              <p className="m-0 text-hint text-muted">Sobre el costo de cada producto.</p>
            </Panel>
          </li>
        </ul>
      </section>

      <Panel
        aria-labelledby="catalogo-filtros"
        data-recorrido="precios-productos"
        className="flex flex-col gap-4"
      >
        <h2 id="catalogo-filtros" className="m-0 text-h3">
          Tus productos
        </h2>
        <p className="m-0 text-hint text-muted">
          La ganancia es «sobre el costo»: si te cuesta $ 100 y lo vendés a $ 130, ganás 30 %.{' '}
          <InfoTip tema="ganancia sobre el costo">
            Se calcula sobre lo que te cuesta, no sobre lo que cobrás. Si un producto te cuesta $
            200 y lo vendés a $ 250, la diferencia es $ 50: ganás 25 % sobre el costo.
          </InfoTip>
        </p>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <InputField
            label="Buscar un producto"
            type="search"
            autoComplete="off"
            value={busqueda}
            onChange={(event) => setBusqueda(event.target.value)}
          />
          <SelectField
            label="Categoría"
            value={categoria}
            onChange={(event) => setCategoria(event.target.value)}
          >
            <option value="">Todas</option>
            {categorias.map((nombre) => (
              <option key={nombre} value={nombre}>
                {nombre}
              </option>
            ))}
          </SelectField>
          <SelectField
            label="Ordenar por"
            value={orden}
            onChange={(event) => setOrden(event.target.value as Orden)}
          >
            <option value="ganancia">Ganancia, de la más baja a la más alta</option>
            <option value="nombre">Nombre, de la A a la Z</option>
          </SelectField>
        </div>
        <label className="flex min-h-12 w-fit cursor-pointer items-center gap-3 text-base font-semibold">
          <input
            type="checkbox"
            checked={soloBajas}
            onChange={(event) => setSoloBajas(event.target.checked)}
            className="size-6 shrink-0 cursor-pointer accent-accent-700"
          />
          Sólo con ganancia baja
        </label>
      </Panel>

      <p role="status" className="m-0 text-hint text-muted">
        {visibles.length === filas.length
          ? `Mostrando ${plural(visibles.length, 'producto', 'productos')}.`
          : `Mostrando ${visibles.length} de ${filas.length} productos.`}
      </p>

      {visibles.length === 0 ? (
        <EmptyState
          icon={<MagnifyingGlassIcon aria-hidden size={32} />}
          title="No encontramos productos con esos filtros"
        >
          <p className="m-0">
            Probá con otro nombre, otra categoría o sacá «Sólo con ganancia baja».
          </p>
          {hayFiltros && (
            <button
              type="button"
              onClick={quitarFiltros}
              className={buttonClasses({ variant: 'primary', className: 'mt-3' })}
            >
              Quitar los filtros
            </button>
          )}
        </EmptyState>
      ) : esEscritorio ? (
        <TablaPrecios
          filas={visibles}
          minimo={reglas.margenMinimo}
          dias={umbrales.diasDeStock}
          onAbrir={onAbrir}
        />
      ) : (
        <TarjetasPrecios
          filas={visibles}
          minimo={reglas.margenMinimo}
          dias={umbrales.diasDeStock}
          onAbrir={onAbrir}
        />
      )}
    </div>
  );
}

type VistaProps = {
  filas: Fila[];
  minimo: number;
  /** Se marca «alcanza para pocos días» por debajo de esta cantidad. */
  dias: number;
  onAbrir: (productoId: string) => void;
};

function StockProducto({ fila, dias }: { fila: Fila; dias: number }) {
  const poco = fila.dias < dias;
  return (
    <span className="flex flex-col items-start gap-1">
      <span>{plural(fila.producto.stock, 'unidad', 'unidades')}</span>
      {poco ? (
        <Badge tone="warn" icon={<WarningIcon size={16} weight="fill" />}>
          {textoDiasDeStock(fila.dias)}
        </Badge>
      ) : (
        <span className="text-hint text-muted">{textoDiasDeStock(fila.dias)}</span>
      )}
    </span>
  );
}

function TablaPrecios({ filas, minimo, dias, onAbrir }: VistaProps) {
  return (
    <div className="overflow-x-auto rounded-lg border border-line/60 bg-surface shadow-sm">
      <table className="w-full border-collapse text-left text-base">
        <caption className="sr-only">Tus productos con su costo, precio y ganancia</caption>
        <thead>
          <tr className="border-b border-line bg-canvas text-hint text-soft">
            <th scope="col" className="px-4 py-3 font-semibold">
              Producto
            </th>
            <th scope="col" className="px-4 py-3 font-semibold">
              Proveedor
            </th>
            <th scope="col" className="px-4 py-3 text-right font-semibold">
              Costo
            </th>
            <th scope="col" className="px-4 py-3 text-right font-semibold">
              Precio
            </th>
            <th scope="col" className="px-4 py-3 font-semibold">
              Ganancia
            </th>
            <th scope="col" className="px-4 py-3 font-semibold">
              Stock
            </th>
          </tr>
        </thead>
        <tbody>
          {filas.map((fila) => (
            <tr key={fila.producto.id} className="border-b border-line/60 last:border-b-0">
              <th scope="row" className="px-4 py-3 text-left align-top font-normal">
                <button
                  type="button"
                  onClick={() => onAbrir(fila.producto.id)}
                  className="inline-flex min-h-11 cursor-pointer items-center gap-1 rounded-md text-left font-semibold text-accent-800 underline decoration-accent-800/40 underline-offset-4 hover:text-accent-900"
                >
                  {fila.producto.nombre}
                </button>
                <span className="block text-hint text-muted">{fila.producto.categoria}</span>
              </th>
              <td className="px-4 py-3 align-top">{fila.proveedor}</td>
              <td className="px-4 py-3 text-right align-top">{costo(fila.producto.costo)}</td>
              <td className="px-4 py-3 text-right align-top font-semibold">
                {pesos(fila.producto.precio)}
              </td>
              <td className="px-4 py-3 align-top">
                <GananciaBadge margen={fila.margen} minimo={minimo} />
              </td>
              <td className="px-4 py-3 align-top">
                <StockProducto fila={fila} dias={dias} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function TarjetasPrecios({ filas, minimo, dias, onAbrir }: VistaProps) {
  return (
    <ul aria-label="Tus productos" className="m-0 flex list-none flex-col gap-3 p-0">
      {filas.map((fila) => (
        <li key={fila.producto.id}>
          <Panel tight className="flex flex-col gap-3">
            <button
              type="button"
              onClick={() => onAbrir(fila.producto.id)}
              className="flex min-h-12 w-full cursor-pointer items-center justify-between gap-3 rounded-md text-left"
            >
              <span className="min-w-0">
                <span className="block text-body font-bold break-words text-accent-800">
                  {fila.producto.nombre}
                </span>
                <span className="block text-hint text-muted">
                  {fila.producto.categoria} · {fila.proveedor}
                </span>
              </span>
              <CaretRightIcon aria-hidden size={22} weight="bold" className="shrink-0 text-soft" />
            </button>
            <dl className="m-0 grid grid-cols-2 gap-x-4 gap-y-2">
              <div>
                <dt className="text-hint text-muted">Costo</dt>
                <dd className="m-0">{costo(fila.producto.costo)}</dd>
              </div>
              <div>
                <dt className="text-hint text-muted">Precio</dt>
                <dd className="m-0 font-semibold">{pesos(fila.producto.precio)}</dd>
              </div>
              <div className="col-span-2">
                <dt className="text-hint text-muted">Ganancia</dt>
                <dd className="m-0">
                  <GananciaBadge margen={fila.margen} minimo={minimo} />
                </dd>
              </div>
              <div className="col-span-2">
                <dt className="text-hint text-muted">Stock</dt>
                <dd className="m-0">
                  <StockProducto fila={fila} dias={dias} />
                </dd>
              </div>
            </dl>
          </Panel>
        </li>
      ))}
    </ul>
  );
}
