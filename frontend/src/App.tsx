import { useEffect, useRef, useState } from 'react';
import type { ClipboardEvent, FormEvent } from 'react';
import { Alert, AppBar, Box, Button, CircularProgress, Container, Dialog, DialogActions, DialogContent, DialogTitle, FormControl, IconButton, InputLabel, MenuItem, Paper, Select, Snackbar, Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField, Toolbar, Tooltip, Typography } from '@mui/material';
import { api, problemFrom, problemMessage, request } from './api';
import type { User } from './generated/api';
import { canonicalDni, emailForServer, isNamePasteAllowed, validateName } from './validation';

type Status = 'active' | 'inactive' | 'all';
type Fields = { firstName: string; lastName: string; dni: string; email: string };
const blank: Fields = { firstName: '', lastName: '', dni: '', email: '' };
const toFields = (user: User): Fields => ({ firstName: user.firstName, lastName: user.lastName, dni: user.dni, email: user.email });
const utc = (date: Date) => date.toISOString().replace('T', ' ').replace('.000Z', ' UTC').replace('Z', ' UTC');

function Login({ onLogin }: { onLogin: (username: string, password: string) => Promise<void> }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setLoading(true); setError('');
    try { await onLogin(username, password); } catch (caught: unknown) { setError(problemMessage(await problemFrom(caught), 'No se pudo iniciar sesión. Comprueba las credenciales.')); }
    finally { setPassword(''); setLoading(false); }
  }
  return <Container maxWidth="sm"><Paper sx={{ mt: 8, p: { xs: 2, sm: 4 } }} component="form" onSubmit={submit}>
    <Stack spacing={2}><Typography variant="h5">Acceso de operador</Typography>
      <TextField required label="Usuario" value={username} onChange={(event) => setUsername(event.target.value)} autoComplete="username" />
      <TextField required label="Contraseña" type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" />
      {error && <Alert severity="error">{error}</Alert>}<Button type="submit" variant="contained" disabled={loading}>{loading ? 'Accediendo…' : 'Iniciar sesión'}</Button>
    </Stack></Paper></Container>;
}

function UserForm({ initial, title, onSave, onCancel }: { initial: Fields; title: string; onSave: (fields: Fields) => Promise<void>; onCancel: () => void }) {
  const [fields, setFields] = useState(initial); const [saving, setSaving] = useState(false); const [error, setError] = useState('');
  const name = validateName(fields.firstName, 100); const lastName = validateName(fields.lastName, 150); const dni = canonicalDni(fields.dni);
  const valid = !name.error && !lastName.error && Boolean(dni) && fields.email.length > 0;
  const set = (key: keyof Fields) => (value: string) => setFields((current) => ({ ...current, [key]: value }));
  const pasteName = (label: string) => (event: ClipboardEvent<HTMLInputElement>) => {
    if (!isNamePasteAllowed(event.clipboardData.getData('text'))) { event.preventDefault(); setError(`${label} contiene caracteres que no se pueden usar.`); }
  };
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!valid) { setError('Revisa los campos marcados.'); return; }
    setSaving(true); setError('');
    try { await onSave({ ...fields, firstName: name.value, lastName: lastName.value, dni: dni!, email: emailForServer(fields.email) }); }
    catch (caught: unknown) { setError(problemMessage(await problemFrom(caught), 'No se pudo guardar la ficha. No se ha repetido la operación.')); }
    finally { setSaving(false); }
  }
  return <Box component="form" onSubmit={submit}><Stack spacing={2}><Typography variant="h6">{title}</Typography>
    <TextField required label="Nombre" value={fields.firstName} onChange={(event) => set('firstName')(event.target.value)} onPaste={pasteName('El nombre')} error={Boolean(name.error)} helperText={name.error ?? `${name.count}/100 caracteres`} />
    <TextField required label="Apellidos" value={fields.lastName} onChange={(event) => set('lastName')(event.target.value)} onPaste={pasteName('Los apellidos')} error={Boolean(lastName.error)} helperText={lastName.error ?? `${lastName.count}/150 caracteres`} />
    <TextField required label="DNI" value={fields.dni} onChange={(event) => set('dni')(event.target.value)} error={fields.dni.length > 0 && !dni} helperText={fields.dni.length > 0 && !dni ? 'Introduce un DNI español válido.' : 'Ocho cifras y letra de control.'} />
    <TextField required label="Correo electrónico" value={fields.email} onChange={(event) => set('email')(event.target.value)} />
    {error && <Alert severity="error">{error}</Alert>}<Stack direction="row" spacing={1}><Button type="button" disabled={saving} onClick={onCancel}>Cancelar</Button><Button type="submit" variant="contained" disabled={saving}>{saving ? 'Guardando…' : 'Guardar'}</Button></Stack>
  </Stack></Box>;
}

