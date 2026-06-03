package com.downme.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.downme.app.R
import com.downme.app.ui.screens.MUSIC_QUALITY_PRESETS
import com.downme.app.ui.screens.QualityPreset
import com.downme.app.ui.screens.VIDEO_QUALITY_PRESETS

@Composable
fun QualityPickerSheetContent(
    selectedQuality: String,
    onQualityChange: (String) -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.quality_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            QualityPickerGrid(
                title = stringResource(R.string.quality_sheet_video),
                qualities = VIDEO_QUALITY_PRESETS,
                selectedQuality = selectedQuality,
                onQualityChange = onQualityChange,
                columns = 2,
            )
            QualityPickerGrid(
                title = stringResource(R.string.quality_sheet_music),
                qualities = MUSIC_QUALITY_PRESETS,
                selectedQuality = selectedQuality,
                onQualityChange = onQualityChange,
                columns = 1,
            )
            Button(
                onClick = onDownload,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    stringResource(R.string.start_download),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun QualityPickerGrid(
    title: String,
    qualities: List<QualityPreset>,
    selectedQuality: String,
    onQualityChange: (String) -> Unit,
    columns: Int,
) {
    if (qualities.isEmpty()) return
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
    qualities.chunked(columns).forEach { rowItems ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rowItems.forEach { quality ->
                QualityPickerOption(
                    quality = quality,
                    selected = selectedQuality == quality.key,
                    onClick = { onQualityChange(quality.key) },
                    modifier = Modifier.weight(1f),
                )
            }
            repeat(columns - rowItems.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QualityPickerOption(
    quality: QualityPreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = MaterialTheme.shapes.medium,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        border =
            if (selected) {
                null
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                quality.label,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }
    }
}
