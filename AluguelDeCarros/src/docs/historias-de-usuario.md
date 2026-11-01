# Histórias de Usuário — Sistema de Aluguel de Carros

## Objetivo

Desenvolver um sistema web que permita aos clientes gerenciar pedidos
de aluguel de automóveis e aos agentes analisar esses pedidos e
registrar os contratos correspondentes.

## Atores

- Cliente: pessoa que solicita o aluguel de um automóvel.
- Agente de empresa: usuário que representa uma empresa participante.
- Agente de banco: usuário que representa um banco participante.

## Histórias de usuário

### HU01 — Cadastro de cliente

Como visitante, quero me cadastrar como cliente para acessar o sistema
e solicitar o aluguel de um automóvel.

Critérios de aceitação:

- O cadastro deve conter usuário, senha, RG, CPF, nome, endereço e profissão.
- Não deve ser permitido cadastrar usuário ou CPF já existente.
- A senha deve ser armazenada de forma protegida, utilizando hash.

### HU02 — Autenticação

Como usuário cadastrado, quero entrar no sistema com usuário e senha
para acessar as funcionalidades permitidas ao meu perfil.

Critérios de aceitação:

- Clientes e agentes devem conseguir realizar login.
- Credenciais inválidas devem impedir o acesso.
- As funcionalidades do sistema devem exigir autenticação.
- O usuário deve conseguir encerrar sua sessão.

### HU03 — Gerenciamento do cadastro do cliente

Como cliente, quero consultar, atualizar e excluir meu cadastro
para manter o controle sobre meus dados.

Critérios de aceitação:

- O cliente deve acessar somente seu próprio cadastro.
- As alterações devem respeitar as validações do cadastro.
- A exclusão deve desativar o acesso e preservar o histórico de pedidos
  e contratos.

### HU04 — Informações de renda

Como cliente, quero informar minhas entidades empregadoras e os
respectivos rendimentos para permitir a análise financeira do pedido.

Critérios de aceitação:

- Cada vínculo deve conter o nome da entidade empregadora e o rendimento.
- Cada cliente pode possuir no máximo três vínculos cadastrados.
- O rendimento informado deve ser maior que zero.
- O cliente deve conseguir consultar, alterar e remover seus vínculos.

### HU05 — Criação de pedido

Como cliente, quero criar um pedido de aluguel para solicitar
um automóvel.

Critérios de aceitação:

- O pedido deve estar associado ao cliente autenticado e a um automóvel.
- O pedido deve registrar o período pretendido para o aluguel.
- A data final deve ser posterior à data inicial.
- O pedido deve iniciar com o status EM_ANALISE.

### HU06 — Consulta de pedidos

Como cliente, quero consultar meus pedidos para acompanhar
os dados e o status de cada solicitação.

Critérios de aceitação:

- O cliente deve visualizar somente seus próprios pedidos.
- A consulta deve apresentar o automóvel, o período e o status do pedido.
- Quando houver avaliação, o cliente deve conseguir consultar o parecer.

### HU07 — Alteração de pedido pelo cliente

Como cliente, quero modificar um pedido para corrigir
as informações da solicitação.

Critérios de aceitação:

- O cliente deve alterar somente seus próprios pedidos.
- A alteração deve ser permitida somente enquanto o pedido estiver EM_ANALISE.
- O sistema deve validar novamente as informações alteradas.

### HU08 — Cancelamento de pedido

Como cliente, quero cancelar um pedido quando não desejar
continuar com a solicitação.

Critérios de aceitação:

- O cliente deve cancelar somente seus próprios pedidos.
- Pedidos EM_ANALISE ou APROVADOS podem ser cancelados.
- Pedidos com contrato já registrado não podem ser cancelados por esse fluxo.
- O cancelamento deve preservar o registro e alterar o status para CANCELADO.

### HU09 — Consulta e alteração por agentes

Como agente, quero consultar e modificar pedidos para realizar
o atendimento e a análise das solicitações.

Critérios de aceitação:

- O agente deve conseguir consultar os pedidos disponíveis para análise.
- O agente deve conseguir modificar os dados da solicitação enquanto
  o pedido estiver EM_ANALISE.
- O agente não pode modificar o cliente responsável pelo pedido.
- O sistema deve validar novamente as informações alteradas.

### HU10 — Avaliação financeira

Como agente, quero registrar uma avaliação financeira para aprovar
ou reprovar um pedido de aluguel.

Critérios de aceitação:

- A avaliação deve identificar o pedido, o agente, a data e o parecer.
- O parecer deve indicar aprovação ou reprovação.
- Somente pedidos EM_ANALISE podem receber uma avaliação.
- A avaliação deve atualizar o status para APROVADO ou REPROVADO.

### HU11 — Registro do contrato de aluguel

Como agente, quero registrar o contrato de um pedido aprovado
para formalizar o aluguel do automóvel.

Critérios de aceitação:

- Somente pedidos APROVADOS podem originar um contrato.
- Cada pedido pode possuir no máximo um contrato de aluguel.
- O contrato deve registrar a data de celebração e o valor do aluguel.
- O registro do contrato deve alterar o status do pedido para CONTRATADO.
- A propriedade do automóvel deve ser identificada conforme o contrato,
  podendo pertencer a um cliente, uma empresa ou um banco.

### HU12 — Associação de contrato de crédito

Como agente de banco, quero associar um contrato de crédito ao contrato
de aluguel para registrar o financiamento concedido.

Critérios de aceitação:

- O contrato de crédito deve identificar o banco concedente.
- O contrato de crédito deve registrar o valor e a quantidade de parcelas.
- O valor e a quantidade de parcelas devem ser maiores que zero.
- Um contrato de aluguel pode possuir no máximo um contrato de crédito.
- A associação de crédito é opcional.

## Regras e decisões iniciais

As decisões abaixo complementam pontos que o enunciado não detalha.
Elas devem ser validadas com o professor e atualizadas quando necessário.

- O cadastro público será destinado a clientes.
- As contas de agentes serão cadastradas previamente, sem permitir
  que um visitante escolha esse perfil no cadastro público.
- A exclusão de cliente será lógica, desativando sua conta.
- O cliente poderá alterar pedidos somente enquanto estiverem EM_ANALISE.
- O cliente poderá cancelar pedidos EM_ANALISE ou APROVADOS.
- Cada pedido terá uma avaliação financeira conclusiva nesta versão.
- O período de aluguel será informado no pedido.
- Os automóveis serão previamente cadastrados para uso no protótipo;
  uma interface de gestão da frota não faz parte desta versão.
- A matrícula será tratada como identificador de registro do automóvel,
  distinto da placa.
- Cada automóvel terá um único proprietário por vez: um cliente ou um agente.
- A propriedade não será transferida automaticamente apenas pela aprovação
  do pedido; seu registro deverá seguir o contrato.

## Fluxo inicial dos pedidos

- Criação: EM_ANALISE.
- Avaliação positiva: APROVADO.
- Avaliação negativa: REPROVADO.
- Cancelamento permitido: CANCELADO.
- Registro de contrato: CONTRATADO.