package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizItem
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.CorrectGreen
import com.example.ui.theme.CorrectGreenContainer
import com.example.ui.theme.EasyGreen
import com.example.ui.theme.GoodBlue
import com.example.ui.theme.HardOrange
import com.example.ui.theme.WrongRed
import com.example.ui.theme.WrongRedContainer

@Composable
fun QuizReviewScreen(
    sessionQuestions: List<QuizItem>,
    currentIndex: Int,
    selectedOption: Int?,
    isAnswerRevealed: Boolean,
    lastAssignedQuality: Int?,
    timeLimitSeconds: Int,
    remainingSeconds: Int,
    isTimerRunning: Boolean,
    availableDecks: List<String>,
    selectedDeck: String?,
    strings: AppStrings,
    onOptionSelected: (Int) -> Unit,
    onRateQuality: (QuizItem, Int) -> Unit,
    onNextQuestion: () -> Unit,
    onStartNewSession: () -> Unit,
    onSelectDeck: (String?) -> Unit,
    onSetTimerDuration: (Int) -> Unit,
    onNavigateToAddCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLanguage = LocalAppLanguage.current

    if (sessionQuestions.isEmpty()) {
        EmptyReviewState(
            strings = strings,
            onStartNewSession = onStartNewSession,
            onNavigateToAddCard = onNavigateToAddCard,
            modifier = modifier
        )
    } else {
        val safeIndex = currentIndex.coerceIn(0, sessionQuestions.size - 1)
        val currentItem = sessionQuestions[safeIndex]

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Deck Selector Chips
            if (availableDecks.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = selectedDeck == null,
                            onClick = { onSelectDeck(null) },
                            label = { Text(strings.allDecksLabel) },
                            leadingIcon = {
                                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                    items(availableDecks) { deck ->
                        FilterChip(
                            selected = selectedDeck == deck,
                            onClick = { onSelectDeck(deck) },
                            label = { Text(deck) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Header & Timer Section
            ReviewHeader(
                currentIndex = safeIndex + 1,
                totalCount = sessionQuestions.size,
                repetition = currentItem.repetition,
                intervalDays = currentItem.interval,
                timeLimitSeconds = timeLimitSeconds,
                remainingSeconds = remainingSeconds,
                isTimerRunning = isTimerRunning,
                strings = strings,
                onSetTimerDuration = onSetTimerDuration,
                onStartNewSession = onStartNewSession
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Main Question Card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Deck & Category Badge
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentItem.deckName.ifBlank { "Medborgarskap" },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Question Text
                    val primaryQuestion = currentItem.getLocalizedQuestion(currentLanguage.code)
                    Text(
                        text = primaryQuestion,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 26.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    // Secondary translation if available
                    val secondaryQuestion = when (currentLanguage.code) {
                        "sv" -> currentItem.questionAr.ifBlank { currentItem.questionEn }
                        "ar" -> currentItem.questionSv.ifBlank { currentItem.questionEn }
                        else -> currentItem.questionSv.ifBlank { currentItem.questionAr }
                    }

                    if (secondaryQuestion.isNotBlank() && secondaryQuestion != primaryQuestion) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = secondaryQuestion,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = FontStyle.Italic,
                                lineHeight = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Options list
                    currentItem.options.forEachIndexed { index, optionText ->
                        OptionButton(
                            optionIndex = index,
                            optionText = optionText,
                            isSelected = selectedOption == index,
                            isAnswerRevealed = isAnswerRevealed,
                            isCorrect = index == currentItem.correctIndex,
                            onClick = { onOptionSelected(index) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            // Answer explanation & Time-weighted SM-2 section
            AnimatedVisibility(
                visible = isAnswerRevealed,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 40 })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Next Question button
                    Button(
                        onClick = onNextQuestion,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_next_question"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = strings.nextQuestionBtn,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Explanation Card
                    if (currentItem.explanation.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("explanation_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = strings.explanationLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentItem.explanation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // SM-2 Manual Override section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sm2_rating_section"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.rateRecallPrompt,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (lastAssignedQuality != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "التقييم التلقائي بالوقت: Q=$lastAssignedQuality",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Sm2Button(
                                    label = strings.rateAgain,
                                    subtext = "Q=1",
                                    backgroundColor = WrongRed,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sm2_again_button"),
                                    onClick = { onRateQuality(currentItem, 1) }
                                )

                                Sm2Button(
                                    label = strings.rateHard,
                                    subtext = "Q=2",
                                    backgroundColor = HardOrange,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sm2_hard_button"),
                                    onClick = { onRateQuality(currentItem, 2) }
                                )

                                Sm2Button(
                                    label = strings.rateGood,
                                    subtext = "Q=4",
                                    backgroundColor = GoodBlue,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sm2_good_button"),
                                    onClick = { onRateQuality(currentItem, 4) }
                                )

                                Sm2Button(
                                    label = strings.rateEasy,
                                    subtext = "Q=5",
                                    backgroundColor = EasyGreen,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sm2_easy_button"),
                                    onClick = { onRateQuality(currentItem, 5) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReviewHeader(
    currentIndex: Int,
    totalCount: Int,
    repetition: Int,
    intervalDays: Int,
    timeLimitSeconds: Int,
    remainingSeconds: Int,
    isTimerRunning: Boolean,
    strings: AppStrings,
    onSetTimerDuration: (Int) -> Unit,
    onStartNewSession: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.questionProgress(currentIndex, totalCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Restart Session button
                IconButton(onClick = onStartNewSession, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = strings.newSessionBtn,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${strings.repetitionBadge(repetition)} | ${strings.intervalBadge(intervalDays)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { currentIndex.toFloat() / totalCount.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Timer row & speed controller
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timer countdown badge
            if (timeLimitSeconds > 0) {
                val timerColor = when {
                    remainingSeconds <= 3 -> WrongRed
                    remainingSeconds <= 6 -> HardOrange
                    else -> MaterialTheme.colorScheme.primary
                }

                Surface(
                    color = timerColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, timerColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isTimerRunning) {
                            CircularProgressIndicator(
                                progress = { remainingSeconds.toFloat() / timeLimitSeconds.toFloat() },
                                modifier = Modifier.size(16.dp),
                                color = timerColor,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = timerColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = "${remainingSeconds}s",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = timerColor
                        )
                    }
                }
            } else {
                Text(
                    text = "المؤقت: معطل",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Timer duration toggle chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(0 to "Off", 10 to "10s", 15 to "15s", 25 to "25s").forEach { (sec, label) ->
                    val isSelected = timeLimitSeconds == sec
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSetTimerDuration(sec) },
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionButton(
    optionIndex: Int,
    optionText: String,
    isSelected: Boolean,
    isAnswerRevealed: Boolean,
    isCorrect: Boolean,
    onClick: () -> Unit
) {
    val language = LocalAppLanguage.current
    val optionLetters = if (language.isRtl) {
        listOf("أ", "ب", "ج", "د", "هـ", "و")
    } else {
        listOf("A", "B", "C", "D", "E", "F")
    }
    val letter = optionLetters.getOrElse(optionIndex) { "${optionIndex + 1}" }

    val (containerColor, borderColor, contentColor) = when {
        isAnswerRevealed && isCorrect -> {
            Triple(CorrectGreenContainer, CorrectGreen, CorrectGreen)
        }
        isAnswerRevealed && isSelected && !isCorrect -> {
            Triple(WrongRedContainer, WrongRed, WrongRed)
        }
        isSelected -> {
            Triple(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primary
            )
        }
        else -> {
            Triple(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                MaterialTheme.colorScheme.onSurface
            )
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !isAnswerRevealed, onClick = onClick)
            .testTag("option_$optionIndex"),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected || (isAnswerRevealed && isCorrect)) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = if (isSelected || (isAnswerRevealed && isCorrect)) borderColor else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isAnswerRevealed && isCorrect) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (isAnswerRevealed && isSelected) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = letter,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = optionText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Sm2Button(
    label: String,
    subtext: String,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun EmptyReviewState(
    strings: AppStrings,
    onStartNewSession: () -> Unit,
    onNavigateToAddCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(46.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = strings.noCardsDueTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = strings.noCardsDueDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onStartNewSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_start_new_session"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.newSessionBtn, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateToAddCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("empty_state_add_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.addNewCard)
            }
        }
    }
}
