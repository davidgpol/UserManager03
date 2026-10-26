/** Runs the read-only CSRF/session recovery once and always preserves the mutation's original error. */
export async function recoverAfterForbidden<T>(operation: () => Promise<T>, statusOf: (error: unknown) => number | undefined, recover: () => Promise<void>): Promise<T> {
  try { return await operation(); } catch (error) {
    if (statusOf(error) === 403) {
      try { await recover(); } catch { /* The UI must present the original rejected mutation. */ }
    }
    throw error;
  }
}
