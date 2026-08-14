package com.arunrk.simplenote.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arunrk.simplenote.core.error.AppError
import com.arunrk.simplenote.core.error.isRetryable
import com.arunrk.simplenote.presentation.error.displayMessage
import com.arunrk.simplenote.presentation.error.displayTitle

/**
 * The three non-content states every data-backed screen needs.
 *
 * Kept together because they must stay visually consistent, and because having one place for
 * them makes it obvious that a screen has handled all three.
 */

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    CentredMessage(
        modifier = modifier,
        title = title,
        description = description,
    )
}

/**
 * A failure the user can act on.
 *
 * The retry button appears only when retrying could plausibly help — [AppError.isRetryable]
 * decides, so a validation failure does not offer an action that would fail identically.
 */
@Composable
fun ErrorState(
    error: AppError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CentredMessage(
        modifier = modifier,
        title = error.displayTitle(),
        description = error.displayMessage(),
        titleColor = MaterialTheme.colorScheme.error,
        action = if (error.isRetryable) {
            { Button(onClick = onRetry) { Text("Try again") } }
        } else {
            null
        },
    )
}

@Composable
private fun CentredMessage(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    action: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                textAlign = TextAlign.Center,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            action?.let {
                Box(modifier = Modifier.padding(top = 8.dp)) { it() }
            }
        }
    }
}
