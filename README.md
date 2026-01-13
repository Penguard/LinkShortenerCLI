# Link Shortener CLI (Java)

Консольный сервис сокращения ссылок с:
- уникальными короткими ссылками для каждого пользователя (UUID без авторизации),
- лимитом переходов,
- временем жизни (TTL) задаваемым системой,
- автоудалением “истёкщих” ссылок,
- правами владельца на изменение лимита и удаление,
- открытием исходного URL в браузере.

## Требования
- Java 17+
- Maven 3.8+

## Конфигурация
Файл: `config/application.properties`

Параметры:
- `app.baseUrl` — отображаемый домен короткой ссылки (например `clck.ru`)
- `app.ttlSeconds` — TTL в секундах (например `86400`)
- `app.cleanupIntervalSeconds` — период автоочистки протухших ссылок (сек)
- `app.codeLength` — длина кода (6..16)
- `app.openBrowser` — открывать URL в браузере (`true/false`)

## Сборка и запуск
```bash
mvn test
mvn package
java -jar target/urlShort-1.0.0.jar
```
## Команды CLI

- **help** –> список всех команд
- **whoami** –> UUID текущего пользователя
- **new-user** –> создать и перейти в нового пользователя
- **login "uuid"** –> зайти в конкретного пользователя
- **create "url\" "maxClicks"** –> создать короткую ссылку 
- **open "code"** –> открыть коротку ссылку
- **"app.baseUrl"/"code"** –> можно просто вставить короткую ссылку (например: clck.ru/ABC123xy)

- **list** –> список всех ссылок пользователя
- **update-limit "code" "newLimit"** –> установка нового лимита для кода
- **delete "code"** –> удаление ссылки по коду
- **cleanup** –> очистка истёкщих ссылок
- **exit | quit** –> выход из приложения

## Примеры

> whoami 
> 
> user=8b8d2f2c-2df8-4a3f-8d07-1c6c1c3b0a2a

> create "https://ya.ru" 3
>
> OK short=clck.ru/ABC123xy code=ABC123xy expiresAt=... user=...

> clck.ru/ABC123xy
>
> status=ACTIVE clicks=1/3 expiresAt=... msg=OK
> 
> Opened: https://ya.ru

> update-limit ABC123xy 10
> 
>status=ACTIVE msg=Limit updated

> delete ABC123xy
> 
> status=ACTIVE msg=Deleted

## Тестирование

* уникальность кодов для разных пользователей,
* подсчёт/блокировку лимита,
* TTL и автоудаление,
* права владельца (update/delete),
* валидацию URL,
* парсинг CLI и ввод короткой ссылки baseUrl/code.