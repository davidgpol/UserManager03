#!/usr/bin/env python3
"""Create private, ephemeral synthetic PostgreSQL TLS/configuration. Never print values."""
from pathlib import Path
import os,secrets,subprocess,sys
p=Path(sys.argv[1]);p.mkdir(parents=True,exist_ok=True,mode=0o700)
def write(name,value):
    path=p/name;path.write_text(value);path.chmod(0o600)
write('spring.datasource.username','um03_app')
write('spring.flyway.user','um03_migrate')
for name in ['spring.datasource.password','spring.flyway.password']:write(name,secrets.token_hex(32))
url='jdbc:postgresql://db:5432/usermanager03?sslmode=verify-full&sslrootcert=/run/secrets/db.crt'
for name in ['spring.datasource.url','spring.flyway.url']:write(name,url)
subprocess.run(['openssl','req','-x509','-newkey','rsa:3072','-noenc','-days','2','-subj','/CN=db','-addext','subjectAltName=DNS:db','-keyout',str(p/'db.key'),'-out',str(p/'db.crt')],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
(p/'db.key').chmod(0o600);(p/'db.crt').chmod(0o644)
print('Private synthetic test configuration prepared; values omitted.')
