# Руководство пользователя

Это приложение демонстрирует, как использовать **Redis** для кеширования в **Spring Boot**.

## 📋 Требования

- **Java 21**
- **Gradle**
- **Docker** (для запуска Redis и PostgreSQL)
- **Docker Compose**

## 🚀 Установка

### 1. Клонируйте репозиторий

```bash
git clone https://github.com/your-repo/demo-redis.git
cd demo-redis
```

### 2. Настройка Docker Compose

В файле `docker-compose.yml` настроены сервисы **Redis** и **PostgreSQL**:

```yaml
services:
  redis:
    image: redis:latest
    container_name: my_redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    restart: unless-stopped

  postgres:
    image: postgres:latest
    container_name: my_postgres
    ports:
      - "5435:5435"
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: user_db
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

volumes:
  redis_data:
    driver: local
  postgres_data:
    driver: local
```

### 3. Запустите сервисы

```bash
docker-compose up -d
```

### 4. Настройка приложения

```properties
spring.application.name=demo-redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

spring.datasource.url=jdbc:postgresql://localhost:5435/user_db
spring.datasource.username=postgres
spring.datasource.password=postgres

spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

## 📂 Структура проекта

- **RedisConfig.java**: Настройка Redis-кеша.
- **UserController.java**: REST API для управления пользователями.
- **UserService.java**: Бизнес-логика с использованием кеша.
- **UserRepository.java**: Репозиторий для доступа к базе данных.
- **User.java**: Сущность пользователя.
- **GlobalExceptionHandler.java**: Обработка исключений.

## 🔧 Конфигурация Redis

Файл `RedisConfig.java` отвечает за подключение к Redis и настройку кеширования.

```java
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(1))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext
                                .SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext
                                .SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer())
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                .build();
    }
}
```

### Основные настройки:

- **entryTtl(Duration.ofMinutes(1))**: Время жизни кеша - 1 минута.
- **disableCachingNullValues()**: Отключает кеширование `null` значений.
- **serializeKeysWith** и **serializeValuesWith**: Настройка сериализации ключей и значений.


## 🧩 Сервис Пользователя с Кешированием

Файл `UserService.java` содержит бизнес-логику и аннотации для кеширования.

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User save(User user) {
        return userRepository.save(user);
    }

    @Cacheable(value = "user", key = "#id")
    public User getById(Integer id) {
        return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @CacheEvict(value = "user", key = "#user.id")
    public User update(User user) {
        return userRepository.save(user);
    }

    @CacheEvict(value = "user", key = "#id")
    public void delete(Integer id) {
        userRepository.deleteById(id);
    }
}
```

### Аннотации:

- **@Cacheable**: Кеширует результат метода.
- **@CacheEvict**: Удаляет данные из кеша при обновлении или удалении.

## 💡 Как Это Работает

1. **Сохранение пользователя**:
   - Метод `save` сохраняет пользователя в базе данных.
   - Данные не кешируются на этом этапе.

2. **Получение пользователя**:
   - Первый запрос берет данные из базы данных и сохраняет в кеш Redis.
   - Последующие запросы берут данные из кеша, ускоряя доступ.

3. **Обновление пользователя**:
   - Метод `update` изменяет данные в базе.
   - Кеш для этого пользователя удаляется, чтобы при следующем запросе данные обновились.

4. **Удаление пользователя**:
   - Метод `delete` удаляет пользователя из базы данных.
   - Кеш для этого пользователя также удаляется.
