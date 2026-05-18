











--------------------------------------------------------------------------------------------------------------------
Файли, що відповідають за роботу з БД:

Інфраструктура: docker-compose.yml — піднімає сам сервер PostgreSQL та створює порожню базу.

Конфігурація підключення: application.properties — містить URL (jdbc:postgresql://localhost:5432/udtracker_db), кредоси та вказівки для Hibernate.

Схема таблиць (DDL): Класи в пакеті com.udtracker.api.model (AppUser.class, Player.class, MatchData.class) виступають як креслення таблиць завдяки JPA-анотаціям (@Entity, @Table).

Маніпуляція даними (DML): Інтерфейси в com.udtracker.api.repository (AppUserRepository.class, PlayerRepository.class, MatchRepository.class) автоматично генерують CRUD-запити.
++++++++
Руками створювати таблиці чи писати SQL-запити тобі не потрібно. Уся база розгорнеться автоматично, якщо ти виконаєш два кроки:

1. Запустиш Docker Compose і виконаєш в терміналі КОМПІЛЯТОРА (у мене intelij idea там є термінал одразу) і "docker-compose up -d" у папці проєкту. Це підніме сервер БД та створить порожню базу udtracker_db.

2. Запустиш Spring Boot застосунок (Просто запустити проект).
--------------------------------------------------------------------------------------------------------------------------
API краще своє поставити але впринципі похуй

VALORANT - HenrikDEV https://api.henrikdev.xyz/dashboard 
FACEIT - https://docs.faceit.com/getting-started/intro/start-here/
DOTA 2 - opendota.com. Це безкоштовний open-source проєкт, який не вимагає API-ключів для базових лімітів
-------------------------------------------------------------------------------------------------------------------------
http://localhost:8080/swagger-ui/index.html# - сваггер (можна зайти коли піднято спрінг бут)
http://localhost:8080 - сам сайт

для провірки апі мої аккаунти 
Valorant - "ТРО ВАРШАВА" "naxyi"
CS2 Faceit - "Ange1uw"
Dota 2 - "1141448080" (він приймає тіки від стіма старий friend ID 32)
