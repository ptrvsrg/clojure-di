# Простой DI Пример: Java с Clojure-контейнером

Этот проект демонстрирует, как Java-приложение может использовать библиотеку **`clojure-di`** для внедрения зависимостей через стандартные аннотации `@Inject`.

## Зависимости

*   **Java 17+**
*   **Maven 3.6+**
*   **Clojure 1.12+** (управляется через Maven)

## Как запустить?

### 1. Собрать и установить DI-библиотеку

Для начала необходимо собрать и установить нашу библиотеку `clojure-di` в локальный Maven-репозиторий, чтобы этот пример мог ее найти.

```bash
cd /path/to/clojure-di/project
lein install
```

### 2. Собрать и запустить Java-пример

Теперь, когда библиотека доступна, перейдите в папку с этим примером и соберите его.

```bash
cd /path/to/java-di-example
mvn clean compile package
```

Запустите собранный JAR-файл:

```bash
java -jar target/java-di-example-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Вы должны увидеть в консоли:

```
---------- Starting Java Application ----------
SimpleDatabase created!
UserService created and injected with Database!
Service processed: User data for ID: 1
-----------------------------------------------
```
