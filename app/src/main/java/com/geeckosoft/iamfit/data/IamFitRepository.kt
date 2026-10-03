package com.geeckosoft.iamfit.data

import android.content.Context

/**
 * Fachada del backend para la UI. Encapsula el cliente HTTP y el token de Sanctum
 * para que los ViewModels no conozcan detalles de red ni de almacenamiento.
 */
class IamFitRepository(context: Context) {

    private val appContext = context.applicationContext
    private val api = IamFitApi(appContext)

    fun isLoggedIn(): Boolean = TokenStore.get(appContext) != null

    suspend fun login(email: String, password: String): AuthData = api.login(email, password)

    suspend fun register(name: String, email: String, password: String): AuthData =
        api.register(name, email, password)

    fun logout() = api.logout()

    suspend fun me(): UserDto = api.me()

    suspend fun energy(): EnergyData = api.energy()

    suspend fun diary(): DiaryData = api.diary()

    suspend fun weights(days: Int = 7): List<WeightEntry> = api.weights(days)

    suspend fun streak(): StreakData = api.streak()

    suspend fun profile(): ProfileData = api.profile()

    suspend fun health(): HealthDto = api.health()

    suspend fun diagnosticsAi(): DiagnosticsDto = api.diagnosticsAi()

    suspend fun searchFood(query: String): FoodSearchData = api.searchFood(query)

    suspend fun foodLookup(lookupId: Long): FoodLookupData = api.foodLookup(lookupId)

    suspend fun routineAdvice(routineId: Long): RoutineAdviceData = api.routineAdvice(routineId)
}
