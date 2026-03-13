# Esports Match Tracker — Java / JDBC / PostgreSQL

Система управления базой данных киберспортивных матчей. Предметная область: результаты и статистика турнирных матчей.

## Требования

- Java 17+
- Maven 3.8+
- PostgreSQL 14+ (локально, порт 5432)

---

## Быстрый старт

```bash
cd esports
mvn clean package -q
java -jar target/esports-db-1.0.jar
```

---

## Первый запуск

1. Запустите приложение
2. На вкладке **Вход** выберите «↳ Создать esports_matches»
3. Введите `admin` и `adminpass` → нажмите **Войти**
4. БД будет создана автоматически вместе со всеми процедурами

---

## Аутентификация

### Встроенные аккаунты (создаются автоматически)
| Логин   | Пароль    | Роль  |
|---------|-----------|-------|
| admin   | adminpass | admin |
| guest   | guestpass | guest |

### Регистрация
- Минимум 3 символа в логине, ровно 8 в пароле
- Для регистрации администратора нужен код: **`admin`**

---

## Права доступа

Реализованы через роли PostgreSQL (`es_admin` / `es_guest`) — не через скрытие кнопок.

| Операция                          | Admin | Guest |
|-----------------------------------|:-----:|:-----:|
| Просмотр / поиск                  |  ✅   |  ✅   |
| Добавление записи                 |  ✅   |  ❌   |
| Редактирование                    |  ✅   |  ❌   |
| Удаление по ID                    |  ✅   |  ❌   |
| Удаление по турниру               |  ✅   |  ❌   |
| Очистка таблицы                   |  ✅   |  ❌   |
| Создание пользователя БД          |  ✅   |  ❌   |
| Удаление базы данных              |  ✅   |  ❌   |
| Экспорт в Excel / Импорт из JSON  |  ✅   |  ✅   |

---

## Структура таблицы `matches`

| Поле            | Тип         | Описание                         |
|-----------------|-------------|----------------------------------|
| id              | SERIAL PK   | Автоинкремент                    |
| match_date      | DATE        | Дата матча (YYYY-MM-DD)          |
| match_duration  | TIME        | Длительность (HH:MM:SS)          |
| tournament      | VARCHAR(128)| Название турнира                 |
| game            | VARCHAR(64) | Дисциплина (CS2, Dota 2, LoL...) |
| format          | VARCHAR(16) | Bo1 / Bo2 / Bo3 / Bo5            |
| team1           | VARCHAR(64) | Первая команда                   |
| team2           | VARCHAR(64) | Вторая команда                   |
| winner          | VARCHAR(64) | Победитель (NULL = нет данных)   |

---

## Хранимые процедуры и функции

| Имя                        | Тип       | Описание                                    |
|----------------------------|-----------|---------------------------------------------|
| `fn_app_login`             | FUNCTION  | Аутентификация, возвращает роль             |
| `fn_get_all`               | FUNCTION  | Все матчи                                   |
| `fn_get_by_id`             | FUNCTION  | Матч по ID                                  |
| `fn_search_by_tournament`  | FUNCTION  | Поиск по турниру (ILIKE)                    |
| `sp_add_match`             | PROCEDURE | Добавить матч (с валидацией)                |
| `sp_update_match`          | PROCEDURE | Обновить запись                             |
| `sp_delete_by_id`          | PROCEDURE | Удалить по ID                               |
| `sp_delete_by_tournament`  | PROCEDURE | Удалить все матчи турнира                   |
| `sp_clear_table`           | PROCEDURE | Очистить таблицу (TRUNCATE + restart ID)    |
| `sp_app_register`          | PROCEDURE | Регистрация нового пользователя приложения  |
| `sp_create_user`           | PROCEDURE | Создание пользователя (только для admin)    |

> Из JDBC вызываются **только** хранимые процедуры и функции. Произвольный SQL запрещён.

---

## Импорт из JSON

Формат файла (массив объектов):
```json
[
  {
    "match_date": "2024-03-15",
    "match_duration": "01:42:30",
    "tournament": "IEM Katowice 2024",
    "game": "Counter-Strike 2",
    "format": "Bo3",
    "team1": "Natus Vincere",
    "team2": "Team Vitality",
    "winner": "Team Vitality"
  }
]
```

Пример: `sample_matches.json`
