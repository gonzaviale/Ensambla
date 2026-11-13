import { useState } from 'react';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { CajaMostrador } from '@/components/caja/CajaMostrador';
import { Icon } from '@/components/sistema/Icon';
import { useSistemaStore } from '@/stores/sistemaStore';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { allBlocks, makeBlock } from '@/lib/sistema/core';
import { prepararCaja } from '@/lib/caja';

export function CajaPage() {
  useTituloPagina('Caja');
  const sistemas = useSistemaStore(s => s.sistemas);
  const activeId = useSistemaStore(s => s.activeId);
  const [elegido, setElegido] = useState(activeId ?? '');
  const [tabla, setTabla] = useState('');
  const [error, setError] = useState('');
  const sistema = sistemas.find(s => s.id === elegido) ?? sistemas[0];
  const inventarios = allBlocks(sistema).filter(b => b.type === 'inventory');
  const inventario = inventarios.find(b => b.id === tabla) ?? inventarios[0];
  const ventas = allBlocks(sistema).find(b => b.type === 'sales' && b.fields.some(f => f.key === 'product' && f.target === inventario?.id));
  const lista = !!inventario?.fields.some(f => f.key === 'saleMode') && !!ventas && ventas.fields.some(f => f.key === 'quantity' && f.min === 0.001);
  const preparar = () => {
    if (!sistema) return;
    try {
      useSistemaStore.getState().actualizarSistema(sistema.id, copia => {
        let destino = inventario?.id;
        if (!destino) { const nueva = makeBlock('inventory'); copia.sections[0]?.blocks.push(nueva); destino = nueva.id; }
        prepararCaja(copia, destino);
      });
      setError('');
    } catch (e) { setError(e instanceof Error ? e.message : 'Revisá la configuración.'); }
  };
  return <><CabeceraComun active="caja" /><main id="main" className="caja-pagina" tabIndex={-1}>
    <header className="caja-cabecera"><div><span className="caja-antetitulo">TU MOSTRADOR</span><h1>Caja</h1><p>Escaneá, sumá y cobrá. Todo en el mismo lugar.</p></div><span className="caja-negocio"><Icon name="box" />{sistema?.name ?? 'Tu negocio'}</span></header>
    {sistemas.length > 1 && <div className="caja-opciones" aria-label="Elegir cuaderno de caja">{sistemas.map(s => <button type="button" key={s.id} aria-pressed={s.id === sistema?.id} onClick={() => { setElegido(s.id); setTabla(''); }}>{s.name}</button>)}</div>}
    {inventarios.length > 1 && <div className="caja-opciones" aria-label="Elegir inventario">{inventarios.map(b => <button type="button" key={b.id} aria-pressed={b.id === inventario?.id} onClick={() => setTabla(b.id)}>{b.name}</button>)}</div>}
    {error && <p role="alert" className="caja-error">{error}</p>}
    {!sistema ? <section className="caja-preparar"><Icon name="box" /><h2>Primero, tu negocio</h2><p>Creá tu espacio desde Mi Negocio. Después vas a poder cobrar con sus productos desde acá.</p></section>
      : !lista || !inventario || !ventas ? <section className="caja-preparar"><Icon name="orders" /><h2>Dejemos tu caja lista</h2><p>Usaremos {inventario?.name ?? 'un inventario nuevo'} de {sistema.name}. Se habilitan favoritos y venta por unidad o peso, y se prepara una tabla de ventas conectada.</p><button type="button" className="btn primary" onClick={preparar}>Preparar caja</button></section>
        : <CajaMostrador key={`${sistema.id}:${inventario.id}`} sistemaId={sistema.id} inventarioId={inventario.id} ventasId={ventas.id} />}
  </main></>;
}
