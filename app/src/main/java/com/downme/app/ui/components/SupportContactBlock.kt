package com.downme.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.downme.app.R

private val ContactLineHeight =
    LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    )

@Composable
fun SupportContactBlock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val websiteUrl = stringResource(R.string.support_website_url).trim()
    val downloadPageUrl = stringResource(R.string.download_page_url).trim()
    val privacyPolicyUrl = stringResource(R.string.privacy_policy_url).trim()
    val supportEmail = stringResource(R.string.support_email).trim()
    val chatUrl = stringResource(R.string.support_chat_url).trim()
    val hasWebsite = websiteUrl.isNotEmpty()
    val hasDownloadPage = downloadPageUrl.isNotEmpty()
    val hasPrivacyPolicy = privacyPolicyUrl.isNotEmpty()
    val hasEmail = supportEmail.isNotEmpty()
    val hasChat = chatUrl.isNotEmpty()
    val bodyStyle =
        MaterialTheme.typography.bodyMedium.copy(
            lineHeight = 24.sp,
            lineHeightStyle = ContactLineHeight,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
        )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text =
                stringResource(
                    if (hasWebsite || hasDownloadPage || hasPrivacyPolicy || hasEmail || hasChat) {
                        R.string.about_contact_body
                    } else {
                        R.string.about_contact_body_pending
                    },
                ),
            style = bodyStyle,
        )
        if (hasWebsite || hasDownloadPage || hasPrivacyPolicy || hasEmail || hasChat) {
            Spacer(modifier = Modifier.height(2.dp))
        }
        if (hasDownloadPage) {
            Button(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadPageUrl)))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(stringResource(R.string.cta_get_app))
                }
            }
        }
        if (hasWebsite) {
            Button(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl)))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Outlined.Language, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(stringResource(R.string.cta_visit_website))
                }
            }
        }
        if (hasPrivacyPolicy) {
            FilledTonalButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl)))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Outlined.Language, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(stringResource(R.string.cta_privacy_policy))
                }
            }
        }
        if (hasEmail) {
            FilledTonalButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$supportEmail")))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(stringResource(R.string.cta_contact_support))
                }
            }
        }
        if (hasChat) {
            FilledTonalButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(chatUrl)))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(stringResource(R.string.cta_contact_support))
                }
            }
        }
    }
}
