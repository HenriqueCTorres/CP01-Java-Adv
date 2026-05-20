# 🔍 Bug Hunt — Library Management API

> **Modalidade:** Individual ou dupla  
> **Tempo sugerido:** 90 minutos  
> **Nível:** Intermediário

---

## Contexto

Você recebeu uma API REST de gerenciamento de biblioteca em Spring Boot. Ela tem testes automatizados escritos e funcionando — ou melhor, *deveriam* estar funcionando.

O desenvolvedor anterior deixou **6 erros escondidos** no código de produção antes de sair. Sua missão é encontrar e corrigir todos eles.

---

## Regras

1. **Não altere nenhum arquivo em `src/test/`**. Os testes são a verdade.
2. Modifique livremente qualquer arquivo em `src/main/`.
3. Meta: `BUILD SUCCESS` com todos os testes passando.
4. Entregue um relatório (modelo no final).

---

## Como começar

```bash
# Rodar todos os testes
mvn test

# Focar em uma classe específica
mvn test -Dtest="NomeDaClasseTest"
```

---

## Dicas gerais

- Leia a mensagem de falha com calma antes de abrir qualquer arquivo. Ela já diz muita coisa.
- Os erros estão espalhados entre as camadas **controller**, **service** e **repository**. Não estão todos no mesmo lugar.
- Um dos erros é sobre o que a API *devolve* para quem a consome — não sobre lógica interna.
- Nem todo erro explode com uma exceção barulhenta. Alguns simplesmente retornam um resultado silenciosamente errado.
- Quando um teste de repositório falha, vale a pena olhar com atenção o que a query está de fato fazendo.
- Quando um teste de serviço falha por uma exceção *inesperada* (ou pela *ausência* de uma esperada), pense em qual responsabilidade essa camada deveria ter.

---

## Relatório de entrega

Para cada bug encontrado:

```
Bug #N
Arquivo e camada:
O que estava errado:
Como você percebeu (mensagem do teste):
O que você corrigiu:
```

---

*Um bom desenvolvedor não é o que nunca erra — é o que sabe ler um teste com atenção.*
