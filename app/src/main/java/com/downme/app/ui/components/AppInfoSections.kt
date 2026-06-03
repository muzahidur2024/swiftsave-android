package com.downme.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.downme.app.R

@Composable
fun HowToDownloadSection(
    onOpenTutorial: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InfoCard(
        modifier = modifier,
        title = stringResource(R.string.settings_help_section_title),
    ) {
        VisualDownloadGuideStrip()
        Text(
            stringResource(R.string.settings_visual_guide_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        FilledTonalButton(
            onClick = onOpenTutorial,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.settings_open_full_guide))
            }
        }
    }
}

@Composable
fun AboutDownMeSection(modifier: Modifier = Modifier) {
    val bodyStyle =
        MaterialTheme.typography.bodyMedium.copy(
            lineHeight = 22.sp,
            lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = Trim.None,
                ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
        )
    val titleStyle =
        MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
        )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.about_section_header),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        )
        InfoCard(title = stringResource(R.string.about_app_title), titleStyle = titleStyle) {
            Text(text = stringResource(R.string.about_app_body), style = bodyStyle)
        }
        InfoCard(title = stringResource(R.string.about_disclaimer_title), titleStyle = titleStyle) {
            Text(
                text = stringResource(R.string.about_disclaimer_body),
                style = bodyStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}

@Composable
fun LegalSupportSection(modifier: Modifier = Modifier) {
    InfoCard(
        modifier = modifier,
        title = stringResource(R.string.legal_support_title),
    ) {
        SupportContactBlock()
    }
}

@Composable
private fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
    ),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title, style = titleStyle, modifier = Modifier.fillMaxWidth())
            content()
        }
    }
}
