import { NotebookIcon } from '@phosphor-icons/react/dist/csr/Notebook';
import { MagnifyingGlassIcon } from '@phosphor-icons/react/dist/csr/MagnifyingGlass';
import { StorefrontIcon } from '@phosphor-icons/react/dist/csr/Storefront';

const DOLORES = [
  {
    Icono: NotebookIcon,
    pregunta: '¿Quién me debe?',
    texto: 'Tenés el fiado anotado, pero encontrarlo te lleva tiempo.',
  },
  {
    Icono: MagnifyingGlassIcon,
    pregunta: '¿Dónde está ese gasto?',
    texto: 'Entre tickets, hojas y mensajes, un dato puede quedar perdido.',
  },
  {
    Icono: StorefrontIcon,
    pregunta: '¿Cuánto llevo vendido hoy?',
    texto: 'Después de atender todo el día, todavía falta ordenar las ventas.',
  },
] as const;

export function Problema() {
  return (
    <section className="ens-section" id="el-problema" aria-labelledby="problema-titulo">
      <div className="ens-wrap ens-problem">
        <div>
          <p className="ens-eyebrow">Un día cualquiera en tu negocio</p>
          <h2 id="problema-titulo">Entre atender y anotar, siempre queda algo para después</h2>
          <p className="ens-lead">
            Cuando tenés una venta en una hoja, un gasto en otra y un fiado entre los mensajes,
            necesitás reunir toda la información para hacer las cuentas.
          </p>
        </div>
        <ul className="ens-pains">
          {DOLORES.map(({ Icono, pregunta, texto }) => (
            <li key={pregunta}>
              <span className="ens-icon">
                <Icono aria-hidden size={26} />
              </span>
              <div>
                <h3>“{pregunta}”</h3>
                <p>{texto}</p>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
