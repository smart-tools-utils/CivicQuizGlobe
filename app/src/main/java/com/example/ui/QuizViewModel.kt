package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiCardGenerator
import com.example.data.assets.AssetIngestion
import com.example.data.db.AppDatabase
import com.example.data.model.QuizItem
import com.example.data.repository.QuizRepository
import com.example.data.schema.AppSchemaGenerator
import com.example.logic.SM2Calculator
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LanguageManager
import com.example.ui.i18n.getStrings
import com.example.worker.WorkScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing multi-decks, smart on-demand random sessions,
 * countdown timer with time-weighted SM-2 calculations, and schema export.
 */
class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: QuizRepository

    // Current app language
    private val _currentLanguage = MutableStateFlow(LanguageManager.getSavedLanguage(application))
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    // All available decks in database
    val distinctDecks: StateFlow<List<String>>

    // Selected deck filter (null means All Decks)
    private val _selectedDeck = MutableStateFlow<String?>(null)
    val selectedDeck: StateFlow<String?> = _selectedDeck.asStateFlow()

    // Questions due today (traditional daily queue)
    val dueQuestions: StateFlow<List<QuizItem>>

    // All questions in library
    val allQuestions: StateFlow<List<QuizItem>>

    // Active session batch (Smart On-Demand Session)
    private val _activeSessionQuestions = MutableStateFlow<List<QuizItem>>(emptyList())
    val activeSessionQuestions: StateFlow<List<QuizItem>> = _activeSessionQuestions.asStateFlow()

    // Current question index in active session
    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    // Currently selected option index
    private val _selectedOption = MutableStateFlow<Int?>(null)
    val selectedOption: StateFlow<Int?> = _selectedOption.asStateFlow()

    // Is answer revealed
    private val _isAnswerRevealed = MutableStateFlow(false)
    val isAnswerRevealed: StateFlow<Boolean> = _isAnswerRevealed.asStateFlow()

    // Last calculated quality grade (0..5)
    private val _lastAssignedQuality = MutableStateFlow<Int?>(null)
    val lastAssignedQuality: StateFlow<Int?> = _lastAssignedQuality.asStateFlow()

    // --- TIMER & TIME-WEIGHTED EVALUATION STATE ---
    // Timer setting per question in seconds (0 = disabled, 10, 15, 20, 30)
    private val _timeLimitSeconds = MutableStateFlow(15)
    val timeLimitSeconds: StateFlow<Int> = _timeLimitSeconds.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(15)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private var timerJob: Job? = null
    private var questionStartTimeMillis: Long = 0L

    // AI card generation state
    private val _isGeneratingAi = MutableStateFlow(false)
    val isGeneratingAi: StateFlow<Boolean> = _isGeneratingAi.asStateFlow()

    private val _generatedAiCard = MutableStateFlow<QuizItem?>(null)
    val generatedAiCard: StateFlow<QuizItem?> = _generatedAiCard.asStateFlow()

    // Transient feedback messages
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        val database = AppDatabase.getInstance(application)
        repository = QuizRepository(database.quizDao())

        val currentTime = System.currentTimeMillis()

        distinctDecks = repository.distinctDecks
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = listOf("Medborgarskap")
            )

        dueQuestions = repository.getDueQuestions(currentTime)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        allQuestions = repository.allQuestions
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // First launch ingestion
        viewModelScope.launch {
            AssetIngestion.ingestQuestionsIfEmpty(application, repository)
            // Auto-start initial smart session
            startSmartSession(limit = 20)
        }

        WorkScheduler.scheduleDailyReviewReminder(application)
    }

    /**
     * Start a smart on-demand review session (Smart Random Batching):
     * Overcomes the 24h barrier by prioritizing:
     * 1. Due questions
     * 2. New unreviewed questions
     * 3. Lowest easeFactor / hard questions
     * Randomly shuffled to limit items.
     */
    fun startSmartSession(deckName: String? = _selectedDeck.value, limit: Int = 20) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val batch = repository.getSmartSessionBatch(currentTime, limit, deckName)
            _activeSessionQuestions.value = batch
            _currentQuestionIndex.value = 0
            _selectedOption.value = null
            _isAnswerRevealed.value = false
            _lastAssignedQuality.value = null
            startQuestionTimer()
        }
    }

    /**
     * Set active deck filter.
     */
    fun selectDeck(deckName: String?) {
        _selectedDeck.value = deckName
        startSmartSession(deckName = deckName, limit = 20)
    }

    /**
     * Change timer duration setting (0 = disabled, 10, 15, 20, 30 seconds).
     */
    fun setTimerDuration(seconds: Int) {
        _timeLimitSeconds.value = seconds
        _remainingSeconds.value = seconds
        restartTimerForCurrentQuestion()
    }

    /**
     * Starts or resets the countdown timer for the active question.
     */
    fun startQuestionTimer() {
        timerJob?.cancel()
        val limit = _timeLimitSeconds.value

        if (limit <= 0) {
            _isTimerRunning.value = false
            _remainingSeconds.value = 0
            questionStartTimeMillis = System.currentTimeMillis()
            return
        }

        _remainingSeconds.value = limit
        _isTimerRunning.value = true
        questionStartTimeMillis = System.currentTimeMillis()

        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000L)
                if (!_isAnswerRevealed.value) {
                    _remainingSeconds.value -= 1
                }
            }

            // Time out condition reached
            if (!_isAnswerRevealed.value) {
                onQuestionTimedOut()
            }
        }
    }

    private fun restartTimerForCurrentQuestion() {
        if (!_isAnswerRevealed.value) {
            startQuestionTimer()
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = false
    }

    /**
     * User selected an option. Stops timer, reveals answer,
     * and automatically evaluates Time-Weighted SM-2.
     */
    fun onOptionSelected(index: Int) {
        if (_isAnswerRevealed.value) return

        stopTimer()
        _selectedOption.value = index
        _isAnswerRevealed.value = true

        val currentItem = getCurrentItem() ?: return
        val isCorrect = index == currentItem.correctIndex
        val timeLimitMillis = _timeLimitSeconds.value * 1000L
        val timeTakenMillis = (System.currentTimeMillis() - questionStartTimeMillis).coerceAtLeast(100L)

        // Apply Time-Weighted SM-2
        val (updatedItem, quality) = SM2Calculator.calculateSM2WithTime(
            item = currentItem,
            isCorrect = isCorrect,
            isTimedOut = false,
            timeTakenMillis = timeTakenMillis,
            timeLimitMillis = timeLimitMillis
        )

        _lastAssignedQuality.value = quality

        viewModelScope.launch {
            repository.update(updatedItem)
            val strings = _currentLanguage.value.getStrings()
            _toastMessage.value = if (isCorrect) {
                if (quality == 5) "⚡ ${strings.rateEasy} (Q=5)" else "👍 ${strings.rateGood} (Q=$quality)"
            } else {
                "❌ ${strings.rateAgain} (Q=$quality)"
            }
        }
    }

    /**
     * Called when the countdown timer hits 0 without an answer.
     */
    private fun onQuestionTimedOut() {
        stopTimer()
        _isAnswerRevealed.value = true
        val currentItem = getCurrentItem() ?: return

        // Timeout treated as quality 0 (Blackout)
        val (updatedItem, quality) = SM2Calculator.calculateSM2WithTime(
            item = currentItem,
            isCorrect = false,
            isTimedOut = true,
            timeTakenMillis = _timeLimitSeconds.value * 1000L,
            timeLimitMillis = _timeLimitSeconds.value * 1000L
        )

        _lastAssignedQuality.value = quality

        viewModelScope.launch {
            repository.update(updatedItem)
            _toastMessage.value = "⏰ انتهى الوقت! (Time out -> Q=0)"
        }
    }

    /**
     * Reveal answer manually if needed.
     */
    fun revealAnswer() {
        if (_selectedOption.value != null && !_isAnswerRevealed.value) {
            onOptionSelected(_selectedOption.value!!)
        }
    }

    /**
     * Manual SM-2 grade adjustment if user overrides the time-weighted grade.
     */
    fun onRateAndSubmitQuality(item: QuizItem, quality: Int) {
        viewModelScope.launch {
            val updatedItem = SM2Calculator.calculateSM2(item, quality)
            repository.update(updatedItem)
            _lastAssignedQuality.value = quality

            val strings = _currentLanguage.value.getStrings()
            _toastMessage.value = if (quality >= 3) {
                strings.toastReviewScheduled(updatedItem.interval)
            } else {
                strings.toastReviewTomorrow
            }
        }
    }

    /**
     * Advance to the next question in the session and restart timer.
     */
    fun nextQuestion() {
        val total = _activeSessionQuestions.value.size
        if (_currentQuestionIndex.value < total - 1) {
            _currentQuestionIndex.value += 1
            _selectedOption.value = null
            _isAnswerRevealed.value = false
            _lastAssignedQuality.value = null
            startQuestionTimer()
        }
    }

    fun getCurrentItem(): QuizItem? {
        val list = _activeSessionQuestions.value
        val index = _currentQuestionIndex.value
        return if (list.isNotEmpty() && index in list.indices) list[index] else null
    }

    /**
     * Copy standardized AI Studio JSON Schema to clipboard.
     */
    fun copySchemaToClipboard() {
        val copied = AppSchemaGenerator.copySchemaToClipboard(getApplication())
        _toastMessage.value = if (copied) {
            "تم نسخ قالب الـ JSON Schema للحافظة بنجاح! 📋"
        } else {
            "تعذر النسخ للحافظة"
        }
    }

    /**
     * Change app language and persist selection.
     */
    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        LanguageManager.saveLanguage(getApplication(), language)
    }

    /**
     * Manually create a new card.
     */
    fun createManualQuestion(
        question: String,
        options: List<String>,
        correctIndex: Int,
        explanation: String,
        deckName: String = "Medborgarskap",
        onSuccess: () -> Unit
    ) {
        val strings = _currentLanguage.value.getStrings()
        if (question.isBlank() || options.size < 2 || options.any { it.isBlank() }) {
            _toastMessage.value = strings.toastFillRequiredFields
            return
        }

        viewModelScope.launch {
            val newItem = QuizItem(
                id = 0,
                question = question.trim(),
                options = options.map { it.trim() },
                correctIndex = correctIndex.coerceIn(0, options.size - 1),
                explanation = explanation.trim(),
                deckName = deckName.ifBlank { "Medborgarskap" }.trim(),
                interval = 0,
                repetition = 0,
                easeFactor = 2.5f,
                nextReviewDate = System.currentTimeMillis()
            )
            repository.insert(newItem)
            _toastMessage.value = strings.toastCardSaved
            onSuccess()
        }
    }

    /**
     * Generate citizenship question with AI.
     */
    fun generateQuestionWithAi(topic: String) {
        viewModelScope.launch {
            val strings = _currentLanguage.value.getStrings()
            _isGeneratingAi.value = true
            _generatedAiCard.value = null
            try {
                val generated = AiCardGenerator.generateQuizItem(topic)
                _generatedAiCard.value = generated
                _toastMessage.value = strings.toastAiGenerated
            } catch (e: Exception) {
                _toastMessage.value = strings.toastAiFailed(e.message ?: "Unknown error")
            } finally {
                _isGeneratingAi.value = false
            }
        }
    }

    /**
     * Save generated AI question to library.
     */
    fun saveGeneratedAiQuestion(onSuccess: () -> Unit) {
        val item = _generatedAiCard.value ?: return
        viewModelScope.launch {
            val strings = _currentLanguage.value.getStrings()
            repository.insert(item)
            _generatedAiCard.value = null
            _toastMessage.value = strings.toastCardSaved
            onSuccess()
        }
    }

    fun clearGeneratedAiCard() {
        _generatedAiCard.value = null
    }

    /**
     * Delete question.
     */
    fun deleteQuestion(item: QuizItem) {
        viewModelScope.launch {
            val strings = _currentLanguage.value.getStrings()
            repository.delete(item)
            _toastMessage.value = strings.toastCardDeleted
        }
    }

    /**
     * Import questions from a JSON string under a specific deckName.
     */
    fun importQuestionsFromJson(
        jsonString: String,
        targetDeckName: String = "Medborgarskap",
        replaceExisting: Boolean = false,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val items = AssetIngestion.parseQuestionsJson(
                    jsonString = jsonString,
                    defaultDeckName = targetDeckName.ifBlank { "Medborgarskap" }
                )
                if (items.isEmpty()) {
                    onError("No valid questions found in JSON array.")
                    return@launch
                }

                if (replaceExisting) {
                    if (targetDeckName.isNotBlank() && targetDeckName != "All") {
                        repository.deleteDeck(targetDeckName)
                    } else {
                        repository.deleteAll()
                    }
                }

                repository.insertAll(items)
                startSmartSession(deckName = _selectedDeck.value, limit = 20)

                val strings = _currentLanguage.value.getStrings()
                _toastMessage.value = strings.toastImportSuccess(items.size)
                onSuccess(items.size)
            } catch (e: Exception) {
                val strings = _currentLanguage.value.getStrings()
                val errorMsg = e.localizedMessage ?: "Invalid JSON format"
                _toastMessage.value = strings.toastImportError(errorMsg)
                onError(errorMsg)
            }
        }
    }

    /**
     * Import questions from picked file URI under a specific deckName.
     */
    fun importQuestionsFromUri(
        uri: Uri,
        targetDeckName: String = "Medborgarskap",
        replaceExisting: Boolean = false,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (jsonString.isNullOrBlank()) {
                    onError("The selected file is empty.")
                    return@launch
                }
                importQuestionsFromJson(jsonString, targetDeckName, replaceExisting, onSuccess, onError)
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Failed to read file"
                val strings = _currentLanguage.value.getStrings()
                _toastMessage.value = "${strings.fileReadError}: $errorMsg"
                onError(errorMsg)
            }
        }
    }

    /**
     * Load bundled asset pack by filename.
     */
    fun loadBundledPack(
        assetFilename: String,
        deckName: String = "Medborgarskap",
        replaceExisting: Boolean = true,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val jsonString = AssetIngestion.readAssetFile(getApplication(), assetFilename)
                val items = AssetIngestion.parseQuestionsJson(jsonString, defaultDeckName = deckName)
                if (replaceExisting) {
                    repository.deleteDeck(deckName)
                }
                repository.insertAll(items)
                startSmartSession(deckName = deckName, limit = 20)

                val strings = _currentLanguage.value.getStrings()
                _toastMessage.value = strings.toastImportSuccess(items.size)
                onSuccess(items.size)
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Failed to read assets"
                _toastMessage.value = "Error: $errorMsg"
                onError(errorMsg)
            }
        }
    }

    /**
     * Reset learning progress.
     */
    fun resetAllProgress() {
        viewModelScope.launch {
            val strings = _currentLanguage.value.getStrings()
            val currentTime = System.currentTimeMillis()
            repository.resetAll(currentTime)
            _selectedOption.value = null
            _isAnswerRevealed.value = false
            startSmartSession(deckName = _selectedDeck.value, limit = 20)
            _toastMessage.value = strings.toastProgressReset
        }
    }

    fun triggerTestNotification() {
        val strings = _currentLanguage.value.getStrings()
        WorkScheduler.triggerImmediateCheck(getApplication())
        _toastMessage.value = strings.toastNotificationTriggered
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
