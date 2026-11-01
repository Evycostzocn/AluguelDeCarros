# Rota — Sistema de Aluguel de Carros

Aplicação acadêmica para solicitar aluguel de automóveis, analisar pedidos e
registrar contratos, com uma interface React e uma API Spring Boot integrada ao
PostgreSQL do Supabase. A interface utiliza a identificação visual textual “rota”.

README adaptado do [template do Prof. João Paulo Aramuni](https://github.com/joaopauloaramuni/desenvolvimento-e-integracao-de-aplicacoes-web/blob/main/TEMPLATES/template_README.md),
com as seções aplicáveis ao sistema realmente implementado.

## Índice

- [Links úteis](#links-úteis)
- [Sobre o projeto](#sobre-o-projeto)
- [Funcionalidades principais](#funcionalidades-principais)
- [Tecnologias utilizadas](#tecnologias-utilizadas)
- [Arquitetura](#arquitetura)
- [Instalação e execução](#instalação-e-execução)
- [Deploy](#deploy)
- [Estrutura de pastas](#estrutura-de-pastas)
- [Demonstração](#demonstração)
- [Testes](#testes)
- [Documentações utilizadas](#documentações-utilizadas)
- [Autores](#autores)
- [Contribuição](#contribuição)
- [Agradecimentos](#agradecimentos)
- [Licença](#licença)

## Links úteis

| Documento | Conteúdo |
|---|---|
| [Configuração local](AluguelDeCarros/src/docs/configuracao-local.md) | Ambiente, banco, migrações e dados de demonstração |
| [API](AluguelDeCarros/src/docs/api.md) | Rotas, payloads, respostas, sessão e CSRF |
| [Histórias de usuário](AluguelDeCarros/src/docs/historias-de-usuario.md) | Requisitos e critérios de aceitação |
| [Casos de uso](AluguelDeCarros/src/docs/casos-de-uso.md) | Atores, permissões e diagrama |
| [Diagrama de classes](AluguelDeCarros/src/docs/uml.puml) | Entidades, tipos e associações |
| [Diagrama de pacotes](AluguelDeCarros/src/docs/diagrama-de-pacotes.puml) | Organização real de back-end e front-end |
| [Histórico da modelagem](AluguelDeCarros/src/docs/historico-modelagem.md) | Evolução e consulta às versões no Git |
| [Front-end](AluguelDeCarros/frontend/README.md) | Integração e roteiro de validação manual |

Os arquivos `.puml` e o bloco PlantUML dos casos de uso podem ser abertos em um
visualizador PlantUML. O GitHub não renderiza esses diagramas automaticamente.
Não há URL de demonstração publicada informada.

## Sobre o projeto

O sistema organiza o fluxo entre clientes que solicitam automóveis e agentes
que analisam a viabilidade financeira e formalizam o aluguel. Foi desenvolvido
para o trabalho acadêmico de Desenvolvimento e Integração de Aplicações Web,
Laboratório 02 — Aluguel de Carros.

O objetivo é exercitar modelagem, persistência relacional, autenticação,
autorização e integração entre aplicação web e API. Dados de instituição,
turma, período e integrantes ainda devem ser preenchidos pelos autores.

## Funcionalidades principais

| Perfil | Funcionalidades implementadas |
|---|---|
| Visitante | Cadastro público de cliente e login |
| Cliente | Consultar/editar cadastro, desativar conta, gerenciar até três rendas, consultar automóveis, criar/consultar/alterar/cancelar pedidos próprios, consultar avaliação e contrato |
| Agente EMPRESA | Consultar pedidos, alterar solicitações em análise, aprovar/reprovar, registrar e consultar contratos |
| Agente BANCO | Mesmas operações de agente, além de conceder crédito ao contrato |
| Todos os autenticados | Consultar sessão e perfil, navegar conforme permissões e sair |

O cliente é desativado sem apagar histórico. Senhas são protegidas com BCrypt;
a autenticação usa sessão HTTP, cookie HttpOnly e CSRF. CPF é normalizado e tem
os dígitos verificadores validados. Unicidades e bloqueios transacionais protegem
operações concorrentes pela API.

O fluxo de status é controlado pelo servidor:

```text
EM_ANALISE -> APROVADO -> CONTRATADO
EM_ANALISE -> REPROVADO
EM_ANALISE -> CANCELADO
APROVADO   -> CANCELADO
```

Não estão implementados: recuperação/troca de senha, CRUD administrativo de
agentes ou frota, pagamentos, juros, reserva de disponibilidade, prevenção de
sobreposição de períodos, transferência de propriedade ou aplicativo mobile.
As listas não têm paginação. Todos os agentes ativos podem analisar todos os
pedidos; não existe carteira de atendimento por agente.

## Tecnologias utilizadas

| Camada | Tecnologias |
|---|---|
| Back-end | Java 25, Spring Boot 4.0.9, Spring MVC, Spring Security, Jakarta Validation |
| Persistência | Spring Data JPA/Hibernate, driver PostgreSQL, Flyway, HikariCP |
| Banco | PostgreSQL no Supabase, conexão JDBC pelo Session pooler com SSL |
| Front-end | JavaScript/JSX, React 19, React Router 7, Vite 7, CSS responsivo |
| Estado e integração | React Context/hooks e Fetch API; cookie de sessão e CSRF |
| Build | Maven Wrapper e npm com `package-lock.json` |
| Testes existentes | JUnit, MockMvc, Spring Security Test, Testcontainers com PostgreSQL 17 |
| Modelagem | PlantUML e Markdown, versionados com Git |

As versões Java/Spring estão no [pom.xml](AluguelDeCarros/pom.xml); as faixas npm
estão no [package.json](AluguelDeCarros/frontend/package.json), e as versões
resolvidas no lockfile do front-end. Não há H2, JWT ou autenticação Supabase Auth.

## Arquitetura

O back-end é uma aplicação única organizada em camadas no pacote
`com.example.AluguelDeCarros`. Controllers recebem DTOs validados, serviços
aplicam regras e transações e repositories acessam entidades JPA. `config`
configura segurança, CORS e a carga opcional de desenvolvimento. As entidades
não são serializadas como contratos HTTP.

O React usa os módulos `main.jsx` (rotas/layout), `auth.jsx` (sessão e proteção),
`pages.jsx` (telas), `ui.jsx` (componentes/hooks) e `api.js` (acesso HTTP).
O proxy do Vite encaminha `/api` ao Spring Boot em `localhost:8080`.

`Usuario` é abstrato e usa JOINED: `Cliente` e `Agente` compartilham a PK da
tabela `usuarios`, que centraliza a unicidade do login. Automóvel tem exatamente
um proprietário, cliente ou agente, com CHECK no banco. Valores monetários usam
BigDecimal/NUMERIC(15,2). Cada pedido tem no máximo uma avaliação e um contrato;
cada contrato pode ter um crédito.

Flyway gerencia V1 e V2 no schema `public`. Hibernate apenas valida o schema
(`ddl-auto=validate`). Não se habilita baseline automático nem Flyway clean.
Rendas bloqueiam o cliente; alterações de pedido bloqueiam o pedido; crédito
bloqueia o contrato. A limitação de três rendas não é garantida contra SQL externo.

## Instalação e execução

### Pré-requisitos

- JDK 25 e `JAVA_HOME`/`PATH` configurados.
- Node.js 22.12+ (recomendado Node 24) e npm.
- Bash; comandos documentados para Fedora/Linux.
- Projeto Supabase e credenciais PostgreSQL para executar a aplicação.
- Docker acessível somente para a suíte de integração com Testcontainers.
- Internet para a primeira resolução de dependências. Maven global não é necessário.

### Variáveis de ambiente e back-end

A partir da raiz do repositório:

```bash
cd AluguelDeCarros
export DB_URL='jdbc:postgresql://<HOST_DO_SESSION_POOLER>:5432/postgres?sslmode=require'
export DB_USERNAME='postgres.<REFERENCIA_DO_PROJETO>'
read -r -s -p 'Senha do PostgreSQL: ' DB_PASSWORD
printf '\n'
export DB_PASSWORD
bash ./mvnw spring-boot:run
```

Substitua os placeholders por dados do Session pooler. As variáveis precisam
estar disponíveis no processo que inicia o Spring. `.env` não é carregado
automaticamente. Não coloque senha na URL, no repositório ou no front-end.
O Wrapper resolve as dependências Maven ao executar a aplicação.

| Variável adicional | Uso |
|---|---|
| `FRONTEND_ORIGINS` | Origens CORS exatas, separadas por vírgula; padrão `http://localhost:5173` |
| `SESSION_COOKIE_SECURE` | `true` para execução por HTTPS; padrão local `false` |
| `DEV_EMPRESA_PASSWORD` / `DEV_BANCO_PASSWORD` | Senhas para carga opcional de agentes no perfil dev |

### Banco de dados

A inicialização aplica `V1__cria_usuarios_clientes_vinculos_renda.sql` e
`V2__fluxo_aluguel.sql` antes de validar as entidades. Uma base com objetos
preexistentes sem histórico Flyway pode causar conflito: investigue sem apagar
dados nem habilitar baseline para esconder a divergência. Schemas internos do
Supabase, como `auth`, não são modificados.

### Front-end

Em outro terminal, a partir da raiz do repositório:

```bash
cd AluguelDeCarros/frontend
npm ci
npm run dev
```

Acesse `http://localhost:5173`. Não é necessário `.env` do front-end: a API usa
caminhos relativos `/api`. A interface envia `credentials: 'include'`, obtém
CSRF e renova o token após login. Ao recarregar, consulta `/api/auth/me`.

Para gerar os arquivos estáticos:

```bash
npm run build
```

A saída fica em `frontend/dist/`. Veja os detalhes no
[guia local](AluguelDeCarros/src/docs/configuracao-local.md).

## Deploy

Não há implantação publicada ou pipeline de CI/CD confirmado nesta entrega.
Para publicar o front-end, o servidor precisa servir `dist/`, oferecer fallback
para `index.html` nas rotas React e encaminhar `/api` ao back-end. O proxy de
desenvolvimento do Vite não acompanha o bundle. Use HTTPS, cookie Secure e
origens CORS adequadas; a configuração atual pressupõe front-end/API no mesmo site.

## Estrutura de pastas

```text
README.md
AluguelDeCarros/
├── pom.xml
├── mvnw / mvnw.cmd / .mvn/
├── src/
│   ├── main/
│   │   ├── java/com/example/AluguelDeCarros/
│   │   │   ├── AluguelDeCarrosApplication.java
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/                 # V1 e V2
│   ├── test/java/com/example/AluguelDeCarros/
│   └── docs/                                # Requisitos e modelagem
└── frontend/
    ├── README.md
    ├── package.json / package-lock.json
    ├── index.html / vite.config.js
    └── src/
        ├── api.js
        ├── auth.jsx
        ├── main.jsx
        ├── pages.jsx
        ├── ui.jsx
        └── style.css
```

`target/`, `node_modules/`, `.vite/` e `dist/` são gerados localmente e não são
parte da modelagem. Comandos npm devem ser executados em `frontend/`.

## Demonstração

O roteiro manual está no [README do front-end](AluguelDeCarros/frontend/README.md).
Em resumo: cadastrar cliente, informar renda, solicitar automóvel, avaliar como
agente, registrar contrato e conceder crédito como banco.

Há carga opcional no perfil `dev`, ativada também por `app.dev.seed=true`, para
`demo.empresa`, `demo.banco` e dois automóveis. As senhas são fornecidas por
variáveis de ambiente; nenhuma senha padrão é publicada. A carga não executa no
perfil padrão. Consulte o guia local antes de usá-la em um banco de desenvolvimento.

**A preencher após validação:** capturas reais das telas e, se exigido, vídeo de
apresentação. Não há demonstração online ou evidências visuais anexadas.

## Testes

Os testes existentes cobrem domínio, formato/validação, ocultação de hash e
fluxos HTTP com PostgreSQL descartável, incluindo autorização e concorrência.
Para executá-los, a partir de `AluguelDeCarros/`:

```bash
bash ./mvnw clean verify
```

Sem Docker, somente os testes unitários podem ser selecionados explicitamente:

```bash
bash ./mvnw -Dtest=ClienteTest,ValidacaoEntidadesTest test
```

Isso não substitui a suíte completa. Não há testes automatizados do front-end.

| Evidência anterior nesta colaboração | Resultado registrado |
|---|---|
| Migrações V1/V2 em PostgreSQL 17 descartável | Aplicadas; Hibernate validou o schema |
| Última suíte de back-end executada pelo assistente | 18 testes: 16 passaram, 2 falharam, nenhum ignorado |
| Correções posteriores | ID de vínculo e simulação de cookie corrigidos; reexecução pendente |
| Build do front-end após reorganização | Passou, 44 módulos transformados |
| Supabase | Conexão anterior informada pela usuária; aplicação de V1/V2 não confirmada pelo assistente |
| Integração manual e carga dev | Validação ainda pendente nesta colaboração |

Esta revisão altera apenas documentação. Não foram executados testes, builds
ou comandos de inicialização. Não há percentual de cobertura informado.

## Documentações utilizadas

- [Template de README do enunciado](https://github.com/joaopauloaramuni/desenvolvimento-e-integracao-de-aplicacoes-web/blob/main/TEMPLATES/template_README.md).
- [Referência Spring Boot 4.0](https://docs.spring.io/spring-boot/4.0/).
- [Persistência de autenticação no Spring Security](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/persistence.html).
- Documentação local de requisitos, API e modelagem vinculada em Links úteis.
- Código-fonte, `pom.xml`, `package.json` e migrações como referência da implementação.

## Autores

**Preenchimento pendente pelos integrantes; nenhum nome foi presumido.**

| Informação | Preencher |
|---|---|
| Integrante(s) | Nome completo, matrícula e perfil GitHub de cada integrante |
| Instituição e curso | Nome oficial |
| Turma e período letivo | Turma / semestre / ano |
| Docente responsável | Confirmar nome; a autoria do template não identifica automaticamente o docente da turma |
| Entrega | Data e identificação exigidas pelo enunciado |

## Contribuição

Preserve as migrações aplicadas; mudanças de banco exigem novas versões.
Atualize requisitos e diagramas quando o comportamento mudar. Nunca versione
segredos ou artefatos gerados. Registre correções e resultados reais antes de
considerar uma entrega validada. As versões anteriores dos modelos permanecem
nos commits Git; consulte o [histórico documentado](AluguelDeCarros/src/docs/historico-modelagem.md).

## Agradecimentos

Crédito ao Prof. João Paulo Aramuni pela disponibilização do template de README.
Outros agradecimentos devem ser incluídos pelos integrantes, se pertinentes.

## Licença

Não foi identificada licença de distribuição definida para este projeto.
**A definir pelos autores**, conforme as orientações acadêmicas. Nenhuma licença
foi presumida a partir do template utilizado.
