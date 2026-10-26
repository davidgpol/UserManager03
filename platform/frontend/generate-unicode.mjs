import { createHash } from 'node:crypto';
import { readFile, writeFile } from 'node:fs/promises';

const [sourcePath, outputPath] = process.argv.slice(2);
if (!sourcePath || !outputPath) throw new Error('Usage: generate-unicode.mjs SOURCE OUTPUT');
const source = await readFile(sourcePath, 'utf8');
const checksum = createHash('sha256').update(source).digest('hex');
const expected = '7676ab755a41ef82108460238569e60ad65c191ddafe61b36c6765ec1353f293';
if (checksum !== expected) throw new Error(`Unexpected Unicode source checksum: ${checksum}`);

const ranges = [];
for (const line of source.split(/\r?\n/)) {
  const match = line.match(/^([0-9A-F]+)(?:\.\.([0-9A-F]+))?\s*;\s*([A-Za-z]+)/);
  if (!match) continue;
  const start = Number.parseInt(match[1], 16);
  const end = Number.parseInt(match[2] ?? match[1], 16);
  ranges.push([start, end, match[3]]);
}
if (ranges.length !== 4099) throw new Error(`Expected 4099 ranges, got ${ranges.length}`);
ranges.sort((left, right) => left[0] - right[0]);
let next = 0;
for (const [start, end] of ranges) {
  if (start !== next || end < start) throw new Error(`Unicode coverage gap/overlap at U+${next.toString(16)}`);
  next = end + 1;
}
if (next !== 0x110000) throw new Error('Unicode coverage does not end at U+10FFFF');

const serialized = ranges.map(([start, end, category]) => `[${start},${end},'${category}']`).join(',');
const output = `// Generated from api/unicode/DerivedGeneralCategory-16.0.0.txt. Do not edit.\n` +
  `export const UNICODE_VERSION = '16.0.0';\nexport const UNICODE_SOURCE_SHA256 = '${checksum}';\n` +
  `type Range = readonly [number, number, string];\nconst RANGES: readonly Range[] = [${serialized}];\n` +
  `export function categoryOf(codePoint: number): string { let low=0, high=RANGES.length-1; while(low<=high){const mid=(low+high)>>>1;const r=RANGES[mid];if(codePoint<r[0]) high=mid-1; else if(codePoint>r[1]) low=mid+1; else return r[2];} throw new Error('Code point outside Unicode range'); }\n`;
await writeFile(outputPath, output);
