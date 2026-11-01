import React, { createContext, useContext, useEffect, useState } from 'react';
import { Link, Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { api, clearCsrf, renewCsrf } from './api';
import { ActionForm, Field, Notice } from './ui';
const Context = createContext();
export const useAuth = () => useContext(Context);
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null), [loading, setLoading] = useState(true), [error, setError] = useState(''), [expired, setExpired] = useState(false);
  async function restore() {
    setLoading(true); setError('');
    try { setUser(await api('/auth/me')); setExpired(false); }
    catch (e) { if (e.status !== 401) setError(e.message); }
    finally { setLoading(false); }
  }
  useEffect(() => { restore(); const expire = () => { setUser(null); setExpired(true); }; window.addEventListener('session-expired', expire); return () => window.removeEventListener('session-expired', expire); }, []);
  async function login(values) {
    clearCsrf(); await renewCsrf();
    const logged = await api('/auth/login', 'POST', values);
    clearCsrf(); await renewCsrf(); setUser(logged); setExpired(false);
  }
  function clear() { setUser(null); setExpired(false); clearCsrf(); }
  async function logout() { await api('/auth/logout', 'POST'); clear(); }
  return <Context.Provider value={{ user, loading, error, expired, restore, login, logout, clear }}>{children}</Context.Provider>;
}
export function Protected({ roles }) {
  const auth = useAuth(), location = useLocation();
  if (auth.loading) return <div className="startup" role="status">Preparando sua sessão…</div>;
  if (auth.error) return <div className="startup"><Notice error={auth.error}/><button onClick={auth.restore}>Reconectar</button></div>;
  if (!auth.user) return <Navigate to="/login" replace state={{ from: location.pathname }}/>;
  if (roles && !roles.includes(auth.user.perfil)) return <Navigate to="/pedidos" replace/>;
  return <Outlet/>;
}
export function ProfileFields({ data = {} }) { return <div className="form-grid"><Field label="Nome completo" name="nome" maxLength={200} defaultValue={data.nome} autoComplete="name"/><Field label="CPF" name="cpf" maxLength={14} defaultValue={data.cpf} inputMode="numeric"/><Field label="RG" name="rg" maxLength={30} defaultValue={data.rg}/><Field label="Profissão" name="profissao" maxLength={150} defaultValue={data.profissao}/><Field label="Endereço" name="endereco" maxLength={500} defaultValue={data.endereco} autoComplete="street-address"/></div>; }
export function AuthPage({ register = false }) {
  const auth = useAuth(), navigate = useNavigate(), location = useLocation();
  if (auth.loading) return <div className="startup">Preparando sua sessão…</div>;
  if (auth.error) return <div className="startup"><Notice error={auth.error}/><button onClick={auth.restore}>Reconectar</button></div>;
  if (auth.user) return <Navigate to="/pedidos" replace/>;
  async function submit(v) {
    if (register) {
      if (new TextEncoder().encode(v.senha).length > 72) throw new Error('A senha deve ter no máximo 72 bytes. Reduza a quantidade de caracteres.');
      await api('/clientes', 'POST', v);
      navigate('/login', { replace: true, state: { registered: true } });
    } else { await auth.login(v); const from = location.state?.from; navigate(from?.startsWith('/') && !from.startsWith('//') ? from : '/pedidos', { replace: true }); }
  }
  return <div className="auth-layout"><aside className="auth-art"><Link className="brand" to="/login"><span className="brand-mark">r.</span> rota</Link><div><p className="eyebrow">SEU PRÓXIMO DESTINO</p><h1>O caminho<br/>começa aqui.</h1><p>Escolha um carro, acompanhe seu pedido e tenha tudo em um só lugar.</p><div className="road" aria-hidden="true"><span/><span/><span/></div></div><small>Aluguel de carros · Projeto acadêmico</small></aside><main className="auth-main"><div className="auth-card"><p className="eyebrow">BEM-VINDO À ROTA</p><h1>{register ? 'Crie sua conta' : 'Bom ter você aqui.'}</h1><p className="muted">{register ? 'Cadastre-se como cliente para começar.' : 'Entre para acompanhar seus próximos caminhos.'}</p><Notice success={location.state?.registered ? 'Cadastro realizado. Entre com seu usuário e senha.' : ''}/>{auth.expired && !location.state?.registered && <p className="muted">Entre para continuar.</p>}<ActionForm key={register ? 'register' : 'login'} onSubmit={submit} label={register ? 'Criar conta' : 'Entrar'} successMessage=""><Field label="Usuário" name="usuario" autoComplete="username" minLength={register ? 3 : undefined} maxLength={100} pattern={register ? String.raw`[a-zA-Z0-9_.\-]{3,100}` : undefined}/><Field label="Senha" name="senha" type="password" autoComplete={register ? 'new-password' : 'current-password'} minLength={register ? 8 : undefined} maxLength={72}/>{register && <><p className="hint">Senha: ao menos 8 caracteres. Usuário: letras, números, ponto, hífen ou sublinhado.</p><ProfileFields/></>}</ActionForm><p className="auth-switch">{register ? 'Já tem uma conta?' : 'Ainda não tem uma conta?'} <Link to={register ? '/login' : '/cadastro'}>{register ? 'Entrar' : 'Cadastre-se'}</Link></p></div></main></div>;
}
