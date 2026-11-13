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
        campo('price','currency'),campo('date','date')],rows:[]}],
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
    const ultimo={...request,recibido:null,lineas:[{productoId:pan,cantidad:1.5,precio:4000,modo:'PESO'}]};
    const simultaneas=await Promise.all([cliente.pedir('/api/caja/ventas','POST',{...ultimo,id:randomUUID()}),
      cliente.pedir('/api/caja/ventas','POST',{...ultimo,id:randomUUID()})]);
    assert.deepEqual(simultaneas.map(r=>r.estado).sort(),[200,409]);
    console.log(JSON.stringify({resultado:'correcto',comprobaciones:[
      'sesión y CSRF reales','venta mixta: dos unidades y medio kilo','total y vuelto',
      'idempotencia persistente','aislamiento por negocio','revalidación de precio y stock',
      'editor anterior rechazado','recarga desde PostgreSQL','precio histórico conservado',
      'dos cajas compiten por la última existencia: sólo una confirma']},null,2));
  } finally {
    await cliente.pedir(ruta,'DELETE').catch(()=>undefined);
  }
}
principal().catch(error=>{console.error(error);process.exitCode=1;});
