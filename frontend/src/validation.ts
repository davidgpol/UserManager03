import { categoryOf } from './unicode.generated.ts';

const content = new Set(['L', 'M', 'N', 'P', 'S']);
const allowed = new Set(['L', 'M', 'N', 'P', 'S']);
const isAllowedCategory = (category: string) => allowed.has(category[0]) || category === 'Zs' || category === 'Cf';
export type NameValidation = { value: string; count: number; error?: string };

/** Matches UserValues.trimSpaces: UCD spaces plus Java's accepted C0 controls. */
function isDniTrimSpace(point: number): boolean {
  return (point >= 0x0009 && point <= 0x000d) || (point >= 0x001c && point <= 0x001f) || ['Zs', 'Zl', 'Zp'].includes(categoryOf(point));
}
const isNameTrimSpace = (point: number) => categoryOf(point) === 'Zs';

function nextPoint(value: string, index: number): { point: number; width: number } | undefined {
  const unit = value.charCodeAt(index);
  if (unit >= 0xd800 && unit <= 0xdbff) {
    const next = value.charCodeAt(index + 1);
    if (next < 0xdc00 || next > 0xdfff) return undefined;
    return { point: value.codePointAt(index)!, width: 2 };
  }
  if (unit >= 0xdc00 && unit <= 0xdfff) return undefined;
  return { point: unit, width: 1 };
}

/** True when clipboard text contains a code point that can never be retained in a name. */
export function isNamePasteAllowed(raw: string): boolean {
  for (let index = 0; index < raw.length; index += 1) {
    const unit = raw.charCodeAt(index);
    if (unit >= 0xd800 && unit <= 0xdbff) {
      const next = raw.charCodeAt(index + 1);
      if (next < 0xdc00 || next > 0xdfff) return false;
      const category = categoryOf(raw.codePointAt(index)!);
      if (!isAllowedCategory(category)) return false;
      index += 1;
    } else if (unit >= 0xdc00 && unit <= 0xdfff) {
      return false;
    } else if (!isAllowedCategory(categoryOf(unit))) {
      return false;
    }
  }
  return true;
}

export function validateName(raw: string, maximum: number): NameValidation {
  let start = 0;
  while (start < raw.length) {
    const current = nextPoint(raw, start);
    if (!current) return { value: raw, count: 0, error: 'Contiene un sustituto UTF-16 aislado.' };
    if (!isNameTrimSpace(current.point)) break;
    start += current.width;
  }
  let end = raw.length;
  while (end > start) {
    const unit = raw.charCodeAt(end - 1);
    const before = unit >= 0xdc00 && unit <= 0xdfff ? end - 2 : end - 1;
    const current = before < start ? undefined : nextPoint(raw, before);
    if (!current || before + current.width !== end) return { value: raw, count: 0, error: 'Contiene un sustituto UTF-16 aislado.' };
    if (!isNameTrimSpace(current.point)) break;
    end = before;
  }
  const trimmed = raw.slice(start, end);
  let hasContent = false;
  let count = 0;
  let invalid = false;
  for (let index = start; index < end;) {
    const current = nextPoint(raw, index);
    if (!current) return { value: trimmed, count, error: 'Contiene un sustituto UTF-16 aislado.' };
    const category = categoryOf(current.point);
    if (!isAllowedCategory(category)) invalid = true;
    if (content.has(category[0])) hasContent = true;
    count += 1;
    index += current.width;
  }
  if (invalid) return { value: trimmed, count, error: 'Incluye un carácter Unicode no permitido.' };
  if (!hasContent) return { value: trimmed, count, error: 'Debe incluir al menos una letra, marca, número, puntuación o símbolo.' };
  if (count < 1 || count > maximum) return { value: trimmed, count, error: `Debe contener entre 1 y ${maximum} puntos de código Unicode tras el recorte exterior.` };
  return { value: trimmed, count };
}

export function canonicalDni(raw: string): string | undefined {
  let start = 0; let end = raw.length;
  while (start < end) { const current = nextPoint(raw, start); if (!current) return undefined; if (!isDniTrimSpace(current.point)) break; start += current.width; }
  while (end > start) { const unit = raw.charCodeAt(end - 1); const before = unit >= 0xdc00 && unit <= 0xdfff ? end - 2 : end - 1; const current = before < start ? undefined : nextPoint(raw, before); if (!current || before + current.width !== end) return undefined; if (!isDniTrimSpace(current.point)) break; end = before; }
  const value = raw.slice(start, end); if (!/^[0-9]{8}[A-Za-z]$/.test(value)) return undefined;
  const letters = 'TRWAGMYFPDXBNJZSQVHLCKE'; return letters[Number(value.slice(0, 8)) % 23] === value[8].toUpperCase() ? `${value.slice(0, 8)}${value[8].toUpperCase()}` : undefined;
}

/** Email syntax belongs to Jakarta Email on the server; the UI must not alter it. */
export const emailForServer = (value: string) => value;
