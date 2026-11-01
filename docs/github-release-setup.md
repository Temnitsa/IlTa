# GitHub Releases и автообновление IlTa

## 1. Создать публичный репозиторий

1. Создайте на GitHub **пустой публичный** репозиторий, например `IlTa`
   (не добавляйте README/license при создании).
2. В этом проекте выполните:

```bash
git init
git add .
git commit -m "IlTa 2.4.0: release signing and in-app updater"
git branch -M main
git remote add origin https://github.com/<owner>/IlTa.git
git push -u origin main
```

> Если `git init` не нужен, потому что папка уже часть другого репозитория, создайте
> отдельный каталог и перенесите проект туда.

## 2. Добавить secrets для подписи

Release-keystore лежит локально в `keystore/ilta-release.jks` и **не должен попадать в git**
(уже в `.gitignore`). Пароли хранятся в `keystore.properties` (тоже игнорируется).

В GitHub: **Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Значение |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | вывод команды ниже |
| `RELEASE_STORE_PASSWORD` | `storePassword` из `keystore.properties` |
| `RELEASE_KEY_PASSWORD` | `keyPassword` из `keystore.properties` (совпадает с storePassword) |

Получить base64:

```bash
base64 -i keystore/ilta-release.jks | pbcopy   # macOS
# Linux: base64 -w0 keystore/ilta-release.jks
```

## 3. Выпустить релиз

1. Поднимите `versionCode` и `versionName` в `app/build.gradle.kts`
   (сейчас `6` / `2.4.0`).
2. Закоммитьте, создайте тег и запушьте:

```bash
git add -A
git commit -m "IlTa 2.4.0"
git tag v2.4.0
git push origin main
git push origin v2.4.0
```

Workflow `.github/workflows/release.yml` соберёт подписанный APK, посчитает SHA-256
и создаст GitHub Release с файлами:
- `IlTa-v2.4.0-release.apk`
- `checksums.txt`

## 4. Как работает обновление в приложении

- При запуске приложение (если есть интернет) запрашивает
  `https://api.github.com/repos/<owner>/<repo>/releases/latest`.
- Если в релизе есть APK новее установленной версии — в нижнем меню у шестерёнки
  **Настройки** появляется красная точка, а в разделе «Обновления» — версия и кнопка
  «Скачать и установить».
- Проверку можно запустить вручную: **Настройки → Обновления → Проверить обновления**.
- Перед установкой APK проверяется: пакет, versionCode, подпись (должна совпадать
  с текущей), а если GitHub отдаёт `digest` — SHA-256.
- Установка идёт через системный установщик; пользователь подтверждает её
  (Android не разрешает тихую установку обычным приложениям).

## 5. Важно про подпись

Текущий keystore — новый release-ключ. Установленная debug-версия **не обновится поверх**
него (подписи разные): один раз удалите старую версию и установите новый APK.
Прогресс сохраняется через **Настройки → Данные → Сохранить в файл** и затем импорт.
Все последующие релизы должны подписываться **тем же** keystore, иначе обновление
не установится.
