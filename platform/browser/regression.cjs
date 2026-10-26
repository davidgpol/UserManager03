const {chromium,expect}=require('/opt/browser/node_modules/@playwright/test');const fs=require('node:fs');const assert=require('node:assert/strict');
(async()=>{
 const browser=await chromium.launch({args:['--no-sandbox','--ignore-certificate-errors-spki-list='+process.env.UM03_TLS_SPKI]});const context=await browser.newContext({viewport:{width:1440,height:1000}});const page=await context.newPage();page.setDefaultTimeout(10000);
 const password=fs.readFileSync('/private/operator-password','utf8');
 async function login(){await page.goto('https://localhost:18443/login');await page.getByLabel('Usuario').fill('operador');await page.getByLabel('Contraseña').fill(password);await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();await expect(page.getByRole('button',{name:'Nueva ficha',exact:true})).toBeVisible();await expect(page.getByRole('progressbar')).toHaveCount(0);}
 await login();await page.getByRole('button',{name:'Nueva ficha',exact:true}).click();
 for(const [name,value] of [['Nombre','Control Sintético'],['Apellidos','Sesión'],['DNI','74000000M'],['Correo electrónico','session@example.test']])await page.getByLabel(name,{exact:false}).fill(value);
 // Generate a valid DNI, avoiding any frontend short circuit.
 await page.getByLabel('DNI').fill('74000000'+'TRWAGMYFPDXBNJZSQVHLCKE'[74000000%23]);
 let posts=0;page.on('request',r=>{if(r.url().endsWith('/api/users')&&r.method()==='POST')posts++;});
 await page.evaluate(async()=>{const t=await fetch('/api/auth/csrf').then(r=>r.json());const r=await fetch('/api/auth/logout',{method:'POST',headers:{'X-CSRF-TOKEN':t.token}});if(r.status!==204)throw new Error('Logout setup failed');});
 await page.getByRole('button',{name:'Guardar',exact:true}).click();await expect(page.getByText('Acceso de operador')).toBeVisible();await expect(page.getByRole('dialog')).toHaveCount(0);assert.equal(posts,1);
 await page.screenshot({path:'/evidence/REG-CSRF-401.png'});
 await login();
 let release;const gate=new Promise(resolve=>release=resolve);let intercepted;const ready=new Promise(resolve=>intercepted=resolve);
 await page.route('**/api/users/*',async route=>{const response=await route.fetch();intercepted();await gate;await route.fulfill({response});});
 await page.getByRole('button',{name:/Ver detalle de/}).first().click();await ready;await page.getByRole('button',{name:'Cerrar',exact:true}).click();await page.getByRole('button',{name:'Cerrar sesión',exact:true}).click();await expect(page.getByText('Acceso de operador')).toBeVisible();release();await page.unrouteAll({behavior:'wait'});await expect(page.getByRole('dialog')).toHaveCount(0);
 await login();await expect(page.getByRole('dialog')).toHaveCount(0);await page.screenshot({path:'/evidence/REG-detail-session.png'});
 // Raw invalid paste is rejected without silently repairing it.
 await page.getByRole('button',{name:'Nueva ficha',exact:true}).click();const name=page.getByLabel('Nombre');await name.fill('Ana');
 await name.evaluate(el=>{const d=new DataTransfer();d.setData('text/plain','A\tB');el.dispatchEvent(new ClipboardEvent('paste',{clipboardData:d,bubbles:true,cancelable:true}));});await expect(name).toHaveValue('Ana');await expect(page.getByRole('alert')).toContainText('caracteres que no se pueden usar');
 await name.fill('A'.repeat(200001));await expect(page.getByText(/entre 1 y 100/)).toBeVisible();await name.fill('𐐀'.repeat(100));await expect(page.getByText('100/100 caracteres')).toBeVisible();await page.screenshot({path:'/evidence/REG-unicode.png'});
 await page.getByRole('button',{name:'Cancelar',exact:true}).click();await page.getByRole('button',{name:'Cerrar sesión',exact:true}).click();
 fs.writeFileSync('/evidence/regression.json',JSON.stringify({at:new Date().toISOString(),result:'PASS',cases:['real client 403→csrf200→session401 clears private UI; mutation sent once','detail resolving after logout does not reopen after login','raw tab paste rejected, 200001 code points handled, 100 supplementary accepted']},null,2));await browser.close();console.log('Session and Unicode browser regression PASS');
})().catch(e=>{console.error(String(e).replace(/UM03SESSION=[^\s;,'"]+/g,'UM03SESSION=[REDACTED]'));process.exit(1)});
