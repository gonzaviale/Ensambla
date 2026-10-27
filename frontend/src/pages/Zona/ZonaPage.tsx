import { ContenidoPremium } from '@/components/planes/ContenidoPremium';
import { env } from '@/config/env';
import { useMemo, useState, type FormEvent, type ReactNode } from 'react';
import { Link } from '@tanstack/react-router';
import { Btn } from '@/components/sistema/Btn';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Icon } from '@/components/sistema/Icon';
import { PageFooter } from '@/components/sistema/PageFooter';
import { ResumenFrenteALaZona } from '@/components/sistema/zona/ResumenFrenteALaZona';
import { TarjetaDePrecio } from '@/components/sistema/zona/TarjetaDePrecio';
import { varsDeColor } from '@/components/sistema/colores';
import { parecido, pesos } from '@/lib/sistema/alertas';
import { allBlocks, normalize } from '@/lib/sistema/core';
import {
  MINIMO_DE_COMERCIOS,
  productosPropios,
  propioDe,
  type ProductoDeZona,
  type Zona,
} from '@/lib/sistema/zona';
import { useAccionesDeAvisos } from '@/hooks/avisos/useAccionesDeAvisos';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { useAnalisisDeZona, useZona } from '@/hooks/zona/queries';
import { useSistemaStore } from '@/stores/sistemaStore';

type Vista = 'precios' | 'cerca' | 'otra';

const VISTAS: { clave: Vista; etiqueta: string; icono: 'chart' | 'orders' | 'search' }[] = [
  { clave: 'precios', etiqueta: 'Precios', icono: 'chart' },
  { clave: 'cerca', etiqueta: 'Qué se vende cerca', icono: 'orders' },
  { clave: 'otra', etiqueta: 'Otra zona', icono: 'search' },
];

const TITULOS: Record<Vista, { titulo: ReactNode; texto: string }> = {
  precios: {
    titulo: (
      <>
        Mirá cuánto se cobra en tu zona.
        <br />
        Sin saber quién cobra qué.
      </>
    ),
    texto: 'Promedios de al menos 3 comercios. Nadie ve qué cobra cada uno.',
  },
  cerca: {
    titulo: (
      <>
        Qué se vende cerca.
        <br />Y qué todavía no tenés.
      </>
    ),
    texto: 'Lo que más sale cerca. Sumá lo que te falte.',
  },
  otra: {
    titulo: (
      <>
        Probá con otra zona.
        <br />
        Para decidir dónde abrir o a quién mirar.
      </>
    ),
    texto: 'Escribí un barrio o una dirección para ver el mismo análisis.',
  },
};

const sinSuficientes = (zona: Zona) => zona.comercios < MINIMO_DE_COMERCIOS;

function SinDatosSuficientes({ zona }: { zona: Zona }) {
  return (
    <div className="empty">
      <Icon name="users" />
      <p>
        {zona.comercios === 0
          ? 'No encontramos comercios en esta zona.'
          : zona.comercios === 1
            ? 'Hay 1 comercio en esta zona.'
            : `Hay ${zona.comercios} comercios en esta zona.`}{' '}
        Hacen falta al menos {MINIMO_DE_COMERCIOS} para mostrar datos, así nadie queda identificado.
      </p>
    </div>
  );
}

// «Mi zona»: precios y ventas de los comercios cercanos, siempre agregados y anónimos. Sin mapa y sin
// comercios individuales; con menos de tres comercios no se muestra ningún dato.
export function ZonaPage() {
  useTituloPagina('Mi zona');
  const zona = useZona();
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const [vista, setVista] = useState<Vista>('precios');
  const propios = useMemo(() => productosPropios(sistemas), [sistemas]);
  const { titulo, texto } = TITULOS[vista];

  return (
    <>
      <CabeceraComun active="zone" />
      <main id="main" tabIndex={-1} className="page-wrap zona-page reveal">
        <Link className="text-link" to="/ubicacion">
          Probar ubicación y rubro de mi comercio
        </Link>
        <div className="zona-cabeza">
          <div className="page-title">
            <div className="eyebrow">
              DATOS DE {zona.nombre ? zona.nombre.toUpperCase() : 'TU ZONA'}
            </div>
            <h1>{titulo}</h1>
            <p>{texto}</p>
          </div>
          <div className="zona-vistas" role="tablist" aria-label="Qué querés ver">
            {VISTAS.map(({ clave, etiqueta, icono }) => (
              <button
                key={clave}
                type="button"
                role="tab"
                aria-selected={vista === clave}
                className={vista === clave ? 'active' : ''}
                onClick={() => setVista(clave)}
              >
                <Icon name={icono} /> {etiqueta}
              </button>
            ))}
          </div>
        </div>
        {vista === 'precios' && (
          <VistaDePrecios zona={zona} propios={propios} titulo={`Precios en ${zona.nombre}`} />
        )}
        {vista === 'cerca' && <VistaDeLoQueSeVende zona={zona} />}
        {vista === 'otra' && <VistaDeOtraZona />}
        {vista !== 'otra' && (
          <ContenidoPremium acceso={zona.acceso} recurso="recomendaciones" compacto />
        )}
        <PageFooter />
      </main>
    </>
  );
}

