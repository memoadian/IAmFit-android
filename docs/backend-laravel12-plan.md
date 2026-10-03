# Plan de backend — IAm-fit (Laravel 12)
# Reconciliado con la implementación real

> **Propósito**: documento ejecutable por un agente de IA (DeepSeek Flash, Codex,
> Copilot, etc.). Versión reconciliada con el código realmente implementado y con la
> **decisión de producto B confirmada**: el producto es catálogo nutricional +
> entrenamiento, y la carga de IA vive en el backend (no en la app Android).

| Campo | Valor |
| --- | --- |
| Implementación real | `/home/memoadian/apps/php/iamfit` (Laravel 12) |
| App Android | `/home/memoadian/AndroidStudioProjects/IAmFit` |
| Fecha de análisis | 2026-10-02 |
| Método | Revisión de código, rutas, migraciones, config, seeders y tests |

---

## 1. Veredicto de reconciliación

El backend implementado **coincide en el stack y las decisiones de arquitectura**
(Laravel 12, Sanctum, PostgreSQL, multiusuario, Groq detrás de una interfaz, Docker,
español) pero **no en el contrato funcional ni en el modelo de dominio**.

- El plan original describía una app de **registro conversacional con IA**:
  texto libre → Groq con JSON estructurado → persistir `meal`/`workout` → alimentar
  Home y Progreso.
- La implementación es una app de **catálogo nutricional + planeación de
  entrenamiento**: búsqueda de alimentos con enriquecimiento (Open Food Facts →
  USDA → IA), diario contra un catálogo, catálogo de músculos/ejercicios, rutinas
  armadas por el usuario y consejo de cargas por IA.

El feature que el plan llamaba "el núcleo" (`POST /api/v1/log`) **no existe** en la
implementación.

**Decisión tomada (2026-10-02)**: se confirma la **Opción B** (sección 5). El registro
conversacional queda **archivado** (sección 6) y el trabajo activo se centra en
consolidar el backend y en mover al BE toda la carga de IA, sacando la key de Groq del
APK (secciones 4, 7 y 9).

---

## 2. Decisiones de arquitectura: cerradas vs. estado real

Decisiones confirmadas por el usuario y su estado en el código:

| Decisión | Estado real | Nota |
| --- | --- | --- |
| API para móvil | ✅ Cumple | `routes/api.php`, prefijo `/api` (sin versión) |
| Sanctum (tokens) | ✅ Cumple | `laravel/sanctum ^4.0`, `auth:sanctum` |
| Multiusuario desde el día 1 | ✅ Cumple | `user_id` FK en todas las tablas de datos |
| PostgreSQL | ✅ Cumple, **v17** | `docker-compose.yml`: `postgres:17` |
| Groq tras interfaz | ✅ Cumple | `App\Contracts\AiChatProvider` + `GroqChatProvider` |
| Docker local | ✅ Cumple, topología distinta | Un contenedor app + pgsql + adminer |
| Producción en VPS DigitalOcean | ❌ No existe | Sin `docker-compose.prod.yml` ni SSL |
| Cola en Redis | ❌ No existe | `QUEUE_CONNECTION=database`, sin servicio Redis |

---

## 3. Estado actual del backend

### 3.1 Stack

- Laravel `^12.0`, PHP `^8.2` (imagen `php:8.4-fpm`).
- Laravel Sanctum `^4.0`.
- PostgreSQL 17 (`jsonb`, extensión `pg_trgm` para búsqueda tolerante a typos).
- Cola, cache y sesión en driver `database` (no Redis).
- HTTP Client de Laravel para Groq y fuentes nutricionales.
- PHPUnit `^11.5` (no Pest), Laravel Pint, Laravel Pail.
- Locale de la app: `es` (`APP_LOCALE=es`).

Dependencias de IA/datos en `config/services.php`: `groq`, `usda` y `openfoodfacts`.
El orden de fuentes nutricionales vive en `config/nutrition.php`.

