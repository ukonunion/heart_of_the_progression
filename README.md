# Heart of the Progression

Исходный код всех **21 версий** мода для **Minecraft 1.20.1 / Forge / Java 17**.
Актуальный публичный выпуск — **1.0.0**, ранее называвшийся 1.8.4.
Каждый выпуск находится в собственной папке `versions/<версия>` и содержит исходники,
тесты, ресурсы, Gradle Wrapper, конфигурацию сборки, лицензию и документацию того выпуска.

**[Открыть актуальные исходники 1.0.0](versions/1.0.0)** ·
**[Все изменения](CHANGELOG.md)** · **[Старая и новая нумерация](VERSION-MAP.md)**

## Версии / Versions

| Версия / Version | Исходники / Source | Документация / Documentation |
|---|---|---|
| 0.1.0 | [versions/0.1.0](versions/0.1.0) | [README](versions/0.1.0/README.ru.md) |
| 0.2.0 | [versions/0.2.0](versions/0.2.0) | [README](versions/0.2.0/README.ru.md) |
| 0.3.0 | [versions/0.3.0](versions/0.3.0) | [README](versions/0.3.0/README.ru.md) |
| 0.4.0 | [versions/0.4.0](versions/0.4.0) | [README](versions/0.4.0/README.ru.md) |
| 0.5.0 | [versions/0.5.0](versions/0.5.0) | [README](versions/0.5.0/README.ru.md) |
| 0.5.1 | [versions/0.5.1](versions/0.5.1) | [README](versions/0.5.1/README.ru.md) |
| 0.5.2 | [versions/0.5.2](versions/0.5.2) | [README](versions/0.5.2/README.ru.md) |
| 0.5.3 | [versions/0.5.3](versions/0.5.3) | [README](versions/0.5.3/README.ru.md) |
| 0.6.0 | [versions/0.6.0](versions/0.6.0) | [README](versions/0.6.0/README.ru.md) |
| 0.7.0 | [versions/0.7.0](versions/0.7.0) | [README](versions/0.7.0/README.ru.md) |
| 0.7.1 | [versions/0.7.1](versions/0.7.1) | [README](versions/0.7.1/README.ru.md) |
| 0.8.0 | [versions/0.8.0](versions/0.8.0) | [README](versions/0.8.0/README.ru.md) |
| 0.8.1 | [versions/0.8.1](versions/0.8.1) | [README](versions/0.8.1/README.ru.md) |
| 0.8.2 | [versions/0.8.2](versions/0.8.2) | [README](versions/0.8.2/README.ru.md) |
| 0.8.3 | [versions/0.8.3](versions/0.8.3) | [README](versions/0.8.3/README.ru.md) |
| 0.8.4 | [versions/0.8.4](versions/0.8.4) | [README](versions/0.8.4/README.ru.md) |
| 0.9.0 | [versions/0.9.0](versions/0.9.0) | [README](versions/0.9.0/README.ru.md) |
| 0.9.1 | [versions/0.9.1](versions/0.9.1) | [README](versions/0.9.1/README.ru.md) |
| 0.9.2 | [versions/0.9.2](versions/0.9.2) | [README](versions/0.9.2/README.ru.md) |
| 0.9.3 | [versions/0.9.3](versions/0.9.3) | [README](versions/0.9.3/README.ru.md) |
| 1.0.0 | [versions/1.0.0](versions/1.0.0) | [README](versions/1.0.0/README.ru.md) |

## Сборка

Перейдите в папку выбранной версии. Нужны JDK 17 и интернет для загрузки зависимостей.

```powershell
cd versions/1.0.0
.\gradlew.bat build
```

На Linux/macOS: `./gradlew build`. Готовый мод находится в `build/libs`.
Инструкции и результаты проверок конкретного выпуска приведены в его `README.ru.md` и `TESTING.md`.
Параметры `-Pparcool`, `-Pepicfight`, `-Pirons` включают соответствующие моды для разработческого запуска.
Для проверенного набора Iron's 3.16.3 использовалась Iron's Lib 1.20.1-2.2.0:
`-PironsLibVersion=1.20.1-2.2.0`. Локальные Maven-кэши не публикуются.

## Состав исходников

Java-код, тесты, JSON/TOML, текстуры, примеры конфигурации и настройки сборки сохранены
побайтово из подготовленных архивов выпусков. `SOURCE-MANIFEST.json` содержит SHA-256
архивов и каждого включённого файла. `FILES.txt` в каждой папке перечисляет опубликованные файлы.
Скриншоты, GIF, автоматические логи проверок, каталоги `run`/`build` и кэши в эту ветку
не включены. Ссылки на исторические материалы в документации относятся к полным архивам выпусков.

## License and credits

Source code: [MIT](LICENSE). Rights to supplied image assets remain with their authors;
see `ASSET-CREDITS.txt` and `TEXTURES.ru.md` in each version. Third-party mods are not bundled.

Спасибо за текстурки [Имя]! / Thanks to [Имя] for the textures!

## English

This branch contains the source of all 21 releases of **Heart of the Progression**.
Each `versions/<version>` directory is a standalone Gradle project. The latest release is
**1.0.0** (formerly 1.8.4). Build it with Java 17 using `./gradlew build` or `gradlew.bat build`.
See the per-version documentation for optional integrations and development tests.
Source, tests, build configuration and assets retain their original bytes; generated media,
verification logs and caches are omitted. The source manifest records archive and file checksums.
