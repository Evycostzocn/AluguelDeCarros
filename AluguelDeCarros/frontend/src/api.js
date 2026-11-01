let csrf;
let pendingCsrf;
export function clearCsrf() { csrf = undefined; pendingCsrf = undefined; }

export class ApiError extends Error {
  constructor(message, status) { super(message); this.status = status; }
}
async function request(path, options = {}) {
  let response;
  try { response = await fetch(`/api${path}`, { ...options, credentials: 'include', headers: { Accept: 'application/json', ...options.headers } }); }
  catch { throw new ApiError('Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.', 0); }
  const text = await response.text();
  let data;
  try { data = text ? JSON.parse(text) : null; } catch { data = null; }
  if (!response.ok) {
    if (response.status === 401 && path !== '/auth/login') {
      clearCsrf(); window.dispatchEvent(new Event('session-expired'));
    }
    if (response.status === 403) clearCsrf();
    const fallback = response.status >= 500 ? 'O servidor está indisponível. Tente novamente mais tarde.' : `Não foi possível concluir a operação (${response.status}).`;
    throw new ApiError(data?.mensagem || fallback, response.status);
  }
  if (response.status !== 204 && data === null) throw new ApiError('O servidor retornou uma resposta inesperada.', response.status);
  return data;
}
export async function renewCsrf() {
  if (!pendingCsrf) pendingCsrf = request('/auth/csrf').then(data => {
    if (!data?.headerName || !data?.token) throw new ApiError('Não foi possível preparar a sessão segura.', 0);
    csrf = data; return data;
  }).finally(() => { pendingCsrf = undefined; });
  return pendingCsrf;
}
export async function api(path, method = 'GET', body) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const token = csrf || await renewCsrf();
    headers[token.headerName] = token.token;
  }
  // Nunca repetir uma mutação automaticamente: o servidor pode já tê-la realizado.
  return request(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
}
