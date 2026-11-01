# API de aluguel de carros

Base: `http://localhost:8080/api`. Entradas e saídas em JSON (UTF-8).
Java 25 / Spring Boot 4.0.9. Datas `YYYY-MM-DD`; data/hora sem offset no
horário do servidor (`LocalDateTime`). Valores monetários com até 13 dígitos
inteiros e duas casas decimais. IDs são números Long.

## Sessão, CSRF e React + Vite

A autenticação usa usuário e senha com BCrypt, sessão HTTP e cookie
`JSESSIONID` HttpOnly. Não há JWT nem armazenamento de senha no front-end.
O cookie não deve ser lido por JavaScript. Use `credentials: 'include'` em
**todas** as chamadas, inclusive para obter CSRF, cadastrar, entrar e sair.

1. `GET /auth/csrf` cria/usa a sessão e retorna `{ "headerName": "X-CSRF-TOKEN",
   "token": "..." }`.
2. Envie esse token no header indicado em POST, PUT e DELETE, inclusive cadastro,
   login e logout. O token é vinculado à sessão e não deve ser fixado no código.
3. Após login, busque novamente `/auth/csrf`: o ID da sessão e o token são
   renovados. O contexto de segurança é explicitamente salvo na sessão.
4. Logout invalida a sessão. Para entrar novamente, obtenha outro CSRF.
5. `GET /auth/me` recupera o usuário ao recarregar a página. Sessões expiram
   após 30 minutos de inatividade. Conta desativada é recusada também em sessões
   abertas anteriormente.

Exemplo de cliente HTTP (não é implementação de front-end):

```javascript
const base = 'http://localhost:8080/api';
let csrf;
async function renovarCsrf() {
  const response = await fetch(`${base}/auth/csrf`, { credentials: 'include' });
  if (!response.ok) throw new Error('Não foi possível obter CSRF');
  csrf = await response.json();
}
async function api(path, method = 'GET', body) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) headers[csrf.headerName] = csrf.token;
  const response = await fetch(base + path, {
    method, credentials: 'include', headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const data = response.status === 204 ? null : await response.json();
  if (!response.ok) throw Object.assign(new Error(data.mensagem), { status: response.status });
  return data;
}
await renovarCsrf();
await api('/auth/login', 'POST', { usuario: '<USUARIO>', senha: '<SENHA>' });
await renovarCsrf();
const usuario = await api('/auth/me');
```

CORS permite `http://localhost:5173` por padrão, com credenciais. Configure
`FRONTEND_ORIGINS` com origens exatas separadas por vírgula. Não use `*`.
Use consistentemente `localhost`, sem alternar com `127.0.0.1`. O cookie é
SameSite=Lax, adequado ao front-end e API no mesmo site (portas diferentes
são permitidas). Em produção use HTTPS e `SESSION_COOKIE_SECURE=true`;
prefira proxy no mesmo site. Uma implantação cross-site exige revisão explícita
de SameSite, HTTPS e CORS, não apenas trocar a URL.

## Perfis e autorização

- `CLIENTE`: cadastro próprio, renda e pedidos próprios.
- `EMPRESA`: agente de empresa, consulta e análise dos pedidos, contratos.
- `BANCO`: agente com as mesmas permissões de análise e exclusividade para crédito.
- O cadastro público **sempre** cria cliente. Não aceita perfil, ativo, ID ou hash.
- Os agentes são provisionados fora da API pública; existe carga opcional de desenvolvimento.
- Nesta versão todos os agentes ativos podem analisar todos os pedidos e consultar
  contratos. Não há vínculo de atendimento que limite agentes a uma carteira.
- Cliente que tenta acessar pedido, vínculo ou contrato de outro cliente recebe
  404 para não revelar sua existência. Operação de perfil incorreto retorna 403.
- Entidades nunca são retornadas: os contratos HTTP são DTOs.

## Endpoints

