import { useState } from 'react';
import { ReceiptIcon } from '@phosphor-icons/react/dist/csr/Receipt';
import { WalletIcon } from '@phosphor-icons/react/dist/csr/Wallet';
import { HandshakeIcon } from '@phosphor-icons/react/dist/csr/Handshake';
import { PackageIcon } from '@phosphor-icons/react/dist/csr/Package';
import { ArrowRightIcon } from '@phosphor-icons/react/dist/csr/ArrowRight';
import { ArrowCounterClockwiseIcon } from '@phosphor-icons/react/dist/csr/ArrowCounterClockwise';
import { CheckCircleIcon } from '@phosphor-icons/react/dist/csr/CheckCircle';
import { SamiPoses } from './SamiPoses';

const ANOTACIONES = [
  {
    nombre: 'Ventas',
    pose: 'hello',
    Icono: ReceiptIcon,
    mensaje: 'Podés separar qué vendés, cuántas unidades y por qué importe.',
    apunte: 'Vendí 2 paquetes de yerba de 1 kg a $ 3.500 cada uno. Total: $ 7.000.',
    campos: [
      ['Producto', 'Yerba de 1 kg'],
      ['Cantidad', '2 unidades'],
      ['Total de la venta', '$ 7.000'],
    ],
  },
  {
    nombre: 'Gastos',
    pose: 'think',
    Icono: WalletIcon,
    mensaje: 'Identificá en qué gastás y cómo lo pagás para consultar tus gastos después.',
    apunte: 'Pagué $ 18.500 al proveedor por transferencia para reponer mercadería.',
    campos: [
      ['Concepto', 'Reposición de mercadería'],
      ['Medio de pago', 'Transferencia'],
      ['Importe del gasto', '$ 18.500'],
    ],
  },
  {
    nombre: 'Fiados',
    pose: 'wink',
    Icono: HandshakeIcon,
    mensaje: 'Con el nombre y el importe juntos, podés encontrar cada fiado cuando lo necesitás.',
    apunte: 'Ana López llevó productos del almacén y quedó debiendo $ 3.200.',
    campos: [
      ['Cliente', 'Ana López'],
      ['Concepto', 'Productos del almacén'],
      ['Importe pendiente', '$ 3.200'],
    ],
  },
  {
    nombre: 'Productos',
    pose: 'build',
    Icono: PackageIcon,
    mensaje: 'Reuní el nombre, la cantidad y el precio de cada producto en un mismo lugar.',
    apunte: 'Arroz de 1 kg: quedan 12 unidades y cada una cuesta $ 1.800.',
    campos: [
      ['Producto', 'Arroz de 1 kg'],
      ['Cantidad disponible', '12 unidades'],
      ['Precio por unidad', '$ 1.800'],
    ],
  },
] as const;

export function SamiAgente() {
  const [ejemplo, setEjemplo] = useState({ indice: 0, organizado: false });
  const actual = ANOTACIONES[ejemplo.indice] ?? ANOTACIONES[0];
  return (
    <section className="ens-section" id="a-tu-manera" aria-labelledby="a-tu-manera-titulo">
      <div className="ens-wrap ens-your-way ens-your-way--centered">
        <p className="ens-eyebrow">A tu manera</p>
        <h2 id="a-tu-manera-titulo">Tu negocio, tu manera de anotar</h2>
        <p className="ens-lead">
          Empezá por lo que necesitás hoy y sumá el resto cuando lo necesites.
        </p>
        <p className="ens-sami-guide">Elegí qué querés organizar y probá el ejemplo con Sami.</p>
      </div>
      <div className="ens-sami-explorer">
        <div className="ens-sami-categories-band">
          <ul className="ens-wrap ens-sami-categories" aria-label="Elegí qué querés organizar">
            {ANOTACIONES.map(({ nombre, Icono }, indice) => (
              <li key={nombre}>
                <button
                  type="button"
                  aria-pressed={ejemplo.indice === indice}
                  aria-controls="sami-ejemplo"
                  onClick={() => setEjemplo({ indice, organizado: false })}
                >
                  <span className="ens-sami-category-icon">
                    <Icono aria-hidden size={24} />
                  </span>
                  <span>{nombre}</span>
                </button>
              </li>
            ))}
          </ul>
        </div>
        <div className="ens-sami-playground">
          <div
            className="ens-wrap ens-sami-workspace"
            id="sami-ejemplo"
            role="region"
            aria-label={`Ejemplo de ${actual.nombre}`}
          >
            <div className="ens-sami-companion" key="sami-unico">
              <SamiPoses pose={actual.pose} />
              <p className="ens-sami-message" key={`${actual.nombre}-${ejemplo.organizado}`}>
                <strong>Sami, tu asistente</strong>
                {ejemplo.organizado
                  ? 'Así podés revisar cada dato por separado antes de guardarlo en tu sección.'
                  : actual.mensaje}
              </p>
            </div>
            <div className="ens-sami-notebook" key={`seccion-${actual.nombre}`}>
              <div className="ens-sami-notebook-head">
                <span>Tu sección · {actual.nombre}</span>
                <span>Ejemplo</span>
              </div>
              <div className="ens-sami-notebook-body">
                <p className="ens-sami-view-label">
                  {ejemplo.organizado ? 'Tu información organizada' : 'Tu anotación'}
                </p>
                <div className="ens-sami-page" key={String(ejemplo.organizado)}>
                  {ejemplo.organizado ? (
                    <dl className="ens-sami-fields">
                      {actual.campos.map(([nombre, valor]) => (
                        <div key={nombre}>
                          <dt>{nombre}</dt>
                          <dd>{valor}</dd>
                        </div>
                      ))}
                    </dl>
                  ) : (
                    <blockquote className="ens-sami-handwriting">{actual.apunte}</blockquote>
                  )}
                </div>
                <button
                  type="button"
                  className="ens-primary ens-sami-organize"
                  onClick={() =>
                    setEjemplo((previo) => ({ ...previo, organizado: !previo.organizado }))
                  }
                >
                  {ejemplo.organizado ? (
                    <>
                      <ArrowCounterClockwiseIcon aria-hidden size={20} />
                      Volver a la anotación
                    </>
                  ) : (
                    <>
                      Ver cómo se organiza
                      <ArrowRightIcon aria-hidden size={20} />
                    </>
                  )}
                </button>
                <p className="ens-sami-status" role="status">
                  {ejemplo.organizado ? (
                    <>
                      <CheckCircleIcon aria-hidden size={18} />
                      Ejemplo de {actual.nombre.toLocaleLowerCase('es')} organizado para revisar.
                    </>
                  ) : (
                    'Probá cómo se ve tu anotación con los datos separados.'
                  )}
                </p>
              </div>
            </div>
          </div>
        </div>
        <p className="ens-wrap ens-sami-disclaimer">
          Este ejemplo usa datos ficticios y no guarda información.
        </p>
      </div>
    </section>
  );
}
