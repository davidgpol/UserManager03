import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { categoryOf } from './unicode.generated.ts';
import { recoverAfterForbidden } from './recovery.ts';
import { canonicalDni, emailForServer, isNamePasteAllowed, validateName } from './validation.ts';

type UnicodeFixture = { id: string; input: string; candidateAfterTrim: string | null; codePointCount: number | null; valid: boolean };
type DniFixture = { input: string; canonical: string | null; valid: boolean };

const unicode = JSON.parse(await readFile(new URL('../../api/fixtures/unicode.json', import.meta.url), 'utf8')) as { cases: UnicodeFixture[] };
for (const fixture of unicode.cases) {
  const result = validateName(fixture.input, fixture.id.includes('lastName') ? 150 : 100);
  assert.equal(Boolean(result.error), !fixture.valid, fixture.id);
  if (fixture.candidateAfterTrim !== null) assert.equal(result.value, fixture.candidateAfterTrim, fixture.id);
  if (fixture.codePointCount !== null) assert.equal(result.count, fixture.codePointCount, fixture.id);
}

const dniFixture = JSON.parse(await readFile(new URL('../../api/fixtures/dni-query-errors.json', import.meta.url), 'utf8')) as { dni: DniFixture[] };
for (const fixture of dniFixture.dni) assert.equal(canonicalDni(fixture.input) ?? null, fixture.canonical, fixture.input);
assert.equal(canonicalDni('\ufeff00000000T'), undefined, 'FEFF is not a backend trim character');

const emailFixture = JSON.parse(await readFile(new URL('../../api/fixtures/email.json', import.meta.url), 'utf8')) as { cases: Array<{ value: string }> };
for (const fixture of emailFixture.cases) assert.equal(emailForServer(fixture.value), fixture.value, fixture.value);

assert.equal(isNamePasteAllowed('Ana María'), true);
assert.equal(isNamePasteAllowed('Ana\tMaría'), false);
assert.equal(isNamePasteAllowed('Ana\u0000María'), false);
const huge = `\u00a0${'A'.repeat(200_001)}\u3000`;
const hugeResult = validateName(huge, 100);
assert.equal(hugeResult.count, 200_001);
assert.ok(hugeResult.error, 'large input must report the limit instead of overflowing arguments');
assert.equal(validateName(`\u2003${'A'.repeat(100)}\u3000`, 100).count, 100);

const source = await readFile(new URL('../../api/unicode/DerivedGeneralCategory-16.0.0.txt', import.meta.url), 'utf8');
const ranges = [...source.matchAll(/^([0-9A-F]+)(?:\.\.([0-9A-F]+))?\s*;\s*([A-Za-z]+)/gm)].map((match) => [Number.parseInt(match[1], 16), Number.parseInt(match[2] ?? match[1], 16), match[3]] as const);
let covered = 0;
for (const [start, end, category] of ranges) for (let point = start; point <= end; point += 1) { assert.equal(categoryOf(point), category, `U+${point.toString(16)}`); covered += 1; }
assert.equal(covered, 0x110000, 'generated table covers every Unicode scalar slot');

const originalFetch = globalThis.fetch;
let privateStateCleared = false;
const responses = [
  new Response(JSON.stringify({ code: 'CSRF_INVALID' }), { status: 403, headers: { 'Content-Type': 'application/problem+json' } }),
  new Response(JSON.stringify({ token: 'fresh' }), { status: 200, headers: { 'Content-Type': 'application/json' } }),
  new Response(JSON.stringify({ code: 'AUTHENTICATION_REQUIRED' }), { status: 401, headers: { 'Content-Type': 'application/problem+json' } }),
];
globalThis.fetch = async () => responses.shift()!;
await assert.rejects(
  () => recoverAfterForbidden(
    async () => { const response = await fetch('/api/users', { method: 'POST' }); if (!response.ok) throw response; },
    (error) => error instanceof Response ? error.status : undefined,
    async () => {
      const csrf = await fetch('/api/auth/csrf'); assert.equal(csrf.status, 200);
      const session = await fetch('/api/auth/session'); if (session.status === 401) privateStateCleared = true;
    },
  ),
  (error: unknown) => error instanceof Response && error.status === 403,
);
assert.equal(privateStateCleared, true, '403 recovery session 401 clears private state through the common handler');
globalThis.fetch = originalFetch;

console.log(`validation tests passed: ${unicode.cases.length} Unicode fixtures, ${dniFixture.dni.length} DNI fixtures, ${covered} Unicode points`);
