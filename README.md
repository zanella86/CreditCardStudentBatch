# CREDIT CARD STUDENT BATCH

Aplicativo para carga/processamento **em lote** dos dados dos alunos.

---
<!-- 
# OVERVIEW

---
--> 
# GRUPO

- RM346315: Lais Kagawa ([lakagawa](https://github.com/lakagawa))
- RM346511: Jônatha Lacerda Gonzaga ([jhowlacerda](https://github.com/jhowlacerda))
- RM346958: Thiago de Souza Zanella ([zanella86](https://github.com/zanella86))

---

# REPOSITÓRIOS RELACIONADOS

https://github.com/zanella86/CreditCardStudentWeb

---

# FERRAMENTAS / TECNOLOGIAS

- Java 17.0.2
- Git 2.31.1.windows.1 / Github
- Gradle 7.6.1
- IntelliJ IDEA Community Edition (2022.2.1)
- MySQL Workbench 8.0.31 CE
- Spring Boot 3.0.4 (Java 17+)
- Spring Batch 5.0.1

---

# CONSTRUÇÃO/JUSTIFICATIVA

<u>Utilizamos o Spring Framework:</u>

- **Spring Batch:** Para carga dos arquivos/integração

<u>Outros:</u>

- MySQL Driver: Para utilização do MySQL
- Lombok: Para redução da verbosidade

![Spring Initializr](docs/spring-initializr-setup.PNG)

---

# PARA TESTAR

## Bancos de dados

### MySQL

- Crie um *database schema* no MySQL chamado `credit-card-student`

![MySQL-Create-Schema](docs/mysql-schema-create.PNG)

## IDE

### Intellij

Adicione os parâmetros para conexão:

> MYSQL_USERNAME=meu_usuario;MYSQL_PASSWORD=minha-senha

![Intellij-bootRun-Arguments](docs/intellij-bootrun-arguments.PNG)

## Aplicação

> Antes de executar esta aplicação, é importante executar a aplicação do projeto [Web](https://github.com/zanella86/CreditCardStudentWeb) <u>ao menos uma vez</u> para que as entidades sejam criadas corretamente.

---

# REFERÊNCIAS

- [Prof.º Fabio Tadashi - Travel](https://github.com/fabiotadashi/1SCJR-travel)
- [Spring Initializr](https://start.spring.io/;)
- [Spring Batch - Reference Documentation](https://docs.spring.io/spring-batch/docs/current/reference/html/index.html) 
- [Baeldung - Spring Boot with Spring Batch](https://www.baeldung.com/spring-boot-spring-batch)
- [Baeldung - Configuring Skip Logic in Spring Batch](https://www.baeldung.com/spring-batch-skip-logic)
- [StackOverFlow - How to skip blank lines in CSV using FlatFileItemReader and chunks](https://stackoverflow.com/questions/29673524/how-to-skip-blank-lines-in-csv-using-flatfileitemreader-and-chunks)
- [StackOverFlow - How to solve deprecation warning of JobBuilderFactory and StepBuilderFactory](https://stackoverflow.com/questions/75508267/how-to-solve-deprecation-warning-of-jobbuilderfactory-and-stepbuilderfactory)
  - [Spring Batch 5.0 Migration Guide](https://github.com/spring-projects/spring-batch/wiki/Spring-Batch-5.0-Migration-Guide)
- [Um guia para o Spring Batch 5.0](https://www.youtube.com/watch?v=Jzf9ofPy_xk)
  - [Giuliana Bezerra / guide-sb-v5](https://github.com/giuliana-bezerra/guide-sb-v5)
- [Craftsmen - Processing files with fixed line length using Spring Batch](https://craftsmen.nl/processing-files-with-fixed-line-length-using-spring-batch/)