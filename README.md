# CSCE466 Mod - Minecraft 1.21.4 Forge

Портированный мод с ImGui через Mixins.

## Требования
- Java 21
- Forge 1.21.4 (52.0.28+)
- Gradle 8.8+

## Сборка
```bash
./gradlew build
```

Готовый JAR будет в `build/libs/`

## Управление
Нажмите клавишу **L** в игре, чтобы открыть меню ImGui.

## Особенности
- Полная поддержка ImGui через Mixin (RenderSystemMixin)
- Сохранены все оригинальные функции (Zeus, алмазы и т.д.)
- Нет ошибок компиляции

## Структура
- `RenderSystemMixin.java` — перехватывает рендер для ImGui
- `ImGuiRenderer.java` — ядро рендеринга ImGui
- `ImGuiScreen.java` — экран с меню модa
