# Configuração local do back-end

## Pré-requisitos

- Fedora com Bash e JDK 25 (`JAVA_HOME` e `PATH` configurados para esse JDK).
- Acesso à internet para baixar dependências com o Maven Wrapper do projeto.
- Projeto Supabase ativo, credenciais PostgreSQL e acesso de rede ao Session
  pooler na porta 5432, banco `postgres`.

O projeto usa Spring Boot **4.0.9**, versão estável da linha 4.0.x
([documentação oficial](https://docs.spring.io/spring-boot/4.0/)).
Não é necessário instalar Maven globalmente.

## Configurar o ambiente no Bash

A partir da raiz do repositório, entre na subpasta que contém `pom.xml` e `mvnw`:

```bash
cd AluguelDeCarros
java -version
bash ./mvnw --version

export DB_URL='jdbc:postgresql://<HOST_DO_SESSION_POOLER>:5432/postgres?sslmode=require'
export DB_USERNAME='postgres.<REFERENCIA_DO_PROJETO>'
read -r -s -p 'Senha do PostgreSQL: ' DB_PASSWORD
printf '\n'
export DB_PASSWORD
```

Substitua os placeholders pelos dados da opção **Session pooler** no painel de
conexão do Supabase. Use a senha do banco, não uma chave de API. Não coloque
credenciais na URL JDBC. A leitura silenciosa evita escrever a senha no comando
e no histórico do Bash. Não use `set -x` nem imprima as variáveis.

`DB_URL`, `DB_USERNAME` e `DB_PASSWORD` precisam estar disponíveis no **processo
que inicia a aplicação**. O `export` vale para o terminal atual e seus processos
filhos. Para executar pela IDE, configure o ambiente da execução nela ou abra a
IDE a partir desse terminal. Uma IDE já aberta não recebe automaticamente as
variáveis exportadas em outro terminal.

O Spring Boot **não carrega `.env` automaticamente**. Este roteiro usa variáveis
exportadas no shell. Arquivos `.env` locais estão ignorados pelo Git; nunca
versione credenciais ou inclua segredos em capturas e relatórios.

## Executar e verificar

No mesmo terminal, com as variáveis exportadas:

```bash
bash ./mvnw clean verify
bash ./mvnw spring-boot:run
```

`verify` compila, executa os testes e empacota a aplicação. Os testes de contexto
e integração usam PostgreSQL 17 descartável via Testcontainers e exigem Docker
acessível, sem usar DB_URL/DB_USERNAME/DB_PASSWORD do Supabase. Não desative os
testes nem configure um dialect manual para esconder falhas de conexão.

Nos logs, procure evidência de uma conexão PostgreSQL aberta pelo Hikari:

```text
HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@...
HikariPool-1 - Start completed.
```

`Added connection` identifica uma conexão efetivamente aberta pelo driver.
Confira também a inicialização do `EntityManagerFactory` e a ausência de erros
posteriores. Apenas o servidor web iniciar ou aparecer `Started
AluguelDeCarrosApplication` não comprova conexão com o banco. Revise os logs
antes de compartilhá-los, pois podem conter dados de conexão.

Encerre a aplicação iniciada nesse terminal com `Ctrl+C`. Ao terminar:

```bash
unset DB_URL DB_USERNAME DB_PASSWORD
```

## Configuração e diagnóstico

- A URL JDBC deve incluir `sslmode=require`. O datasource também define esse
  modo SSL; não use parâmetros SSL conflitantes na URL.
- O Hikari permite no máximo cinco conexões por instância da aplicação.
- `spring.jpa.hibernate.ddl-auto=validate` valida as entidades contra o schema
  criado pelo Flyway; Hibernate não cria nem atualiza tabelas.
- `spring.jpa.open-in-view=false` desativa Open EntityManager in View.
- O Hibernate detecta o dialect pela conexão, sem configuração manual.
- Variáveis ausentes: exporte as três no processo correto.
- Autenticação recusada: confira o usuário completo do pooler e a senha do banco.
- Timeout ou DNS: confira host, porta, rede, restrições de acesso e estado do
  Supabase. A conexão continua pendente até haver evidência de conexão aberta.
- Falha de download Maven pode impedir as verificações antes de tentar conectar
  ao banco; confira o acesso ao Maven Central.

## Entidades e migrações

A primeira parte do domínio está em `com.example.AluguelDeCarros.model`:
`Usuario` abstrato, `Cliente` e `VinculoRenda`. A herança é JOINED: `usuarios`
centraliza ID identity, login único, hash e conta ativa; `clientes.id` é PK e FK
para `usuarios.id`. CPF é String com exatamente 11 dígitos, sem pontuação;
a anotação de formato da entidade não verifica dígitos verificadores, mas a
camada de serviço da API normaliza e valida os dígitos. Os campos obrigatórios
possuem Bean Validation e restrições SQL. O serviço de cadastro produz BCrypt;
o construtor recebe somente o hash. Seu getter é restrito por `@JsonIgnore` na
serialização. Entradas e saídas HTTP usam exclusivamente DTOs.

`vinculos_renda` tem ID identity, FK obrigatória para clientes, empregador e
rendimento `NUMERIC(15,2)` positivo (13 dígitos inteiros, duas casas decimais).
O domínio usa BigDecimal e Bean Validation. As validações de campos são
executadas pelo Jakarta Validation no ciclo de persistência, não automaticamente
pela chamada dos construtores. Um vínculo recém-criado deve ser adicionado a um
cliente antes de persistir.

`Cliente.adicionarVinculoRenda` verifica proprietário e limite antes de alterar
os dois lados. Adicionar novamente a mesma instância é idempotente; adicionar
um quarto lança exceção sem alterar a coleção. A coleção é inicializada,
somente leitura para consumidores e não possui setter. Os métodos internos de
associação são restritos ao pacote model. As duas associações são LAZY; a
coleção propaga PERSIST/MERGE e usa orphanRemoval. Remover um vínculo sincroniza
os lados e exclui esse vínculo no flush. Não reutilize um vínculo persistido
removido em outro cliente: crie outro, respeitando esse ciclo de vida.
Não há equals/hashCode customizados ou comparação por IDs ainda não gerados.

O limite de três é uma regra do domínio, complementada por `@Size(max=3)`;
**não protege sozinho contra SQL direto ou transações concorrentes**. O serviço
agora bloqueia o cliente com PESSIMISTIC_WRITE nas alterações de renda; não
existe trigger de contagem para alterações externas ao back-end.
`desativar()` muda `ativo` para false. Não há operação de exclusão física de
clientes; JPA por si só não impede que código futuro execute uma remoção indevida.

Não havia ferramenta de migração. Foram adicionados `spring-boot-starter-flyway`
e `flyway-database-postgresql`, gerenciados pelo Spring Boot. Na inicialização
(inclusive do teste de contexto), Flyway aplica
`db/migration/V1__cria_usuarios_clientes_vinculos_renda.sql`, antes da validação
Hibernate. A migração cria as três tabelas, PKs, FKs, unicidade de login e CPF,
checks de campos e rendimento e índice na FK de vínculos. Apenas `public` é
configurado; schemas internos como `auth` não são alterados.

`clean-disabled=true`, `baseline-on-migrate=false` e `create-schemas=false`
evita limpeza ou baseline automáticos. Não execute Flyway clean, não use
`ddl-auto=update`/`create-drop` e não edite V1 depois de aplicada; mudanças futuras
exigem V2, V3 etc. O comando **Maven** `clean` abaixo limpa apenas os artefatos
locais de build (`target`), não o banco.

Se `public` já tiver objetos sem histórico Flyway, a migração pode ser recusada.
Pare e investigue os objetos e o histórico, sem apagar dados nem habilitar
baseline para contornar o conflito. Não se usa `IF NOT EXISTS` para ocultar
incompatibilidades. A execução inicial cria estrutura persistente no banco.

### Validação local

Sem Docker, é possível executar explicitamente apenas os testes unitários:

```bash
bash ./mvnw -Dtest=ClienteTest,ValidacaoEntidadesTest test
```

Esse comando seleciona explicitamente os testes unitários e não substitui a
verificação completa. Com Docker disponível, `clean verify` não requer Supabase.
Para iniciar a aplicação real, exporte as três variáveis no mesmo terminal:

```bash
bash ./mvnw clean verify
bash ./mvnw spring-boot:run
```

Verifique sucesso das migrações V1 e V2 no log do Flyway, inicialização do
EntityManagerFactory sem erros de schema e o resultado dos testes. No SQL Editor
do Supabase, uma conferência somente leitura do histórico é:

```sql
SELECT version, description, success
FROM public.flyway_schema_history
ORDER BY installed_rank;
```

## Back-end completo e dados de demonstração

Consulte [api.md](api.md) para endpoints, payloads, sessão, CSRF e decisões de negócio.
A migração incremental `V2__fluxo_aluguel.sql` cria agentes, automóveis, pedidos,
avaliações e contratos. V1 foi preservada. `ddl-auto=validate`, Flyway clean
desabilitado e baseline automático desabilitado permanecem configurados.

Para testes completos (PostgreSQL descartável, incluindo V1 e V2):

```bash
docker info
bash ./mvnw clean verify
```

O teste de contexto original também usa esse banco, sem enfraquecer a validação
Hibernate. Não há H2 nem fallback silencioso. Testcontainers gerencia os seus
próprios containers e os remove ao fim; bancos externos não são apagados.
O SSL é desabilitado somente no datasource de teste para o PostgreSQL local
sem TLS. Na aplicação Supabase, continua obrigatório.

Para carregar demonstração, use apenas um banco de desenvolvimento e o perfil
`dev`, com ativação adicional `app.dev.seed=true`. Informe senhas no terminal:

```bash
read -r -s -p 'Senha do agente empresa: ' DEV_EMPRESA_PASSWORD
printf '\n'
export DEV_EMPRESA_PASSWORD
read -r -s -p 'Senha do agente banco: ' DEV_BANCO_PASSWORD
printf '\n'
export DEV_BANCO_PASSWORD
bash ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--app.dev.seed=true
```

A carga cria `demo.empresa`, `demo.banco` e automóveis DEMO-001/DEMO-002. É
idempotente em execuções sequenciais: não duplica registros nem redefine senhas
existentes. Uma colisão com perfil incompatível é erro, sem sobrescrever dados.
Não execute a carga simultaneamente em múltiplas instâncias. No perfil padrão,
nenhuma carga é executada. Senhas ficam somente em hashes BCrypt no banco;
não são inseridas em migrações. Após a carga, retire as variáveis do ambiente:

```bash
unset DEV_EMPRESA_PASSWORD DEV_BANCO_PASSWORD
```

Os agentes de produção devem ser provisionados por um processo administrativo
controlado com BCrypt; não há endpoint público para isso. Automóveis devem ser
cadastrados previamente com exatamente um proprietário e IDs válidos.

`FRONTEND_ORIGINS` configura as origens CORS (padrão `http://localhost:5173`).
Para HTTPS, configure `SESSION_COOKIE_SECURE=true`. O padrão HTTP local usa
cookie HttpOnly e SameSite=Lax. Configure o fuso do processo de maneira consistente
para datas de auditoria (por exemplo `TZ=America/Sao_Paulo`).

A execução de testes com Testcontainers não comprova aplicação das migrações
no Supabase. Para validar o banco real, execute `spring-boot:run` com suas variáveis
exportadas e confira Flyway e EntityManagerFactory. Não compartilhe credenciais
ou logs contendo segredos.

## Registro da verificação desta entrega

- Compilação Java 25 executada com sucesso antes das correções finais.
- V1 e V2 aplicadas em PostgreSQL 17 descartável, com validação Hibernate bem-sucedida.
- Última suíte executada: 18 testes, 16 passaram, dois falharam, nenhum ignorado.
- As falhas identificaram ID nulo na resposta de criação de vínculo e simulação
  incompleta de cookie válido no teste de troca do ID de sessão. Ambas foram
  corrigidas no código, mas a reexecução foi recusada na solicitação de permissão
  para Maven/Docker fora do sandbox. A correção dos dois casos ainda não foi
  confirmada por nova execução.
- Execute `bash ./mvnw clean verify` para validar o estado final antes de integrar.
- Supabase não foi acessado nesta entrega: as variáveis não estavam disponíveis.
