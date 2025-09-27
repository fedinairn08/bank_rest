# 🏦 Система управления банковскими картами

REST API для управления банковскими картами с поддержкой аутентификации, авторизации и переводов между картами.

## 📋 Описание проекта

Система предоставляет REST API для управления банковскими картами с возможностью:
- Создания и управления картами
- Просмотра карт с маскированием номеров
- Переводов между собственными картами
- Аутентификации и авторизации пользователей

## 💳 Атрибуты карты

- **Номер карты** - зашифрован, отображается маской: `**** **** **** 1234`
- **Владелец** - пользователь системы
- **Срок действия** - дата истечения карты
- **Статус** - Активна, Заблокирована, Истек срок
- **Баланс** - текущий баланс карты

## 🚀 Технологический стек

- **Java 21** - основной язык разработки
- **Spring Boot 3.5.6** - фреймворк для создания REST API
- **Spring Security** - безопасность и аутентификация
- **Spring Data JPA** - работа с базой данных
- **PostgreSQL** - основная база данных
- **Liquibase** - управление миграциями БД
- **JWT** - токены для аутентификации
- **Swagger/OpenAPI** - документация API
- **Docker** - контейнеризация
- **MapStruct** - маппинг объектов
- **Lombok** - упрощение кода

## 🏗️ Архитектура проекта

```
src/main/java/com/example/bankcards/
├── config/           # Конфигурация (Security, OpenAPI)
├── controller/       # REST контроллеры
├── dto/             # Data Transfer Objects
│   ├── request/     # DTO для запросов
│   └── response/    # DTO для ответов
├── entity/          # JPA сущности
├── enums/           # Перечисления
├── exception/       # Обработка исключений
├── mapper/          # MapStruct мапперы
├── repository/      # JPA репозитории
├── security/        # JWT и безопасность
├── service/         # Бизнес-логика
└── util/            # Утилиты
```

## 🔐 Роли и возможности

### 👨‍💼 Администратор (ADMIN)
- Создание, блокировка, активация, удаление карт
- Управление пользователями
- Просмотр всех карт в системе
- Полный доступ к API

### 👤 Пользователь (USER)
- Просмотр своих карт (с поиском и пагинацией)
- Запрос блокировки карты
- Переводы между собственными картами
- Просмотр баланса карт

## 🛠️ Установка и запуск

### Предварительные требования
- Java 21+
- Maven 3.6+
- Docker и Docker Compose
- PostgreSQL (если запуск без Docker)

### Запуск через Docker Compose

1. **Клонирование репозитория:**
```bash
git clone <repository-url>
cd bank_rest
```

2. **Настройка переменных окружения:**
Для dev-профиля настройки уже находятся в файле `application-dev.properties`:
```properties
DB_USERNAME=root
DB_PASSWORD=root
DB_URL=jdbc:postgresql://localhost:5434/bank_rest

JWT_SECRET=aZyHAW5ir1CCYn+SuZF4S63acRqtcg7JXwV97pC0q7E=
JWT_EXPIRATION=86400000

SECRET_KEY=7xK9#pL2mQv@nR8sT4wZ!fD6cB1yHj5W
```

Для production используйте переменные окружения или создайте `application-prod.properties`.

3. **Запуск приложения:**
```bash
docker-compose up -d
```

4. **Проверка запуска:**
- Приложение: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html#
- База данных: localhost:5434

### Локальный запуск

1. **Установка зависимостей:**
```bash
mvn clean install
```

2. **Настройка базы данных:**
```bash
# Создание базы данных
createdb bank_rest

# Или через Docker
docker run --name postgres -e POSTGRES_DB=bank_rest -e POSTGRES_USER=root -e POSTGRES_PASSWORD=root -p 5432:5432 -d postgres:latest
```

3. **Запуск приложения:**
```bash
# С dev-профилем (использует application-dev.properties)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Или через IDE: установите активный профиль "dev"
```

## 📚 API Документация

### Основные эндпоинты

#### Аутентификация
- `POST /api/auth/login` - вход в систему
- `POST /api/auth/register` - регистрация пользователя

#### Управление картами (ADMIN)
- `POST /api/cards` - создание новой карты (только ADMIN)
- `GET /api/cards/{cardId}` - получение карты по ID (только ADMIN)
- `PUT /api/cards/{cardId}` - обновление карты (только ADMIN)
- `DELETE /api/cards/{cardId}` - удаление карты (только ADMIN)
- `GET /api/cards/my` - получение карт текущего пользователя
- `GET /api/cards/admin/all` - получение всех карт (только ADMIN)
- `POST /api/cards/{cardId}/block` - блокировка карты (только ADMIN)
- `POST /api/cards/{cardId}/activate` - активация карты (только ADMIN)

#### Пользовательские операции с картами
- `GET /api/user/cards/active` - активные карты пользователя
- `GET /api/user/cards/blocked` - заблокированные карты пользователя
- `GET /api/user/cards/{cardId}/balance` - баланс конкретной карты
- `GET /api/user/cards/total-balance` - общий баланс всех карт пользователя
- `POST /api/user/cards/{cardId}/request-block` - запрос на блокировку карты
- `POST /api/user/cards/transfer` - перевод между своими картами
- `GET /api/user/cards/transfers` - история переводов пользователя

#### Переводы (ADMIN)
- `GET /api/transfers/{transferId}` - получение перевода по ID (только ADMIN)
- `GET /api/transfers/admin/all` - получение всех переводов (только ADMIN)

#### Управление пользователями (только ADMIN)
- `GET /api/admin/users` - список пользователей с пагинацией
- `GET /api/admin/users/{userId}` - получение пользователя по ID
- `POST /api/admin/users` - создание пользователя
- `PUT /api/admin/users/{userId}` - обновление пользователя
- `DELETE /api/admin/users/{userId}` - удаление пользователя
- `GET /api/admin/users/role/{role}` - получение пользователей по роли

### Примеры запросов

#### Аутентификация
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password"
  }'
```

#### Создание карты
```bash
curl -X POST http://localhost:8080/api/cards \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt-token>" \
  -d '{
    "cardNumber": "5382450010988575",
    "cardHolder": "Irina Fedina",
    "expirationDate": "2026-09-27",
    "userId": 1
  }'
```

## 🔒 Безопасность

- **JWT токены** для аутентификации
- **Ролевая авторизация** (ADMIN/USER)
- **Шифрование номеров карт** в базе данных
- **Маскирование** отображения номеров карт
- **Валидация** всех входящих данных
- **Обработка ошибок** с информативными сообщениями

## 🗄️ База данных

### Миграции
Миграции управляются через Liquibase и находятся в `src/main/resources/db/migration/`:
- `changelog-master.yaml` - основной файл миграций
- `initial/` - начальные миграции
- `v2/` - версионные миграции

### Основные таблицы
- `users` - пользователи системы
- `roles` - роли пользователей
- `cards` - банковские карты
- `transfers` - история переводов

## 🧪 Тестирование

### Запуск тестов
```bash
mvn test
```

### Покрытие тестами
- **Unit тесты** для сервисов
- **Unit тесты** для контроллеров

## 📊 Мониторинг и логирование

- **Логирование через Spring Boot Logging**
- **Health check для Docker**

### Docker образ
```bash
docker build -t bank-rest-app .
docker run -p 8080:8080 bank-rest-app
```