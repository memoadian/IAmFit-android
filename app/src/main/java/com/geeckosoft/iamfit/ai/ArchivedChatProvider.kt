package com.geeckosoft.iamfit.ai

/**
 * El registro conversacional (texto libre -> IA) quedó archivado por la decisión
 * de producto B. Se conserva el contrato [ChatProvider] para no romper la UI de
 * chat, pero ya no se hace ninguna llamada a un LLM desde el cliente: la IA vive
 * en el backend y la app la consume vía endpoints.
 */
class ArchivedChatProvider : ChatProvider {

    override val name: String = "archived"

    override suspend fun complete(
        systemPrompt: String,
        userText: String,
        jsonSchema: String?,
    ): ChatResult {
        throw AiException(
            "Registro conversacional archivado (decisión B).",
            "El registro por chat ya no está disponible. Usa el diario de alimentos o la búsqueda de alimentos.",
        )
    }
}
