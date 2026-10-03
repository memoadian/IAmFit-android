package com.geeckosoft.iamfit.ui.screens.log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.geeckosoft.iamfit.ai.Ai
import com.geeckosoft.iamfit.ai.AiException
import kotlinx.coroutines.launch

private data class ChatBubble(val text: String, val fromUser: Boolean, val isError: Boolean = false)

private const val LOG_SYSTEM_PROMPT =
    "Eres el asistente de registro de IAm-fit. El usuario te describe algo que comió o " +
        "un entrenamiento que hizo, en lenguaje natural. Responde en español, breve (máximo 2 " +
        "frases), confirmando qué registraste y dando una estimación aproximada de calorías o " +
        "esfuerzo si aplica. No pidas más datos salvo que sea imprescindible."

@Composable
fun LogScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val messages = remember {
        mutableStateListOf(
            ChatBubble(
                text = "Cuéntame qué comiste o qué entrenamiento hiciste y lo registro por ti.",
                fromUser = false,
            ),
        )
    }
    var input by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("Registrar con IA", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Describe tu comida o entrenamiento en tus palabras",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(messages) { _, bubble ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 3 },
                ) {
                    ChatBubbleRow(bubble)
                }
            }
            if (isSending) {
                item { TypingIndicator() }
            }
        }

        Surface(
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ej. 2 huevos y un café con leche") },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                    ),
                    singleLine = true,
                )
                Spacer(Modifier.width(8.dp))
                val canSend = input.isNotBlank() && !isSending
                IconButton(
                    onClick = {
                        val text = input.trim()
                        if (text.isEmpty()) return@IconButton
                        input = ""
                        messages.add(ChatBubble(text = text, fromUser = true))
                        isSending = true
                        scope.launch {
                            val bubble = try {
                                val result = Ai.chat.complete(systemPrompt = LOG_SYSTEM_PROMPT, userText = text)
                                ChatBubble(text = result.content.trim(), fromUser = false)
                            } catch (e: AiException) {
                                ChatBubble(text = e.userMessage, fromUser = false, isError = true)
                            } catch (e: Exception) {
                                ChatBubble(text = "Error inesperado: ${e.message}", fromUser = false, isError = true)
                            }
                            messages.add(bubble)
                            isSending = false
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                        ),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Enviar",
                        tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleRow(bubble: ChatBubble) {
    val alignment = if (bubble.fromUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = when {
        bubble.isError -> MaterialTheme.colorScheme.errorContainer
        bubble.fromUser -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        bubble.isError -> MaterialTheme.colorScheme.onErrorContainer
        bubble.fromUser -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val shape = if (bubble.fromUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (!bubble.fromUser) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 6.dp, bottom = 6.dp)
                        .size(18.dp),
                )
            }
            Surface(color = bubbleColor, shape = shape, shadowElevation = 1.dp) {
                Text(
                    bubble.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(end = 6.dp)
                .size(18.dp),
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            }
        }
    }
}