function VistaDePrecios({
  zona,
  propios,
  titulo,
}: {
  zona: Zona;
  propios: ReturnType<typeof productosPropios>;
  titulo: string;
}) {
  const [consulta, setConsulta] = useState('');
  if (sinSuficientes(zona)) return <SinDatosSuficientes zona={zona} />;

  const buscado = normalize(consulta);
  // Primero lo que tenés (para comparar de entrada) y después el resto, por nombre.
  const productos = zona.productos
    .filter((producto) => normalize(producto.nombre).includes(buscado))
    .sort(
      (a, b) =>
        Number(Boolean(propioDe(b, propios))) - Number(Boolean(propioDe(a, propios))) ||
        a.nombre.localeCompare(b.nombre, 'es'),
    );

  return (
    <div className="zona-grid">
      <section className="zona-panel">
        <label className="searchbox zona-busca">
          <Icon name="search" />
          <input
            className="input"
            type="search"
            placeholder="Buscá un producto. Ej.: yerba"
            aria-label="Buscar un producto"
            value={consulta}
            onChange={(evento) => setConsulta(evento.target.value)}
          />
        </label>
        <div className="row between">
          <h2>{titulo}</h2>
          <span className="tag outline">{productos.length}</span>
        </div>
        <div className="zona-lista">
          {productos.length ? (
            productos.map((producto) => (
              <TarjetaDePrecio
                key={producto.nombre}
                producto={producto}
                propio={propioDe(producto, propios)}
              />
            ))
          ) : (
            <div className="empty">
              <Icon name="search" />
              <p>No hay datos de ese producto en tu zona. Probá con otra palabra.</p>
            </div>
          )}
        </div>
      </section>
      <aside className="zona-lateral">
        <ResumenFrenteALaZona productos={zona.productos} propios={propios} zona={zona.nombre} />
      </aside>
    </div>
  );
}

