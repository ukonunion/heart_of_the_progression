# Heart of the Progression 2.1.0 — оружие Iron’s

Оружие Iron’s Spellbooks полностью отключено для получения и использования, кроме посохов. В установленной версии 1.21.1-3.16.3 запрещены 13 предметов; сохранены 7 посохов. Запрет действует независимо от настройки общей маны, в обычном и творческом режиме, с Epic Fight и без него.

## Отключено

| Название | ID |
|---|---|
| Magehunter | `irons_spellbooks:magehunter` |
| Spellbreaker | `irons_spellbooks:spellbreaker` |
| Claymore | `irons_spellbooks:claymore` |
| Decrepit Flamberge | `irons_spellbooks:keeper_flamberge` |
| Legionnaire Flamberge | `irons_spellbooks:legionnaire_flamberge` |
| Amethyst Rapier | `irons_spellbooks:amethyst_rapier` |
| Misery | `irons_spellbooks:misery` |
| Autoloader Crossbow | `irons_spellbooks:autoloader_crossbow` |
| Hither-Thither Wand | `irons_spellbooks:hither_thither_wand` |
| Hellrazor | `irons_spellbooks:hellrazor` |
| Decrepit Scythe | `irons_spellbooks:decrepit_scythe` |
| Boreal Blade | `irons_spellbooks:boreal_blade` |
| Twilight Gale | `irons_spellbooks:twilight_gale` |

## Доступные посохи

| Название | ID |
|---|---|
| Graybeard Staff | `irons_spellbooks:graybeard_staff` |
| Pyrium Staff | `irons_spellbooks:pyrium_staff` |
| Artificer's Cane | `irons_spellbooks:artificer_cane` |
| Ice Staff | `irons_spellbooks:ice_staff` |
| Rod o' Lightning | `irons_spellbooks:lightning_rod` |
| The Blood Staff | `irons_spellbooks:blood_staff` |
| Staff of the Nines | `irons_spellbooks:staff_of_the_nines` |

Трость изобретателя (`artificer_cane`) и громоотвод (`lightning_rod`) оставлены: сам Iron’s реализует их как StaffItem. Посох Девяти также сохранён. Палочка Hither-Thither относится к другим предметам раздела оружия и отключена.

## Как действует запрет

- Рецепты с запрещённым результатом убираются при загрузке ресурсов, включая ковку и рецепты из другого пространства имён. Подмена набора рецептов через API также фильтруется по результату.
- Запрещённые предметы убраны из обычных творческих вкладок и их поисковой выдачи.
- Финальный фильтр добычи удаляет оружие после всех глобальных loot-модификаторов. Отдельные выброшенные экземпляры не появляются в мире.
- Торговля запрещёнными результатами или ингредиентами удаляется; старые сохранённые сделки с опустевшими результатами/ценой не оставляют бесплатных обменов.
- Загрузка ItemStack из NBT удаляет старое оружие в инвентарях, сундуках, эндер-сундуках, предметах и снаряжении существ. Прямая выдача в инвентарь также отбрасывается; новые экземпляры, вставленные командами/пакетами/другими модами, удаляются из живого инвентаря, открытого контейнера и курсора.
- Атака, использование, добыча, запуск применения и продолжение использования заблокированы. Проверяются обычная атака, нативный strike Epic Fight и применение встроенного заклинания оружия. Отмена запрещённого удара не расходует стамину.
- Посохи сохраняют загрузку, рецепты, вкладки, добычу и применение заклинаний. Книги, свитки, броня, материалы и оружие остальных модов продолжают работать.

Технические записи предметов остаются в реестре, чтобы не ломать ссылки и код Iron’s; получать и использовать эти предметы нельзя. Сторонние окна просмотра предметов, например JEI/REI, отдельно не интегрировались: этих модов нет в проверяемой сборке. Запрет получения и использования применяется независимо от таких окон.

**Старые экземпляры запрещённого оружия удаляются при загрузке сохранения.** Уровни персонажа, RPG XP, атрибуты, навыки и остальные предметы сохраняются.
