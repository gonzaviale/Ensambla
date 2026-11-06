import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { PageFooter } from '@/components/sistema/PageFooter';
import { Icon } from '@/components/sistema/Icon';
import { SelectorProveedor } from '@/components/proveedores/SelectorProveedor';
import { PedidoProveedor } from '@/components/proveedores/PedidoProveedor';
import { ComprobanteProveedor } from '@/components/proveedores/ComprobanteProveedor';
import { HistorialProveedor } from '@/components/proveedores/HistorialProveedor';
import { CostosProveedor } from '@/components/proveedores/CostosProveedor';
import { GastosProveedor } from '@/components/proveedores/GastosProveedor';
import { StockProveedor } from '@/components/proveedores/StockProveedor';
import { PedidoImprimible } from '@/components/proveedores/PedidoImprimible';
import { useUsuario } from '@/hooks/auth/queries';
import { useProveedoresDemo } from '@/hooks/proveedores/useProveedoresDemo';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import type { VistaProveedor } from '@/types/proveedores';
import type { NombreDeIcono } from '@/lib/sistema/iconos';

const VISTAS: { id: VistaProveedor; titulo: string; icono: NombreDeIcono }[] = [
  { id: 'pedido', titulo: 'Preparar pedido', icono: 'orders' },
  { id: 'comprobantes', titulo: 'Compras e historial', icono: 'upload' },
  { id: 'costos', titulo: 'Costos y gastos', icono: 'chart' },
  { id: 'stock', titulo: 'Stock de prueba', icono: 'box' },
];

export function ProveedoresPage() {
  const usuario = useUsuario();
  useTituloPagina('Proveedores · Demo');
  // Montar por cuenta impide trasladar el estado local de una persona a otra al cambiar de sesión.
  return <ProveedoresDeLaCuenta key={usuario.id} usuarioId={usuario.id} />;
}

function ProveedoresDeLaCuenta({ usuarioId }: { usuarioId: string }) {
  const demo = useProveedoresDemo(usuarioId);
  return (
    <>
      <CabeceraComun active="proveedores" />
      <main id="main" className="page-wrap prov-page reveal" tabIndex={-1}>
        <header className="prov-portada">
          <div>
            <span className="prov-eyebrow">TU NEGOCIO, MEJOR CONECTADO</span>
            <h1>Proveedores</h1>
            <p>
              Pedí lo que falta. Registrá lo que llega.
              <br />
              Entendé cómo cambian tus costos.
            </p>
          </div>
          <div className="prov-portada-icono">
            <Icon name="box" />
            <span>
              <Icon name="check" />
            </span>
          </div>
        </header>
        <div className="prov-demo">
          <span className="prov-insignia atencion">Prototipo funcional</span>
          <p>
            Datos ficticios, guardados sólo en este navegador y separados por cuenta. Esta prueba no
            modifica tus cuadernos reales. La lectura de archivos es simulada.
          </p>
        </div>
        <ol className="prov-pasos" aria-label="Cómo funciona">
          <li>
            <span>1</span>
            <div>
              <strong>Armá el pedido</strong>
              <small>Una sugerencia que vos ajustás</small>
            </div>
          </li>
          <li>
            <span>2</span>
            <div>
              <strong>Revisá la compra</strong>
              <small>Comprobante, stock y registro</small>
            </div>
          </li>
          <li>
            <span>3</span>
            <div>
              <strong>Compará y decidí</strong>
              <small>Costos, precios y gastos del local</small>
            </div>
          </li>
        </ol>
        <SelectorProveedor {...demo} />
        <nav className="prov-vistas" aria-label="Secciones de proveedores">
          {VISTAS.map((v) => (
            <button
              key={v.id}
              type="button"
              aria-current={demo.vista === v.id ? 'page' : undefined}
              className={demo.vista === v.id ? 'active' : ''}
              onClick={() => demo.setVista(v.id)}
            >
              <Icon name={v.icono} /> {v.titulo}
            </button>
          ))}
        </nav>
        <div role="status" className={`prov-estado ${demo.aviso ? 'visible' : ''}`}>
          {demo.aviso}
        </div>
        {demo.error && (
          <div className="prov-error" role="alert">
            {demo.error}
          </div>
        )}
        {demo.vista === 'pedido' && <PedidoProveedor {...demo} />}
        {demo.vista === 'comprobantes' && (
          <>
            <ComprobanteProveedor {...demo} />
            <HistorialProveedor {...demo} />
          </>
        )}
        {demo.vista === 'costos' && (
          <>
            <CostosProveedor {...demo} />
            <GastosProveedor {...demo} />
          </>
        )}
        {demo.vista === 'stock' && <StockProveedor {...demo} />}
        <PedidoImprimible {...demo} />
      </main>
      <PageFooter />
    </>
  );
}