function VistaDeLoQueSeVende({ zona }: { zona: Zona }) {
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const acciones = useAccionesDeAvisos();
  const propios = useMemo(() => productosPropios(sistemas), [sistemas]);
  if (sinSuficientes(zona)) return <SinDatosSuficientes zona={zona} />;

  // Los productos nuevos se suman al primer inventario que tenga la persona.
  const destino = sistemas
    .map((sistema) => ({
      sistema,
      inventario: allBlocks(sistema).find((bloque) => bloque.type === 'inventory'),
    }))
    .find((candidato) => candidato.inventario);
  const nombres = sistemas.flatMap((sistema) =>
    allBlocks(sistema)
      .filter((bloque) => bloque.type === 'inventory')
      .flatMap((bloque) => bloque.rows.map((fila) => String(fila.name ?? ''))),
  );
  const yaLoTiene = (producto: ProductoDeZona) =>
    nombres.some((nombre) => parecido(nombre, producto.nombre) === 1);

  const productos = [...zona.productos].sort(
    (a, b) =>
      b.ventas.comercios - a.ventas.comercios ||
      b.ventas.unidadesPorSemana - a.ventas.unidadesPorSemana,
  );

  const sumar = (producto: ProductoDeZona) => {
    if (!destino?.inventario) return;
    const precio = Math.round(producto.precio.promedio / 10) * 10;
    acciones.ejecutar({
      tipo: 'crear',
      texto: 'Sumar a mis productos',
      sistemaId: destino.sistema.id,
      bloqueId: destino.inventario.id,
      valores: { name: producto.nombre, quantity: 0, price: precio, cost: '', minimum: 5 },
      confirmacion: `Sumamos ${producto.nombre} a tu inventario, a ${pesos(precio)}.`,
    });
  };

  return (
    <div className="zona-grid">
      <section className="zona-panel">
        {!destino && (
          <div className="info-box">
            Para sumar productos a tu inventario necesitás un cuaderno con un bloque de Inventario.
          </div>
        )}
        <div className="row between">
          <h2>Lo más vendido en {zona.nombre}</h2>
          <span className="tag outline">{productos.length}</span>
        </div>
        <div className="zona-lista">
          {productos.map((producto) => {
            const porcentaje = Math.round((producto.ventas.comercios / zona.comercios) * 100);
            const lo = yaLoTiene(producto);
            return (
              <article
                key={producto.nombre}
                className="zona-tarjeta"
                style={varsDeColor(lo ? 'blue' : 'orange')}
              >
                <div className="zona-tarjeta-cabeza">
                  <div>
                    <strong>{producto.nombre}</strong>
                    <div className="zona-meta">
                      Lo venden {producto.ventas.comercios} de {zona.comercios} comercios · unas{' '}
                      {producto.ventas.unidadesPorSemana} unidades por semana cada uno
                    </div>
                  </div>
                  <div className="zona-precio">
                    {pesos(producto.precio.promedio)}
                    <small>promedio</small>
                  </div>
                </div>
                <div
                  className="zona-barra"
                  role="img"
                  aria-label={`${porcentaje}% de los comercios de la zona lo venden`}
                >
                  <i style={{ width: `${porcentaje}%` }} />
                </div>
                <div className="zona-acciones">
                  {lo ? (
                    <span className="zona-etiqueta justo">
                      <Icon name="check-circle" /> Ya lo tenés
                    </span>
                  ) : (
                    <Btn cls="soft small" disabled={!destino} onClick={() => sumar(producto)}>
                      <Icon name="plus" /> Sumar a mis productos
                    </Btn>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      </section>
      <aside className="zona-lateral">
        <ResumenFrenteALaZona productos={zona.productos} propios={propios} zona={zona.nombre} />
      </aside>
    </div>
  );
}

function VistaDeOtraZona() {
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const propios = useMemo(() => productosPropios(sistemas), [sistemas]);
  const analisis = useAnalisisDeZona();
  const [lugar, setLugar] = useState('');
  const [error, setError] = useState('');

  const analizar = (evento: FormEvent) => {
    evento.preventDefault();
    const limpio = lugar.trim();
    if (limpio.length < 2) {
      setError('Escribí un barrio o una dirección.');
      return;
    }
    setError('');
    analisis.mutate({ lugar: limpio });
  };

  const resultado = analisis.data;
  return (
    <div className="zona-panel">
      <form className="zona-otra" onSubmit={analizar}>
        <label className="searchbox">
          <Icon name="search" />
          <input
            className="input"
            placeholder="Un barrio o una dirección. Ej.: Palermo"
            aria-label="Barrio o dirección"
            maxLength={120}
            value={lugar}
            onChange={(evento) => setLugar(evento.target.value)}
          />
        </label>
        <button className="btn primary" type="submit" disabled={analisis.isPending}>
          {analisis.isPending ? 'Analizando…' : 'Analizar la zona'}
        </button>
      </form>
      <div className="form-error" role="alert">
        {error || (analisis.isError ? 'No pudimos analizar esa zona. Probá de nuevo.' : '')}
      </div>
      {resultado ? (
        sinSuficientes(resultado) ? (
          <SinDatosSuficientes zona={resultado} />
        ) : (
          <div className="zona-grid">
            <section className="zona-panel">
              <div className="row between">
                <h2>Precios en {resultado.nombre}</h2>
                <span className="tag outline">{resultado.productos.length}</span>
              </div>
              <div className="zona-lista">
                {resultado.productos.map((producto) => (
                  <TarjetaDePrecio
                    key={producto.nombre}
                    producto={producto}
                    propio={propioDe(producto, propios)}
                  />
                ))}
              </div>
            </section>
            <aside className="zona-lateral">
              <ResumenFrenteALaZona
                productos={resultado.productos}
                propios={propios}
                zona={resultado.nombre}
              />
            </aside>
            <ContenidoPremium acceso={resultado.acceso} recurso="recomendaciones" compacto />
          </div>
        )
      ) : env.mocks === 'completo' ? (
        <div className="simulation-label">
          <strong>Análisis simulado</strong>
          Los datos de otra zona son de ejemplo. Cuando exista el servicio real, vas a ver los
          precios y ventas agregados de los comercios de ese lugar.
        </div>
      ) : null}
    </div>
  );
}