| Método e caminho | Permissão | Sucesso / retorno |
|---|---|---|
| GET `/auth/csrf` | Público | 200 token e headerName |
| POST `/auth/login` | Público + CSRF | 200 usuário autenticado |
| GET `/auth/me` | Autenticado | 200 usuário autenticado |
| POST `/auth/logout` | Sessão + CSRF | 204 |
| POST `/clientes` | Público + CSRF | 201 cliente |
| GET `/clientes/me` | Cliente | 200 cliente |
| PUT `/clientes/me` | Cliente | 200 cliente atualizado |
| DELETE `/clientes/me` | Cliente | 204 conta desativada e sessão invalidada |
| GET `/clientes/me/vinculos-renda` | Cliente | 200 array de vínculos |
| POST `/clientes/me/vinculos-renda` | Cliente | 201 vínculo |
| PUT `/clientes/me/vinculos-renda/{id}` | Dono | 200 vínculo |
| DELETE `/clientes/me/vinculos-renda/{id}` | Dono | 204 |
| GET `/automoveis` | Autenticado | 200 array de automóveis |
| POST `/pedidos` | Cliente | 201 pedido |
| GET `/pedidos` | Autenticado | 200 array: próprios para cliente, todos para agente |
| GET `/pedidos/{id}` | Dono ou agente | 200 pedido |
| PUT `/pedidos/{id}` | Dono ou agente, EM_ANALISE | 200 pedido |
| POST `/pedidos/{id}/cancelamento` | Dono, EM_ANALISE/APROVADO | 200 pedido |
| POST `/pedidos/{id}/avaliacao` | Agente, EM_ANALISE | 201 pedido com avaliação |
| POST `/pedidos/{id}/contrato` | Agente, APROVADO | 201 contrato |
| GET `/contratos/{id}` | Dono do pedido ou agente | 200 contrato |
| POST `/contratos/{id}/credito` | BANCO | 201 contrato com crédito |

Todas as mutações requerem CSRF. Logout sem sessão também é idempotente quando
há um token válido. Não existem endpoints de listagem genérica de clientes,
CRUD de frota, troca de proprietário, definição de status ou cadastro público de agentes.
As listas não têm paginação nesta entrega acadêmica.

## Payloads de entrada

Cadastro (`POST /clientes`):

```json
{
  "usuario": "cliente.exemplo",
  "senha": "<SENHA_DE_8_CARACTERES_OU_MAIS>",
  "rg": "1234567",
  "cpf": "529.982.247-25",
  "nome": "Cliente Exemplo",
  "endereco": "Rua Exemplo, 10",
  "profissao": "Analista"
}
```

CPF aceita dígitos ou pontuação usual, é normalizado para 11 dígitos e tem
os dígitos verificadores validados. Login é sensível a maiúsculas, contém de
3 a 100 caracteres ASCII (letras, números, `_`, `.` e `-`) e não pode ser alterado
nesta versão. A senha possui no mínimo 8 caracteres e no máximo 72 bytes UTF-8
(limite BCrypt). Login e CPF têm unicidade no banco, inclusive para contas desativadas.

`PUT /clientes/me` recebe somente `rg`, `cpf`, `nome`, `endereco` e `profissao`,
todos obrigatórios. Não altera usuário, senha ou perfil. Limites: RG 30, nome
200, endereço 500 e profissão 150 caracteres.

Login: `{ "usuario": "<USUARIO>", "senha": "<SENHA>" }`.

Renda (POST/PUT):

```json
{ "entidadeEmpregadora": "Empresa Exemplo", "rendimento": 3500.50 }
```

Empregador tem até 200 caracteres e rendimento deve ser positivo. Até três
vínculos. POST cria, PUT substitui dados do vínculo, DELETE remove o vínculo
fisicamente. Desativar cliente não remove seus vínculos nem o histórico.

Pedido (POST/PUT):

```json
{ "automovelId": 1, "dataInicio": "2027-01-01", "dataFim": "2027-01-05" }
```

Data final deve ser posterior à inicial. Não recebe `clienteId`, `status`,
`proprietarioId` ou datas de auditoria. Campos desconhecidos retornam 400.

Avaliação:

```json
{ "parecer": "Renda compatível com a solicitação", "aprovada": true }
```

Parecer obrigatório, até 2000 caracteres. `false` reprova. Responsável e data são
preenchidos pelo servidor. Uma única avaliação conclusiva por pedido.

Contrato:

```json
{ "dataCelebracao": "2026-11-01", "valor": 1200.00 }
```

Valor positivo. Responsável vem da sessão. Um contrato por pedido.

Crédito:

```json
{ "valor": 1000.00, "quantidadeParcelas": 10 }
```

Valor e quantidade positivos. `bancoId` não é aceito: usa o banco autenticado.
Um crédito opcional por contrato. Não há regra de juros, teto relativo ao aluguel
ou cronograma de parcelas nesta modelagem.

## Respostas

