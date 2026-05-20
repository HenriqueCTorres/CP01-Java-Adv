# Relatório de Bugs — Library Management API

> **Total de bugs encontrados e corrigidos:** 6

> **Membros:**
> - Henrique Cunha Torres, RM: 565119
> - Felipe Bezerra Beatriz, RM: 564723
> - Max Hayashi Batista, RM: 563717
---

## Bug #1

**Arquivo e camada:** `src/main/java/com/example/library/service/BookService.java` — Service

**O que estava errado:**
O método `create()` não verificava se o ISBN já existia no banco antes de tentar salvar o livro. O teste `create_duplicateIsbn` esperava que uma `BusinessException` contendo a palavra "ISBN" fosse lançada, e que `bookRepository.save()` nunca fosse chamado. Sem a verificação, o método ia direto ao `save()`, ou tentava salvar e deixava a constraint de unicidade do banco explodir com uma exception inesperada.

**Como foi percebido (mensagem do teste):**
```
create - should throw on duplicate ISBN
Expected BusinessException to be thrown, but nothing was thrown.
verify(bookRepository, never()).save(any()) → FAIL
```

**O que foi corrigido:**
Adicionada a verificação no início de `create()`:
```java
if (bookRepository.existsByIsbn(request.getIsbn())) {
    throw new BusinessException("ISBN '" + request.getIsbn() + "' is already in use");
}
```

---

## Bug #2

**Arquivo e camada:** `src/main/java/com/example/library/service/BookService.java` — Service

**O que estava errado:**
No método `getGenreStats()`, os índices do array de resultado da query estavam invertidos. A query JPQL retorna `(genre, COUNT, AVG)` — ou seja, `row[0]` = genre (String), `row[1]` = count (Long). O código mapeava `row[1]` como genre e `row[0]` como count, causando um `ClassCastException` ou resultado errado silencioso.

**Como foi percebido (mensagem do teste):**
```
getGenreStats - should return stats
assertThat(stats.get(0).getGenre()).isEqualTo("Fantasy") → FAIL (recebia "5" ou ClassCastException)
assertThat(stats.get(0).getCount()).isEqualTo(5L) → FAIL
```

**O que foi corrigido:**
Invertidos os índices de volta à ordem correta:
```java
.genre((String) row[0])
.count((Long) row[1])
```

---

## Bug #3

**Arquivo e camada:** `src/main/java/com/example/library/repository/BookRepository.java` — Repository

**O que estava errado:**
A query `findByPriceRange` usava `BETWEEN :max AND :min`, com os parâmetros na ordem errada. Como `BETWEEN` exige que o primeiro valor seja o menor, a query retornava zero resultados para qualquer intervalo de preço válido.

**Como foi percebido (mensagem do teste):**
```
Should find books in price range
assertThat(page.getContent()).hasSize(1) → FAIL (hasSize(0))
```

**O que foi corrigido:**
Corrigida a ordem da cláusula BETWEEN:
```java
@Query("SELECT b FROM Book b WHERE b.price BETWEEN :min AND :max")
```

---

## Bug #4

**Arquivo e camada:** `src/main/java/com/example/library/service/AuthorService.java` — Service

**O que estava errado:**
O método `create()` tinha a anotação `@CachePut(key = "#id")`, mas o método não possui nenhum parâmetro chamado `id`. O Spring Expression Language (SpEL) não consegue resolver `#id` e lança uma `SpelEvaluationException` em tempo de execução ao tentar gravar no cache.

**Como foi percebido (mensagem do teste):**
```
create - Should create author successfully
org.springframework.expression.spel.SpelEvaluationException: 
  EL1008E: Property or field 'id' cannot be found...
```

**O que foi corrigido:**
Alterada a key para usar o ID do objeto retornado pelo método:
```java
@CachePut(value = CacheConfig.AUTHORS_CACHE, key = "#result.id")
```

---

## Bug #5

**Arquivo e camada:** `src/main/java/com/example/library/service/AuthorService.java` — Service

**O que estava errado:**
O método `delete()` não verificava se o autor possuía livros antes de deletá-lo. O teste `shouldThrowWhenHasBooks` esperava que uma `BusinessException` com a mensagem `"Cannot delete author"` fosse lançada quando o autor tivesse livros associados, e que `authorRepository.delete()` nunca fosse chamado. Sem essa verificação, o autor era deletado independentemente (ou a cascade levaria os livros junto, violando regras de negócio).

**Como foi percebido (mensagem do teste):**
```
delete - Should throw when author has books
Expected BusinessException to be thrown, but nothing was thrown.
verify(authorRepository, never()).delete(any()) → FAIL
```

**O que foi corrigido:**
Adicionada a verificação antes de deletar:
```java
if (!author.getBooks().isEmpty()) {
    throw new BusinessException("Cannot delete author with existing books. Remove the books first.");
}
```

---

## Bug #6

**Arquivo e camada:** `src/main/java/com/example/library/controller/AuthorController.java` — Controller

**O que estava errado:**
O endpoint `POST /api/v1/authors` retornava `ResponseEntity.ok(model)` (HTTP 200 OK) ao criar um novo autor. O teste `create_valid` esperava o status HTTP **201 Created**, que é o código semântico correto para criação de recursos REST.

**Como foi percebido (mensagem do teste):**
```
POST /api/v1/authors - 201 when valid + authenticated
Expected: 201
Actual: 200
```

**O que foi corrigido:**
Alterado para retornar 201 com o header `Location` apontando para o novo recurso:
```java
return ResponseEntity.created(URI.create("/api/v1/authors/" + author.getId()))
        .body(model);
```

---

## Resumo

| # | Camada     | Arquivo                  | Tipo do Bug                          |
|---|------------|--------------------------|--------------------------------------|
| 1 | Service    | `BookService.java`       | Validação ausente (ISBN duplicado)   |
| 2 | Service    | `BookService.java`       | Índices invertidos no mapeamento     |
| 3 | Repository | `BookRepository.java`    | Parâmetros invertidos no BETWEEN     |
| 4 | Service    | `AuthorService.java`     | SpEL inválido na anotação @CachePut  |
| 5 | Service    | `AuthorService.java`     | Validação de negócio ausente         |
| 6 | Controller | `AuthorController.java`  | HTTP status code errado (200 vs 201) |
