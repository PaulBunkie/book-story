# Book's Story - Development Fork

Это форк оригинального проекта [Book's Story](https://github.com/Acclorite/book-story) для разработки и экспериментов.

## Оригинальный проект

**Book's Story** — бесплатное и открытое приложение для чтения электронных книг с дизайном Material You, построенное на Jetpack Compose.

- **Оригинальный репозиторий**: [Acclorite/book-story](https://github.com/Acclorite/book-story)
- **Лицензия**: GPL-3.0-only
- **Автор**: Acclorite

## Настройка для разработки

### Требования
- Android Studio (с Java 21)
- Gradle 8.13
- Android SDK 26+

### Конфигурация
Проект настроен для работы с Java 21 из Android Studio:
- `org.gradle.java.home` указывает на JBR Android Studio
- Исправлен checksum для Gradle 8.13

### Сборка
```bash
./gradlew clean :app:assembleDebug
```

## Изменения в этом форке

### v1.0.0-dev
- ✅ Настроена Java 21 для корректной сборки
- ✅ Исправлен checksum Gradle wrapper
- ✅ Создана ветка для разработки

## Планы развития

- [ ] Оптимизация производительности
- [ ] Улучшение UI/UX
- [ ] Добавление новых функций
- [ ] Исправление багов
- [ ] Добавление тестов

## Синхронизация с оригиналом

```bash
# Получить изменения из оригинального репозитория
git fetch upstream
git merge upstream/master

# Отправить изменения в наш форк
git push origin feature/development-setup
```

## Вклад в проект

Этот форк предназначен для экспериментов и разработки. Для официальных изменений обращайтесь к [оригинальному репозиторию](https://github.com/Acclorite/book-story).

---

**Важно**: Этот форк создан в образовательных целях. Все права на оригинальный код принадлежат Acclorite.
