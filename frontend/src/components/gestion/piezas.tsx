import type { ReactNode } from 'react';
import { Icon } from '@/components/sistema/Icon';
import { Sami } from '@/components/sistema/Sami';
import { conPuntos, montoDe, pesos } from '@/lib/gestion/formato';
import type { NombreDeIcono } from '@/lib/sistema/iconos';

// Piezas chicas que comparten las tres pantallas de «Mi negocio».

export type PantallaDeNegocio =
  'cierre' | 'compras' | 'proyeccion' | 'pedidos' | 'ofertas' | 'monotributo';

const PANTALLAS: { clave: PantallaDeNegocio; etiqueta: string; icono: NombreDeIcono }[] = [
  { clave: 'cierre', etiqueta: 'Cierre del día', icono: 'wallet' },
  { clave: 'compras', etiqueta: 'Compras', icono: 'box' },
  { clave: 'proyeccion', etiqueta: '¿Me conviene?', icono: 'chart' },
  { clave: 'pedidos', etiqueta: 'Pedidos', icono: 'orders' },
  { clave: 'ofertas', etiqueta: 'Ofertas', icono: 'spark' },
  { clave: 'monotributo', etiqueta: 'Monotributo', icono: 'note' },
];

/** Las tres pestañas de «Mi negocio». */
export function PestanasDeNegocio({
  activa,
  onElegir,
}: {
  activa: PantallaDeNegocio;
  onElegir: (pantalla: PantallaDeNegocio) => void;
}) {
  return (
    <nav className="gn-pestanas" aria-label="Mi negocio">
      {PANTALLAS.map(({ clave, etiqueta, icono }) => (
        <button
          key={clave}
          type="button"
          className={clave === activa ? 'active' : ''}
          aria-current={clave === activa ? 'page' : undefined}
          onClick={() => onElegir(clave)}
        >
          <Icon name={icono} /> {etiqueta}
        </button>
      ))}
    </nav>
  );
}

export type Tono = 'neutro' | 'bien' | 'mal' | 'ojo';

/** Una cifra grande con su explicación. */
export function Cifra({
  etiqueta,
  valor,
  detalle,
  tono = 'neutro',
}: {
  etiqueta: string;
  valor: string;
  detalle?: ReactNode;
  tono?: Tono;
}) {
  return (
    <div className="gn-cifra" data-tono={tono}>
      <span className="gn-cifra-etiqueta">{etiqueta}</span>
      <strong className="gn-cifra-valor">{valor}</strong>
      {detalle && <span className="gn-cifra-detalle">{detalle}</span>}
    </div>
  );
}

export type PasoDeCuenta = {
  etiqueta: string;
  valor: number;
  /** `resta` se muestra con «−», `total` cierra un tramo de la cuenta. */
  clase?: 'suma' | 'resta' | 'total';
  detalle?: string;
};

/** Una cuenta paso a paso, como se haría en el cuaderno: cada resta con su motivo y los subtotales marcados. */
export function CuentaPasoAPaso({ pasos, titulo }: { pasos: PasoDeCuenta[]; titulo?: string }) {
  return (
    <table className="gn-cuenta">
      {titulo && <caption>{titulo}</caption>}
      <tbody>
        {pasos.map((paso) => (
          <tr key={paso.etiqueta} data-clase={paso.clase ?? 'suma'}>
            <th scope="row">
              {paso.etiqueta}
              {paso.detalle && <small>{paso.detalle}</small>}
            </th>
            <td>
              {paso.clase === 'resta' ? '− ' : ''}
              {pesos(Math.abs(paso.valor))}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/** Lo que dice Sami, con su cara al lado (a la izquierda, o a la derecha con `lado`). */
export function DiceSami({
  children,
  pose = 'hello',
  lado = 'izquierda',
}: {
  children: ReactNode;
  pose?: 'hello' | 'think' | 'wink';
  lado?: 'izquierda' | 'derecha';
}) {
  return (
    <div className="gn-sami" data-lado={lado === 'derecha' ? 'derecha' : undefined}>
      <Sami pose={pose} />
      <div className="gn-sami-texto">{children}</div>
    </div>
  );
}

/** Un mensaje de WhatsApp tal como le llegaría a la otra persona, o como llegó del cliente (`de`). */
export function MensajeDeWhatsapp({
  para,
  de,
  hora,
  texto,
}: {
  para?: string;
  de?: string;
  hora?: string;
  texto: string;
}) {
  return (
    <figure className="gn-whatsapp" data-entrante={de ? true : undefined}>
      <figcaption>
        {de ? (
          <>
            De <strong>{de}</strong>
          </>
        ) : (
          <>
            Para <strong>{para}</strong>
          </>
        )}{' '}
        · por WhatsApp{hora && ` · ${hora}`}
      </figcaption>
      <p>{texto}</p>
    </figure>
  );
}

/** Un campo para escribir plata: muestra los puntos de miles y devuelve el número. */
export function CampoDePlata({
  valor,
  onCambiar,
  etiqueta,
  placeholder = '0',
}: {
  valor: number;
  onCambiar: (valor: number) => void;
  /** Para lectores de pantalla, cuando el campo no tiene una etiqueta visible. */
  etiqueta?: string;
  placeholder?: string;
}) {
  return (
    <span className="gn-monto">
      <span aria-hidden="true">$</span>
      <input
        type="text"
        inputMode="numeric"
        value={conPuntos(valor)}
        placeholder={placeholder}
        aria-label={etiqueta}
        onChange={(evento) => onCambiar(montoDe(evento.target.value))}
      />
    </span>
  );
}

/** Un bloque de la pantalla con título y una bajada corta. */
export function Seccion({
  titulo,
  bajada,
  children,
  acciones,
}: {
  titulo: string;
  bajada?: ReactNode;
  children: ReactNode;
  acciones?: ReactNode;
}) {
  return (
    <section className="gn-seccion">
      <header>
        <div>
          <h2>{titulo}</h2>
          {bajada && <p>{bajada}</p>}
        </div>
        {acciones}
      </header>
      {children}
    </section>
  );
}
