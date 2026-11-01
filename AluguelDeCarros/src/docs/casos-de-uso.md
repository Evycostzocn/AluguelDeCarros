# Casos de Uso — Sistema de Aluguel de Carros

## Atores

- Visitante.
- Usuário autenticado.
- Cliente, especialização de usuário autenticado.
- Agente, especialização de usuário autenticado.
- Agente de empresa, especialização de agente.
- Agente de banco, especialização de agente.

## Casos de uso

| ID | Caso de uso | Ator | História relacionada |
|---|---|---|---|
| UC01 | Cadastrar cliente | Visitante | HU01 |
| UC02 | Realizar login | Cliente e agente | HU02 |
| UC03 | Encerrar sessão | Usuário autenticado | HU02 |
| UC04 | Consultar cadastro | Cliente | HU03 |
| UC05 | Atualizar cadastro | Cliente | HU03 |
| UC06 | Desativar conta | Cliente | HU03 |
| UC07 | Gerenciar vínculos de renda | Cliente | HU04 |
| UC08 | Criar pedido de aluguel | Cliente | HU05 |
| UC09 | Consultar meus pedidos | Cliente | HU06 |
| UC10 | Alterar meu pedido | Cliente | HU07 |
| UC11 | Cancelar meu pedido | Cliente | HU08 |
| UC12 | Consultar pedidos e detalhes | Agente | HU09 |
| UC13 | Alterar pedido em análise | Agente | HU09 |
| UC14 | Avaliar pedido financeiramente | Agente | HU10 |
| UC15 | Registrar contrato de aluguel | Agente | HU11 |
| UC16 | Associar contrato de crédito | Agente de banco | HU12 |
| UC17 | Consultar automóveis | Usuário autenticado | HU13 |
| UC18 | Consultar contrato e crédito | Cliente dono e agente | HU14 |

## Condições de acesso

- O cadastro de cliente e o login são acessíveis sem autenticação.
- Os demais casos de uso exigem conta ativa, autenticação e autorização.
- Cadastro e login também exigem token CSRF; obter esse token é uma operação
  técnica pública, não um caso de uso de negócio.
- Empresa e banco são perfis de Agente (enum TipoAgente), não classes Java distintas.
- O cliente acessa somente seus próprios dados e pedidos.
- As operações sobre pedidos devem respeitar seu status.
- Registrar contrato exige um pedido aprovado.
- Associar crédito exige contrato existente sem crédito e agente BANCO autenticado.
- Agentes podem consultar todos os pedidos e contratos; clientes somente os próprios.
- Cancelamento cabe ao cliente dono em EM_ANALISE ou APROVADO; após contrato não é permitido.

O login é uma condição de acesso aos casos protegidos. Ele não é
representado como uma relação include, pois não precisa ser executado
novamente a cada operação.

## Diagrama de casos de uso

O bloco abaixo utiliza PlantUML.

~~~plantuml
@startuml
left to right direction

actor Visitante
actor "Usuário autenticado" as Usuario
actor Cliente
actor Agente
actor "Agente de empresa" as Empresa
actor "Agente de banco" as Banco

Usuario <|-- Cliente
Usuario <|-- Agente
Agente <|-- Empresa
Agente <|-- Banco

rectangle "Sistema de Aluguel de Carros" {
    usecase "Cadastrar cliente" as UC01
    usecase "Realizar login" as UC02
    usecase "Encerrar sessão" as UC03

    usecase "Consultar cadastro" as UC04
    usecase "Atualizar cadastro" as UC05
    usecase "Desativar conta" as UC06
    usecase "Gerenciar vínculos de renda" as UC07

    usecase "Criar pedido de aluguel" as UC08
    usecase "Consultar meus pedidos" as UC09
    usecase "Alterar meu pedido" as UC10
    usecase "Cancelar meu pedido" as UC11

    usecase "Consultar pedidos e detalhes" as UC12
    usecase "Alterar pedido em análise" as UC13
    usecase "Avaliar pedido financeiramente" as UC14
    usecase "Registrar contrato de aluguel" as UC15
    usecase "Associar contrato de crédito" as UC16
    usecase "Consultar automóveis" as UC17
    usecase "Consultar contrato e crédito" as UC18
}

Visitante --> UC01
Cliente --> UC02
Agente --> UC02
Usuario --> UC03

Cliente --> UC04
Cliente --> UC05
Cliente --> UC06
Cliente --> UC07
Cliente --> UC08
Cliente --> UC09
Cliente --> UC10
Cliente --> UC11

Agente --> UC12
Agente --> UC13
Agente --> UC14
Agente --> UC15

Banco --> UC16
Usuario --> UC17
Cliente --> UC18
Agente --> UC18

@enduml
~~~

## Correspondência com a implementação

- UC01/UC04–UC07: `ClienteController` e `ClienteService`.
- UC02: `AuthController`; UC03: filtro de logout configurado em `SecurityConfig`.
- UC08–UC18 (exceto autenticação/cadastro): `AluguelController` e `AluguelService`.
- UC06 desativa a conta e invalida a sessão; não apaga o cliente.
- Avaliação é retornada no detalhe do pedido; contrato inclui seu crédito opcional.
- Consultar usuário autenticado e obter CSRF são endpoints de suporte à interface.

Consulte [api.md](api.md) para as rotas e [historico-modelagem.md](historico-modelagem.md)
para as versões anteriores.
