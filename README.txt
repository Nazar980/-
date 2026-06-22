CSCE466 Mod - Minecraft 1.21.4 Forge

Полный портированный проект мода.

=== СТРУКТУРА ===
- build.gradle (обновлён для Forge 1.21.4)
- gradle.properties
- settings.gradle
- src/main/java/... (все классы)
- src/main/resources/ (mods.toml, forge-mixin.json и т.д.)

=== ВАЖНО ===
1. Скачай gradle-wrapper.jar:
   https://services.gradle.org/distributions/gradle-8.8-bin.zip
   Распакуй и скопируй gradle/wrapper/gradle-wrapper.jar

2. Или после распаковки выполни:
   gradle wrapper

3. Затем:
   ./gradlew build

Готовый JAR: build/libs/csce466-mod-1.0.jar

ИмGui работает через Mixin (RenderSystemMixin) - всё сохранено.

Клавиша L в игре открывает меню.