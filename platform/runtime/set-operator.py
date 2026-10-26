#!/usr/bin/env python3
"""Change only the private operator configuration. Restart app to apply it."""
import getpass,os,pathlib,subprocess,sys
root=pathlib.Path(__file__).resolve().parents[2]
private=pathlib.Path(sys.argv[1]).resolve()
if private.is_relative_to(root) or not (private/'app').is_dir(): raise SystemExit('Existing private directory outside Git required.')
username=input('Operator username: ').strip()
if not username: raise SystemExit('Username required.')
password=getpass.getpass('New operator password (hidden): ')
if len(password)<16: raise SystemExit('Use at least 16 characters.')
result=subprocess.run(['docker','run','--rm','-i','--network','none','--user','1001:1001','--label','com.hariklabs.project=usermanager03','--label','com.hariklabs.lifecycle=temporary','--label','com.hariklabs.run='+os.environ['PAPERCLIP_RUN_ID'],'--entrypoint','bash','--mount',f'type=bind,source={root}/platform/runtime/OperatorHash.java,target=/source/OperatorHash.java,readonly',os.environ['UM03_APP_IMAGE'],'-c','cd /tmp && jar xf /app/application.jar BOOT-INF/lib && java -cp "BOOT-INF/lib/*" /source/OperatorHash.java'],input=password.encode(),stdout=subprocess.PIPE,check=True)
value=result.stdout.decode();assert value.startswith('{argon2}$argon2id$')
for name,value in [('um03.operator.username',username),('um03.operator.password-hash',value)]:
 target=private/'app'/name;temporary=target.with_suffix('.new')
 fd=os.open(temporary,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
 with os.fdopen(fd,'w') as stream:stream.write(value)
 temporary.replace(target)
password=None
print('Private operator configuration updated; restart only app. No credential was printed.')
