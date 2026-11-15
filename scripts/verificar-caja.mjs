import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';

const base = process.env.ENSAMBLA_URL || 'http://localhost:8088';
function sesion() {
  const cookies = new Map();
  let leidoEn;
  async function pedir(ruta, metodo = 'GET', cuerpo, conCsrf = true) {
    const headers = { Origin: new URL(base).origin };
    if (metodo !== 'GET' && conCsrf) {
      const token = await pedir('/api/auth/csrf');
      headers[token.datos.headerName] = token.datos.token;
    }
    headers.Cookie = [...cookies].map(([k,v]) => `${k}=${v}`).join('; ');
    if (cuerpo) headers['Content-Type'] = 'application/json';
    if (leidoEn && metodo === 'PUT') headers['X-Leido-En'] = leidoEn;
    const response = await fetch(new URL(ruta, base), { method: metodo, headers,
      body: cuerpo ? JSON.stringify(cuerpo) : undefined, signal: AbortSignal.timeout(20000) });
    for (const cookie of response.headers.getSetCookie()) {
      const par = cookie.split(';')[0]; const indice = par.indexOf('=');
      cookies.set(par.slice(0,indice),par.slice(indice+1));
    }
    leidoEn = response.headers.get('X-Leido-En') || leidoEn;
    const texto = await response.text();
    return { estado: response.status, datos: texto ? JSON.parse(texto) : null };
  }
  return { pedir };
}
async function exigir(cliente, ruta, estado, metodo, cuerpo, csrf) {
  const response = await cliente.pedir(ruta,metodo,cuerpo,csrf);
  assert.equal(response.estado,estado,`${metodo || 'GET'} ${ruta}: ${JSON.stringify(response.datos)}`);
  return response.datos;
}
async function cuenta() {
  const cliente = sesion();
  const usuario = { email: `caja-${randomUUID()}@example.test`, contrasenia: `Caja9!${randomUUID()}` };
  await exigir(cliente,'/api/auth/register',201,'POST',{...usuario,nombre:'Verificación de Caja',nombreNegocio:'Caja de prueba'});
  await exigir(cliente,'/api/auth/login',200,'POST',usuario);
  return cliente;
}
const campo = (key,type,extras={}) => ({key,label:key,type,required:false,...extras});
async function principal() {
  const cliente = await cuenta(), ajeno = await cuenta();
  const proyectoId=randomUUID(),inventarioId=randomUUID(),ventasId=randomUUID(),coca=randomUUID(),pan=randomUUID();
  const clientesId=randomUUID(),clienteId=randomUUID();
  const ahora=new Date().toISOString();
  const sistema={id:proyectoId,name:'Verificación real de Caja',createdAt:ahora,updatedAt:ahora,sections:[{
    id:randomUUID(),name:'Mi negocio',blocks:[{
      id:inventarioId,type:'inventory',name:'Inventario',description:'',content:'',color:'orange',width:'full',
      fields:[campo('name','text',{required:true}),campo('price','currency'),campo('quantity','number',{min:0}),
        campo('saleMode','select',{options:['unidad','peso']}),campo('stockControl','checkbox'),campo('favorite','checkbox')],
      rows:[{id:coca,name:'Coca-Cola',price:2700,quantity:10,saleMode:'unidad',stockControl:true,favorite:true},
        {id:pan,name:'Pan francés',price:4000,quantity:2,saleMode:'peso',stockControl:true,favorite:true}],
    },{id:ventasId,type:'sales',name:'Ventas',description:'',content:'',color:'yellow',width:'full',
      fields:[campo('product','relation',{target:inventarioId,required:true}),campo('quantity','number',{min:0.001,required:true}),
        campo('price','currency'),campo('date','date'),campo('customer','relation',{target:clientesId}),
        campo('credit','checkbox')],rows:[]},
      {id:clientesId,type:'clients',name:'Clientes',description:'',content:'',color:'blue',width:'full',
        fields:[campo('name','text',{required:true}),campo('phone','text')],
        rows:[{id:clienteId,name:'Ana de prueba',phone:'1112345678'}]}],
  }]};
  const request={id:randomUUID(),proyectoId,inventarioId,ventasId,medio:'EFECTIVO',recibido:10000,
    lineas:[{productoId:coca,cantidad:2,precio:2700,modo:'UNIDAD'},{productoId:pan,cantidad:0.5,precio:4000,modo:'PESO'}]};
  const ruta=`/api/sistemas/${proyectoId}`;
  try {
    await exigir(cliente,ruta,200,'PUT',sistema);
    await exigir(sesion(),'/api/caja/ventas',401,'POST',request,false);
    await exigir(cliente,'/api/caja/ventas',403,'POST',request,false);
    const venta=await exigir(cliente,'/api/caja/ventas',200,'POST',request);
    assert.equal(venta.total,7400);assert.equal(venta.vuelto,2600);
    assert.deepEqual(await exigir(cliente,'/api/caja/ventas',200,'POST',request),venta);
    await exigir(ajeno,'/api/caja/ventas',404,'POST',{...request,id:randomUUID()});
    await exigir(cliente,'/api/caja/ventas',409,'POST',{...request,id:randomUUID(),lineas:[{...request.lineas[0],precio:2701}]});
    await exigir(cliente,'/api/caja/ventas',409,'POST',{...request,id:randomUUID(),lineas:[{...request.lineas[0],cantidad:100}]});
    await exigir(cliente,ruta,409,'PUT',sistema);
    const recargado=(await exigir(cliente,'/api/sistemas',200)).find(s=>s.id===proyectoId);
    const bloques=recargado.sections.flatMap(s=>s.blocks);
    assert.equal(bloques.find(b=>b.id===ventasId).rows.length,2);
    assert.equal(bloques.find(b=>b.id===inventarioId).rows.find(p=>p.id===coca).quantity,8);
    assert.equal(bloques.find(b=>b.id===inventarioId).rows.find(p=>p.id===pan).quantity,1.5);
    const inventario=bloques.find(b=>b.id===inventarioId);
    inventario.rows.find(p=>p.id===coca).price=3000;
    await exigir(cliente,ruta,200,'PUT',recargado);
    assert.deepEqual(await exigir(cliente,'/api/caja/ventas',200,'POST',request),venta,'El precio nuevo cambió el comprobante histórico');
    const consultaRuta='/api/caja/resumen?'+new URLSearchParams({proyectoId,inventarioId});
    const anterior=await exigir(cliente,consultaRuta,200);
    const mixta={...request,id:randomUUID(),medio:'MIXTO',recibido:1500,
      lineas:[{productoId:coca,cantidad:1,precio:3000,modo:'UNIDAD'}],
      pagos:[{medio:'EFECTIVO',importe:1000},{medio:'TRANSFERENCIA',importe:2000}]};
    await exigir(cliente,'/api/caja/ventas',400,'POST',{...mixta,id:randomUUID(),pagos:[{medio:'EFECTIVO',importe:1}]});
    const comprobanteMixto=await exigir(cliente,'/api/caja/ventas',200,'POST',mixta);
    assert.equal(comprobanteMixto.vuelto,500);
    assert.deepEqual(comprobanteMixto.pagos,mixta.pagos);
    assert.deepEqual(await exigir(cliente,'/api/caja/ventas',200,'POST',mixta),comprobanteMixto);
    const solicitudCierre={id:randomUUID(),proyectoId,inventarioId,revision:anterior.abierta.revision,
      fondoInicial:500,entradas:100,retiros:200,contado:8800,nota:'Revisión completa'};
    await exigir(cliente,'/api/caja/cierres',409,'POST',solicitudCierre);
    const resumen=await exigir(cliente,consultaRuta,200);
    assert.equal(resumen.abierta.total,10400);
    assert.equal(resumen.abierta.efectivo,8400);
    assert.equal(resumen.abierta.transferencia,2000);
    assert.equal(resumen.abierta.ventas.length,2);
    solicitudCierre.revision=resumen.abierta.revision;
    await exigir(cliente,'/api/caja/cierres',403,'POST',solicitudCierre,false);
    await exigir(ajeno,'/api/caja/cierres',404,'POST',solicitudCierre);
    await exigir(cliente,'/api/caja/cierres',400,'POST',{...solicitudCierre,contado:null});
    await exigir(cliente,'/api/caja/cierres',400,'POST',{...solicitudCierre,contado:0,nota:'  '});
    const cierre=await exigir(cliente,'/api/caja/cierres',200,'POST',solicitudCierre);
    assert.equal(cierre.esperado,8800);assert.equal(cierre.diferencia,0);
    assert.deepEqual(await exigir(cliente,'/api/caja/cierres',200,'POST',solicitudCierre),cierre);
    await exigir(ajeno,'/api/caja/cierres/'+cierre.id,404);
    assert.equal((await exigir(cliente,consultaRuta,200)).abierta.ventas.length,0);
    const ultimo={...request,recibido:null,lineas:[{productoId:pan,cantidad:1.5,precio:4000,modo:'PESO'}]};
    const simultaneas=await Promise.all([cliente.pedir('/api/caja/ventas','POST',{...ultimo,id:randomUUID()}),
      cliente.pedir('/api/caja/ventas','POST',{...ultimo,id:randomUUID()})]);
    assert.deepEqual(simultaneas.map(r=>r.estado).sort(),[200,409]);
    const posterior=await exigir(cliente,consultaRuta,200);
    assert.equal(posterior.abierta.ventas.length,1);
    assert.equal(posterior.cierres[0].total,10400);
    assert.deepEqual(await exigir(cliente,'/api/caja/cierres/'+cierre.id,200),cierre);
    assert.deepEqual(await exigir(cliente,'/api/caja/cierres',200,'POST',solicitudCierre),cierre);
    assert.equal((await exigir(cliente,consultaRuta,200)).abierta.ventas.length,1);
    const deudaBase={...request,id:randomUUID(),clienteId,medio:'TRANSFERENCIA',recibido:null,
      lineas:[{productoId:coca,cantidad:1,precio:3000,modo:'UNIDAD'}]};
    await exigir(cliente,'/api/caja/ventas',400,'POST',{...deudaBase,clienteId:null,pagos:[]});
    await exigir(cliente,'/api/caja/ventas',404,'POST',{...deudaBase,clienteId:randomUUID(),pagos:[]});
    const parcial=await exigir(cliente,'/api/caja/ventas',200,'POST',{
      ...deudaBase,pagos:[{medio:'TRANSFERENCIA',importe:1000}]});
    assert.equal(parcial.saldo,2000);assert.equal(parcial.estado,'PARCIAL');
    const fiada=await exigir(cliente,'/api/caja/ventas',200,'POST',{...deudaBase,id:randomUUID(),pagos:[]});
    assert.equal(fiada.saldo,3000);assert.equal(fiada.estado,'PENDIENTE');
    const panelRuta='/api/negocio/panel?'+new URLSearchParams({proyectoId});
    assert.equal((await exigir(cliente,panelRuta,200)).metricas.teDeben,5000);
    const conDeudas=await exigir(cliente,consultaRuta,200);
    const cierreConDeudas=await exigir(cliente,'/api/caja/cierres',200,'POST',{
      id:randomUUID(),proyectoId,inventarioId,revision:conDeudas.abierta.revision,
      fondoInicial:0,entradas:0,retiros:0,contado:conDeudas.abierta.efectivo,nota:''});
    const cobro={id:randomUUID(),proyectoId,inventarioId,aplicaciones:[
      {ventaId:parcial.id,pagos:[{medio:'EFECTIVO',importe:2000}]},
      {ventaId:fiada.id,pagos:[{medio:'TRANSFERENCIA',importe:1000}]}]};
    await exigir(ajeno,'/api/caja/cobros',404,'POST',cobro);
    await exigir(cliente,'/api/caja/cobros',400,'POST',{...cobro,id:randomUUID(),aplicaciones:[
      {ventaId:parcial.id,pagos:[{medio:'EFECTIVO',importe:2001}]}]});
    const pago=await exigir(cliente,'/api/caja/cobros',200,'POST',cobro);
    assert.equal(pago.importe,3000);
    assert.deepEqual(await exigir(cliente,'/api/caja/cobros',200,'POST',cobro),pago);
    const cuentasRuta='/api/caja/ventas?'+new URLSearchParams({proyectoId,inventarioId});
    const cuentas=await exigir(cliente,cuentasRuta,200);
    assert.equal(cuentas.ventas.find(v=>v.id===parcial.id).estado,'PAGADA');
    assert.equal(cuentas.ventas.find(v=>v.id===fiada.id).saldo,2000);
    assert.equal(cuentas.cobros.length,1);
    assert.equal(cuentas.cuentas.length,1);
    assert.equal(cuentas.cuentas[0].saldo,2000);
    assert.equal(cuentas.cuentas[0].cantidadPendientes,1);
    assert.equal(cuentas.cuentas[0].pendienteDesde,fiada.creadoEn);
    const panelParcial=await exigir(cliente,panelRuta,200);
    assert.equal(panelParcial.metricas.teDeben,2000);
    assert.equal(panelParcial.flujoDeCaja.reduce((sum,s)=>sum+s.entradas,0),20400);
    const soloCobros=await exigir(cliente,consultaRuta,200);
    assert.equal(soloCobros.abierta.ventas.length,0);
    assert.equal(soloCobros.abierta.cobros.length,1);
    assert.equal(soloCobros.abierta.efectivo,2000);
    assert.equal(soloCobros.abierta.transferencia,1000);
    await exigir(cliente,'/api/caja/cierres',200,'POST',{
      id:randomUUID(),proyectoId,inventarioId,revision:soloCobros.abierta.revision,
      fondoInicial:0,entradas:0,retiros:0,contado:2000,nota:'Sólo cobros de deudas'});
    assert.deepEqual(await exigir(cliente,'/api/caja/cierres/'+cierreConDeudas.id,200),cierreConDeudas);
    assert.deepEqual(await exigir(cliente,'/api/caja/cobros',200,'POST',cobro),pago);
    assert.equal((await exigir(cliente,consultaRuta,200)).abierta.cobros.length,0);
    await exigir(cliente,'/api/caja/cobros',200,'POST',{id:randomUUID(),proyectoId,inventarioId,
      aplicaciones:[{ventaId:fiada.id,pagos:[{medio:'TARJETA',importe:2000}]}]});
    const saldadas=await exigir(cliente,cuentasRuta,200);
    assert.equal(saldadas.ventas.find(v=>v.id===fiada.id).estado,'PAGADA');
    assert.equal(saldadas.ventas.find(v=>v.id===fiada.id).saldo,0);
    assert.equal((await exigir(cliente,panelRuta,200)).metricas.teDeben,0);
    const final=(await exigir(cliente,'/api/sistemas',200)).find(s=>s.id===proyectoId);
    assert.equal(final.sections.flatMap(s=>s.blocks).find(b=>b.id===inventarioId).rows.find(p=>p.id===coca).quantity,5);
    const libre={id:randomUUID(),proyectoId,inventarioId,ventasId,clienteId,medio:"MIXTO",recibido:600,
      lineas:[{productoId:null,descripcion:"Servicio de prueba",cantidad:1,precio:2500,modo:"UNIDAD"}],
      pagos:[{medio:"EFECTIVO",importe:500},{medio:"TRANSFERENCIA",importe:500}]};
    const importeLibre=await exigir(cliente,'/api/caja/ventas',200,'POST',libre);
    assert.equal(importeLibre.lineas[0].nombre,'Servicio de prueba');
    assert.equal(importeLibre.lineas[0].productoId,null);
    assert.equal(importeLibre.saldo,1500);assert.equal(importeLibre.vuelto,100);
    assert.deepEqual(await exigir(cliente,'/api/caja/ventas',200,'POST',libre),importeLibre);
    await exigir(cliente,'/api/caja/ventas',400,'POST',{...libre,id:randomUUID(),clienteId:null});
    await exigir(cliente,'/api/caja/ventas',400,'POST',{...libre,id:randomUUID(),lineas:[{...libre.lineas[0],descripcion:''}]});
    await exigir(cliente,'/api/caja/ventas',400,'POST',{...libre,id:randomUUID(),lineas:[{...libre.lineas[0],cantidad:2}]});
    await exigir(cliente,'/api/caja/ventas',200,'POST',{...libre,id:randomUUID(),pagos:[],recibido:null});
    await exigir(cliente,'/api/caja/ventas',200,'POST',{...libre,id:randomUUID(),clienteId:null,medio:'TARJETA',
      pagos:[{medio:'TARJETA',importe:2500}],recibido:null});
    const despuesLibre=(await exigir(cliente,'/api/sistemas',200)).find(s=>s.id===proyectoId);
    assert.deepEqual(despuesLibre.sections.flatMap(s=>s.blocks).find(b=>b.id===inventarioId).rows,
      final.sections.flatMap(s=>s.blocks).find(b=>b.id===inventarioId).rows,'Un importe libre alteró el inventario');
    const panelTrasImportes=await exigir(cliente,panelRuta,200);
    assert.equal(panelTrasImportes.metricas.vendiste,29900);
    assert.equal(panelTrasImportes.metricas.teDeben,4000);
    assert.equal(panelTrasImportes.flujoDeCaja.reduce((suma,semana)=>suma+semana.entradas,0),25900);
    console.log(JSON.stringify({resultado:'correcto',comprobaciones:[
      'Inicio incluye importes libres en ventas sin duplicar cobros ni deudas',
      'importe libre pagado, parcial mixto y fiado sin productos ni stock',
      'recuento obligatorio con cero válido y motivo de diferencias',
      'saldos y primera deuda agrupados por cliente desde el backend',
      'sesión y CSRF reales','venta mixta: dos unidades y medio kilo','total y vuelto',
      'idempotencia persistente','aislamiento por negocio','revalidación de precio y stock',
      'editor anterior rechazado','recarga desde PostgreSQL','precio histórico conservado',
      'dos cajas compiten por la última existencia: sólo una confirma',
      'pago mixto y vuelto sólo sobre efectivo','resumen por medio de pago sin duplicar ventas',
      'cierre con recuento y diferencia','revisión obligatoria ante ventas nuevas',
      'historial inmutable y aislado por negocio','venta posterior en un período nuevo',
      'reintentar el cierre anterior conserva las ventas posteriores abiertas',
      'cliente real obligatorio para fiado y pago parcial','saldo pendiente hasta completar la deuda',
      'un cobro aplicado a dos ventas sin duplicación por reintentos',
      'cierre sólo con cobros y fotografía anterior conservada',
      'Inicio usa el saldo real y el flujo de cobros sin duplicar ventas',
      'cobrar después de cerrar no reabre ventas ni vuelve a descontar stock']},null,2));
  } finally {
    await cliente.pedir(ruta,'DELETE').catch(()=>undefined);
  }
}
principal().catch(error=>{console.error(error);process.exitCode=1;});