### 3.2 Infraestructura Docker real

`docker-compose.yml` define:

| Servicio | Contenido | Puertos host |
| --- | --- | --- |
| `iamfit-app` | **Un solo contenedor** con nginx + php-fpm + supervisor (`queue:listen`) | `8001:80` |
| `pgsql` | `postgres:17`, healthcheck `pg_isready` | `5433:5432` |
| `adminer` | Admin UI para Postgres | `8084:8080` |

Detalles:

- Bind mount `.:/var/www/html`; `WWWUSER`/`WWWGROUP` como ARG para permisos.
- `extra_hosts: host.docker.internal:host-gateway`.
- Sin servicio Redis, sin contenedor `worker` separado, sin Nginx externo.
- Wrapper `./iamfit` para `artisan`, `composer`, `tinker`, `test`, `pint`, `psql`.
- Job `ResolveFoodLookup` corre con `queue:listen` (database) vía supervisor.

### 3.3 Modelo de dominio implementado

19 migraciones (16 de negocio), 15 modelos. Núcleo por área:

**Identidad y perfil**

| Tabla | Campos clave |
| --- | --- |
| `users` | Default de Laravel |
| `profiles` | `sex` enum(`male`,`female`), `birthdate`, `height_cm`, `activity_level` enum, `goal` enum(`lose`,`maintain`,`gain`), `goal_rate_kg_per_week` nullable, `locale` default `es-MX` |
| `body_weight_entries` | `weight_kg`, `measured_on`, `source` enum(`manual`,`scale`,`import`), `note`; único `(user_id, measured_on)` |

**Nutrición**

| Tabla | Campos clave |
| --- | --- |
| `foods` | `name`, `brand`, `barcode`, `source` enum(`off`,`usda`,`ai`,`manual`), `external_id`, valores **por 100 g** (`kcal`, `protein_g`, `carb_g`, `fat_g`, `fiber_g`, `sugar_g`, `sat_fat_g`, `sodium_mg`), `micros` jsonb, `verified_at`, softDeletes, único `(source, external_id)`, índice GIN trigram en `lower(name)` |
| `food_portions` | `food_id`, `label`, `grams`, `is_default` |
| `food_log_entries` | `food_id`, `food_portion_id`, `meal` enum, `consumed_on` date, `quantity`, `grams` + **snapshot** de `kcal`/macros al registrar; índice `(user_id, consumed_on)` |
| `ai_food_lookups` | `query`, `query_hash` (sha256), `status` enum(`pending`,`processing`,`done`,`failed`), `resolved_by` enum(`off`,`usda`,`ai`), `food_id`, `requested_by`, `raw` jsonb, `error`, tokens |

**Entrenamiento**

| Tabla | Campos clave |
| --- | --- |
| `muscles` | `slug`, `name`, `group` enum(`chest`,`back`,`shoulders`,`arms`,`legs`,`core`) |
| `exercises` | `slug`, `name`, `primary_muscle_id`, `equipment` enum, `mechanic` enum(`compound`,`isolation`), `created_by`, `is_public` |
| `exercise_secondary_muscle` | Pivot ejercicio ↔ músculos secundarios |
| `routines` | `name`, `notes`, `days_per_week`, `is_active` |
| `routine_days` | `label`, `position` |
| `routine_exercises` | `exercise_id`, `position`, `target_sets`, `target_reps_min/max`, `target_rpe`, `rest_seconds`, `note` |
| `workout_sessions` | `routine_day_id` nullable, `performed_at`, `notes`; índice `(user_id, performed_at)` |
| `set_logs` | `workout_session_id`, `exercise_id`, `set_number`, `weight_kg`, `reps`, `rpe`, `is_warmup` |
| `ai_training_advices` | `routine_id`, `context` jsonb, `advice`, `provider`, tokens |

**Reglas de dominio implementadas**

