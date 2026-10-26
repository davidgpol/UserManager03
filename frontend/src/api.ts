import { AuthApi, Configuration, ResponseError, UsersApi } from './generated/api';
import type { Problem } from './generated/api';
import { recoverAfterForbidden } from './recovery.ts';

let csrf = '';
let unauthenticated: () => void = () => undefined;
const configuration = () => new Configuration({ basePath: '', credentials: 'same-origin', apiKey: () => csrf });
const auth = () => new AuthApi(configuration());
const users = () => new UsersApi(configuration());
async function guarded<T>(operation: () => Promise<T>): Promise<T> {
  try { return await operation(); } catch (error) {
    if (error instanceof ResponseError && error.response.status === 401) { csrf = ''; unauthenticated(); }
    throw error;
  }
}

export const api = {
  auth, users,
  setUnauthorized(handler: () => void) { unauthenticated = handler; },
  async csrf() { csrf = (await guarded(() => auth().getCsrfToken())).token; },
  async session() { return guarded(() => auth().getSession()); },
  async login(username: string, password: string) { await this.csrf(); await guarded(() => auth().login({ username, password })); await this.csrf(); return this.session(); },
  async logout() { await guarded(() => auth().logout()); csrf = ''; unauthenticated(); await this.csrf(); },
};

export async function problemFrom(error: unknown): Promise<Problem | undefined> {
  if (!(error instanceof ResponseError)) return undefined;
  try { return await error.response.clone().json() as Problem; } catch { return undefined; }
}

export function problemMessage(problem: Problem | undefined, fallback: string): string {
  if (!problem) return fallback;
  const messages: Record<string, string> = {
    DNI_CONFLICT: 'El DNI ya pertenece a otra ficha.',
    VALIDATION_ERROR: 'Revisa los campos indicados.',
    CSRF_INVALID: 'La protección de la sesión se ha renovado. Revisa y vuelve a enviar.',
    AUTHENTICATION_REQUIRED: 'La sesión ya no está disponible.',
    INVALID_CREDENTIALS: 'No se pudo iniciar sesión. Comprueba las credenciales.',
    SERVICE_UNAVAILABLE: 'El servicio no está disponible ahora. Inténtalo de nuevo.',
    USER_NOT_FOUND: 'La ficha ya no está disponible.',
  };
  const fields: Record<string, string> = { firstName: 'Nombre', lastName: 'Apellidos', dni: 'DNI', email: 'Correo electrónico' };
  if (problem.code === 'VALIDATION_ERROR' && problem.errors?.length) return 'Revisa: ' + [...new Set(problem.errors.map(error => fields[error.field] ?? 'los datos enviados'))].join(', ') + '.';
  return messages[problem.code] ?? problem.detail ?? fallback;
}

export async function request<T>(operation: () => Promise<T>, onCsrf: () => Promise<void>): Promise<T> {
  return recoverAfterForbidden(() => guarded(operation), (error) => error instanceof ResponseError ? error.response.status : undefined, onCsrf);
}
