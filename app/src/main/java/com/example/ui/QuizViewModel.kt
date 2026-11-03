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
import com.example.logic.SM2Calculator
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LanguageManager
import com.example.ui.i18n.getStrings
import com.example.worker.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing quiz review, SM-2 logic, AI card generation, JSON import, and multi-language state.
 */
class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: QuizRepository

    // Current app language (persisted across sessions)
    private val _currentLanguage = MutableStateFlow(LanguageManager.getSavedLanguage(application))
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    // Questions due for review today
    val dueQuestions: StateFlow<List<QuizItem>>

    // All questions in library
    val allQuestions: StateFlow<List<QuizItem>>

    // Current question index in active review session
    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    // Currently selected option index
    private val _selectedOption = MutableStateFlow<Int?>(null)
    val selectedOption: StateFlow<Int?> = _selectedOption.asStateFlow()

    // Is answer revealed to show explanation & SM-2 grading buttons
    private val _isAnswerRevealed = MutableStateFlow(false)
    val isAnswerRevealed: StateFlow<Boolean> = _isAnswerRevealed.asStateFlow()

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

        // Initialize questions from assets if DB is empty
        viewModelScope.launch {
            AssetIngestion.ingestQuestionsIfEmpty(application, repository)
        }

        // Schedule periodic review reminder
        WorkScheduler.scheduleDailyReviewReminder(application)
    }

    /**
     * Change app language and persist selection.
     */
    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        LanguageManager.saveLanguage(getApplication(), language)
    }

    /**
     * User selected an answer option.
     */
    fun onOptionSelected(index: Int) {
        if (!_isAnswerRevealed.value) {
            _selectedOption.value = index
        }
    }

    /**
     * Reveal answer to inspect explanation and grade recall.
     */
    fun revealAnswer() {
        if (_selectedOption.value != null) {
            _isAnswerRevealed.value = true
        }
    }

    /**
     * Apply SM-2 algorithm to user rating (0..5), update database, and notify user.
     */
    fun onRateAndSubmitQuality(item: QuizItem, quality: Int) {
        viewModelScope.launch {
            val updatedItem = SM2Calculator.calculateSM2(item, quality)
            repository.update(updatedItem)

            _selectedOption.value = null
            _isAnswerRevealed.value = false

            val strings = _currentLanguage.value.getStrings()
            _toastMessage.value = if (quality >= 3) {
                strings.toastReviewScheduled(updatedItem.interval)
            } else {
                strings.toastReviewTomorrow
            }
        }
    }

    /**
     * Move to next question manually.
     */
    fun nextQuestion(totalCount: Int) {
        if (_currentQuestionIndex.value < totalCount - 1) {
            _currentQuestionIndex.value += 1
            _selectedOption.value = null
            _isAnswerRevealed.value = false
        }
    }

    /**
     * Manually create a new card.
     */
    fun createManualQuestion(
        question: String,
        options: List<String>,
        correctIndex: Int,
        explanation: String,
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
     * Import questions from a JSON string.
     */
    fun importQuestionsFromJson(
        jsonString: String,
        replaceExisting: Boolean = false,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val items = AssetIngestion.parseQuestionsJson(jsonString)
                if (items.isEmpty()) {
                    onError("No valid questions found in JSON array.")
                    return@launch
                }

                if (replaceExisting) {
                    repository.deleteAll()
                }

                repository.insertAll(items)
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
     * Import questions from a picked file URI (Storage Access Framework).
     */
    fun importQuestionsFromUri(
        uri: Uri,
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
                importQuestionsFromJson(jsonString, replaceExisting, onSuccess, onError)
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Failed to read file"
                val strings = _currentLanguage.value.getStrings()
                _toastMessage.value = "${strings.fileReadError}: $errorMsg"
                onError(errorMsg)
            }
        }
    }

    /**
     * Load Swedish Citizenship Pack from assets/questions.json.
     */
    fun loadSwedishPack(onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        loadBundledPack("questions.json", replaceExisting = true, onSuccess, onError)
    }

    /**
     * Load any bundled asset pack by filename.
     */
    fun loadBundledPack(
        assetFilename: String,
        replaceExisting: Boolean = true,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val jsonString = AssetIngestion.readAssetFile(getApplication(), assetFilename)
                val items = AssetIngestion.parseQuestionsJson(jsonString)
                if (replaceExisting) {
                    repository.deleteAll()
                }
                repository.insertAll(items)
                _currentQuestionIndex.value = 0
                _selectedOption.value = null
                _isAnswerRevealed.value = false

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
     * Reset learning progress for all questions.
     */
    fun resetAllProgress() {
        viewModelScope.launch {
            val strings = _currentLanguage.value.getStrings()
            val currentTime = System.currentTimeMillis()
            repository.resetAll(currentTime)
            _currentQuestionIndex.value = 0
            _selectedOption.value = null
            _isAnswerRevealed.value = false
            _toastMessage.value = strings.toastProgressReset
        }
    }

    /**
     * Trigger background test notification check.
     */
    fun triggerTestNotification() {
        val strings = _currentLanguage.value.getStrings()
        WorkScheduler.triggerImmediateCheck(getApplication())
        _toastMessage.value = strings.toastNotificationTriggered
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }
}