Usuário autenticado: `{ "id": 1, "usuario": "cliente.exemplo", "perfil": "CLIENTE", "ativo": true }`.

Cliente: `id`, `usuario`, `rg`, `cpf` normalizado, `nome`, `endereco`, `profissao`,
`ativo` e `vinculosRenda` (array). Cada vínculo contém `id`, `entidadeEmpregadora`,
`rendimento`. Nenhuma resposta contém senha ou hash.

Automóvel: `id`, `matricula`, `ano`, `marca`, `modelo`, `placa`,
`tipoProprietario` (`CLIENTE` ou `AGENTE`) e `proprietarioId`.

Exemplo de pedido aprovado:

```json
{
  "id": 10, "clienteId": 1,
  "automovel": { "id": 2, "matricula": "DEMO-001", "ano": 2024,
    "marca": "Fiat", "modelo": "Argo", "placa": "DEM1A01",
    "tipoProprietario": "AGENTE", "proprietarioId": 3 },
  "dataCriacao": "2026-11-01T15:00:00",
  "dataInicio": "2027-01-01", "dataFim": "2027-01-05", "status": "APROVADO",
  "avaliacao": { "id": 7, "agenteId": 3, "dataAvaliacao": "2026-11-01T16:00:00",
    "parecer": "Renda compatível", "aprovada": true },
  "contratoId": null
}
```

Contrato:

```json
{
  "id": 8, "pedidoId": 10, "agenteId": 3,
  "dataCelebracao": "2026-11-01", "valor": 1200.00,
  "credito": { "id": 9, "bancoId": 4, "valor": 1000.00, "quantidadeParcelas": 10 }
}
```

`avaliacao`, `contratoId` e `credito` são null enquanto inexistentes.

## Fluxo completo

1. Obter CSRF, cadastrar cliente, fazer login e renovar CSRF.
2. Consultar `/clientes/me`; adicionar até três vínculos de renda.
3. Consultar `/automoveis`; criar pedido com o ID escolhido.
4. Cliente consulta e pode alterar enquanto EM_ANALISE, ou cancelar nesse estado.
5. Em outra sessão, agente faz login, renova CSRF e consulta `/pedidos`.
6. Agente altera a solicitação em análise se necessário e envia avaliação.
7. Se reprovado, pedido termina REPROVADO. Se aprovado, fica APROVADO.
8. Agente registra contrato; pedido fica CONTRATADO. Antes disso, o cliente pode
   cancelar um pedido APROVADO. Um contrato impede cancelamento por este fluxo.
9. Banco autenticado pode associar crédito ao contrato; cliente consulta o contrato
   pelo `contratoId` retornado no pedido.
10. Sair com POST `/auth/logout`. Desativação é DELETE `/clientes/me`, sem apagar histórico.

## Concorrência, decisões e erros

Inclusão/alteração/remoção de renda bloqueia a linha do cliente com
`PESSIMISTIC_WRITE` dentro de transação. Alterações de status, avaliação e criação
de contrato bloqueiam o pedido. Crédito bloqueia o contrato. Isso serializa
operações concorrentes nesses agregados pela API. As FKs e unicidades completam
as garantias; a regra de três rendas não é um trigger e SQL externo deve respeitá-la.

Não há transferência automática de propriedade, reserva de disponibilidade,
prevenção de sobreposição de períodos, exclusão de pedidos, alteração de avaliação
conclusiva ou alteração de contrato nesta entrega. A propriedade do automóvel
continua registrada no veículo e tem CHECK de exatamente um proprietário.
CNPJ identifica a instituição (14 dígitos); múltiplos agentes podem representar
a mesma instituição. Perfis de agente não são editáveis pela API.

Erros consistentes: `{ "status": 409, "mensagem": "..." }`.

- 400: JSON/campos inválidos, CPF inválido ou período incorreto.
- 401: não autenticado, login recusado ou conta desativada.
- 403: perfil sem permissão ou CSRF ausente/inválido (inclusive antes do login).
- 404: recurso inexistente ou pertencente a outro cliente.
- 409: duplicidade, limite de renda, transição proibida ou conflito concorrente.
- 405/415: método HTTP ou tipo de conteúdo não suportado.
- 500: falha inesperada, sem stack trace ou detalhes internos na resposta.

A API não redireciona para login HTML. Trate 401 pedindo nova autenticação e 403
verificando perfil e CSRF. Não repita automaticamente uma mutação após erro sem
consultar seu resultado atual.
