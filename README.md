# Case Tecnico Alura
Seja bem-vindo ao teste para desenvolvedor Java Júnior da Alura. Neste
desafio, simulamos uma parte do nosso domínio para que você possa demonstrar seus conhecimentos. 
Não há respostas certas ou erradas, nosso objetivo é avaliar como você aplica lógica e 
conceitos de orientação a objetos para solucionar problemas.

## Requisitos

- Utilizar java 18+
- Utilizar Spring boot
- Utilizar Spring data JPA
- Utilizar mysql
- utilizar criação de tabelas manuais ([flyway](https://www.baeldung.com/database-migrations-with-flyway))

## Orientações

1. Suba o templete incial do projeto no seu github e deixe o repositório público(Seus commits serão avaliados).
2. Abra o projeto na IDE de sua preferência.
3. requisitos estão em português, mas lembre-se de no código escrever tudo em inglês.
4. bônus não é obrigatório e não possui ordem, então você pode realizar apenas um dos que
   são citados lá, de acordo com sua preferência.

## Desafio

Já disponibilizamos um projeto base como ponto de partida, no qual as tecnologias exigidas já estão configuradas. 
Algumas lógicas relacionadas às entidades usuário e curso já estão implementadas, 
e podem ser utilizadas como orientação para a resolução das questões.

**Importante:** Não se preocupe com a parte visual, toda a interação devem ser feitas
por API.

### Questão 1 — Modelagem de Atividades

Na Alura, os cursos possuem **atividades interativas** que ajudam no processo de aprendizado.  
Elas podem ser de diferentes formatos, cada uma com suas regras específicas.

Você deve implementar a modelagem dessas atividades, de acordo com os requisitos abaixo.  
Os esboços dos endpoints já estão criados — sua tarefa será **implementar a lógica completa** para cada tipo de atividade.

##### Regras gerais
- O enunciado (`statement`) deve ter no mínimo 4 e no máximo 255 caracteres.
- O curso não pode ter duas questões com o mesmo enunciado
- A ordem deve ser um número inteiro positivo.
- Um curso só pode receber atividades se seu status for `BULDING`.

#### Tipos de atividade

##### 1.1 — Atividade de Resposta Aberta

**Endpoint:** `/task/new/opentext`
```bash
curl -w "%{http_code}\n" -X POST http://localhost:8080/task/new/opentext \
  -H "Content-Type: application/json" \
  -d '{
        "courseId": 42,
        "statement": "O que aprendemos na aula de hoje?",
        "order": 1
      }'
 ```

#### 1.2 — Atividade de alternativa única

**Endpoint:** `/task/new/singlechoice`
```bash
curl -w "%{http_code}\n" -X POST http://localhost:8080/task/new/singlechoice \
  -H "Content-Type: application/json" \
  -d '{
        "courseId": 42,
        "statement": "O que aprendemos hoje?",
        "order": 2,
        "options": [
            {
                "option": "Java",
                "isCorrect": true
            },
            {
                "option": "Python",
                "isCorrect": false
            },
            {
                "option": "Ruby",
                "isCorrect": false
            }
        ]
      }'
 ```

##### Regras
- A atividade deve ter no minimo 2 e no máximo 5 alternativas.
- A atividade deve ter uma única alternativa correta.
- As alternativas devem ter no mínimo 4 e no máximo 80 caracteres.
- As alternativas não podem ser iguais entre si.
- As alternativas não podem ser iguais ao enunciado da atividade.

##### 1.3 — Atividade de múltipla escolha

**Endpoint:** `/task/new/multiplechoice`
```bash
curl -w "%{http_code}\n" -X POST http://localhost:8080/task/new/singlechoice \
  -H "Content-Type: application/json" \
  -d '{
        "courseId": 42,
        "statement": "O que aprendemos hoje?",
        "order": 2,
        "options": [
            {
                "option": "Java",
                "isCorrect": true
            },
            {
                "option": "Spring",
                "isCorrect": true
            },
            {
                "option": "Ruby",
                "isCorrect": false
            }
        ]
      }'
 ```

##### Regras
- A atividade deve ter no minimo 3 e no máximo 5 alternativas.
- A atividade deve ter duas ou mais alternativas corretas, e ao menos uma alternativa incorreta.
- As alternativas devem ter no mínimo 4 e no máximo 80 caracteres.
- As alternativas não podem ser iguais entre si.
- As alternativas não podem ser iguais ao enunciado da atividade.

#### 👉👉Importante👈👈
Caso uma nova atividade seja adicionada a um curso com uma ordem que já está em uso, todas as atividades com aquela ordem ou superiores devem ser deslocadas uma posição para frente, garantindo que cada atividade tenha uma ordem única e sequencial.
```
Exemplo:
Se o curso possui as seguintes atividades:
Ordem 1 – Atividade A
Ordem 2 – Atividade B
Ordem 3 – Atividade C

E for adicionada uma nova com ordem 2, a lista será reorganizada assim:

Ordem 1 – Atividade A
Ordem 2 – Nova Atividade
Ordem 3 – Atividade B (foi deslocada)
Ordem 4 – Atividade C (foi deslocada)

Validação de sequência:
A ordem das atividades deve ser contínua, sem saltos. Ou seja, 
não é permitido adicionar uma atividade com ordem 4 se ainda não existem atividades com ordens 3 (ou anteriores).

Exemplo inválido:
Se o curso tem:

Ordem 1 – Atividade A
Ordem 2 – Atividade B

E uma nova atividade tenta ser inserida com ordem 4, o sistema deve lançar um erro informando que a sequência está incorreta.

```

### Questão 2 — Publicação de Cursos

Para publicar um curso, ele deve:

- Conter ao menos uma atividade de cada tipo.
- Ter atividades com `order` em sequência contínua (ex: 1, 2, 3...).
- O curso só pode ser publicado se o status for `BUILDING`.
- Ter o `status` atualizado para `PUBLISHED` e `publishedAt` com a data atual.

Implemente o endpoint `/course/{id}/publish` validando essas regras antes da publicação.

Exemplo de requisição:
```bash
curl -w "%{http_code}\n" -X POST http://localhost:8080/course/42/publish
```

### Bônus (não obrigatório)

- Configure o Spring Security para proteger os endpoints de criação de atividades e criação/publicação de cursos. 
  O acesso deve ser restrito a usuários com a role `INSTRUCTOR`, os demais endpoints de listagens podem ser acessados por qualquer usuário, desde que estejam autenticados.

## Considerações finais

- A avaliação do case será realizada exclusivamente com base nos requisitos e na forma como você utiliza **lógica**,
**orientação a objetos** e **testes**. Qualquer tecnologia fora do escopo, como Swagger, Docker, ou aspectos visuais, 
  não será considerada como um diferencial.
- Testes são obrigatórios e serão avaliados como requisito.
- Caso você tenha alguma dúvida sobre a descrição das questões, faça anotações no código e siga o que considerar mais adequado.
- Outros candidatos estão concorrendo à mesma vaga, e códigos muito semelhantes resultarão na anulação do case.
- Utilize ferramentas de IA, mas tenha cautela com o código gerado automaticamente. Caso avance para a próxima etapa, 
a entrevista síncrona será baseada no código que você produziu.