#!/usr/bin/env python3
"""Fail closed on synthetic business values in captured app/DB logs; emit counts only."""
import json,re,sys
from pathlib import Path
records=json.loads(Path(sys.argv[1]).read_text())
markers={r[k] for r in records for k in ['firstName','lastName','dni','email'] if r.get(k)}
assert markers, 'A nonempty synthetic dataset is required'
assert len(sys.argv)>2, 'At least one captured log is required'
results=[]
for name in sys.argv[2:]:
 text=Path(name).read_text();decoded=text
 assert text.strip(), 'Captured logs must not be empty'
 # JSON logging escapes control characters; inspect decoded messages as well.
 for line in text.splitlines():
  try:decoded+='\n'+json.loads(line).get('message','')
  except (ValueError,AttributeError):pass
 matches=sum(marker in decoded for marker in markers)
 results.append({'file':Path(name).name,'syntheticMarkers':len(markers),'matchingMarkers':matches})
print(json.dumps(results,indent=2))
assert all(r['matchingMarkers']==0 for r in results),'Synthetic values found in logs'
