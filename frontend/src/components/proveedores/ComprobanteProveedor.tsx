import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import type { ProveedoresDemo } from '@/hooks/proveedores/useProveedoresDemo';
import { PanelProveedor } from './PanelProveedor';
import { RevisionProveedor } from './RevisionProveedor';

type Props = Pick<
  ProveedoresDemo,
  | 'datos'
  | 'archivo'
  | 'vistaArchivo'
  | 'seleccionarArchivo'
  | 'simularLectura'
  | 'revision'
  | 'actualizarRevision'
  | 'actualizarLinea'
  | 'agregarLineaCompra'
  | 'revisado'
  | 'setRevisado'
  | 'guardarCompra'
  | 'cancelarRevision'
  | 'errorRevision'
  | 'totalRevision'
>;
export function ComprobanteProveedor(p: Props) {
  return (
    <div className="prov-flujo-compra">
      <PanelProveedor
        titulo="De la compra al stock"
        detalle="Adjuntá un ticket, remito o factura para probar la revisión."
        icono="upload"
      >
        <div className="prov-nota">
          <strong>Esto es una simulación de lectura.</strong> El archivo se previsualiza en tu
          equipo. No se envía a una IA ni se interpreta su contenido. La demo propone renglones
          ficticios editables; sólo se guarda el nombre del archivo, sin conservar el PDF o la
          imagen.
        </div>
        <div className="prov-carga">
          <Icon name="image" />
          <label className="prov-campo">
            Elegir comprobante (PDF o imagen, hasta 10 MB)
            <input
              type="file"
              accept="application/pdf,image/jpeg,image/png,image/webp"
              onChange={(e) => p.seleccionarArchivo(e.target.files?.[0])}
            />
          </label>
          <div className="prov-acciones">
            <Btn cls="primary" disabled={!p.archivo} onClick={() => p.simularLectura()}>
              <Icon name="spark" /> Simular lectura del comprobante
            </Btn>
            <Btn cls="secondary" onClick={() => p.simularLectura(true)}>
              Probar sin archivo
            </Btn>
          </div>
        </div>
        {p.archivo && (
          <details className="prov-adjunto">
            <summary>Ver archivo local: {p.archivo.name}</summary>
            {p.vistaArchivo &&
              (p.archivo.type === 'application/pdf' ? (
                <object
                  data={p.vistaArchivo}
                  type="application/pdf"
                  aria-label="Vista previa del comprobante"
                >
                  <p>
                    Tu navegador no muestra PDF incrustado. Abrí el archivo original en tu
                    dispositivo.
                  </p>
                </object>
              ) : (
                <img src={p.vistaArchivo} alt="Comprobante seleccionado para la simulación" />
              ))}
          </details>
        )}
      </PanelProveedor>
      {p.revision && <RevisionProveedor {...p} revision={p.revision} />}
    </div>
  );
}
