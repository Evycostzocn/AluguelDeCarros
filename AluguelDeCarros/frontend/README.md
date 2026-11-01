# Rota — Front-end

Interface React + Vite em português para o back-end Spring Boot deste repositório.
Requer Node.js 22.12+ (recomendado Node 24) e npm. O front-end fica na pasta
`AluguelDeCarros/frontend`, dentro da pasta do projeto que contém o Maven Wrapper.

## Executar

Na raiz do repositório, em um terminal com as variáveis do banco já exportadas:

```bash
cd AluguelDeCarros
bash ./mvnw spring-boot:run
```

Em outro terminal, a partir da raiz do repositório:

```bash
cd AluguelDeCarros/frontend
npm ci
npm run dev
```

Abra `http://localhost:5173`. A porta é fixa: se estiver ocupada, encerre seu
servidor anterior ou libere a porta. O proxy do Vite encaminha `/api` para
`http://localhost:8080`. Não há credenciais PostgreSQL no front-end.

Para compilar:

```bash
npm run build
```

O resultado fica em `dist/`. `npm run preview` permite servir esse build em
`http://localhost:4173`, também com proxy local. Em produção, configure no servidor
HTTP o encaminhamento de `/api` ao Spring Boot e o fallback de rotas do React
para `index.html`. O proxy do Vite não é incluído nos arquivos estáticos de `dist`.
Use HTTPS e a configuração segura de cookie documentada no back-end.

## Integração

- `src/api.js`: único cliente HTTP, prefixo `/api`, `credentials: 'include'`,
  obtenção de CSRF e header informado pelo servidor. 403 limpa o token para
  renová-lo na próxima operação; não há repetição automática de mutações.
- `src/auth.jsx`: consulta `/auth/me` ao carregar, login, renovação do CSRF após
  login, logout e proteção de rotas. Sessão expirada remove os dados do usuário
  em memória. Falha de rede apresenta reconexão, sem simular logout/sucesso.
- `src/pages.jsx`: cadastro próprio, renda, automóveis, pedidos, avaliações,
  contratos e crédito, usando os DTOs reais.
- `src/main.jsx`: rotas e navegação por perfil. Só CLIENTE acessa o cadastro
  pessoal; somente EMPRESA/BANCO avaliam e contratam; só BANCO concede crédito.
- `src/ui.jsx` e `src/style.css`: formulários, feedback, estados vazios e layout
  responsivo. Ilustrações de carros são genéricas; os dados são sempre da API.

Nenhuma senha, token ou perfil é persistido em localStorage. A sessão usa o cookie
HttpOnly do back-end. Os controles de interface não substituem a autorização do
servidor. Campos como cliente, proprietário, status e banco concedente nunca são
enviados livremente nos formulários.

As listas e permissões seguem [o contrato da API](../src/docs/api.md).
Não há dados fictícios nem preenchimento automático de agentes/frota. Para
preparar dados reais de demonstração, use o perfil dev do back-end conforme
[a configuração local](../src/docs/configuracao-local.md).

## Roteiro de validação manual

1. Inicie o back-end com PostgreSQL configurado e migrações aplicadas. Inicie Vite.
2. Cadastre cliente com CPF válido e entre. Confira mensagens para senha errada,
   CPF inválido ou duplicado. Recarregue a página e confira a sessão restaurada.
3. Edite cadastro; adicione, edite e remova renda. Confira o limite de três vínculos.
4. Consulte automóveis, solicite período válido e abra o pedido criado. Altere-o
   em análise; experimente cancelar outro pedido.
5. Saia e entre como EMPRESA. Consulte o pedido, registre avaliação e contrato
   após aprovação. Confira que campos de alteração desaparecem após a análise.
6. Entre como BANCO, abra o contrato pelo pedido e registre crédito. Confira
   que o formulário desaparece quando o crédito já existe.
7. Volte ao cliente e consulte avaliação, contrato e crédito. Confira que outro
   cliente não consegue abrir os mesmos IDs.
8. Confira logout, expiração da sessão, bloqueio de rotas por perfil e desativação
   de uma conta de teste (ação persistente). Contas desativadas não podem entrar.
9. Interrompa o back-end e observe o erro de conexão sem dados ou sucesso falsos.
   Confira listas vazias e layout em janela estreita.

Nesta etapa só o build do front-end é executado. A validação manual ponta a ponta
precisa de back-end em execução; não foram criados testes automatizados.
