FROM eclipse-temurin:21-jdk-alpine AS build
# Установка Maven
RUN apk update && apk add maven

COPY . /usr/src/app
WORKDIR /usr/src/app
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk-alpine

# Установка часового пояса (Moscow Time)
ENV TZ=Europe/Moscow
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Создание директории приложения
RUN mkdir -p /app
COPY --from=build /usr/src/app/target/*.jar /app/spring-boot-application.jar

# Оптимизация JVM-параметров для контейнера
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Duser.timezone=Europe/Moscow", \
    "-jar", "/app/spring-boot-application.jar"]