import { useState } from 'react';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { FichasParaEmpezar } from '@/components/sistema/FichasParaEmpezar';
import { Icon } from '@/components/sistema/Icon';
import { PageFooter } from '@/components/sistema/PageFooter';
import { varsDeColor } from '@/components/sistema/colores';
import { CATALOG } from '@/lib/sistema/catalogo';
import { allBlocks } from '@/lib/sistema/core';
import type { Color } from '@/lib/sistema/tipos';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { useSistemaStore } from '@/stores/sistemaStore';

// Los cuadernos van rotando los cinco colores de la marca, como en el inicio.
const COLORES: Color[] = ['coral', 'blue', 'orange', 'petrol', 'yellow'];

const sinTildes = (texto: string) =>
  texto
    .normalize('NFD')
    .replace(/\p{M}+/gu, '')
    .toLowerCase();

// «Mis cuadernos»: todos los cuadernos de la persona como tapas de color, y las maneras de crear uno nuevo.
// Sin cuadernos, Sami invita a crear el primero.
export function CuadernosPage() {
  useTituloPagina('Mis cuadernos');
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const navegacion = useNavegacion();
  const [consulta, setConsulta] = useState('');
  const buscado = sinTildes(consulta.trim());
  const visibles = buscado
    ? sistemas.filter((sistema) => sinTildes(sistema.name).includes(buscado))
    : sistemas;

  return (
    <>
      <CabeceraComun active="sistemas" />
      <main id="main" tabIndex={-1} className="home inicio cuadernos-page reveal">
        <div className="cuadernos-cabeza">
          <div>
            <span className="cuadernos-etiqueta">
              <Icon name="note" /> Tus cuadernos
            </span>
            <h1>Mis cuadernos</h1>
            <p>
              {sistemas.length
                ? 'Tocá uno para seguir donde lo dejaste.'
                : 'Acá van a aparecer los cuadernos que armes.'}
            </p>
          </div>
          {sistemas.length > 0 && (
            <div className="cuadernos-cifras">
              <span data-color="orange">
                <strong>{sistemas.length}</strong>{' '}
                {sistemas.length === 1 ? 'cuaderno' : 'cuadernos'}
              </span>
            </div>
          )}
        </div>

        {sistemas.length === 0 ? (
          <section className="cuadernos-vacio" aria-labelledby="cuadernos-vacio-titulo">
            <img src="/mockup/sami-pensando.webp" alt="" />
            <div>
              <h2 id="cuadernos-vacio-titulo">Todavía no tenés cuadernos</h2>
              <p>Creá el primero ahora. Elegí cómo querés empezar:</p>
            </div>
          </section>
        ) : (
          <section className="cuadernos-lista" aria-label="Tus cuadernos">
            {sistemas.length > 3 && (
              <label className="searchbox cuadernos-buscar">
                <Icon name="search" />
                <input
                  className="input"
                  type="search"
                  placeholder="Buscar un cuaderno"
                  aria-label="Buscar un cuaderno"
                  value={consulta}
                  onChange={(evento) => setConsulta(evento.target.value)}
                />
              </label>
            )}
            <ul className="cuadernos-grilla">
              {visibles.map((sistema) => {
                const indice = sistemas.indexOf(sistema);
                const color = COLORES[indice % COLORES.length] ?? 'coral';
                const primero = allBlocks(sistema)[0];
                return (
                  <li key={sistema.id} className="cuadernos-item">
                    <button
                      type="button"
                      className="inicio-cuaderno"
                      style={varsDeColor(color)}
                      data-color={color}
                      onClick={() => void navegacion.abrirSistema(sistema.id)}
                    >
                      <span className="inicio-cuaderno-lomo" aria-hidden="true" />
                      <span className="inicio-cuaderno-tapa">
                        <Icon name={primero ? CATALOG[primero.type].icon : 'note'} />
                        <strong>{sistema.name}</strong>
                        <span>
                          {allBlocks(sistema).length}{' '}
                          {allBlocks(sistema).length === 1 ? 'tabla' : 'tablas'}
                        </span>
                      </span>
                    </button>
                  </li>
                );
              })}
            </ul>
            {visibles.length === 0 && (
              <p className="cuadernos-sin-resultados">Ningún cuaderno se llama así.</p>
            )}
          </section>
        )}

        <section className="inicio-bloque cuadernos-crear" aria-labelledby="cuadernos-crear-titulo">
          <div className="inicio-titulo">
            <h2 id="cuadernos-crear-titulo">
              {sistemas.length ? 'Crear otro cuaderno' : 'Elegí cómo empezar'}
            </h2>
            <p>Con Sami, con una plantilla o desde cero. Lo que armes, después lo podés cambiar.</p>
          </div>
          <FichasParaEmpezar />
        </section>
        <PageFooter />
      </main>
    </>
  );
}
