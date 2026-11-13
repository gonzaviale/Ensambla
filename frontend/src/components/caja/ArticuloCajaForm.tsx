import { useEffect, useRef, useState, type FormEvent } from 'react';
import { DialogoCaja } from './DialogoCaja';
import { dineroCaja, leerPrecio, modoVenta } from '@/lib/caja';
import type { Bloque, Valor } from '@/lib/sistema/tipos';
import type { DatosArticuloCaja, ModoVentaCaja } from '@/types/caja';
import type { SeleccionCaja } from '@/hooks/caja/useCaja';

export function ArticuloCajaForm({ seleccion, inventario, error, buscando, onGuardar, onCerrar }: {
  seleccion: SeleccionCaja; inventario: Bloque; error: string; buscando: boolean;
  onGuardar: (datos: DatosArticuloCaja) => void; onCerrar: () => void;
}) {
  const [nombre, setNombre] = useState(seleccion.nombre);
  const escrito = useRef(false);
  useEffect(() => { if (!escrito.current) setNombre(seleccion.nombre); }, [seleccion.nombre]);
  const [modo, setModo] = useState<ModoVentaCaja>(seleccion.producto ? modoVenta(seleccion.producto) : 'unidad');
  const [precio, setPrecio] = useState(String(seleccion.producto?.price ?? ''));
  const [cantidad, setCantidad] = useState(modo === 'peso' ? '100' : '1');
  const [favorito, setFavorito] = useState(seleccion.favorito);
  const [controlarStock, setControlarStock] = useState(seleccion.producto?.stockControl === true);
  const [extras, setExtras] = useState<Record<string, Valor>>({});
  const [errorLocal, setErrorLocal] = useState('');
  const soloPeso = !!seleccion.producto && modoVenta(seleccion.producto) === 'peso' && !seleccion.soloGuardar;
  const adicionales = inventario.fields.filter(c => c.required && !['name','price','quantity','barcode'].includes(c.key));
  let importe = '';
  try {
    const resultado = Math.round(leerPrecio(precio) * Number(cantidad) / (modo === 'peso' ? 1000 : 1));
    if (Number.isSafeInteger(resultado) && resultado > 0) importe = dineroCaja(resultado);
  } catch { /* El formulario muestra el error al confirmar. */ }
  const guardar = (e: FormEvent) => {
    e.preventDefault();
    if (!nombre.trim()) { setErrorLocal('Completá el nombre del artículo.'); return; }
    onGuardar({ nombre, precio, modo, favorito, controlarStock, cantidad, extras });
  };
  return <DialogoCaja titulo={soloPeso ? seleccion.nombre : seleccion.producto ? 'Editar artículo' : 'Agregar artículo'} onCerrar={onCerrar}>
    {seleccion.mensaje && <p className="caja-nota">{seleccion.mensaje}</p>}
    {buscando && <p role="status">Buscando una referencia… Podés completar los datos mientras tanto.</p>}
    {seleccion.imagen && <img className="caja-foto" src={seleccion.imagen} alt="Referencia del producto" />}
    <form onSubmit={guardar} noValidate className="caja-formulario">
      {!soloPeso && <>
        <label>Nombre del artículo<input value={nombre} onChange={e => { escrito.current = true; setNombre(e.target.value); }} maxLength={120} autoComplete="off" /></label>
        <fieldset><legend>Se vende por</legend><div className="caja-opciones">
          {(['unidad','peso'] as const).map(valor => <button type="button" key={valor} aria-pressed={modo === valor}
            onClick={() => { setModo(valor); setControlarStock(false); setCantidad(valor === 'peso' ? '100' : '1'); }}>{valor === 'peso' ? 'Peso' : 'Unidad'}</button>)}
        </div></fieldset>
        <label>{modo === 'peso' ? 'Precio por kilo ($)' : 'Precio por unidad ($)'}<input inputMode="decimal" value={precio} onChange={e => setPrecio(e.target.value)} placeholder="0,00" /></label>
      </>}
      {soloPeso && <p className="caja-precio-referencia">{dineroCaja(Math.round(Number(precio) * 100))} / kg</p>}
      {!seleccion.soloGuardar && <>
        <label>{modo === 'peso' ? 'Gramos' : 'Cantidad'}<input inputMode="numeric" value={cantidad} onChange={e => setCantidad(e.target.value)} /></label>
        {modo === 'peso' && <><div className="caja-opciones">{[100,250,500,1000].map(g => <button type="button" key={g} onClick={() => setCantidad(String(g))}>{g === 1000 ? '1 kg' : `${g} g`}</button>)}</div>
          <p className="caja-ayuda">Podés calcular antes de pesar y después ajustar los gramos al peso real.</p></>}
        <output className="caja-importe-previo" aria-live="polite">{importe || 'Completá cantidad y precio'}</output>
      </>}
      {!soloPeso && <>
        <label className="caja-check"><input type="checkbox" checked={favorito} onChange={e => setFavorito(e.target.checked)} />Agregar a favoritos</label>
        {seleccion.producto && <label className="caja-check"><input type="checkbox" checked={controlarStock} onChange={e => setControlarStock(e.target.checked)} />Controlar stock al vender</label>}
        {seleccion.producto && modo !== modoVenta(seleccion.producto) && <p className="caja-ayuda">Cambiaste la forma de venta. Revisá las existencias en el inventario antes de volver a controlar stock: para peso, se cuentan en kilos.</p>}
        {!seleccion.producto && <p className="caja-ayuda">Quedará guardado en tu inventario. El costo y el conteo de stock se completan aparte.</p>}
        {adicionales.length > 0 && <details><summary>Otros datos requeridos del inventario</summary>{adicionales.map(campo =>
          <label key={campo.key}>{campo.label}{campo.type === 'select' ? <div className="caja-opciones">{campo.options?.map(opcion => <button type="button" key={opcion}
            aria-pressed={(extras[campo.key] ?? seleccion.producto?.[campo.key]) === opcion} onClick={() => setExtras(v => ({ ...v, [campo.key]: opcion }))}>{opcion}</button>)}</div>
            : <input inputMode={['number','currency'].includes(campo.type) ? 'decimal' : 'text'} value={String(extras[campo.key] ?? seleccion.producto?.[campo.key] ?? '')}
              onChange={e => setExtras(v => ({ ...v, [campo.key]: ['number','currency'].includes(campo.type) ? Number(e.target.value) : e.target.value }))} />}</label>)}</details>}
      </>}
      {(error || errorLocal) && <p className="caja-error" role="alert">{error || errorLocal}</p>}
      <div className="caja-acciones"><button type="button" className="btn secondary" onClick={onCerrar}>Cancelar</button>
        <button type="submit" className="btn primary">{seleccion.soloGuardar ? 'Guardar artículo' : 'Agregar a la compra'}</button></div>
    </form>
  </DialogoCaja>;
}
