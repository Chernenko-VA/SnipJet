# SnipJet

Скриншоты области экрана на Linux (Ubuntu / Wayland) с простым редактором аннотаций. Без `java.awt.Robot` — захват через `gnome-screenshot`, буфер через `wl-copy`.

## Скачать

Пакет `.deb` для amd64:

**[Releases на GitHub](https://github.com/VictoryCh/SnipJet/releases)** — последний релиз `v1.0.0` или [Latest](https://github.com/VictoryCh/SnipJet/releases/latest).

Файл: `snipjet_1.0.0-1_amd64.deb`

## Установка

Зависимости (подтянутся из Depends пакета):

- `gnome-screenshot`
- `wl-clipboard`

```bash
sudo apt install ./snipjet_1.0.0-1_amd64.deb
```

Если apt ругается на `_apt` и путь в домашней папке — скопируйте `.deb` в `/tmp` и установите оттуда.

## Использование

Запуск (меню приложений **SnipJet** или бинарь):

```bash
/opt/snipjet/bin/SnipJet
```

При старте приложение сразу прячет окно и открывает **выбор области**. После снимка открывается редактор (перо, маркер, ластик, текст, Copy / Save). Отмена области (Esc) закрывает приложение.

### Горячая клавиша Print Screen (GNOME / Ubuntu)

Системный Print Screen лучше отключить или заменить:

1. **Параметры → Клавиатура → Сочетания клавиш → Просмотр снимков экрана** — снимите привязку с Print / Print Screen.
2. **Сочетания клавиш → Пользовательские** → добавить:
   - Имя: `SnipJet`
   - Команда: `/opt/snipjet/bin/SnipJet`
   - Клавиша: `Print`

После этого Print Screen сразу запускает выбор области SnipJet.

## Сборка из исходников

Требования: JDK 17+ с `jpackage` (например Liberica Full), `dpkg-dev`.

```bash
./gradlew packageDeb
```

Результат:

`build/compose/binaries/main/deb/snipjet_1.0.0-1_amd64.deb`

## Лицензия

© 2026 — см. репозиторий.