export default function App() {
  const [authenticated, setAuthenticated] = useState<boolean | null>(null); const [items, setItems] = useState<User[]>([]); const [selected, setSelected] = useState<User | null>(null); const [detailId, setDetailId] = useState<string | null>(null); const [detailLoading, setDetailLoading] = useState(false); const [detailError, setDetailError] = useState('');
  const [editing, setEditing] = useState<User | null | undefined>(undefined); const [removing, setRemoving] = useState<User | null>(null); const [removingPending, setRemovingPending] = useState(false); const [removingError, setRemovingError] = useState('');
  const [page, setPage] = useState(0); const [size, setSize] = useState(20); const [totalPages, setTotalPages] = useState(0); const [status, setStatus] = useState<Status>('active'); const [query, setQuery] = useState(''); const [loading, setLoading] = useState(false); const [error, setError] = useState(''); const [notice, setNotice] = useState('');
  const listSequence = useRef(0); const detailSequence = useRef(0); const sessionEpoch = useRef(0);
  const clearPrivateState = () => { sessionEpoch.current += 1; listSequence.current += 1; detailSequence.current += 1; setItems([]); setSelected(null); setDetailId(null); setDetailError(''); setDetailLoading(false); setEditing(undefined); setRemoving(null); setRemovingError(''); setRemovingPending(false); setPage(0); setSize(20); setTotalPages(0); setQuery(''); setStatus('active'); setLoading(false); setError(''); setNotice(''); };
  const closeDetail = () => { detailSequence.current += 1; setDetailId(null); setSelected(null); setDetailError(''); setDetailLoading(false); };
  const recover = async () => { const epoch = sessionEpoch.current; await api.csrf(); const session = await api.session(); if (epoch === sessionEpoch.current) setAuthenticated(session.authenticated); };
  api.setUnauthorized(() => { clearPrivateState(); setAuthenticated(false); });
  const load = async (nextPage = page) => {
    const sequence = ++listSequence.current; setLoading(true); setError(''); setItems([]); setTotalPages(0);
    try {
      const result = await request(() => api.users().listUsers({ page: String(nextPage), size: String(size), status, ...(query === '' ? {} : { q: query }) }), recover);
      if (sequence === listSequence.current) { setItems(result.items); setPage(result.page); setTotalPages(result.totalPages); }
    } catch (caught: unknown) { if (sequence === listSequence.current) { setItems([]); setError(problemMessage(await problemFrom(caught), 'No se pudo cargar el listado. Puedes reintentar.')); } }
    finally { if (sequence === listSequence.current) setLoading(false); }
  };
  const openDetail = async (id: string) => {
    const epoch = sessionEpoch.current; const sequence = ++detailSequence.current;
    setDetailId(id); setSelected(null); setDetailLoading(true); setDetailError('');
    try {
      const user = await request(() => api.users().getUser({ id }), recover);
      if (epoch === sessionEpoch.current && sequence === detailSequence.current) setSelected(user);
    } catch (caught: unknown) {
      if (epoch === sessionEpoch.current && sequence === detailSequence.current) setDetailError(problemMessage(await problemFrom(caught), 'No se pudo consultar la ficha.'));
    } finally { if (epoch === sessionEpoch.current && sequence === detailSequence.current) setDetailLoading(false); }
  };
  useEffect(() => { void (async () => { try { await api.csrf(); const session = await api.session(); setAuthenticated(session.authenticated); } catch { setAuthenticated(false); } })(); }, []);
  useEffect(() => { if (authenticated) void load(0); }, [authenticated, status, size]);
  async function login(username: string, password: string) { const session = await api.login(username, password); clearPrivateState(); setAuthenticated(session.authenticated); }
  async function logout() { try { await request(() => api.logout(), recover); clearPrivateState(); setAuthenticated(false); setNotice('Sesión cerrada.'); } catch (caught: unknown) { setError(problemMessage(await problemFrom(caught), 'No se pudo cerrar la sesión. El estado se ha conservado.')); } }
  async function save(fields: Fields) { const epoch = sessionEpoch.current; const currentEditing = editing; const user = currentEditing ? await request(() => api.users().updateUser({ id: currentEditing.id, userUpdateRequest: fields }), recover) : await request(() => api.users().createUser({ userCreateRequest: fields }), recover); if (epoch !== sessionEpoch.current) return; setEditing(undefined); setNotice(currentEditing ? 'Ficha actualizada.' : 'Ficha creada y activa.'); await openDetail(user.id); if (epoch === sessionEpoch.current) await load(0); }
  async function deactivate() { if (!removing || removingPending) return; const epoch = sessionEpoch.current; const user = removing; setRemovingPending(true); setRemovingError(''); try { await request(() => api.users().deactivateUser({ id: user.id }), recover); if (epoch !== sessionEpoch.current) return; setNotice('Ficha dada de baja.'); setRemoving(null); await load(0); } catch (caught: unknown) { if (epoch === sessionEpoch.current) setRemovingError(problemMessage(await problemFrom(caught), 'No se pudo dar de baja la ficha. Puedes reintentarlo.')); } finally { if (epoch === sessionEpoch.current) setRemovingPending(false); } }
  if (authenticated === null) return <Box sx={{ display: 'grid', placeItems: 'center', minHeight: '100vh' }}><CircularProgress /></Box>;
  if (!authenticated) return <Login onLogin={login} />;
  return <><AppBar position="static"><Toolbar><Typography variant="h6" sx={{ flexGrow: 1 }}>Gestión de usuarios</Typography><Button color="inherit" onClick={() => void logout()}>Cerrar sesión</Button></Toolbar></AppBar>
    <Container maxWidth="lg" sx={{ py: 3 }}><Stack spacing={2}><Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}><TextField label="Buscar" value={query} onChange={(event) => setQuery(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') void load(0); }} fullWidth /><FormControl sx={{ minWidth: { xs: '100%', sm: 150 } }}><InputLabel id="status-label">Estado</InputLabel><Select labelId="status-label" label="Estado" value={status} onChange={(event) => setStatus(event.target.value as Status)}><MenuItem value="active">Activos</MenuItem><MenuItem value="inactive">Inactivos</MenuItem><MenuItem value="all">Todos</MenuItem></Select></FormControl><Button variant="outlined" onClick={() => void load(0)}>Buscar</Button><Button variant="contained" onClick={() => setEditing(null)}>Nueva ficha</Button></Stack>
      {error && <Alert severity="error" action={<Button color="inherit" size="small" onClick={() => void load()}>Reintentar</Button>}>{error}</Alert>}
      {loading ? <Box sx={{ textAlign: 'center' }}><CircularProgress /></Box> : error ? null : items.length === 0 ? <Alert severity="info">No hay fichas para los filtros seleccionados.</Alert> : <><Stack spacing={2} sx={{ display: { xs: 'flex', sm: 'none' } }}>{items.map(user => <Paper key={user.id} sx={{ p: 2 }}><Stack spacing={1}><Typography variant="h6" sx={{ overflowWrap: 'anywhere' }}>{user.firstName} {user.lastName}</Typography><Typography>DNI: {user.dni}</Typography><Typography sx={{ overflowWrap: 'anywhere' }}>{user.email}</Typography><Typography>{user.active ? 'Activa' : 'Inactiva'}</Typography><Stack direction="row" sx={{ flexWrap: 'wrap' }}><Button onClick={() => void openDetail(user.id)}>Ver detalle</Button><Button onClick={() => setEditing(user)}>Editar</Button>{user.active && <Button color="error" onClick={() => setRemoving(user)}>Dar de baja</Button>}</Stack></Stack></Paper>)}</Stack><Paper sx={{ overflowX: 'auto', display: { xs: 'none', sm: 'block' } }}><Table><TableHead><TableRow><TableCell>Nombre</TableCell><TableCell>DNI</TableCell><TableCell>Correo</TableCell><TableCell>Estado</TableCell><TableCell align="right">Acciones</TableCell></TableRow></TableHead><TableBody>{items.map((user) => <TableRow hover key={user.id}><TableCell sx={{ wordBreak: 'break-word' }}>{user.firstName} {user.lastName}</TableCell><TableCell>{user.dni}</TableCell><TableCell sx={{ wordBreak: 'break-word' }}>{user.email}</TableCell><TableCell>{user.active ? 'Activa' : 'Inactiva'}</TableCell><TableCell align="right"><Tooltip title="Ver detalle"><IconButton aria-label={`Ver detalle de ${user.firstName} ${user.lastName}`} onClick={() => void openDetail(user.id)}>⌕</IconButton></Tooltip><Tooltip title="Editar"><IconButton aria-label={`Editar ${user.firstName} ${user.lastName}`} onClick={() => { setEditing(user); }}>✎</IconButton></Tooltip>{user.active && <Tooltip title="Dar de baja"><IconButton aria-label={`Dar de baja ${user.firstName} ${user.lastName}`} color="error" onClick={() => setRemoving(user)}>⌫</IconButton></Tooltip>}</TableCell></TableRow>)}</TableBody></Table></Paper></>}
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ justifyContent: 'space-between', alignItems: { xs: 'stretch', sm: 'center' } }}><Button disabled={page === 0 || loading} onClick={() => void load(page - 1)}>Anterior</Button><Typography>Página {page + 1} de {Math.max(totalPages, 1)}</Typography><Button disabled={!totalPages || page + 1 >= totalPages || loading} onClick={() => void load(page + 1)}>Siguiente</Button><FormControl size="small"><Select value={String(size)} onChange={(event) => setSize(Number(event.target.value))}><MenuItem value="20">20</MenuItem><MenuItem value="50">50</MenuItem><MenuItem value="100">100</MenuItem></Select></FormControl></Stack>
    </Stack></Container>
    <Dialog open={detailId !== null} onClose={closeDetail} fullWidth maxWidth="sm"><DialogTitle>Detalle de ficha</DialogTitle><DialogContent sx={{ overflowWrap: 'anywhere' }}>{detailLoading && <CircularProgress />}{detailError && <Alert severity="error" action={detailId ? <Button onClick={() => void openDetail(detailId)}>Reintentar</Button> : undefined}>{detailError}</Alert>}{selected && <Stack spacing={1}><Typography>{selected.firstName} {selected.lastName}</Typography><Typography>Identificador: {selected.id}</Typography><Typography>DNI: {selected.dni}</Typography><Typography>Correo: {selected.email}</Typography><Typography>Estado: {selected.active ? 'Activa' : 'Inactiva'}</Typography><Typography>Creada: {utc(selected.createdAt)}</Typography><Typography>Actualizada: {utc(selected.updatedAt)}</Typography></Stack>}</DialogContent><DialogActions>{selected && <Button onClick={() => { setEditing(selected); closeDetail(); }}>Editar</Button>}{selected?.active && <Button color="error" onClick={() => { setRemoving(selected); closeDetail(); }}>Dar de baja</Button>}<Button onClick={closeDetail}>Cerrar</Button></DialogActions></Dialog>
    <Dialog open={editing !== undefined} onClose={() => setEditing(undefined)} fullWidth maxWidth="sm"><DialogContent><UserForm title={editing ? 'Editar ficha' : 'Nueva ficha'} initial={editing ? toFields(editing) : blank} onSave={save} onCancel={() => setEditing(undefined)} /></DialogContent></Dialog>
    <Dialog open={removing !== null} onClose={() => { if (!removingPending) setRemoving(null); }}><DialogTitle>Confirmar baja</DialogTitle><DialogContent><Stack spacing={2}><Typography>La ficha quedará inactiva y se conservará su historial.</Typography>{removingError && <Alert severity="error">{removingError}</Alert>}</Stack></DialogContent><DialogActions><Button disabled={removingPending} onClick={() => setRemoving(null)}>Cancelar</Button><Button color="error" variant="contained" disabled={removingPending} onClick={() => void deactivate()}>{removingPending ? 'Dando de baja…' : removingError ? 'Reintentar baja' : 'Confirmar baja'}</Button></DialogActions></Dialog>
    <Snackbar open={Boolean(notice)} autoHideDuration={5000} onClose={() => setNotice('')} message={notice} />
  </>;
}
