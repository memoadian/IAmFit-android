package com.geeckosoft.iamfit.data

import android.content.Context
import com.geeckosoft.iamfit.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Error de una llamada al backend. Igual que `AiException`, separa el detalle
 * técnico (logs) del mensaje seguro para la UI ([userMessage]).
 */
class ApiException(
    message: String,
    val userMessage: String,
    cause: Throwable? = null,
) : Exception(message, cause)

@Serializable
data class DataEnvelope<T>(val data: T)

@Serializable
data class HealthDto(val status: String)

@Serializable
data class DiagnosticsDto(
    val provider: String,
    val status: String,
    val reply: String = "",
)

@Serializable
data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    @SerialName("has_profile") val hasProfile: Boolean = false,
)

@Serializable
data class AuthData(val user: UserDto, val token: String)

@Serializable
data class FoodDto(
    val id: Long,
    val name: String,
    val brand: String? = null,
    val source: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
)

@Serializable
data class FoodSearchData(
    val status: String,
    @SerialName("lookup_id") val lookupId: Long? = null,
    val food: FoodDto? = null,
    val message: String? = null,
)

@Serializable
data class FoodLookupData(
    val status: String,
    @SerialName("resolved_by") val resolvedBy: String? = null,
    val food: FoodDto? = null,
)

@Serializable
data class RoutineAdviceData(
    val advice: String,
    @SerialName("generated_at") val generatedAt: String? = null,
)

@Serializable
data class MeEnvelope(val user: UserDto)

@Serializable
data class EnergyMacros(
    @SerialName("protein_g") val proteinG: Int = 0,
    @SerialName("fat_g") val fatG: Int = 0,
    @SerialName("carb_g") val carbG: Int = 0,
)

@Serializable
data class EnergyData(
    @SerialName("weight_kg") val weightKg: Double? = null,
    val age: Int? = null,
    val bmr: Double = 0.0,
    val tdee: Double = 0.0,
    @SerialName("target_kcal") val targetKcal: Double = 0.0,
    val goal: String? = null,
    val macros: EnergyMacros = EnergyMacros(),
)

@Serializable
data class DiaryTotals(
    val kcal: Double = 0.0,
    @SerialName("protein_g") val proteinG: Double = 0.0,
    @SerialName("carb_g") val carbG: Double = 0.0,
    @SerialName("fat_g") val fatG: Double = 0.0,
)

@Serializable
data class DiaryTarget(
    val kcal: Double = 0.0,
    val macros: EnergyMacros = EnergyMacros(),
)

@Serializable
data class DiaryEntry(
    val id: Long,
    val grams: Double = 0.0,
    val kcal: Double = 0.0,
    @SerialName("protein_g") val proteinG: Double = 0.0,
    @SerialName("carb_g") val carbG: Double = 0.0,
    @SerialName("fat_g") val fatG: Double = 0.0,
    val food: FoodDto? = null,
)

@Serializable
data class DiaryData(
    val date: String,
    val timezone: String? = null,
    val entries: Map<String, List<DiaryEntry>> = emptyMap(),
    val totals: DiaryTotals = DiaryTotals(),
    val target: DiaryTarget? = null,
)

@Serializable
data class WeightEntry(
    val id: Long,
    @SerialName("weight_kg") val weightKg: Double,
    @SerialName("measured_on") val measuredOn: String,
)

@Serializable
data class StreakDay(val date: String, val completed: Boolean = false)

@Serializable
data class StreakData(
    val timezone: String? = null,
    val days: List<StreakDay> = emptyList(),
    @SerialName("current_streak") val currentStreak: Int = 0,
)

@Serializable
data class ProfileDto(
    val id: Long,
    val sex: String? = null,
    val birthdate: String? = null,
    @SerialName("height_cm") val heightCm: Double? = null,
    @SerialName("activity_level") val activityLevel: String? = null,
    val goal: String? = null,
    val locale: String? = null,
    val timezone: String? = null,
)

@Serializable
data class ProfileData(
    val profile: ProfileDto? = null,
    @SerialName("latest_weight_kg") val latestWeightKg: Double? = null,
)

@Serializable
private data class LoginRequest(
    val email: String,
    val password: String,
    @SerialName("device_name") val deviceName: String = "android",
)

@Serializable
private data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerialName("device_name") val deviceName: String = "android",
)

@Serializable
private data class ErrorBody(val message: String? = null)

/**
 * Cliente HTTP de la API del backend. Toda la IA (enriquecimiento de alimentos y
 * consejo de rutinas) pasa por aquí: la app nunca conoce la key de Groq.
 */
