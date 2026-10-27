#!/usr/bin/env python3
"""Real HTTPS conflicts on the isolated runtime only. Never emit auth material."""
import concurrent.futures, http.cookiejar, json, os, ssl, sys, threading
from pathlib import Path
from urllib.request import build_opener, HTTPSHandler, HTTPCookieProcessor, Request
from urllib.error import HTTPError
private=Path(os.environ['UM03_PRIVATE_DIR']);out=Path(os.environ['UM03_EVIDENCE_DIR'])
base=os.environ['UM03_BASE_URL'];assert base=='https://localhost:19443', 'Only isolated port allowed'
context=ssl.create_default_context(cafile=str(private/'access/https.crt'))
password=(private/'access/operator-password').read_text()
def client():return build_opener(HTTPSHandler(context=context),HTTPCookieProcessor(http.cookiejar.CookieJar()))
def request(c,method,path,body=None,token=None,correlation='har110-http'):
 headers={'X-Correlation-ID':correlation,'Content-Type':'application/json'}
 if token:headers['X-CSRF-TOKEN']=token
 req=Request(base+path,data=None if body is None else json.dumps(body).encode(),headers=headers,method=method)
 try:r=c.open(req)
 except HTTPError as e:r=e
 raw=r.read();return r.status,json.loads(raw) if raw else None,r.headers.get('X-Correlation-ID')
def login():
 c=client();token=request(c,'GET','/api/auth/csrf')[1]['token']
 from urllib.parse import urlencode
 req=Request(base+'/api/auth/login',data=urlencode({'username':'operador','password':password}).encode(),headers={'X-CSRF-TOKEN':token,'Content-Type':'application/x-www-form-urlencoded'})
 assert c.open(req).status==204
 return c,request(c,'GET','/api/auth/csrf')[1]['token']
def payload(n,name):return {'firstName':name,'lastName':'Har110SyntheticSurname','dni':str(n)+'TRWAGMYFPDXBNJZSQVHLCKE'[n%23],'email':'har110.synthetic@example.test'}
a,ta=login();b,tb=login();original=payload(81000000,'Har110SyntheticOriginal')
s,u,_=request(a,'POST','/api/users',original,ta);assert s==201
s,p,c=request(a,'POST','/api/users',original,ta,'har110-https-ordinary');assert (s,p['code'],c)==(409,'DNI_CONFLICT','har110-https-ordinary')
other=payload(81000001,'Har110SyntheticUnchanged');s,v,_=request(a,'POST','/api/users',other,ta);assert s==201
s,p,_=request(a,'PUT','/api/users/'+v['id'],original,ta,'har110-https-update');assert (s,p['code'])==(409,'DNI_CONFLICT')
assert request(a,'GET','/api/users/'+v['id'])[1]==v
assert request(a,'DELETE','/api/users/'+u['id'],None,ta)[0]==204
s,p,_=request(a,'POST','/api/users',original,ta);assert (s,p['code'])==(409,'DNI_CONFLICT')
barrier=threading.Barrier(2)
def race(pair):
 c,t=pair;barrier.wait();r=request(c,'POST','/api/users',payload(81000002,'Har110SyntheticRace'),t,'har110-https-race-'+('a' if c is a else 'b'))
 if r[0]==409:assert r[1]['code']=='DNI_CONFLICT'
 return r[0]
with concurrent.futures.ThreadPoolExecutor(2) as pool:assert sorted(pool.map(race,[(a,ta),(b,tb)]))==[201,409]
# Save only synthetic business values for the independent log scanner.
s,page,_=request(a,'GET','/api/users?status=all&size=100');assert s==200
items=page.get('items',page.get('content'));assert items is not None
assert len([v for v in items if v['dni']==payload(81000002,'x')['dni']])==1
(out/'synthetic-records.json').write_text(json.dumps(items,ensure_ascii=False,indent=2))
(out/'https-conflicts.json').write_text(json.dumps({'ordinary':409,'inactive':409,'update':409,'rollback':'unchanged','concurrent':[201,409],'uniqueness':True,'correlation':True},indent=2))
for c,t in [(a,ta),(b,tb)]:assert request(c,'POST','/api/auth/logout',None,t)[0]==204
print('PASS real HTTPS: ordinary/inactive/update/concurrent conflicts, rollback, uniqueness and correlation.')
