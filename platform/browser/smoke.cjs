const { chromium, expect } = require('/opt/browser/node_modules/@playwright/test');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const base = 'https://localhost:18443';
const evidence = '/evidence';
const results = [];
async function record(ids, action) { await action(); results.push({ ids, result: 'PASS', at: new Date().toISOString() }); fs.writeFileSync(`${evidence}/results.json`, JSON.stringify(results, null, 2)); }
(async () => {
  const browser = await chromium.launch({ args: ['--no-sandbox', '--ignore-certificate-errors-spki-list='+process.env.UM03_TLS_SPKI] });
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
  const page = await context.newPage(); page.setDefaultTimeout(10000);
  const errors=[];page.on('pageerror',error=>errors.push(error.message));
  const password = fs.readFileSync('/private/operator-password', 'utf8');
  async function screenshot(name) { await page.screenshot({path:`${evidence}/${name}.png`,fullPage:true}); }
  async function login() { await page.getByLabel('Usuario').fill('operador'); await page.getByLabel('Contraseña').fill(password); await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();await expect(page.getByRole('button',{name:'Nueva ficha',exact:true})).toBeVisible(); }
  async function api(method,path,body,token=true) {
    return page.evaluate(async ({method,path,body,token})=>{
      let headers={Accept:'application/json, application/problem+json'};
      if(token && !['GET','HEAD'].includes(method)){ const t=await fetch('/api/auth/csrf').then(r=>r.json());headers['X-CSRF-TOKEN']=t.token; }
      if(body!==undefined)headers['Content-Type']='application/json';
      const response=await fetch(path,{method,headers,body:body===undefined?undefined:JSON.stringify(body)});
      return {status:response.status,body:response.status===204?null:await response.json(),cache:response.headers.get('cache-control')};
    },{method,path,body,token});
  }
  const dni=n=>String(n).padStart(8,'0')+'TRWAGMYFPDXBNJZSQVHLCKE'[n%23];
  const serial=Number(process.env.UM03_DATASET || '73000000');
  const fields={firstName:'Ána Sintética',lastName:'Prueba Norte',dni:dni(serial),email:'ana.prueba@example.test'};
  let user;
  await record(['SEC-01','SEC-06'],async()=>{
    await page.goto(base); await expect(page.getByText('Acceso de operador')).toBeVisible();
    for(const path of ['/api/users','/api/users/00000000-0000-4000-8000-000000000000']) assert.equal((await api('GET',path)).status,401);
    assert.equal((await api('POST','/api/users',fields,false)).status,403);
    assert.equal((await api('POST','/api/users',fields)).status,401);
    await screenshot('SEC-01-login');
  });
  await record(['SEC-03'],async()=>{
    await page.getByLabel('Usuario').fill('operador-inexistente');await page.getByLabel('Contraseña').fill('synthetic-invalid');await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();
    await expect(page.getByRole('alert')).toContainText('No se pudo iniciar sesión');await expect(page.getByLabel('Contraseña')).toHaveValue('');await screenshot('SEC-03-error');
  });
  await record(['SEC-02','QRY-01','UI-03'],async()=>{await login();await expect(page.getByRole('progressbar')).toHaveCount(0);await screenshot('SEC-02-listado');});
  await record(['USR-01','MUT-09','UI-02','UI-05'],async()=>{
    await page.getByRole('button',{name:'Nueva ficha',exact:true}).click();
    for(const [label,value] of [['Nombre',fields.firstName],['Apellidos',fields.lastName],['DNI',fields.dni],['Correo electrónico',fields.email]])await page.getByLabel(label).fill(value);
    await page.getByRole('button',{name:'Guardar',exact:true}).click();await expect(page.getByText('Ficha creada y activa.')).toBeVisible();await expect(page.getByText('Detalle de ficha',{exact:true})).toBeVisible();await expect(page.getByText(/Creada:/)).toContainText('UTC');await screenshot('USR-01-detalle');
    user=(await api('GET','/api/users?q='+fields.dni)).body.items[0];assert.equal(user.active,true);assert.match(user.createdAt,/Z$/);await page.getByRole('button',{name:'Cerrar',exact:true}).click();await page.getByLabel('Buscar').fill(fields.dni);await page.getByRole('button',{name:'Buscar',exact:true}).click();
  });
  await record(['API-04','API-05','USR-02','USR-03','USR-04','MUT-04','MUT-05','MUT-11'],async()=>{
    assert.equal((await api('POST','/api/users',fields)).status,409);
    for(const key of Object.keys(fields)){const invalid={...fields};delete invalid[key];assert.equal((await api('POST','/api/users',invalid)).status,400);assert.equal((await api('PUT','/api/users/'+user.id,invalid)).status,400);}
    assert.equal((await api('POST','/api/users',{...fields,email:'bad mail'})).status,400);
    for(const key of ['id','active','createdAt','updatedAt'])assert.equal((await api('PUT','/api/users/'+user.id,{...fields,[key]:'untrusted'})).status,400);
    assert.equal((await api('GET','/api/users/not-uuid')).status,400);assert.equal((await api('GET','/api/users/00000000-0000-4000-8000-000000000000')).status,404);
    assert.equal((await api('POST','/api/users',{...fields,active:false})).status,400);
  });
  await record(['MUT-01','MUT-02'],async()=>{
    await page.getByRole('button',{name:`Editar ${fields.firstName} ${fields.lastName}`,exact:true}).click();await page.getByLabel('Nombre').fill('Ána Modificada');await page.getByRole('button',{name:'Guardar',exact:true}).click();await expect(page.getByText('Ficha actualizada.')).toBeVisible();await screenshot('MUT-02-editada');await page.getByRole('button',{name:'Cerrar',exact:true}).click();
    const after=(await api('GET','/api/users/'+user.id)).body;assert.equal(after.createdAt,user.createdAt);assert.equal(after.firstName,'Ána Modificada');assert.ok(after.updatedAt>user.updatedAt);user=after;
  });
  await record(['MUT-07','MUT-08','MUT-10','USR-05','MUT-06'],async()=>{
    const target=`Dar de baja ${user.firstName} ${user.lastName}`;
    await page.getByRole('button',{name:target,exact:true}).click();await screenshot('MUT-07-confirmacion');await page.getByRole('button',{name:'Cancelar',exact:true}).click();assert.equal((await api('GET','/api/users/'+user.id)).body.updatedAt,user.updatedAt);
    await page.getByRole('button',{name:target,exact:true}).click();await page.getByRole('button',{name:'Confirmar baja',exact:true}).click();await expect(page.getByText('Ficha dada de baja.')).toBeVisible();await screenshot('MUT-08-baja');
    const inactive=(await api('GET','/api/users/'+user.id)).body;assert.equal(inactive.active,false);assert.equal(inactive.createdAt,user.createdAt);
    assert.equal((await api('DELETE','/api/users/'+user.id)).status,204);assert.deepEqual((await api('GET','/api/users/'+user.id)).body,inactive);
    assert.equal((await api('POST','/api/users',fields)).status,409);
    const changed=await api('PUT','/api/users/'+user.id,{...fields,firstName:'Ána Inactiva'});assert.equal(changed.body.active,false);
  });
  await record(['API-03','QRY-02','QRY-03','QRY-04','QRY-05','QRY-06','QRY-07','QRY-09','MUT-03'],async()=>{
    for(let i=1;i<=23;i++)assert.equal((await api('POST','/api/users',{firstName:'Bruno '+i,lastName:'Prueba Sur',dni:dni(serial+i),email:`bruno${i}@example.test`})).status,201);
    for(const q of ['ana','INACTIVA','Prueba Norte',fields.dni,'ANA.PRUEBA@'])assert.ok((await api('GET','/api/users?status=all&q='+encodeURIComponent(q))).body.items.some(x=>x.id===user.id),q);
    assert.ok((await api('GET','/api/users?status=inactive')).body.items.some(x=>x.id===user.id));
    const a=(await api('GET','/api/users?status=all&size=100')).body;const b=(await api('GET','/api/users?status=all&size=100')).body;assert.deepEqual(a,b);
    let ids=[];for(let p=0;p<Math.ceil(a.totalElements/5)+1;p++){const r=(await api('GET','/api/users?status=all&size=5&page='+p)).body;ids.push(...r.items.map(x=>x.id));if(!r.hasNext)break;}assert.equal(new Set(ids).size,ids.length);assert.equal(ids.length,a.totalElements);
    const other=(await api('GET','/api/users?q='+dni(serial+1))).body.items[0];assert.equal((await api('PUT','/api/users/'+other.id,fields)).status,409);
    await page.getByLabel('Buscar').fill('');await page.getByRole('button',{name:'Buscar',exact:true}).click();await expect(page.getByRole('button',{name:'Siguiente',exact:true})).toBeEnabled();await page.getByRole('button',{name:'Siguiente',exact:true}).click();await expect(page.getByText(/Página 2 de/)).toBeVisible();await screenshot('QRY-06-pagina2');
  });
  await record(['QRY-08','UI-04'],async()=>{
    await page.getByLabel('Buscar').fill('sin-coincidencia-sintetica');await page.getByRole('button',{name:'Buscar',exact:true}).click();await expect(page.getByText('No hay fichas para los filtros seleccionados.')).toBeVisible();await screenshot('QRY-08-vacio');
    await page.route('**/api/users?**',route=>route.fulfill({status:503,contentType:'application/problem+json',body:JSON.stringify({code:'SERVICE_UNAVAILABLE'})}));await page.getByRole('button',{name:'Buscar',exact:true}).click();await expect(page.getByRole('alert')).toContainText('servicio no está disponible');await screenshot('QRY-08-error');await page.unroute('**/api/users?**');
  });
  await record(['UI-01'],async()=>{
    await page.getByLabel('Buscar').fill('');await page.getByRole('button',{name:'Buscar',exact:true}).click();await expect(page.getByRole('progressbar')).toHaveCount(0);await page.setViewportSize({width:390,height:844});await page.screenshot({path:`${evidence}/UI-01-movil.png`});await page.getByRole('button',{name:'Ver detalle',exact:true}).first().click();await expect(page.getByText('Detalle de ficha',{exact:true})).toBeVisible();await page.screenshot({path:`${evidence}/UI-01-movil-detalle.png`});await page.getByRole('button',{name:'Cerrar',exact:true}).click();assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth),true);await page.setViewportSize({width:1440,height:1000});
  });
  await record(['SEC-04'],async()=>{await page.getByRole('button',{name:'Cerrar sesión',exact:true}).click();await expect(page.getByText('Acceso de operador')).toBeVisible();assert.equal((await api('GET','/api/users')).status,401);await screenshot('SEC-04-logout');});
  await record(['SEC-05'],async()=>{await page.getByLabel('Usuario').fill(fields.email);await page.getByLabel('Contraseña').fill(fields.dni);await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();await expect(page.getByRole('alert')).toContainText('No se pudo iniciar sesión');await screenshot('SEC-05-separacion');});
  assert.deepEqual(errors,[]);await browser.close();console.log('Browser verification passed; synthetic results and screenshots saved.');
})().catch(error=>{console.error(String(error).replace(/UM03SESSION=[^\s;,'"]+/g,'UM03SESSION=[REDACTED]'));process.exit(1);});
