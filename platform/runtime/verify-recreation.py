#!/usr/bin/env python3
"""Controlled permanent recreation; compare data and operator bytes without emitting either.
Run only in the assigned maintenance window. Quiesce writers first; a concurrent write fails comparison.
"""
import hashlib,json,os,subprocess
from pathlib import Path
root=Path(__file__).resolve().parents[2]
private=Path(os.environ['UM03_PRIVATE_DIR']).resolve()
assert private==Path('/opt/Dev/Secrets/UserManager03')
def run(*args):return subprocess.check_output(args,cwd=root)
def inspect(name):return json.loads(run('docker','inspect',name))[0]
def snapshot():
 db=inspect('usermanager03-db-1');app=inspect('usermanager03-app-1')
 assert all(d['Config']['Labels']['com.hariklabs.project']=='usermanager03' and d['Config']['Labels']['com.hariklabs.lifecycle']=='permanent' for d in [db,app])
 volumes=[m['Name'] for m in db['Mounts'] if m['Type']=='volume'];assert volumes==['usermanager03-postgres-data']
 # Raw rows remain in process memory only; stdout contains only comparison booleans/counts.
 data=run('docker','exec','usermanager03-db-1','psql','-X','-qAt','-U','um03_bootstrap','-d','usermanager03','-c','SELECT row_to_json(u)::text FROM (SELECT * FROM user_record ORDER BY id) u')
 credentials=[(private/'app'/f).read_bytes() for f in ['um03.operator.username','um03.operator.password-hash']]
 assert not (private/'access/operator-password').exists()
 return {'data':hashlib.sha256(data).digest(),'count':len(data.splitlines()),'credentials':credentials,'db':db['Id'],'app':app['Id'],'volume':volumes[0]}
# Verify ownership and protected mounts before stopping the application. This
# closes HTTP writers before taking the baseline; no credential rotation occurs.
snapshot()
subprocess.run(['docker','stop','usermanager03-app-1'],check=True,stdout=subprocess.DEVNULL)
before=snapshot()
subprocess.run(['bash','platform/runtime/operate.sh','recreate'],cwd=root,check=True)
after=snapshot()
result={k:before[k]==after[k] for k in ['data','count','credentials','volume']}
result.update(rowsBefore=before['count'],rowsAfter=after['count'],containersRecreated=before['db']!=after['db'] and before['app']!=after['app'],passwordCopyAbsent=True)
print(json.dumps(result,indent=2));assert all(result[k] for k in ['data','count','credentials','volume','containersRecreated'])
