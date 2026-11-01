# Executar localmente

Requisitos: **JDK 25**, **Node.js 22.12+** (recomendado 24), npm e um projeto
Supabase. O Maven é fornecido pelo Wrapper do projeto.

## 1. Back-end

No Bash, a partir da raiz do repositório:

```bash
cd AluguelDeCarros
export DB_URL='jdbc:postgresql://<HOST_DO_SESSION_POOLER>:5432/postgres?sslmode=require'
export DB_USERNAME='postgres.<REFERENCIA_DO_PROJETO>'
read -r -s -p 'Senha do PostgreSQL: ' DB_PASSWORD
printf '\n'
export DB_PASSWORD
bash ./mvnw spring-boot:run
```

Use os dados de **Session pooler** do Supabase: porta `5432`, banco `postgres`
e SSL obrigatório. Não coloque a senha na URL nem em arquivos versionados.
As variáveis devem estar no mesmo terminal que inicia a aplicação; pela IDE,
configure-as no ambiente da execução. Spring Boot não carrega `.env` automaticamente.

O back-end atende em `http://localhost:8080`. Flyway aplica V1/V2 e Hibernate
valida as tabelas. Confira nos logs a conexão `Added connection ...PgConnection`,
o sucesso das migrações e a inicialização do `EntityManagerFactory`. Apenas o
servidor web iniciar não comprova conexão com o banco.

Se houver conflito com tabelas existentes, investigue sem apagar dados ou
habilitar baseline automático.

## 2. Front-end

Em outro terminal, a partir da raiz do repositório:

```bash
cd AluguelDeCarros/frontend
npm ci
npm run dev
```

Abra **http://localhost:5173**. O Vite encaminha `/api` para o back-end.
Não é necessário configurar credenciais de banco no front-end.
Para encerrar os servidores, use `Ctrl+C` nos respectivos terminais.

## 3. Dados de demonstração (opcional)

Para criar os agentes `demo.empresa` e `demo.banco` e dois automóveis, use um
banco de desenvolvimento. Na pasta `AluguelDeCarros`, com as variáveis do banco
exportadas e o back-end normal encerrado:

```bash
read -r -s -p 'Senha do agente empresa: ' DEV_EMPRESA_PASSWORD
printf '\n'
export DEV_EMPRESA_PASSWORD
read -r -s -p 'Senha do agente banco: ' DEV_BANCO_PASSWORD
printf '\n'
export DEV_BANCO_PASSWORD
bash ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--app.dev.seed=true
```

Use senhas com pelo menos 8 caracteres e até 72 bytes. A carga não roda no perfil
padrão e, em execuções sequenciais, não duplica registros nem redefine senhas.
Não execute a carga simultaneamente em várias instâncias.

## 4. Comandos de verificação

Na pasta `AluguelDeCarros`, com Docker disponível:

```bash
bash ./mvnw clean verify
```

Os testes usam PostgreSQL descartável via Testcontainers, não o Supabase pessoal.
Na pasta `AluguelDeCarros/frontend`, para compilar a interface:

```bash
npm run build
```

Esses comandos são instruções; os resultados e pendências registrados estão no
[README principal](../../../README.md#testes).

Mais detalhes: [contrato da API](api.md) e
[roteiro manual do front-end](../../frontend/README.md).