- Los `foods` guardan valores por 100 g; `nutrientsForGrams()` escala a la cantidad.
- `food_log_entries` guarda un **snapshot**: si el alimento se corrige después, el
  historial del usuario no se reescribe.
- Las metas de energía **no se persisten** (`daily_goals` no existe): `EnergyCalculator`
  calcula BMR (Mifflin-St Jeor), TDEE (factores 1.2–1.9), objetivo calórico
  (~7700 kcal/kg, ritmo por defecto: lose −0.5, maintain 0, gain +0.25 kg/semana,
  piso 1.1×BMR) y macros (1.8 g/kg proteína, 25 % grasa, resto carbos).

### 3.4 API implementada (`/api`, sin versión)

| Método | Ruta | Descripción |
| --- | --- | --- |
| `POST` | `/register` | `{name, email, password, device_name?}` → `{user, token}` |
| `POST` | `/login` | `{email, password, device_name?}` → `{user, token}` |
| `POST` | `/logout` | Revoca el token actual |
| `GET` | `/me` | `{user}` |
| `GET` / `PUT` | `/profile` | Perfil + peso inicial (el PUT siembra el primer peso) |
| `GET` / `POST` | `/weight` | Historial de peso; `DELETE /weight/{id}` |
| `GET` | `/energy` | BMR, TDEE, `target_kcal`, `rate_kg_per_week`, macros |
| `GET` | `/foods/search?q=` | `found` (200) o `pending` (202 + `lookup_id`) |
| `GET` | `/foods/lookups/{id}` | Estado del enriquecimiento |
| `GET` | `/foods/{id}` | FoodResource |
| `GET` / `POST` | `/diary?date=` | Entradas agrupadas por `meal`, `totals` y `target`; `DELETE /diary/{id}` |
| `GET` | `/muscles` | Catálogo |
| `GET` | `/exercises?muscle=&group=&equipment=&q=` | Catálogo |
| — | `/routines` | `apiResource` completo, árbol `days[].exercises[]` |
| `POST` | `/routines/{routine}/advice` | Consejo de cargas de la IA (`throttle:ai`) |

**No existen**: `/health`, `/diagnostics/ai`, `/forgot-password`,
`/reset-password`, `/log`, `/daily-summary`, `/progress/streak`, `/goals`.

Formato de respuesta: **mixto** (`{user, token}`, `{status, food}`,
`{date, entries, totals, target}`, `{data}` en Resources). Sin envelope uniforme
documentado.

### 3.5 IA implementada

Contrato `App\Contracts\AiChatProvider::complete(systemPrompt, userText, ?jsonSchema)`,
idéntico al del plan. Bind `AiChatProvider → GroqChatProvider` en `AppServiceProvider`.

`GroqChatProvider` (paridad con la app Android):

- `temperature 0.1`, `max_tokens 1536`, `reasoning_effort low`, timeout 30 s.
- `response_format`: `json_schema` estricto si hay schema; `json_object` si no.
- Traduce 429/401/403/5xx, timeout (`504`) y `finish_reason=length` a `AiException`
  con mensaje seguro para el usuario.

La IA se usa para **dos features distintas** (no para registro conversacional):

1. **`AiNutritionSource`** (asíncrono, vía job `ResolveFoodLookup`): estima nutrientes
   por 100 g de alimentos/platillos que no están en OFF ni USDA. Schema con
   `known:false` si no conoce el alimento; lo guardado queda `verified_at = null`
   ("estimado"). El flujo es `FoodResolver` → OFF → USDA → IA, en orden.
2. **`TrainingAdviceService`** (síncrono): aconseja cargas de arranque, progresión y
   riesgos de sobre-entrenamiento a partir de perfil + rutina + mejores series de las
   últimas 8 semanas. Respuesta en prosa (máx. 200 palabras), guardada en
   `ai_training_advices`.

