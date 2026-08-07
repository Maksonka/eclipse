# AGENTS.md

## Frontend: адаптивность (обязательно)
Любой фронтенд-код (разметка, стили, плеер, формы, попапы) пишем сразу адаптивным:
- mobile-first, гибкие сетки (flex/grid + `minmax`, `fr`, проценты);
- шрифты и отступы через `clamp()` / относительные единицы;
- корректные breakpoints под планшеты и телефоны;
- плеер и видеоконтейнеры растягиваются без полос прокрутки на узких экранах;
- никаких фиксированных ширины/высот там, где элемент должен тянуться.

## Сборка и запуск
- JDK: `C:\Program Files\Java\jdk-25.0.3` (`$env:JAVA_HOME = ...`)
- Компиляция: `.\mvnw.cmd -q compile` (рабочая папка — корень проекта)
- Сервер поднимается на порту **1010** (НЕ 8080), старт:
  `Start-Process cmd.exe -ArgumentList '/c mvnw.cmd spring-boot:run > run.log 2>&1' -WorkingDirectory <корень проекта> -WindowStyle Hidden`
- Лог — `run.log` в корне проекта.
- Транзакции: все STOMP-отправки (`messagingTemplate.convertAndSend*`) в транзакционных методах сервиса оборачивать в `sendAfterCommit(...)` — иначе гонка: клиент реагирует на данные, которые ещё не закоммичены.
- Управление воспроизведением — **только хост**: сервис режет не-хоста (control/playlist.remove/play/next с `IllegalArgumentException`), клиент скрывает нативные контролы (HTML5 — атрибут `controls`; YouTube — `controls:0/1` + `disablekb`, при смене роли плеер пересоздаётся через `syncNativeControls`).

## Watch Together: реакции и статус
- `@MessageMapping("/room.react")` → `WatchRoomService.react(...)` → broadcast на `/topic/room.{id}.reactions` (DTO `WatchRoomReactionDto`: roomId, username, emoji, timestamp). Реакция не персистится.
- Клиент: панель `.reaction-bar` (кнопки эмодзи), летающие `.reaction-float` в слое `.reaction-layer` (pointer-events:none, z-index 20, анимация `reaction-float` 2.2s).
- Индикатор «Вы уже в комнате»: `.room-live-badge` в шапке комнаты (скрывается в `resetRoomView`).

## Тесты (E2E, PowerShell)
Расположены в `C:\Users\2BA0~1\AppData\Local\Temp\opencode\`:
- `watch-playlist-test.ps1` — плейлист (add/remove/play/next, права хоста);
- `watch-stomp-test.ps1` — базовый STOMP (создание, чат, control, история, leave);
- `watch-two-user-test.ps1` — два пользователя (join, контроль, передача хоста, удаление);
- `watch-reaction-test.ps1` — эмодзи-реакции (broadcast обоим участникам, ошибка на пустую).
Запуск: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File <путь>`
Внимание: в .ps1 нельзя писать эмодзи/кириллицу литералами (PS 5.1 читает файл в ANSI) — эмодзи задавать через `[char]::ConvertFromUtf32(0x1F525)`.
