import { useRef, useState } from 'react';
import type { KeyboardEvent, ReactElement } from 'react';
import { Link } from '@tanstack/react-router';
import { CameraIcon, KeyboardIcon, MicrophoneIcon, WhatsappLogoIcon } from '@phosphor-icons/react';
import { variables } from './estilos';
import { SamiCara } from './Sami';

const MODOS = [
  { id: 'foto', nombre: 'Foto', Icono: CameraIcon },
  { id: 'voz', nombre: 'Voz', Icono: MicrophoneIcon },
  { id: 'texto', nombre: 'Texto', Icono: KeyboardIcon },
] as const;

type Modo = (typeof MODOS)[number]['id'];

const PASOS = [
  {
    titulo: 'Elegí cómo cargar',
    texto: 'Sacale una foto al cuaderno, dictale una nota de voz o escribile como en un mensaje.',
  },
  {
    titulo: 'Sami lo entiende',
    texto: 'Lo ordena en tablas y marca los datos que no pudo leer o escuchar con seguridad.',
  },
  {
    titulo: 'Confirmá y listo',
    texto: 'Nada se guarda sin tu OK. Queda en tu sistema, sin papel y sin planillas.',
  },
] as const;

const ANOTACIONES = ['Yerba 500 g · 12', 'Fideos · 8', 'Gaseosa 1,5 L · 5'] as const;

const FILAS = [
  { producto: 'Yerba 500 g', cantidad: '12', duda: false },
  { producto: 'Fideos', cantidad: '8', duda: false },
  { producto: 'Gaseosa 1,5 L', cantidad: '5', duda: true },
] as const;

const BARRAS = 16;
const TEXTO_ESCRITO = 'Llegaron 24 gaseosas a $2.300 c/u';

function PanelFoto() {
  return (
    <div className="foto__visual">
      <div className="foto__hoja">
        <span className="foto__esquina foto__esquina--1" />
        <span className="foto__esquina foto__esquina--2" />
        <span className="foto__esquina foto__esquina--3" />
        <span className="foto__esquina foto__esquina--4" />
        {ANOTACIONES.map((linea) => (
          <p key={linea}>{linea}</p>
        ))}
      </div>
      <span className="foto__flecha" aria-hidden="true">
        →
      </span>
      <div className="foto__tabla">
        <div className="foto__fila foto__fila--cabeza">
          <span>Producto</span>
          <span>Cantidad</span>
        </div>
        {FILAS.map(({ producto, cantidad, duda }) => (
          <div key={producto} className="foto__fila">
            <span>{producto}</span>
            <span className={duda ? 'foto__celda foto__celda--duda' : 'foto__celda'}>
              {cantidad}
            </span>
          </div>
        ))}
        <p className="foto__aviso">Revisá este dato: Sami no estuvo seguro.</p>
      </div>
    </div>
  );
}

function PanelVoz() {
  return (
    <div className="chat">
      <div className="chat__voz" style={variables({ '--d': '0s' })}>
        <span className="chat__mic">
          <MicrophoneIcon aria-hidden size={22} weight="fill" />
        </span>
        <span className="chat__onda" aria-hidden="true">
          {Array.from({ length: BARRAS }, (_, i) => (
            <i key={i} style={variables({ '--i': i })} />
          ))}
        </span>
        <span className="chat__tiempo">0:07</span>
      </div>
      <p className="chat__burbuja chat__burbuja--yo" style={variables({ '--d': '0.9s' })}>
        “Vendí 3 yerbas y 2 paquetes de fideos a Marta.”
      </p>
      <div className="chat__sami" style={variables({ '--d': '1.7s' })}>
        <SamiCara className="chat__cara" />
        <div className="chat__burbuja chat__burbuja--sami">
          <p>
            Anoté <b>2 ventas</b> para Marta. ¿Las confirmás?
          </p>
          <ul className="chat__filas">
            <li>
              <span>Yerba 500 g</span>
              <b>× 3</b>
            </li>
            <li>
              <span>Fideos 500 g</span>
              <b>× 2</b>
            </li>
          </ul>
          <div className="chat__acciones" aria-hidden="true">
            <span className="chat__boton chat__boton--ok">Confirmar</span>
            <span className="chat__boton">Corregir</span>
          </div>
        </div>
      </div>
    </div>
  );
}

