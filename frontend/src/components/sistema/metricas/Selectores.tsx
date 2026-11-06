import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react';
import { Icon } from '@/components/sistema/Icon';
import { varsDeColor } from '@/components/sistema/colores';
import type { Color, Sistema } from '@/lib/sistema/tipos';
import { PERIODOS_DE_VENTAS, type PeriodoDeVentas } from '@/lib/sistema/metricasInicio';

// Hoy, esta semana o este mes, como botones: el elegido queda lleno.
export function SelectorDePeriodo({
  valor,
  onCambio,
  variante = 'claro',
}: {
  valor: PeriodoDeVentas;
  onCambio: (periodo: PeriodoDeVentas) => void;
  variante?: 'claro' | 'oscuro';
}) {
  return (
    <div
      className={`selector-periodo selector-periodo-${variante}`}
      role="group"
      aria-label="Período"
    >
      {PERIODOS_DE_VENTAS.map((opcion) => (
        <button
          key={opcion.clave}
          type="button"
          aria-pressed={valor === opcion.clave}
          onClick={() => onCambio(opcion.clave)}
        >
          {opcion.boton}
        </button>
      ))}
    </div>
  );
}

// Los mismos colores que las tarjetas de «Tus cuadernos», para reconocer cada cuaderno.
const COLORES: Color[] = ['petrol', 'orange', 'blue', 'coral', 'yellow'];

// De qué cuaderno son los números: con uno solo es su nombre; con varios, un botón que abre una lista propia
// (la lista nativa del navegador no sigue el diseño). Se maneja con el mouse, con el dedo o con el teclado.
export function SelectorDeCuaderno({
  cuadernos,
  cuaderno,
  onCambio,
}: {
  cuadernos: Sistema[];
  cuaderno: Sistema;
  onCambio: (id: string) => void;
}) {
  const [abierto, setAbierto] = useState(false);
  const [marcado, setMarcado] = useState(0);
  const contenedor = useRef<HTMLDivElement>(null);
  const boton = useRef<HTMLButtonElement>(null);
  const lista = useRef<HTMLUListElement>(null);
  const idLista = useId();

  useEffect(() => {
    if (!abierto) return undefined;
    lista.current?.focus();
    const alTocarAfuera = (evento: PointerEvent) => {
      if (!contenedor.current?.contains(evento.target as Node)) setAbierto(false);
    };
    document.addEventListener('pointerdown', alTocarAfuera);
    return () => document.removeEventListener('pointerdown', alTocarAfuera);
  }, [abierto]);

  if (cuadernos.length < 2) return <strong className="selector-cuaderno">{cuaderno.name}</strong>;

  const abrir = () => {
    setMarcado(
      Math.max(
        0,
        cuadernos.findIndex((item) => item.id === cuaderno.id),
      ),
    );
    setAbierto(true);
  };
  const cerrar = () => {
    setAbierto(false);
    boton.current?.focus();
  };
  const elegir = (id: string) => {
    if (id !== cuaderno.id) onCambio(id);
    cerrar();
  };
  const alApretarTecla = (evento: KeyboardEvent<HTMLUListElement>) => {
    const ultimo = cuadernos.length - 1;
    const teclas: Record<string, () => void> = {
      ArrowDown: () => setMarcado((actual) => Math.min(actual + 1, ultimo)),
      ArrowUp: () => setMarcado((actual) => Math.max(actual - 1, 0)),
      Home: () => setMarcado(0),
      End: () => setMarcado(ultimo),
      Enter: () => elegir(cuadernos[marcado]?.id ?? cuaderno.id),
      ' ': () => elegir(cuadernos[marcado]?.id ?? cuaderno.id),
      Escape: cerrar,
      Tab: () => setAbierto(false),
    };
    const accion = teclas[evento.key];
    if (!accion) return;
    if (evento.key !== 'Tab') evento.preventDefault();
    accion();
  };

  return (
    <div className="selector-cuaderno-caja" ref={contenedor}>
      <button
        ref={boton}
        type="button"
        className="selector-cuaderno selector-cuaderno-boton"
        aria-haspopup="listbox"
        aria-expanded={abierto}
        aria-controls={abierto ? idLista : undefined}
        aria-label={`Cuaderno: ${cuaderno.name}`}
        onClick={() => (abierto ? setAbierto(false) : abrir())}
        onKeyDown={(evento) => {
          if (evento.key === 'ArrowDown' || evento.key === 'ArrowUp') {
            evento.preventDefault();
            abrir();
          }
        }}
      >
        <span>{cuaderno.name}</span>
        <Icon name="down" />
      </button>
      {abierto && (
        <ul
          ref={lista}
          id={idLista}
          className="selector-cuaderno-lista"
          role="listbox"
          aria-label="Cuadernos"
          tabIndex={-1}
          aria-activedescendant={`${idLista}-${marcado}`}
          onKeyDown={alApretarTecla}
        >
          {cuadernos.map((item, indice) => (
            <li
              key={item.id}
              id={`${idLista}-${indice}`}
              role="option"
              aria-selected={item.id === cuaderno.id}
              data-marcado={indice === marcado ? 'true' : undefined}
              onPointerEnter={() => setMarcado(indice)}
              onClick={() => elegir(item.id)}
            >
              <span
                className="selector-cuaderno-punto"
                style={varsDeColor(COLORES[indice % COLORES.length] ?? 'petrol')}
              />
              <span className="selector-cuaderno-nombre">{item.name}</span>
              {item.id === cuaderno.id && <Icon name="check" />}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
