import React, { useEffect, useState } from 'react';
import { api } from './api';
export const statuses = { EM_ANALISE: 'Em análise', APROVADO: 'Aprovado', REPROVADO: 'Reprovado', CANCELADO: 'Cancelado', CONTRATADO: 'Contratado' };
export const profiles = { CLIENTE: 'Cliente', EMPRESA: 'Agente de empresa', BANCO: 'Agente de banco' };
export const money = value => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
export const date = value => value ? new Intl.DateTimeFormat('pt-BR').format(new Date(value.includes('T') ? value : `${value}T12:00:00`)) : '—';
export const today = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`; };
export function useResource(path) {
  const [state, setState] = useState({ data: null, loading: true, error: '' });
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    let active = true;
    setState({ data: null, loading: true, error: '' });
    api(path).then(data => active && setState({ data, loading: false, error: '' }))
      .catch(e => active && setState({ data: null, loading: false, error: e.message }));
    return () => { active = false; };
  }, [path, revision]);
  return { ...state, reload: () => setRevision(x => x + 1) };
}
export function Notice({ error, success }) { return <>{error && <div className="notice error" role="alert">{error}</div>}{success && <div className="notice success" role="status">{success}</div>}</>; }
export function Resource({ resource, children }) {
  if (resource.loading) return <div className="empty" role="status"><span className="spinner" /> Carregando informações…</div>;
  if (resource.error) return <div className="empty"><Notice error={resource.error}/><button onClick={resource.reload}>Tentar novamente</button></div>;
  return children(resource.data);
}
export function Field({ label, ...props }) { return <label className="field"><span>{label}</span><input required {...props}/></label>; }
export function MoneyField(props) { return <Field type="number" min="0.01" max="9999999999999.99" step="0.01" {...props}/>; }
export function Badge({ status }) { return <span className={`badge ${status}`}>{statuses[status] || status}</span>; }
export function Heading({ eyebrow, title, children }) { return <header className="page-heading"><p className="eyebrow">{eyebrow}</p><h1>{title}</h1>{children && <p className="muted">{children}</p>}</header>; }
export function Empty({ children }) { return <div className="empty">{children}</div>; }
export function ActionForm({ onSubmit, children, label = 'Salvar', successMessage = 'Alterações salvas.', className = '' }) {
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [success, setSuccess] = useState('');
  async function submit(e) {
    e.preventDefault(); if (busy) return;
    const form = e.currentTarget;
    const values = Object.fromEntries(new FormData(form));
    setBusy(true); setError(''); setSuccess('');
    try { await onSubmit(values, form); setSuccess(successMessage); } catch (e) { setError(e.message); }
    finally { setBusy(false); }
  }
  return <form onSubmit={submit} className={className}><Notice error={error} success={success}/><fieldset disabled={busy}>{children}<button className="primary" type="submit">{busy ? 'Aguarde…' : label}</button></fieldset></form>;
}
export function ConfirmAction({ label, message, onConfirm, onSuccess }) {
  const [confirm, setConfirm] = useState(false), [busy, setBusy] = useState(false), [error, setError] = useState('');
  async function run() { setBusy(true); setError(''); try { await onConfirm(); setConfirm(false); onSuccess?.(); } catch(e) { setError(e.message); } finally { setBusy(false); } }
  return <div className="confirmation"><Notice error={error}/>{confirm ? <><p>{message}</p><div className="actions"><button className="danger" disabled={busy} onClick={run}>{busy ? 'Aguarde…' : 'Confirmar'}</button><button disabled={busy} onClick={() => setConfirm(false)}>Voltar</button></div></> : <button className="danger ghost" onClick={() => setConfirm(true)}>{label}</button>}</div>;
}
