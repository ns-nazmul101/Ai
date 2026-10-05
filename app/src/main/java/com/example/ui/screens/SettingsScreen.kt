package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.DevilViewModel
import com.example.ui.theme.DevilAmber
import com.example.ui.theme.DevilCyan
import com.example.ui.theme.DevilGreen
import com.example.ui.theme.DevilRed

@Composable
fun SettingsScreen(
    viewModel: DevilViewModel,
    modifier: Modifier = Modifier
) {
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val femalePreferred by viewModel.femaleVoicePreferred.collectAsStateWithLifecycle()
    val ttsPitch by viewModel.ttsPitch.collectAsStateWithLifecycle()
    val ttsRate by viewModel.ttsSpeechRate.collectAsStateWithLifecycle()
    val isBengali = currentLanguage.startsWith("bn", ignoreCase = true)

    val isGeminiConfigured = BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isBengali) "সেটিংস ও কনফিগারেশন" else "Settings & Voice Config",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isBengali) "ভাষা, কৃত্রিম স্বর এবং এআই কনফিগার করুন" else "Customize assistant language, TTS voice persona, and intelligence preferences.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Language Selection Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("language_selection_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = "Language", tint = DevilCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBengali) "ভাষা নির্বাচন" else "Assistant Language",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // English option
                        LanguageOptionItem(
                            title = "English",
                            subtitle = "en-US",
                            isSelected = !isBengali,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setLanguage("en-US") }
                        )

                        // Bengali option
                        LanguageOptionItem(
                            title = "বাংলা (Bengali)",
                            subtitle = "bn-BD",
                            isSelected = isBengali,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setLanguage("bn-BD") }
                        )
                    }
                }
            }
        }

        // 2. Voice & TTS Persona Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_settings_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = "Voice", tint = DevilRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBengali) "টেক্সট-টু-স্পিচ (ভয়েস)" else "Text-to-Speech (Voice Persona)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Female voice toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBengali) "মহিলা কণ্ঠস্বর (Female Voice)" else "Female-style Voice",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = if (isBengali) "স্বাভাবিক নারী স্বর অগ্রাধিকার পাবে।" else "Prefers female-style voice profiles provided by Android TTS.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = femalePreferred,
                            onCheckedChange = { viewModel.setFemaleVoice(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = DevilRed
                            ),
                            modifier = Modifier.testTag("female_voice_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Voice Pitch Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = if (isBengali) "স্বর পিচ (Pitch)" else "Voice Pitch", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "${String.format("%.1f", ttsPitch)}x", style = MaterialTheme.typography.labelMedium, color = DevilCyan)
                    }
                    Slider(
                        value = ttsPitch,
                        onValueChange = { viewModel.setPitch(it) },
                        valueRange = 0.6f..1.6f,
                        colors = SliderDefaults.colors(thumbColor = DevilCyan, activeTrackColor = DevilCyan)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Speech Rate Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = if (isBengali) "কথার গতি (Speech Rate)" else "Speech Rate", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "${String.format("%.1f", ttsRate)}x", style = MaterialTheme.typography.labelMedium, color = DevilAmber)
                    }
                    Slider(
                        value = ttsRate,
                        onValueChange = { viewModel.setSpeechRate(it) },
                        valueRange = 0.6f..1.6f,
                        colors = SliderDefaults.colors(thumbColor = DevilAmber, activeTrackColor = DevilAmber)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.testTtsVoice() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_voice_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Preview", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBengali) "ভয়েস প্রিভিউ শুনুন" else "Test Voice Speech",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Gemini AI Engine Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = DevilAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBengali) "এআই মডেল ও ইঞ্জিন" else "AI Model & Natural Language",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isGeminiConfigured) DevilGreen else DevilAmber)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isGeminiConfigured) "Gemini 3.5 Flash Online" else "Local Smart Intent Engine (Offline Mode Active)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isGeminiConfigured) DevilGreen else DevilAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isGeminiConfigured) {
                            "Connected to Google Gemini API for deep conversational understanding and complex reasoning."
                        } else {
                            "Built-in bilingual offline parser is handling all phone commands, device diagnostics, apps, and shell actions instantly. To enable open-ended web reasoning, set GEMINI_API_KEY in the AI Studio Secrets panel."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. Data Management Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Data", tint = DevilRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBengali) "ডাটা ও হিস্টোরি" else "Data & Privacy",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.clearChatHistory() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_chat_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isBengali) "চ্যাট ইতিহাস মুছে ফেলুন" else "Clear Conversation History", color = DevilRed)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.clearLogs() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isBengali) "টার্মিনাল লগ মুছে ফেলুন" else "Clear Action Execution Logs", color = DevilRed)
                    }
                }
            }
        }

        // 5. About Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "About", tint = DevilCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "About Devil AI",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Devil AI v1.0.0 — Production-grade AI Voice Assistant for Android. Designed with security guards, official accessibility automations, and root execution diagnostics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LanguageOptionItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) DevilRed.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                1.5.dp,
                if (isSelected) DevilRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) DevilRed else MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = DevilRed, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
