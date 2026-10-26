#!/usr/bin/env python3
"""Generate the fixed Unicode 16 category table, validating complete coverage."""
import hashlib
from pathlib import Path
source = Path('api/unicode/DerivedGeneralCategory-16.0.0.txt').read_bytes()
assert hashlib.sha256(source).hexdigest() == '7676ab755a41ef82108460238569e60ad65c191ddafe61b36c6765ec1353f293'
ranges=[]
for line in source.decode().splitlines():
    line=line.split('#')[0].strip()
    if not line: continue
    span, category = map(str.strip,line.split(';'))
    ends=span.split('..'); ranges.append((int(ends[0],16),int(ends[-1],16),category))
ranges.sort(); cursor=0
for first,last,cat in ranges:
    assert first==cursor and last>=first
    cursor=last+1
assert cursor==0x110000
# Resource keeps the JVM method-size limit independent of table size.
p=Path('backend/src/main/resources/unicode-16-categories.tsv')
p.write_text(''.join(f'{a:X}\t{b:X}\t{c}\n' for a,b,c in ranges))
print(f'Unicode 16.0.0: {len(ranges)} ranges, 1114112 points, full coverage; source checksum OK')