function PanelTexto() {
  return (
    <div className="chat">
      <div className="chat__escribir" style={variables({ '--d': '0s' })}>
        <span
          className="chat__tipeo"
          style={variables({ '--ancho': `${(TEXTO_ESCRITO.length * 0.56).toFixed(1)}em` })}
        >
          {TEXTO_ESCRITO}
        </span>
        <span className="chat__cursor" aria-hidden="true" />
      </div>
      <div className="chat__sami" style={variables({ '--d': '2.3s' })}>
        <SamiCara className="chat__cara" />
        <div className="chat__burbuja chat__burbuja--sami">
          <p>
            Sumé <b>24 unidades</b> al stock. ¿Actualizo el precio?
          </p>
          <ul className="chat__filas">
            <li>
              <span>Stock · Gaseosa 1,5 L</span>
              <b>+ 24</b>
            </li>
            <li>
              <span>Precio de venta</span>
              <b>$2.300</b>
            </li>
          </ul>
          <div className="chat__acciones" aria-hidden="true">
            <span className="chat__boton chat__boton--ok">Confirmar</span>
            <span className="chat__boton">Corregir</span>
          </div>
        </div>
      </div>
    </div>
  );
}

const PANELES: Record<Modo, () => ReactElement> = {
  foto: PanelFoto,
  voz: PanelVoz,
  texto: PanelTexto,
};

export function EntradaDatos() {
  const [modo, setModo] = useState<Modo>('foto');
  const pestanas = useRef<Partial<Record<Modo, HTMLButtonElement | null>>>({});
  const Panel = PANELES[modo];

  const alTeclear = (e: KeyboardEvent<HTMLDivElement>) => {
    const actual = MODOS.findIndex((m) => m.id === modo);
    const destino =
      e.key === 'ArrowRight'
        ? (actual + 1) % MODOS.length
        : e.key === 'ArrowLeft'
          ? (actual - 1 + MODOS.length) % MODOS.length
          : e.key === 'Home'
            ? 0
            : e.key === 'End'
              ? MODOS.length - 1
              : null;
    if (destino === null) return;
    e.preventDefault();
    const siguiente = MODOS[destino];
    if (!siguiente) return;
    setModo(siguiente.id);
    pestanas.current[siguiente.id]?.focus();
  };

  return (
    <section className="seccion" id="entrada">
      <div className="contenedor entrada">
        <div className="entrada__texto revelar">
          <p className="sobretitulo">Cargá como te salga</p>
          <h2>Foto, voz o texto. Sami hace el resto.</h2>
          <p className="entrada__bajada">
            Si hoy anotás todo en un cuaderno, no hace falta empezar de cero. Sacale una foto,
            dictale una nota de voz o escribile, y Sami lo pasa a tablas listas para usar.
          </p>
          <ol className="entrada__pasos">
            {PASOS.map(({ titulo, texto }) => (
              <li key={titulo}>
                <h3>{titulo}</h3>
                <p>{texto}</p>
              </li>
            ))}
          </ol>
          <p className="entrada__whatsapp">
            <WhatsappLogoIcon aria-hidden size={22} weight="fill" />
            También podés mandarle todo a Sami por WhatsApp.
          </p>
          <Link to="/registro" className="boton boton--primario">
            Probalo con tu negocio
          </Link>
        </div>

        <div className="entrada__demo revelar" style={variables({ '--d': '.1s' })}>
          <div
            className="entrada__pestanas"
            role="tablist"
            aria-label="Formas de cargar tus datos"
            onKeyDown={alTeclear}
          >
            {MODOS.map(({ id, nombre, Icono }) => (
              <button
                key={id}
                ref={(el) => {
                  pestanas.current[id] = el;
                }}
                type="button"
                role="tab"
                id={`entrada-pestana-${id}`}
                aria-selected={modo === id}
                aria-controls="entrada-panel"
                tabIndex={modo === id ? 0 : -1}
                className="entrada__pestana"
                onClick={() => setModo(id)}
              >
                <Icono aria-hidden size={20} weight={modo === id ? 'fill' : 'regular'} />
                {nombre}
              </button>
            ))}
          </div>
          <div
            className="entrada__panel"
            id="entrada-panel"
            role="tabpanel"
            aria-labelledby={`entrada-pestana-${modo}`}
          >
            <Panel key={modo} />
          </div>
        </div>
      </div>
    </section>
  );
}
