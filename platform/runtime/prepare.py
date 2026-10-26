#!/usr/bin/env python3
"""Provision a NEW private project directory. Operator input is hidden; never prints secrets."""
import getpass, os, pathlib, secrets, subprocess, sys
root=pathlib.Path(__file__).resolve().parents[2]
private=pathlib.Path(sys.argv[1]).resolve()
if private.is_relative_to(root) or private.exists(): raise SystemExit('Use a new private directory outside the repository.')
password=getpass.getpass('Operator password (hidden): ')
if len(password)<16: raise SystemExit('Use at least 16 characters for the initial operator password.')
private.mkdir(mode=0o700,parents=True)
for name in ['db','app','access']: (private/name).mkdir(mode=0o700)
def write(path,value):
 path.write_text(value);path.chmod(0o600)
def cert(name,cn,san):
 subprocess.run(['openssl','req','-x509','-newkey','rsa:3072','-noenc','-days','365','-subj','/CN='+cn,'-addext','subjectAltName='+san,'-keyout',str(private/(name+'.key')),'-out',str(private/(name+'.crt'))],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
 (private/(name+'.key')).chmod(0o600)
 (private/(name+'.crt')).chmod(0o644)
cert('db/db','db','DNS:db');cert('app/https','localhost','DNS:localhost,IP:127.0.0.1')
(private/'app/db.crt').write_bytes((private/'db/db.crt').read_bytes())
(private/'access/https.crt').write_bytes((private/'app/https.crt').read_bytes())
write(private/'db/bootstrap-password',secrets.token_hex(32))
for role,prefix in [('app','spring.datasource'),('migrate','spring.flyway')]:
 value=secrets.token_hex(32);write(private/'db'/f'{role}-password',value)
 write(private/'app'/f'{prefix}.password',value)
 write(private/'app'/f'{prefix}.url','jdbc:postgresql://db:5432/usermanager03?sslmode=verify-full&sslrootcert=/run/secrets/db.crt')
 write(private/'app'/(f'{prefix}.'+('username' if role=='app' else 'user')),'um03_'+role)
# The prebuilt application supplies the exact Security/Bouncy Castle dependencies.
command=['docker','run','--rm','-i','--network','none','--user','1001:1001','--label','com.hariklabs.project=usermanager03','--label','com.hariklabs.lifecycle=temporary','--label','com.hariklabs.run='+os.environ['PAPERCLIP_RUN_ID'],'--entrypoint','bash','--mount',f'type=bind,source={root}/platform/runtime/OperatorHash.java,target=/source/OperatorHash.java,readonly',os.environ['UM03_APP_IMAGE'],'-c','cd /tmp && jar xf /app/application.jar BOOT-INF/lib && java -cp "BOOT-INF/lib/*" /source/OperatorHash.java']
result=subprocess.run(command,input=password.encode(),stdout=subprocess.PIPE,check=True)
value=result.stdout.decode();assert value.startswith('{argon2}$argon2id$')
write(private/'app/um03.operator.username','operador')
write(private/'app/um03.operator.password-hash',value)
# Private bootstrap handoff, readable only by the authorized OCI owner over existing SSH.
write(private/'access/operator-password',password)
password=None
print('Private configuration prepared. Values omitted. Deliver access/ only over the existing authorized SSH connection.')
