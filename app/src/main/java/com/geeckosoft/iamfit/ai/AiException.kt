package com.geeckosoft.iamfit.ai

/**
 * Error de una llamada al proveedor de IA.
 *
 * Espejo del `AiExtractionException` de inmuebles: separa el detalle técnico
 * (para logs) de un mensaje seguro y accionable para el usuario. Nunca expongas
 * [message] en la UI; usa [userMessage].
 */
class AiException(
    message: String,
    val userMessage: String,
    cause: Throwable? = null,
) : Exception(message, cause)
