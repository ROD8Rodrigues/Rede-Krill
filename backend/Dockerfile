# syntax=docker/dockerfile:1

# Duas fases: o Maven compila onde o JRE nunca vai morar, e a imagem final leva só o jar.
#
# O pom é copiado antes do código para que a resolução de dependências vire uma camada
# própria: mexer em uma classe reinicia o build a partir da cópia de src, sem baixar nada.
# Os cache mounts de /root/.m2 fazem o resto entre builds: o repositório local sobrevive à
# mudança de camada e ao `--no-cache`, e só é descartado com o prune do BuildKit.
#
# O jar vem do finalName fixo no pom (rede-krill-scheduling-api), sem versão no nome, para
# que este COPY não precise mudar a cada release.
#
# O que não mudou de propósito: os testes não rodam aqui (a suíte é papel da CI e do
# `./mvnw verify` local — dentro da imagem eles só custariam minutos e exigiriam o banco
# embarcado), e o `-XX:MaxRAMPercentage` deixa o heap acompanhar o limite do container em
# vez de fixar um -Xmx que vazaria em outra máquina.

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Usuário sem privilégio e sem shell de login: se a aplicação comprometer, o container já
# não é root. O /app pertence a ele porque o volume do jar é somente-leitura por cópia.
RUN addgroup -S app && adduser -S app -G app

COPY --from=build /app/target/rede-krill-scheduling-api.jar app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
