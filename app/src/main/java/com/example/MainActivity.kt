package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.QuizViewModel
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.getStrings
import com.example.ui.screens.AddCardScreen
import com.example.ui.screens.AllCardsScreen
import com.example.ui.screens.QuizReviewScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    quizViewModel: QuizViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by remember { mutableIntStateOf(0) } // 0: Review, 1: Library, 2: Add, 3: Stats
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Notifications permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // State from ViewModel
    val currentLanguage by quizViewModel.currentLanguage.collectAsStateWithLifecycle()
    val strings = currentLanguage.getStrings()

    val dueQuestions by quizViewModel.dueQuestions.collectAsStateWithLifecycle()
    val allQuestions by quizViewModel.allQuestions.collectAsStateWithLifecycle()
    val activeSessionQuestions by quizViewModel.activeSessionQuestions.collectAsStateWithLifecycle()
    val distinctDecks by quizViewModel.distinctDecks.collectAsStateWithLifecycle()
    val selectedDeck by quizViewModel.selectedDeck.collectAsStateWithLifecycle()
    val currentIndex by quizViewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    val selectedOption by quizViewModel.selectedOption.collectAsStateWithLifecycle()
    val isAnswerRevealed by quizViewModel.isAnswerRevealed.collectAsStateWithLifecycle()
    val lastAssignedQuality by quizViewModel.lastAssignedQuality.collectAsStateWithLifecycle()
    val timeLimitSeconds by quizViewModel.timeLimitSeconds.collectAsStateWithLifecycle()
    val remainingSeconds by quizViewModel.remainingSeconds.collectAsStateWithLifecycle()
    val isTimerRunning by quizViewModel.isTimerRunning.collectAsStateWithLifecycle()
    val isGeneratingAi by quizViewModel.isGeneratingAi.collectAsStateWithLifecycle()
    val generatedAiCard by quizViewModel.generatedAiCard.collectAsStateWithLifecycle()
    val toastMessage by quizViewModel.toastMessage.collectAsStateWithLifecycle()

    // Snackbar notifications
    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            quizViewModel.clearToastMessage()
        }
    }

    // Back handler
    BackHandler(enabled = currentTab != 0) {
        currentTab = 0
    }

    // Layout Direction based on current language
    val layoutDirection = if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalAppLanguage provides currentLanguage,
        LocalLayoutDirection provides layoutDirection
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = strings.appTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showLanguageDialog = true },
                            modifier = Modifier.testTag("btn_change_language")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Text(
                                    text = currentLanguage.flag,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = strings.changeLanguage,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    // Review Tab
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        label = { Text(strings.tabReview) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (dueQuestions.isNotEmpty()) {
                                        Badge { Text("${dueQuestions.size}") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.School, contentDescription = strings.tabReview)
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_review_tab")
                    )

                    // Library Tab
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        label = { Text(strings.tabLibrary) },
                        icon = {
                            Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = strings.tabLibrary)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_library_tab")
                    )

                    // Add / AI / Import Tab
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        label = { Text(strings.tabAdd) },
                        icon = {
                            Icon(Icons.Default.AddCircle, contentDescription = strings.tabAdd)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_add_tab")
                    )

                    // Stats Tab
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        label = { Text(strings.tabStats) },
                        icon = {
                            Icon(Icons.Default.Insights, contentDescription = strings.tabStats)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_stats_tab")
                    )
                }
            }
        ) { innerPadding ->
            val contentModifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)

            when (currentTab) {
                0 -> {
                    QuizReviewScreen(
                        sessionQuestions = activeSessionQuestions,
                        currentIndex = currentIndex,
                        selectedOption = selectedOption,
                        isAnswerRevealed = isAnswerRevealed,
                        lastAssignedQuality = lastAssignedQuality,
                        timeLimitSeconds = timeLimitSeconds,
                        remainingSeconds = remainingSeconds,
                        isTimerRunning = isTimerRunning,
                        availableDecks = distinctDecks,
                        selectedDeck = selectedDeck,
                        strings = strings,
                        onOptionSelected = quizViewModel::onOptionSelected,
                        onRateQuality = quizViewModel::onRateAndSubmitQuality,
                        onNextQuestion = quizViewModel::nextQuestion,
                        onStartNewSession = { quizViewModel.startSmartSession(limit = 20) },
                        onSelectDeck = quizViewModel::selectDeck,
                        onSetTimerDuration = quizViewModel::setTimerDuration,
                        onNavigateToAddCard = { currentTab = 2 },
                        modifier = contentModifier
                    )
                }
                1 -> {
                    AllCardsScreen(
                        questions = allQuestions,
                        strings = strings,
                        onDeleteQuestion = quizViewModel::deleteQuestion,
                        modifier = contentModifier
                    )
                }
                2 -> {
                    AddCardScreen(
                        isGeneratingAi = isGeneratingAi,
                        generatedAiCard = generatedAiCard,
                        availableDecks = distinctDecks,
                        strings = strings,
                        onGenerateAiCard = quizViewModel::generateQuestionWithAi,
                        onSaveGeneratedAiCard = { onSuccess ->
                            quizViewModel.saveGeneratedAiQuestion(onSuccess)
                        },
                        onClearGeneratedAiCard = quizViewModel::clearGeneratedAiCard,
                        onCreateManualCard = { q, opts, idx, exp, deck, onSuccess ->
                            quizViewModel.createManualQuestion(q, opts, idx, exp, deck, onSuccess)
                        },
                        onImportQuestionsJson = { jsonStr, deck, replace, onSuccess, onError ->
                            quizViewModel.importQuestionsFromJson(jsonStr, deck, replace, onSuccess, onError)
                        },
                        onImportQuestionsFromUri = { uri, deck, replace, onSuccess, onError ->
                            quizViewModel.importQuestionsFromUri(uri, deck, replace, onSuccess, onError)
                        },
                        onLoadBundledPack = { packName, deck, replace, onSuccess, onError ->
                            quizViewModel.loadBundledPack(packName, deck, replace, onSuccess, onError)
                        },
                        onCopySchema = quizViewModel::copySchemaToClipboard,
                        onCardAddedSuccess = {
                            currentTab = 1
                        },
                        modifier = contentModifier
                    )
                }
                3 -> {
                    StatsScreen(
                        questions = allQuestions,
                        strings = strings,
                        onTriggerTestNotification = quizViewModel::triggerTestNotification,
                        onResetAllProgress = quizViewModel::resetAllProgress,
                        modifier = contentModifier
                    )
                }
            }
        }

        // Language Selection Dialog
        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.selectLanguageTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AppLanguage.entries.forEach { lang ->
                            val isSelected = lang == currentLanguage
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        quizViewModel.setLanguage(lang)
                                        showLanguageDialog = false
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = lang.flag,
                                        fontSize = 24.sp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = lang.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            quizViewModel.setLanguage(lang)
                                            showLanguageDialog = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text(strings.cancelBtn)
                    }
                }
            )
        }
    }
}