Rate limits de IA: `throttle:ai` = 6 llamadas/min **globales** (Groq limita por
organización, no por usuario). Los tokens se guardan en `ai_food_lookups` y
`ai_training_advices`; no hay tabla central `ai_request_logs` con latencia/estado.

### 3.6 Auth, validación y seguridad

- Sanctum con `device_name` opcional; `logout` elimina el token actual.
- `throttle:auth` = 10/min por IP en register/login.
- Autorización por recurso con checks de `user_id` (ej. `abort_unless` en diary y
  routines).
- `$fillable` explícito en todos los modelos.
- Requests: `ProfileRequest`, `StoreFoodLogRequest`, `StoreRoutineRequest` con
  validación de rangos y reglas (`before_or_equal:today`, `between`, `Rule::in`).
- La key de Groq vive solo en `.env` del backend.

### 3.7 Timezones y riesgo conocido de "día local"

- Migraciones usan `$table->timestamps()` (sin zona horaria).
- `consumed_on` / `measured_on` son `date` planas escritas con `today()` (zona del
  servidor).
- No hay parámetro `timezone` ni columnas `date` generadas.

Esto reproduce el riesgo que el plan original anticipaba: un registro cerca de la
medianoche puede caer en el día equivocado según la zona del usuario. Ver P1.

### 3.8 Tests y seeders

- **PHPUnit** contra PostgreSQL dedicado `iamfit_test` (los tests dependen de `jsonb`,
  `pg_trgm` y `similarity()`; SQLite no sirve).
- ~12 métodos de test: `AuthTest`, `FoodDiaryTest`, `ProfileEnergyTest`,
  `RoutineTest`, `EnergyCalculatorTest` + los `ExampleTest` del skeleton.
- **No hay** tests con `Http::fake()` para la IA, ni de `/log`, `daily-summary` o
  `streak`.
- Seeders: `MuscleSeeder`, `ExerciseSeeder`, `FoodSeeder`; en entorno local crea el
  usuario `Memo` (`memoadian@gmail.com`). No hay meta diaria ni datos demo de comidas.
- Pint está como dev-dependency; no hay CI ni configuración de PHPStan.

### 3.9 Deuda menor observada

- `composer.json` conserva nombre/descripción del skeleton (`laravel/laravel`).
- `database/database.sqlite` es remanente del skeleton (la app es pgsql).
- El directorio del backend **no es un repositorio git** (no hay `.git`): conviene
  versionarlo antes de seguir iterando.

---

## 4. Alcance y backlog tras la decisión B

### Archivado (fuera de alcance)

- `POST /api/v1/log` y el chat conversacional que clasifica `meal`/`workout`.
- Tablas `meals`, `workouts` y `ai_request_logs`.
- `daily_goals` persistidas y endpoints `/daily-summary` y `/goals` (las metas se
  calculan al vuelo con `EnergyCalculator`, que es el diseño vigente).
- Resumen en la sección 6 por si se revive el feature más adelante.

### P0 — Mover la carga de IA al backend y cerrar la ruta directa a Groq

1. Android: eliminar `GROQ_API_KEY`/`GROQ_MODEL` de `app/build.gradle.kts` y
   `local.properties` (la key sale del APK).
2. Android: consumir los endpoints del backend para toda la IA —
   `/foods/search` (el enriquecimiento con IA ya vive en el BE vía
   `ResolveFoodLookup`) y `/routines/{routine}/advice` (`TrainingAdviceService`).
3. Android: cliente HTTP con token de Sanctum (OkHttp + kotlinx.serialization ya
   están en la app) y almacenamiento seguro del token (Keystore).
4. Backend: `git init` + commit inicial del estado actual (hoy no hay `.git`).
5. Backend: endpoint `GET /health` básico para smoke test y monitoreo.

### P1 — Consistencia y calidad

6. Envelope `{data}` uniforme + formato de error estándar documentado.
7. Backed enums de PHP en `app/Enums` (hoy hay enums nativos de Postgres + strings
   sueltos) sin romper las migraciones existentes.
