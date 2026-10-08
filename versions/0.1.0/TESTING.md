# Проверки Heart of the Progression 0.1.0

Проверено 4 октября 2026 года на Windows 11, JDK 17.0.20.1, Gradle 8.8, Forge 47.4.10, Minecraft 1.20.1.

## Подтверждённые результаты

- `gradlew compileJava`: **успешно**, ошибок компиляции нет.
- `gradlew build`: **успешно**, создан reobfuscated JAR с refmap. Четыре JUnit-теста: 0 ошибок, 0 пропусков.
- `gradlew runGameTestServer`: **14 / 14 обязательных GameTest прошли** в Forge SERVER environment, с реально применёнными Mixins.
- `gradlew runClient -Psmoke`: **RPG_CLIENT_SMOKE PASSED**, реальный клиент, integrated server, сетевой обмен и четыре PNG со снимками framebuffer.
- `gradlew runClient -Psmoke -Prelogin`: проверка сохранённого прогресса после полного завершения и нового запуска клиента и integrated server; результат записан в `docs/verification/relogin-smoke.log`.
- Отдельный production dedicated server из официального Forge installer загрузил **готовый распространяемый JAR** в SRG-окружении, дошёл до `Done (4.375s)!`, затем штатно остановлен командой `stop`; exit code 0. Проверка выполнялась на loopback 127.0.0.1 с включённой online authentication.
- В JAR присутствует `rpgprogression.refmap.json`; пакета `gametest` в нём нет. Клиентский код не загружается на dedicated server.

Логи и JUnit XML находятся в `docs/verification`. Снимки: `docs/screenshots`.

## Соответствие сценариям из задания

| № | Что проверено | Проверка |
|---|---|---|
| 1 | Уровень 1, XP 0, очки 0, пять атрибутов по 10 | defaultsAndLevels |
| 2 | XP после реального ServerPlayerGameMode.destroyBlock; отменённый break и воздух не дают XP | miningSuccessAndPlacedBlockProtection, cancelledBreakAndGameModes |
| 3 | XP после смерти зомби от player damage source | mobKillAndFishing |
| 4 | Успешный ItemFishedEvent через Forge EventBus, категории fish/junk/treasure | mobKillAndFishing |
| 5 | 20 блоков движения дают 1 XP; полёт и скачок координат исключены | sprintDistanceAndExclusions |
| 6–7 | 810 XP дают уровень и очко; остаток при нескольких уровнях; max level | defaultsAndLevels |
| 8–10 | Strength → ATTACK_DAMAGE; Vitality → MAX_HEALTH; Endurance → maxStamina | attributesAndPacketValidation |
| 11 | Sprint расходует 4/сек | staminaAndExhaustion |
| 12 | Реальная Player.attack расходует 12 | attackAndMiningSlowdown |
| 13 | Успешная добыча расходует стамину | miningSuccessAndPlacedBlockProtection |
| 14–15 | Задержка и восстановление, запрет sprint, порог выхода, штраф attack speed | staminaAndExhaustion |
| 16 | Agility → MOVEMENT_SPEED; повторное применение не складывает UUID-modifier | attributesAndPacketValidation |
| 17 | Реальные use ticks и release лука: полная критическая стрела за 16 тиков при Agility 20 | bowAndCrossbowUseEvents |
| 18 | Реальные use ticks и release арбалета с Quick Charge III: заряжен за 8 тиков | bowAndCrossbowUseEvents |
| 19–20 | 0/27 при Endurance 10; 3/27 при 12; 27/27 при 28; все промежуточные пороги | lockedSlotInteractions, JUnit |
| 21 | Shift-click, все ClickType, drag-последовательность, прямой add, Forge insert/extract, слияние в закрытый стек | lockedSlotInteractions |
| 22–23 | Ручной крафт 2×2 и 3×3 заблокирован; quickMove не обходит; повышение Intelligence делает результат доступным | craftingBothGridsAndUnlock |
| 24 | Полная сериализация/deserialization entity capability в NBT; дробная дистанция сохраняется; отдельный перезапуск клиента/сервера | saveLoadAndDeathClone, relogin smoke |
| 25 | Clone после invalidateCaps переносит уровень/XP/атрибуты и восстанавливает стамину | saveLoadAndDeathClone |
| 26 | GameTest SERVER и production dedicated server с release JAR | Серверные журналы |

Дополнительно проверены: переполнение формул XP, убывающая отдача Agility, кап защиты, creative/spectator, отказ при неверном индексе/нуле очков/достигнутом maxAttribute, ограничение текущего HP, безопасный перенос старых предметов и сохранение overflow как ItemEntity.

GameTest использует Forge FakePlayer с настоящими capability, инвентарём, меню, оружием и событиями. Клиентский smoke-test использует обычного локального игрока и SimpleChannel: синхронизация уровня/статов, C2S SpendPoint, серверная трата ровно одного очка и последующий S2C ответ. Визуально просмотрены русское меню при GUI Scale 2 и 3, 27 замков и HUD. PNG получены непосредственно из Minecraft framebuffer, это не макеты.

## Воспроизведение

```powershell
.\gradlew.bat compileJava
.\gradlew.bat build
.\gradlew.bat runGameTestServer
```

Для автоматического клиентского теста используйте отдельный тестовый мир: после GameTest скопируйте созданную папку `run/world` в `run/saves/RPGSmoke`. **Не используйте пользовательский игровой мир:** smoke-test устанавливает тестовые значения персонажа. Укажите в `run/options.txt` `lang:ru_ru`, `guiScale:2`; затем:

```powershell
.\gradlew.bat runClient -Psmoke
.\gradlew.bat runClient -Psmoke -Prelogin
```

Первый запуск проверяет меню и сетевую трату очка, сохраняет снимки в `run/screenshots` и завершает игру. Второй проверяет сохранённые уровень 12, XP 542, Strength 15 и 1 свободное очко после полного перезапуска. Обычный `runClient` без свойств не запускает автоматический тест.

## Практические границы проверки

Не проводились длительные нагрузочные испытания с большим количеством игроков и совместимость с конкретными сторонними модпаками. Два внешних клиента с Mojang-аутентификацией одновременно не подключались. Проверены стандартные player crafting grids; нестандартные машины сторонних модов требуют адаптеров.

Смена измерения обрабатывается Forge PlayerChangedDimensionEvent и повторной установкой модификаторов; отдельный проход через портал в клиентском smoke-test не выполнялся. Отслеживание поршней реализовано консервативно и не является защитой от всех нестандартных способов перемещения блоков сторонними модами. Реальные алгоритмы наград рыбалки проверены через событие готового улова; ожидание случайной поклёвки не автоматизировалось.

Компилятор Forge 47.4.10 выдаёт предупреждения deprecation для старых конструкторов ResourceLocation и loading-context accessors. Ошибок компиляции нет. Gradle сообщает о deprecated API внутри используемых плагинов относительно будущего Gradle 9; проект закреплён на Gradle 8.8. В dev-среде Mixin может сообщать об отсутствии refmap в classpath запуска, но release JAR содержит его и отдельно проверен на production server.
