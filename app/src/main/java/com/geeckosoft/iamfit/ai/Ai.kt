package com.geeckosoft.iamfit.ai

/**
 * Punto de acceso único a la IA para el resto de la app.
 *
 * Decisión de producto B (2026-10-02): la carga de IA vive en el backend. La app
 * ya NO habla con Groq directamente ni lleva la API key en el APK; el
 * enriquecimiento de alimentos y el consejo de rutinas se consumen vía
 * [com.geeckosoft.iamfit.data.IamFitApi].
 *
 * El registro conversacional quedó archivado, por eso [chat] resuelve a
 * [ArchivedChatProvider].
 */
object Ai {

    val chat: ChatProvider = ArchivedChatProvider()
}
