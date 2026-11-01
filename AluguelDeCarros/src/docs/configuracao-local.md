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
bash ./mvnw verify
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
- `spring.jpa.hibernate.ddl-auto=none` impede a geração automática de tabelas
  pelo Hibernate nesta etapa.
- `spring.jpa.open-in-view=false` desativa Open EntityManager in View.
- O Hibernate detecta o dialect pela conexão, sem configuração manual.
- Variáveis ausentes: exporte as três no processo correto.
- Autenticação recusada: confira o usuário completo do pooler e a senha do banco.
- Timeout ou DNS: confira host, porta, rede, restrições de acesso e estado do
  Supabase. A conexão continua pendente até haver evidência de conexão aberta.
- Falha de download Maven pode impedir as verificações antes de tentar conectar
  ao banco; confira o acesso ao Maven Central.

Esta etapa não cria tabelas, entidades ou funcionalidades de negócio.