class IamFitApi(
    private val context: Context,
    private val baseUrl: String = BuildConfig.API_BASE_URL,
    private val client: OkHttpClient = defaultClient(),
    private val json: Json = defaultJson(),
) {
    suspend fun health(): HealthDto =
        json.decodeFromString(get("health", auth = false))

    suspend fun diagnosticsAi(): DiagnosticsDto =
        json.decodeFromString<DataEnvelope<DiagnosticsDto>>(post("diagnostics/ai", "{}")).data

    suspend fun login(email: String, password: String): AuthData {
        val body = json.encodeToString(LoginRequest.serializer(), LoginRequest(email, password))
        val auth = json.decodeFromString<DataEnvelope<AuthData>>(post("login", body, auth = false)).data
        TokenStore.save(context, auth.token)

        return auth
    }

    suspend fun register(name: String, email: String, password: String): AuthData {
        val body = json.encodeToString(RegisterRequest.serializer(), RegisterRequest(name, email, password))
        val auth = json.decodeFromString<DataEnvelope<AuthData>>(post("register", body, auth = false)).data
        TokenStore.save(context, auth.token)

        return auth
    }

    suspend fun me(): UserDto =
        json.decodeFromString<DataEnvelope<MeEnvelope>>(get("me")).data.user

    suspend fun energy(): EnergyData =
        json.decodeFromString<DataEnvelope<EnergyData>>(get("energy")).data

    suspend fun diary(date: String? = null, timezone: String = deviceTimezone()): DiaryData {
        val query = buildMap {
            put("timezone", timezone)
            date?.let { put("date", it) }
        }

        return json.decodeFromString<DataEnvelope<DiaryData>>(get("diary", query = query)).data
    }

    suspend fun weights(days: Int = 7, timezone: String = deviceTimezone()): List<WeightEntry> {
        val body = get("weight", query = mapOf("days" to days.toString(), "timezone" to timezone))

        return json.decodeFromString<DataEnvelope<List<WeightEntry>>>(body).data
    }

    suspend fun streak(timezone: String = deviceTimezone()): StreakData =
        json.decodeFromString<DataEnvelope<StreakData>>(
            get("progress/streak", query = mapOf("timezone" to timezone)),
        ).data

    suspend fun profile(): ProfileData =
        json.decodeFromString<DataEnvelope<ProfileData>>(get("profile")).data

    /** Búsqueda de alimentos; el enriquecimiento con IA ocurre en el backend. */
    suspend fun searchFood(query: String): FoodSearchData =
        json.decodeFromString<DataEnvelope<FoodSearchData>>(
            get("foods/search", query = mapOf("q" to query)),
        ).data

    suspend fun foodLookup(lookupId: Long): FoodLookupData =
        json.decodeFromString<DataEnvelope<FoodLookupData>>(get("foods/lookups/$lookupId")).data

    /** Consejo de cargas de la IA para una rutina del usuario. */
    suspend fun routineAdvice(routineId: Long): RoutineAdviceData =
        json.decodeFromString<DataEnvelope<RoutineAdviceData>>(
            post("routines/$routineId/advice", "{}"),
        ).data

    fun logout() = TokenStore.clear(context)

    private suspend fun get(
        path: String,
        query: Map<String, String> = emptyMap(),
        auth: Boolean = true,
    ): String = execute(buildRequest(path, query, method = "GET", body = null, auth = auth))

    private suspend fun post(path: String, body: String, auth: Boolean = true): String =
        execute(buildRequest(path, emptyMap(), method = "POST", body = body, auth = auth))

    private fun buildRequest(
        path: String,
        query: Map<String, String>,
        method: String,
        body: String?,
        auth: Boolean,
    ): Request {
        val url = (baseUrl.trimEnd('/') + "/" + path)
            .toHttpUrl()
            .newBuilder()
            .apply { query.forEach { (key, value) -> addQueryParameter(key, value) } }
            .build()

        val builder = Request.Builder().url(url)

        if (method == "POST") {
            builder.post((body ?: "{}").toRequestBody(JSON_MEDIA_TYPE))
        }

        if (auth) {
            TokenStore.get(context)?.let { builder.header("Authorization", "Bearer $it") }
        }

        return builder.build()
    }

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    throw errorFor(response.code, raw)
                }

                raw
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: IOException) {
            throw ApiException(
                "Error de red con el backend: ${e.message}",
                "No se pudo conectar con el servidor. Revisa tu conexión e intenta de nuevo.",
                e,
            )
        }
    }

    private fun errorFor(code: Int, raw: String): ApiException {
        val message = runCatching { json.decodeFromString<ErrorBody>(raw).message }.getOrNull()

        val userMessage = when (code) {
            401 -> "Tu sesión expiró. Vuelve a iniciar sesión."
            403 -> message ?: "No autorizado para este recurso."
            404 -> message ?: "Recurso no encontrado."
            422 -> message ?: "Los datos enviados no son válidos."
            429 -> message ?: "Demasiadas solicitudes. Intenta de nuevo en un momento."
            else -> message ?: "Ocurrió un error (HTTP $code)."
        }

        return ApiException("HTTP $code: $raw", userMessage)
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun deviceTimezone(): String = java.util.TimeZone.getDefault().id

        fun defaultJson() = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        fun defaultClient(): OkHttpClient {
            val builder = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS)

            if (BuildConfig.DEBUG) {
                // BASIC: sólo línea de petición/respuesta, nunca headers (llevan el token).
                builder.addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
                )
            }

            return builder.build()
        }
    }
}
