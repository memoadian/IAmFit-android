package com.geeckosoft.iamfit.ai

/**
 * Abstrae la llamada a un proveedor de LLM (Groq hoy; mañana un backend propio,
 * OpenAI, Gemini, un modelo on-device...). El resto de la app depende de este
 * contrato, no de la implementación concreta. Cambiar de proveedor = una nueva
 * clase que implemente esto, sin tocar nada más.
 *
 * Espejo del contrato `AiChatProvider` del backend.
 */
interface ChatProvider {

    /** Identificador corto, sólo para logging (p. ej. "groq"). */
    val name: String

    /**
     * Envía [systemPrompt] + [userText] y devuelve la respuesta del modelo.
     *
     * @param jsonSchema JSON Schema (estilo structured outputs de OpenAI) que la
     *   respuesta debe cumplir, o `null` para texto/JSON libre.
     * @throws AiException si el proveedor falla (HTTP, timeout, respuesta vacía...).
     */
    suspend fun complete(
        systemPrompt: String,
        userText: String,
        jsonSchema: String? = null,
    ): ChatResult
}

/**
 * Resultado de [ChatProvider.complete]: el texto devuelto por el modelo y los
 * tokens consumidos (si el proveedor los reporta).
 */
data class ChatResult(
    val content: String,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
)
