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

`verify` compila, executa os testes e empacota a aplicação. O teste existente
`contextLoads`, com `@SpringBootTest`, carrega também a persistência e precisa
das credenciais e do banco acessível. Não desative o teste nem configure um
dialect manual para esconder falhas de conexão.

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
a validação de formato **não verifica os dígitos verificadores**. Os campos
obrigatórios possuem Bean Validation e restrições SQL. Não há hashing nesta
etapa: o construtor recebe somente um hash já produzido pela futura autenticação.
O hash não tem getter e é ignorado pelo Jackson. Entidades não serão contratos
HTTP; a futura API deverá usar DTOs.

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
**não protege sozinho contra SQL direto ou transações concorrentes**. A futura
camada transacional precisará coordenar alterações concorrentes (por exemplo,
bloqueio do cliente); esta migração não implementa trigger de contagem.
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

### Validação desta etapa

Sem credenciais, execute somente os testes independentes do banco:

```bash
bash ./mvnw -Dtest=ClienteTest,ValidacaoEntidadesTest test
```

Esse comando seleciona explicitamente os testes unitários e não substitui a
verificação completa. Com as três variáveis exportadas no mesmo terminal:

```bash
bash ./mvnw clean verify
bash ./mvnw spring-boot:run
```

Verifique sucesso da migração V1 no log do Flyway, inicialização do
EntityManagerFactory sem erros de schema e o resultado dos testes. No SQL Editor
do Supabase, uma conferência somente leitura do histórico é:

```sql
SELECT version, description, success
FROM public.flyway_schema_history
ORDER BY installed_rank;
```

Na implementação desta etapa, as variáveis não estavam disponíveis no processo
do assistente. A migração e a compatibilidade com o Supabase ainda precisam ser
validadas com esses comandos, mesmo que a conexão da etapa anterior já tenha
sido validada. Não compartilhe credenciais ou logs contendo segredos.
