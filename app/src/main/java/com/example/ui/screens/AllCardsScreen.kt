package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.QuizItem
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.CorrectGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CardFilter {
    ALL,
    DUE_TODAY,
    IN_PROGRESS,
    MASTERED
}

@Composable
fun AllCardsScreen(
    questions: List<QuizItem>,
    strings: AppStrings,
    onDeleteQuestion: (QuizItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CardFilter.ALL) }
    var itemToDelete by remember { mutableStateOf<QuizItem?>(null) }

    val currentTime = System.currentTimeMillis()

    val filteredList = remember(questions, searchQuery, selectedFilter) {
        questions.filter { item ->
            val matchesSearch = item.question.contains(searchQuery, ignoreCase = true) ||
                    item.explanation.contains(searchQuery, ignoreCase = true) ||
                    item.options.any { it.contains(searchQuery, ignoreCase = true) }
            val matchesFilter = when (selectedFilter) {
                CardFilter.ALL -> true
                CardFilter.DUE_TODAY -> item.nextReviewDate <= currentTime
                CardFilter.IN_PROGRESS -> item.nextReviewDate > currentTime && item.repetition < 3
                CardFilter.MASTERED -> item.repetition >= 3 || item.interval >= 21
            }
            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_cards_field"),
            placeholder = { Text(strings.searchPlaceholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == CardFilter.ALL,
                    onClick = { selectedFilter = CardFilter.ALL },
                    label = { Text(strings.filterAll(questions.size)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
            item {
                val dueCount = questions.count { it.nextReviewDate <= currentTime }
                FilterChip(
                    selected = selectedFilter == CardFilter.DUE_TODAY,
                    onClick = { selectedFilter = CardFilter.DUE_TODAY },
                    label = { Text(strings.filterDue(dueCount)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
            item {
                val inProgressCount = questions.count { it.nextReviewDate > currentTime && it.repetition < 3 }
                FilterChip(
                    selected = selectedFilter == CardFilter.IN_PROGRESS,
                    onClick = { selectedFilter = CardFilter.IN_PROGRESS },
                    label = { Text(strings.filterLearning(inProgressCount)) }
                )
            }
            item {
                val masteredCount = questions.count { it.repetition >= 3 || it.interval >= 21 }
                FilterChip(
                    selected = selectedFilter == CardFilter.MASTERED,
                    onClick = { selectedFilter = CardFilter.MASTERED },
                    label = { Text(strings.filterMastered(masteredCount)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.noCardsFound,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    CardItemRow(
                        item = item,
                        currentTime = currentTime,
                        strings = strings,
                        onDeleteClick = { itemToDelete = item }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(strings.deleteCardTitle) },
            text = { Text(strings.deleteCardMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteQuestion(target)
                        itemToDelete = null
                    }
                ) {
                    Text(strings.deleteBtn, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(strings.cancelBtn)
                }
            }
        )
    }
}

@Composable
private fun CardItemRow(
    item: QuizItem,
    currentTime: Long,
    strings: AppStrings,
    onDeleteClick: () -> Unit
) {
    val isDue = item.nextReviewDate <= currentTime
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val formattedDate = remember(item.nextReviewDate) {
        dateFormat.format(Date(item.nextReviewDate))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_row_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.deckName.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = item.deckName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Surface(
                        color = if (isDue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isDue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isDue) strings.dueTodayLabel else "${strings.nextReviewLabel} $formattedDate",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = strings.deleteBtn,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.question,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            val correctAnswer = item.options.getOrNull(item.correctIndex) ?: ""
            if (correctAnswer.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = CorrectGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = correctAnswer,
                        style = MaterialTheme.typography.bodySmall,
                        color = CorrectGreen,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${strings.intervalBadge(item.interval)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${strings.repetitionLabel} ${item.repetition}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${strings.easeLabel} ${"%.2f".format(item.easeFactor)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