8. `timestampTz` + manejo de `timezone` (riesgo del "día local" descrito en 3.7).
9. Tests con `Http::fake()` para las features de IA (enriquecimiento de alimentos,
   consejo de rutina) y tests de casos pendientes (búsqueda `pending` → `done`,
   unicidad de peso por día).
10. `forgot-password` / `reset-password` (requiere mailer).
11. `POST /diagnostics/ai` opcional: sustituto del "Probar conexión" de la
    `ProfileScreen`.
12. Racha semanal para la pantalla Progreso, derivable de `/diary` + `/weight`
    (si la UI la necesita; hoy no tiene backend).

### P2 — Infraestructura y operación

13. Redis para cola/cache/sesión + contenedor `worker` separado.
14. `docker-compose.prod.yml` y target `production` del Dockerfile.
15. Despliegue en VPS DigitalOcean + SSL + backups de Postgres.
16. Seeders con datos demo de comidas/rutinas (hoy solo catálogos y usuario local).
17. CI (Pint + tests) y análisis estático opcional (PHPStan/Larastan).

---

## 5. Decisión tomada: Opción B

Confirmada por el usuario (2026-10-02): **el producto es catálogo nutricional +
entrenamiento**. El registro conversacional queda archivado como "no vigente".

Además, la **carga de IA pasa al backend**: la app Android deja de hablar con Groq
directamente y consume el backend, que hoy ya centraliza las dos features de IA
(enriquecimiento de alimentos y consejo de rutinas). El objetivo de seguridad de sacar
la key del APK se mantiene vigente dentro de la Opción B (P0).

Consecuencias para el plan:

- El dominio, endpoints y flujos de la sección 3 son **el producto**, no features
  adicionales.
- El diseño conversacional de la sección 6 queda solo como referencia archivada.
- El backlog activo es el de la sección 4 (P0/P1/P2).

---

## 6. Archivado — registro conversacional (no vigente)

> **Estado: archivado por decisión B (2026-10-02).** El contenido de esta sección se
> conserva solo como referencia histórica para revivir el feature si algún día se
> decide. Ningún agente debe implementarlo sin una instrucción explícita nueva del
> usuario.

Especificación que había definido el plan original, resumida a continuación.

### 6.1 Flujo `POST /api/v1/log`

```text
Android -> POST /api/v1/log {text, eaten_at?, timezone?}
   -> Groq con json_schema "log_entry" (strict)
   -> validar JSON y mapear a Meal o Workout
   -> transacción: insert meal|workout + ai_request_logs
   -> 201 {data:{type, message, meal|workout}}
```

Regla: si `text` mezcla comida y entrenamiento, devolver `422` pidiendo registrarlos
por separado.

### 6.2 Prompt del sistema

```text
Eres el asistente de registro de IAm-fit. El usuario describe en lenguaje natural
algo que comió o un entrenamiento que hizo.
Clasifica la entrada como "meal" o "workout".
Si es "meal": título en español, tipo (breakfast|lunch|dinner|snack), fecha/hora
aproximadas, calorías y macros en gramos con estimaciones razonables.
Si es "workout": título, tipo (strength|cardio|mobility|sport|other), intensidad
(low|moderate|high|very_high), duración en minutos y calorías quemadas estimadas.
Responde SIEMPRE en JSON válido según el schema proporcionado.
```

### 6.3 JSON Schema `log_entry`

