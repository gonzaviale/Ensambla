import { PackageIcon, SparkleIcon, TableIcon, UsersIcon } from '@phosphor-icons/react';

/** Ilustración de las piezas del sistema: no representa registros del usuario. */
export function SistemaIlustracion() {
  return (
    <div className="proyectos-system-illustration" aria-hidden="true">
      <span className="proyectos-system-sticker">
        <SparkleIcon size={15} />
        Cada pieza encuentra su lugar.
      </span>
      <div className="proyectos-system-heading">
        <i />
        <strong>Un sistema. Tu forma de organizarlo.</strong>
        <span>Ejemplo</span>
      </div>
      <div className="proyectos-system-board">
        <div className="proyectos-example-module proyectos-example-wide">
          <div className="proyectos-example-title">
            <i>
              <PackageIcon size={20} />
            </i>
            <strong>Productos</strong>
            <span>•••</span>
          </div>
          <div className="proyectos-example-columns">
            <span>Nombre</span>
            <span>Stock</span>
            <span>Precio</span>
          </div>
          {[0, 1, 2].map((fila) => (
            <div key={fila} className="proyectos-example-row">
              <span />
              <span />
              <span />
            </div>
          ))}
        </div>
        <div className="proyectos-example-module proyectos-example-clients">
          <div className="proyectos-example-title">
            <i>
              <UsersIcon size={20} />
            </i>
            <strong>Clientes</strong>
          </div>
          <div className="proyectos-example-tags">
            <span>Nombre</span>
            <span>Teléfono</span>
            <span>Dirección</span>
          </div>
          <div className="proyectos-example-lines">
            <i />
            <i />
            <i />
          </div>
        </div>
        <div className="proyectos-example-module proyectos-example-orders">
          <div className="proyectos-example-title">
            <i>
              <TableIcon size={20} />
            </i>
            <strong>Pedidos</strong>
          </div>
          <div className="proyectos-example-tags">
            <span>Cliente</span>
            <span>Fecha</span>
            <span>Estado</span>
          </div>
          <div className="proyectos-example-lines">
            <i />
            <i />
            <i />
          </div>
        </div>
      </div>
      <p className="proyectos-system-caption">Tablas que se adaptan a tu negocio.</p>
    </div>
  );
}
