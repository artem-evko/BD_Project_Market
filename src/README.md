# BD Project Market – Backend

## Пакетная структура

- `config` – конфигурация Spring (security, swagger, async, scheduler, web и т.п.)
- `security` – всё, что связано с JWT, паролями, user details
- `controller` – REST-контроллеры `/api/...`
- `service` – интерфейсы бизнес-логики
- `service.impl` – реализации сервисов
- `repository` – Spring Data JPA-репозитории
- `domain.entity` – JPA-сущности (таблицы БД)
- `domain.enum` – перечисления доменной модели
- `dto.request` – DTO для входящих запросов
- `dto.response` – DTO для ответов
- `mapper` – маппинг Entity ↔ DTO
- `exception` – исключения и глобальный обработчик
- `scheduler` – планировщики задач
- `util` – чистые утилитарные классы
- `component` – стартовые раннеры и вспомогательные компоненты

Entity живут только в `domain.entity`, DTO – только в `dto.*`

### Инициализация базы (один раз)
в application.yml указать свой пароль от бд postgres затем выполнить команду
```bash
psql -U postgres -d postgres

CREATE DATABASE shop_db;
\q
```
Либо в pgAdmin/DBeaver в SQL-консоли
```
CREATE DATABASE shop_db;
\q
```