```json
{
  "type": "object",
  "properties": {
    "type": { "enum": ["meal", "workout"] },
    "title": { "type": "string" },
    "message": { "type": "string" },
    "meal": {
      "type": ["object", "null"],
      "properties": {
        "meal_type": { "enum": ["breakfast", "lunch", "dinner", "snack"] },
        "eaten_at": { "type": "string", "format": "date-time" },
        "calories": { "type": "integer", "minimum": 0 },
        "protein_g": { "type": "integer", "minimum": 0 },
        "carbs_g": { "type": "integer", "minimum": 0 },
        "fat_g": { "type": "integer", "minimum": 0 }
      },
      "required": ["meal_type", "eaten_at", "calories", "protein_g", "carbs_g", "fat_g"],
      "additionalProperties": false
    },
    "workout": {
      "type": ["object", "null"],
      "properties": {
        "workout_type": { "enum": ["strength", "cardio", "mobility", "sport", "other"] },
        "started_at": { "type": "string", "format": "date-time" },
        "duration_min": { "type": "integer", "minimum": 0 },
        "calories_burned": { "type": "integer", "minimum": 0 },
        "intensity": { "enum": ["low", "moderate", "high", "very_high"] }
      },
      "required": ["workout_type", "started_at", "duration_min", "calories_burned", "intensity"],
      "additionalProperties": false
    }
  },
  "required": ["type", "title", "message"],
  "additionalProperties": false
}
```

### 6.4 Tablas nuevas y endpoints

- `meals`: `user_id`, `title`, `type`, `eaten_at` (tz), `calories`, `protein_g`,
  `carbs_g`, `fat_g`, `raw_input`, `ai_metadata` jsonb.
- `workouts`: análogo con `started_at`, `duration_min`, `calories_burned`,
  `intensity`.
- `ai_request_logs`: `provider`, `model`, `purpose`, tokens, `latency_ms`, `status`,
  `error` (auditoría central de IA).
- Endpoints: `GET/POST /api/v1/log`, `GET /api/v1/daily-summary?date=&timezone=`,
  `GET/POST /api/v1/progress/weight?days=`, `GET /api/v1/progress/streak`,
  `GET/PUT /api/v1/goals`, `GET /api/v1/health`, `POST /api/v1/diagnostics/ai`.

La racha se calcula: día completado = al menos una comida o un entrenamiento con día
local igual a la fecha.

### 6.5 Errores de IA → HTTP

| Caso | HTTP |
| --- | --- |
| API key vacía / config | 503 |
| Red / timeout | 504 |
| 429 | 503 (o 429) |
| 401 / 403 | 502 |
| 5xx | 502 |
| JSON inválido/truncado | 502 |
| `finish_reason=length` | 422 |

---

## 7. Cliente Android: mover la carga de IA al backend

Estado actual: la app llama a Groq directamente y la key viaja incrustada en el APK
(`BuildConfig` desde `local.properties`). Con la decisión B no se elimina la IA, sino
**su ejecución en el cliente**: todas las llamadas de IA pasan por el backend.

Pasos:

1. Quitar `GROQ_API_KEY`/`GROQ_MODEL` de `app/build.gradle.kts` y `local.properties`
   (incluido el bloque de `localProperties` en Gradle).
2. Crear un cliente HTTP de API (OkHttp + kotlinx.serialization ya están en la app)
   contra la API del backend y guardar el token de Sanctum con
   EncryptedSharedPreferences/Keystore.
3. Sustituir las llamadas directas a Groq por endpoints del backend:
   búsqueda/enriquecimiento de alimentos vía `/foods/search` + `/foods/lookups/{id}`
   y consejo de cargas vía `/routines/{routine}/advice`.
4. Conectar las pantallas a datos reales: `/energy`, `/profile`, `/weight`, `/diary`,
   `/muscles`, `/exercises`, `/routines`.
5. Añadir `ViewModel` + repositorio y persistencia local (Room/DataStore) para
   `home`, `progress` y `profile`.
6. El chat de `LogScreen` queda fuera de alcance (feature archivado): se reemplaza por
   el flujo de diario (`/diary`) o por búsqueda de alimentos; la decisión de UX se
   coordina aparte.
7. El "Probar conexión" de `ProfileScreen` pasa a usar `GET /health` (o
   `POST /diagnostics/ai` cuando exista) en lugar de Groq directo.

