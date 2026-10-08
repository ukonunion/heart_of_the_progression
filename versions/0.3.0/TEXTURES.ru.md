# Текстура меню Heart of the Progression 0.3.0

Оставлен один PNG: `src/main/resources/assets/rpgprogression/textures/gui/character.png` (320×180). Это исходный фон пользователя без изменений. Дополнительные иконки, PNG кнопок, полос и замков удалены. Текст, модель, полосы, кнопки и замки рисуются кодом; сердца, еда и броня HUD используют стандартный атлас Minecraft и поддерживают обычные ресурс-паки.

## Замена фона без сборки

1. Распакуйте `heart-of-the-progression-textures-0.3.0.zip` в `resourcepacks/rpgprogression-custom/`, чтобы `pack.mcmeta` лежал в корне папки.
2. Замените `assets/rpgprogression/textures/gui/character.png`, сохраняя размер 320×180 и расположение панелей.
3. Включите ресурс-пак и нажмите F3+T в игре.

Можно редактировать готовую папку `heart-of-the-progression-textures-0.3.0`. В исходниках разметка находится в `client/gui/CharacterScreen.java` и использует логический холст 640×360 (двойной размер PNG), HUD — в `client/hud/CenteredHud.java`, цвета полос — в `client/gui/GuiTextures.java`.

Пак 0.2.0 с прежними дополнительными текстурами сохранён в старом архиве. Версия 0.3.0 не обращается к этим файлам.
