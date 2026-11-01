# API — referência rápida

Base: `http://localhost:8080/api`. Envie JSON com `Content-Type: application/json`.
Datas usam `YYYY-MM-DD`; valores monetários devem ser positivos, com até duas
casas decimais. Os caminhos abaixo são relativos a `/api`.

## Autenticação

A API usa sessão com cookie HttpOnly, não JWT. No navegador:

1. Use `credentials: 'include'` em todas as chamadas.
2. Obtenha `GET /auth/csrf`, que retorna `headerName` e `token`.
3. Envie o token no header indicado em POST, PUT e DELETE, inclusive cadastro,
   login e logout.
4. Após login, obtenha outro token CSRF. Ao recarregar, consulte `/auth/me`.
5. Logout invalida a sessão. Conta desativada não pode acessar operações protegidas.

Perfis: `CLIENTE`, `EMPRESA` e `BANCO`. O cadastro público cria somente clientes.
Agentes consultam todos os pedidos e contratos; clientes acessam somente os próprios.

## Rotas

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


## Campos de entrada

Todos os campos listados são obrigatórios. Não envie campos extras, como perfil,
status, cliente responsável ou banco concedente: eles são controlados pelo servidor.

| Operação | Corpo JSON |
|---|---|
| Login | `{"usuario":"<USUARIO>","senha":"<SENHA>"}` |
| Cadastro | `usuario`, `senha`, `rg`, `cpf`, `nome`, `endereco`, `profissao` |
| Atualizar cadastro | `rg`, `cpf`, `nome`, `endereco`, `profissao` |
| Adicionar/editar renda | `{"entidadeEmpregadora":"Empresa","rendimento":3500.50}` |
| Criar/editar pedido | `{"automovelId":1,"dataInicio":"2027-01-01","dataFim":"2027-01-05"}` |
| Avaliar pedido | `{"parecer":"Renda compatível","aprovada":true}` |
| Registrar contrato | `{"dataCelebracao":"2026-11-01","valor":1200.00}` |
| Registrar crédito | `{"valor":1000.00,"quantidadeParcelas":10}` |

Cancelamento, logout e exclusões não precisam de corpo.

- Usuário: 3–100 caracteres, com letras ASCII, números, `_`, `.` ou `-`.
- Senha de cadastro: mínimo de 8 caracteres e máximo de 72 bytes UTF-8.
- CPF: aceita pontuação, é normalizado e tem dígitos verificadores validados.
- Usuário e CPF são únicos; usuário, senha e perfil não são editáveis nesta versão.
- Cliente pode ter até três vínculos; rendimento deve ser positivo.
- Data final do pedido deve ser posterior à inicial.
- Avaliação é conclusiva: `true` aprova e `false` reprova.
- Há no máximo uma avaliação e um contrato por pedido, e um crédito por contrato.
- Crédito exige quantidade inteira e positiva de parcelas e agente BANCO autenticado.

## Respostas e status

O login e `/auth/me` retornam `id`, `usuario`, `perfil` e `ativo`.
As listas retornam arrays; respostas 204 não têm corpo. Senhas e hashes nunca
são retornados.

O pedido contém automóvel, período, status, `avaliacao` e `contratoId`.
O contrato contém pedido, responsável, data, valor e `credito` opcional.
`avaliacao`, `contratoId` e `credito` são `null` enquanto inexistentes.
Os campos completos e limites estão em
[ApiDtos.java](../main/java/com/example/AluguelDeCarros/dto/ApiDtos.java).

Fluxo dos pedidos:

```text
EM_ANALISE → APROVADO → CONTRATADO
EM_ANALISE → REPROVADO
EM_ANALISE ou APROVADO → CANCELADO
```

Só pedidos EM_ANALISE podem ser alterados. Cancelamento cabe ao cliente dono;
registro de contrato cabe a agentes após aprovação. Desativar cliente preserva
seu histórico. Contratos não transferem a propriedade do automóvel.

## Erros

Formato: `{"status":409,"mensagem":"..."}`.

| Código | Significado |
|---|---|
| 400 | Dados ou campos inválidos |
| 401 | Sem autenticação, login recusado ou conta desativada |
| 403 | Perfil sem permissão ou CSRF inválido/ausente |
| 404 | Recurso inexistente ou de outro cliente |
| 409 | Duplicidade, limite de renda ou operação incompatível com o estado atual |
| 405 / 415 | Método HTTP ou tipo de conteúdo não suportado |
| 500 | Falha inesperada, sem detalhes internos na resposta |

Não repita mutações automaticamente após erro. Consulte o estado atual antes
de tentar novamente.

Veja [como executar](configuracao-local.md) e o
[roteiro manual do front-end](../../frontend/README.md).