---

## 8. Infraestructura objetivo (P2)

Cuando se vaya a producción en el VPS de DigitalOcean:

- Añadir servicio `redis` y pasar `QUEUE_CONNECTION`, `CACHE_STORE` y
  `SESSION_DRIVER` a Redis.
- Separar `worker` (`php artisan queue:work redis --tries=3 --timeout=90`) del
  contenedor web.
- Crear `docker-compose.prod.yml`: sin bind mount de código, sin publicar `db` ni
  `redis`, healthchecks, `restart: unless-stopped`, imagen con `--no-dev`.
- Nginx del host (Certbot, SSL 443) → proxy al contenedor `web:80`.
- Publicación: build local + pull en el VPS o `git pull` + build en el VPS.
- Backups de Postgres (`pg_dump`) programados y `.env` fuera de la imagen.

---

## 9. Guía para un agente ejecutor

### 9.0 Antes de tocar código

1. Leer este documento completo.
2. La decisión ya está tomada: **Opción B** (sección 5). No implementar el feature
   conversacional archivado (sección 6).
3. Versionar el backend con git (`git init`, commit inicial del estado actual).
4. Levantar el entorno: `cp .env.example .env`, `docker compose up -d --build`,
   `./iamfit artisan key:generate`, `./iamfit artisan migrate --seed`.

### 9.1 Ruta vigente (Opción B)

**Backend**

1. `git init` + commit del estado actual (P0-4).
2. Añadir `GET /health` (P0-5).
3. Unificar envelope `{data}` y formato de error (P1-6).
4. Backed enums PHP en `app/Enums` sin romper migraciones (P1-7).
5. `timestampTz` + `timezone` con migración explícita (P1-8).
6. Tests con `Http::fake()` para IA y casos pendientes (P1-9).
7. `forgot/reset-password` y `diagnostics/ai` (P1-10, P1-11).
8. Racha semanal si la UI de Progreso la requiere (P1-12).
9. P2 de infraestructura cuando haya VPS: Redis, worker, `docker-compose.prod.yml`,
   SSL y backups (sección 8).

**Android** (sección 7)

10. Quitar la key de Groq del APK y crear cliente HTTP con token de Sanctum.
11. Pasar toda la IA por el backend (`/foods/search`, `/routines/{id}/advice`).
12. Conectar pantallas a los endpoints reales y añadir ViewModel/repositorio.

### 9.2 Verificación

```bash
./iamfit test
./iamfit pint --test
docker compose exec pgsql psql -U iamfit -d iamfit -c "CREATE DATABASE iamfit_test OWNER iamfit;"
```

Smoke test de la API:

```bash
curl -X POST http://localhost:8001/api/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Demo","email":"demo@iamfit.local","password":"password123"}'
```

---

## 10. Riesgos y mitigaciones

| Riesgo | Mitigación |
| --- | --- |
| Alcance desalineado con el plan | Backlog activo en la sección 4; no revivir la sección 6 sin orden explícita |
| Salida de IA no determinista | JSON Schema estricto + validación de rango + `temperature 0.1` |
| Costo/límite de Groq | `throttle:ai` global + tokens guardados por feature |
| Día local mal calculado | `timestampTz` + parámetro `timezone` (P1) |
| Registros duplicados por reintento (diario) | Validación en cliente + unicidad donde aplique |
| Token de Sanctum filtrado | Expiración, logout, HTTPS |
| Datos de alimentos sin verificar | Badge "estimado" vía `verified_at` + flujo admin |
| Pérdida de datos de Postgres | Volumen `iamfit-pgsql` + backups `pg_dump` en prod |
| Sin Redis en prod | Migrar driver y añadir servicio antes de desplegar |
| Sin control de versiones | `git init` + commit del estado actual |
| Key de Groq aún en el APK | P0: repuntar Android al backend y centralizar la IA en el BE (sección 7) |
