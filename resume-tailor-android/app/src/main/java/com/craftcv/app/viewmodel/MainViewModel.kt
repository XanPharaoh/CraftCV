package com.craftcv.app.viewmodel

import android.app.Activity
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.craftcv.app.billing.BillingManager
import com.craftcv.app.data.api.RetrofitClient
import com.craftcv.app.data.models.*
import com.craftcv.app.data.preferences.UserPrefs
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.io.File

// ── UI State sealed classes ──────────────────────────────────────────────────
sealed class UiState {
    data object Idle    : UiState()
    data object Loading : UiState()
    data class Success(val response: TailorResponse) : UiState()
    data class Error(val message: String, val isUsageLimit: Boolean = false) : UiState()
}

sealed class CoverLetterUiState {
    data object Idle    : CoverLetterUiState()
    data object Loading : CoverLetterUiState()
    data class Success(val data: CoverLetterResponse) : CoverLetterUiState()
    data class Error(val message: String) : CoverLetterUiState()
}

sealed class RewriteUiState {
    data object Idle    : RewriteUiState()
    data object Loading : RewriteUiState()
    data class Success(val original: String, val rewritten: String) : RewriteUiState()
    data class Error(val message: String) : RewriteUiState()
}

// ── ViewModel ────────────────────────────────────────────────────────────────
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPrefs(application)
    private val api   = RetrofitClient.api

    // Tailor state
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Cover letter — now pulled from combined tailor response, fallback to separate call
    private val _coverLetterState = MutableStateFlow<CoverLetterUiState>(CoverLetterUiState.Idle)
    val coverLetterState: StateFlow<CoverLetterUiState> = _coverLetterState.asStateFlow()

    // Quick standalone cover letter (from dashboard)
    private val _quickCoverLetterState = MutableStateFlow<CoverLetterUiState>(CoverLetterUiState.Idle)
    val quickCoverLetterState: StateFlow<CoverLetterUiState> = _quickCoverLetterState.asStateFlow()

    // Bullet rewrite state
    private val _rewriteState = MutableStateFlow<RewriteUiState>(RewriteUiState.Idle)
    val rewriteState: StateFlow<RewriteUiState> = _rewriteState.asStateFlow()

    // History
    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    // Working Draft inside the Editor
    private val _draftResume = MutableStateFlow<TailorResponse?>(null)
    val draftResume: StateFlow<TailorResponse?> = _draftResume.asStateFlow()

    // In-place re-analyze loading state (does NOT touch _uiState to avoid navigation)
    private val _isReanalyzing = MutableStateFlow(false)
    val isReanalyzing: StateFlow<Boolean> = _isReanalyzing.asStateFlow()
    private val _reanalyzeError = MutableStateFlow<String?>(null)
    val reanalyzeError: StateFlow<String?> = _reanalyzeError.asStateFlow()

    // Persisted inputs so ResultsScreen can use them for cover letter generation
    var lastResumeText: String = ""
        private set
    var lastJobDesc: String = ""
        private set

    // Local user profile (not persisted to API)
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // User status from backend
    private val _userStatus = MutableStateFlow<UserStatus?>(null)
    val userStatus: StateFlow<UserStatus?> = _userStatus.asStateFlow()

    // Pro debug flag — MUST be false for release builds
    private val DEBUG_PRO = false

    // Pro status — driven by Google Play Billing (authoritative) + DataStore cache
    private val _isPro = MutableStateFlow(DEBUG_PRO)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _usesRemaining = MutableStateFlow(5)
    val usesRemaining: StateFlow<Int> = _usesRemaining.asStateFlow()

    // Google Play Billing
    val billingManager = BillingManager(
        context          = getApplication(),
        coroutineScope   = viewModelScope,
        onProStatusChanged = { isProNow ->
            _isPro.value = isProNow
            viewModelScope.launch { prefs.setProStatus(isProNow) }
        },
        onPurchaseToken  = { token, productId -> verifyPurchaseWithBackend(token, productId) },
    )

    // Snackbar
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private var deviceId: String = ""

    init {
        viewModelScope.launch {
            deviceId = prefs.ensureDeviceId()
            // Restore cached Pro status immediately while Billing initialises
            if (DEBUG_PRO || prefs.isPro.first()) _isPro.value = true
            fetchStatus()
            restoreSavedInputs()
        }
        billingManager.connect()
    }

    override fun onCleared() {
        super.onCleared()
        billingManager.disconnect()
    }

    // ── Restore saved inputs from DataStore ──────────────────────────────────
    private suspend fun restoreSavedInputs() {
        prefs.lastResume.first().let { if (it.isNotBlank()) lastResumeText = it }
        prefs.lastJobDesc.first().let { if (it.isNotBlank()) lastJobDesc = it }
        prefs.profileJson.first().let { json ->
            if (json.isNotBlank()) {
                try { _userProfile.value = Gson().fromJson(json, UserProfile::class.java) }
                catch (_: Exception) { /* corrupt data — ignore */ }
            }
        }
        prefs.draftResumeJson.first().let { json ->
            if (json.isNotBlank()) {
                try { _draftResume.value = Gson().fromJson(json, TailorResponse::class.java) }
                catch (_: Exception) { /* corrupt data — ignore */ }
            }
        }
    }

    // ── Profile ──────────────────────────────────────────────────────────────
    fun saveProfile(profile: UserProfile) {
        _userProfile.value = profile
        viewModelScope.launch {
            prefs.setHasSeenProfile(true)
            prefs.saveProfile(Gson().toJson(profile))
        }
    }

    /**
     * When the LLM extracts contact information from the uploaded resume,
     * fill in any empty profile fields so the name / title / location
     * appear in the preview and DOCX export without manual entry.
     */
    private fun autoFillProfileFromContact(contact: ContactInfo) {
        if (contact.fullName.isBlank() && contact.currentTitle.isBlank() && contact.location.isBlank()) return
        val current = _userProfile.value ?: UserProfile()
        val updated = current.copy(
            fullName     = current.fullName.ifBlank { contact.fullName },
            currentTitle = current.currentTitle.ifBlank { contact.currentTitle },
            location     = current.location.ifBlank { contact.location },
        )
        if (updated != current) {
            saveProfile(updated)
        }
    }

    fun skipProfile() {
        viewModelScope.launch { prefs.setHasSeenProfile(true) }
    }

    // ── Status ───────────────────────────────────────────────────────────────
    fun fetchStatus() {
        viewModelScope.launch {
            try {
                val response = api.getUserStatus(deviceId)
                if (response.isSuccessful) {
                    val status = response.body()
                    _userStatus.value = status
                    if (status != null) {
                        _usesRemaining.value = status.usesRemaining
                        // Merge: backend Pro flag (from /verify-purchase) takes priority
                        if (status.isPro) {
                            _isPro.value = true
                            prefs.setProStatus(true)
                        }
                    }
                }
            } catch (_: Exception) { /* silent — non-critical */ }
        }
    }

    // ── Tailor (combined: bullets + cover letter + insights) ─────────────────
    fun tailorResume(resumeText: String, jobDescription: String, fileUri: Uri? = null) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            lastJobDesc    = jobDescription
            val location = _userProfile.value?.location ?: ""

            try {
                val response = if (fileUri != null) {
                    val context = getApplication<Application>()
                    val tempFile = copyUriToTemp(context, fileUri)
                    val fileName = getFileNameFromUri(fileUri) ?: "resume.pdf"
                    val mimeType = context.contentResolver.getType(fileUri) ?: "application/pdf"

                    // Read the file text locally so lastResumeText reflects the actual file
                    try {
                        val ext = fileName.substringAfterLast('.', "").lowercase()
                        if (ext == "txt") {
                            lastResumeText = tempFile.readText()
                        } else {
                            lastResumeText = "[Uploaded: $fileName]"
                        }
                    } catch (_: Exception) {
                        lastResumeText = "[Uploaded: $fileName]"
                    }
                    prefs.saveLastInputs(lastResumeText, jobDescription)

                    val filePart    = MultipartBody.Part.createFormData("resume_file", fileName, tempFile.asRequestBody(mimeType.toMediaTypeOrNull()))
                    val jobDescPart = jobDescription.toRequestBody("text/plain".toMediaTypeOrNull())
                    val devicePart  = deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
                    val locPart     = location.toRequestBody("text/plain".toMediaTypeOrNull())

                    api.tailorWithFile(filePart, jobDescPart, devicePart, locPart).also { tempFile.delete() }
                } else {
                    lastResumeText = resumeText
                    prefs.saveLastInputs(resumeText, jobDescription)
                    api.tailorWithText(
                        resumeText     = resumeText,
                        jobDescription = jobDescription,
                        deviceId       = deviceId,
                        location       = location,
                    )
                }

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _uiState.value = UiState.Success(body)

                    // Auto-save this as the working draft
                    _draftResume.value = body
                    prefs.saveDraftResume(Gson().toJson(body))

                    // Auto-populate profile from extracted contact info
                    autoFillProfileFromContact(body.contactInfo)

                    // Pre-populate cover letter from combined response
                    if (body.coverLetter.isNotBlank()) {
                        _coverLetterState.value = CoverLetterUiState.Success(
                            CoverLetterResponse(
                                coverLetter = body.coverLetter,
                                subjectLine = body.subjectLine,
                            )
                        )
                    }

                    fetchStatus()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    val isUsageLimit = response.code() == 402
                    _uiState.value = UiState.Error(errorBody, isUsageLimit)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Network error. Please check your connection.")
            }
        }
    }

    // ── Cover Letter (fallback if not in combined response) ──────────────────
    fun generateCoverLetter(resumeText: String, jobDescription: String) {
        // If already populated from combined tailor response, skip
        if (_coverLetterState.value is CoverLetterUiState.Success) return

        viewModelScope.launch {
            _coverLetterState.value = CoverLetterUiState.Loading
            try {
                val location = _userProfile.value?.location ?: ""
                val response = api.generateCoverLetter(
                    resumeText     = resumeText,
                    jobDescription = jobDescription,
                    deviceId       = deviceId,
                    location       = location,
                )
                _coverLetterState.value = if (response.isSuccessful && response.body() != null) {
                    CoverLetterUiState.Success(response.body()!!)
                } else {
                    CoverLetterUiState.Error("Failed to generate cover letter")
                }
            } catch (e: Exception) {
                _coverLetterState.value = CoverLetterUiState.Error(e.message ?: "Network error.")
            }
        }
    }

    fun resetCoverLetter() {
        _coverLetterState.value = CoverLetterUiState.Idle
    }

    // ── Quick Standalone Cover Letter (from Dashboard) ─────────────────────────
    fun generateQuickCoverLetter(resumeText: String, jobDescription: String) {
        viewModelScope.launch {
            _quickCoverLetterState.value = CoverLetterUiState.Loading
            try {
                val location = _userProfile.value?.location ?: ""
                val response = api.generateCoverLetter(
                    resumeText     = resumeText,
                    jobDescription = jobDescription,
                    deviceId       = deviceId,
                    location       = location,
                )
                _quickCoverLetterState.value = if (response.isSuccessful && response.body() != null) {
                    CoverLetterUiState.Success(response.body()!!)
                } else {
                    CoverLetterUiState.Error("Failed to generate cover letter")
                }
            } catch (e: Exception) {
                _quickCoverLetterState.value = CoverLetterUiState.Error(e.message ?: "Network error.")
            }
        }
    }

    fun resetQuickCoverLetter() {
        _quickCoverLetterState.value = CoverLetterUiState.Idle
    }

    // ── Re-analyze Draft ─────────────────────────────────────────────────────
    fun reanalyzeDraft(jobDescription: String) {
        val draft = _draftResume.value ?: return
        if (_isReanalyzing.value) return // prevent double-tap

        val compiledText = buildString {
            appendLine(draft.contactInfo.fullName)
            appendLine(draft.contactInfo.currentTitle)
            appendLine("Summary:\n" + draft.professionalSummary)
            appendLine("Skills:\n" + draft.skills.joinToString(", "))
            draft.experience.forEach {
                appendLine("${it.jobTitle} at ${it.company} (${it.dates})")
                it.bullets.forEach { b -> appendLine("- $b") }
            }
            draft.education.forEach {
                appendLine("${it.degree} at ${it.institution} (${it.graduationDate})")
            }
        }

        viewModelScope.launch {
            _isReanalyzing.value = true
            _reanalyzeError.value = null
            try {
                val location = _userProfile.value?.location ?: ""
                val response = api.tailorWithText(
                    resumeText     = compiledText,
                    jobDescription = jobDescription,
                    deviceId       = deviceId,
                    location       = location,
                )
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    // Only update the draft — do NOT touch _uiState so the screen stays visible
                    _draftResume.value = body
                    prefs.saveDraftResume(Gson().toJson(body))
                    // Also refresh cover letter if one was generated
                    if (body.coverLetter.isNotBlank()) {
                        _coverLetterState.value = CoverLetterUiState.Success(
                            CoverLetterResponse(coverLetter = body.coverLetter, subjectLine = body.subjectLine)
                        )
                    }
                } else {
                    _reanalyzeError.value = "Re-analyze failed (${response.code()}). Try again."
                }
            } catch (e: Exception) {
                _reanalyzeError.value = e.message ?: "Network error."
            } finally {
                _isReanalyzing.value = false
            }
        }
    }

    fun clearReanalyzeError() {
        _reanalyzeError.value = null
    }

    // ── Bullet Rewrite ───────────────────────────────────────────────────────
    fun rewriteBullet(bullet: String, jobDescription: String) {
        viewModelScope.launch {
            _rewriteState.value = RewriteUiState.Loading
            try {
                val response = api.rewriteBullet(
                    bullet         = bullet,
                    jobDescription = jobDescription,
                    deviceId       = deviceId,
                )
                if (response.isSuccessful && response.body() != null) {
                    _rewriteState.value = RewriteUiState.Success(
                        original = bullet,
                        rewritten = response.body()!!.rewrittenBullet,
                    )
                } else {
                    _rewriteState.value = RewriteUiState.Error("Rewrite failed. Try again.")
                }
            } catch (e: Exception) {
                _rewriteState.value = RewriteUiState.Error(e.message ?: "Network error.")
            }
        }
    }

    fun resetRewriteState() {
        _rewriteState.value = RewriteUiState.Idle
    }

    // ── Interactive Editor Draft Updater ─────────────────────────────────────
    fun updateDraftResume(updated: TailorResponse) {
        // --- Live Local ATS Scoring ---
        val fullText = buildString {
            append(updated.professionalSummary).append(" ")
            append(updated.skills.joinToString(" ")).append(" ")
            updated.experience.forEach {
                append(it.jobTitle).append(" ")
                append(it.bullets.joinToString(" ")).append(" ")
            }
            updated.education.forEach {
                append(it.degree).append(" ")
            }
        }.lowercase()

        val remainingMissing = updated.missingKeywords.filter { kw ->
            !fullText.contains(kw.lowercase())
        }

        val keywordsAdded = updated.missingKeywords.size - remainingMissing.size
        val newScore = if (updated.missingKeywords.isNotEmpty() && keywordsAdded > 0) {
            val boost = (keywordsAdded.toFloat() / updated.missingKeywords.size.toFloat()) * (100 - updated.atsScore)
            minOf(100, updated.atsScore + boost.toInt())
        } else {
            updated.atsScore
        }

        val newReason = if (keywordsAdded > 0 && remainingMissing.isEmpty()) {
            "Excellent! You've successfully added all missing keywords. Your resume matches very well."
        } else if (keywordsAdded > 0) {
            "Improved! You added $keywordsAdded keywords. Keep adding to boost your score further."
        } else {
            updated.atsReason
        }

        val finalUpdated = updated.copy(
            atsScore = newScore,
            atsReason = newReason,
            missingKeywords = remainingMissing
        )

        _draftResume.value = finalUpdated
        viewModelScope.launch {
            prefs.saveDraftResume(Gson().toJson(finalUpdated))
        }
    }

    // ── History ──────────────────────────────────────────────────────────────
    fun fetchHistory() {
        viewModelScope.launch {
            try {
                val response = api.getHistory(deviceId)
                if (response.isSuccessful && response.body() != null) {
                    _history.value = response.body()!!
                }
            } catch (_: Exception) { /* silent */ }
        }
    }

    // ── DOCX Export (Pro only) ───────────────────────────────────────────────
    fun downloadDocx(tailorData: TailorResponse, template: String = "professional") {
        val profile = _userProfile.value
        viewModelScope.launch {
            try {
                _snackbarMessage.emit("Generating resume…")
                val response = api.generateDocx(
                    deviceId             = deviceId,
                    bullets              = JSONArray(tailorData.tailoredBullets).toString(),
                    coverLetter          = tailorData.coverLetter,
                    template             = template,
                    fullName             = profile?.fullName ?: "",
                    currentTitle         = profile?.currentTitle ?: "",
                    location             = profile?.location ?: "",
                    education            = profile?.education ?: "",
                    skills               = JSONArray(profile?.skills ?: emptyList<String>()).toString(),
                    targetRole           = profile?.targetRole ?: "",
                    professionalSummary  = tailorData.professionalSummary,
                    experience           = Gson().toJson(tailorData.experience),
                )
                if (response.isSuccessful && response.body() != null) {
                    val bytes = response.body()!!.bytes()
                    saveDocxToDownloads(bytes)
                    _snackbarMessage.emit("Resume saved to Downloads!")
                } else if (response.code() == 403) {
                    _snackbarMessage.emit("DOCX export is a Pro feature.")
                } else {
                    _snackbarMessage.emit("Failed to generate DOCX.")
                }
            } catch (e: Exception) {
                _snackbarMessage.emit("Download failed: ${e.message}")
            }
        }
    }

    private fun saveDocxToDownloads(bytes: ByteArray) {
        val context = getApplication<Application>()
        val fileName = "resume_${System.currentTimeMillis()}.docx"
        val mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { os -> os.write(bytes) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(it, values, null, null)
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, fileName)
            file.writeBytes(bytes)
        }
    }

    // ── Upgrade ──────────────────────────────────────────────────────────────
    /**
     * Opens the Google Play subscription sheet.
     * @param activity The current foreground Activity (required by Play Billing).
     * @param planIndex 0 = monthly, 1 = yearly
     */
    fun upgradeToPro(activity: Activity, planIndex: Int = 1) {
        billingManager.launchBillingFlow(activity, planIndex)
    }

    /** Called automatically after a successful Play Store purchase to register it server-side. */
    fun verifyPurchaseWithBackend(token: String, productId: String) {
        viewModelScope.launch {
            try {
                val response = api.verifyPurchase(
                    deviceId      = deviceId,
                    purchaseToken = token,
                    productId     = productId,
                )
                if (response.isSuccessful) {
                    _isPro.value = true
                    prefs.setProStatus(true)
                    _snackbarMessage.emit("Pro activated! Thank you 🎉")
                }
            } catch (e: Exception) {
                // Play Billing library is the source of truth — don't block Pro access on network errors
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    suspend fun hasSeenProfile(): Boolean = prefs.hasSeenProfile.first()
    fun resetState() { _uiState.value = UiState.Idle }

    fun grantAdUse() {
        viewModelScope.launch {
            try {
                val resp = api.grantAdUse(deviceId)
                if (resp.isSuccessful && resp.body() != null) {
                    _usesRemaining.value = resp.body()!!.usesRemaining
                }
            } catch (_: Exception) { /* silent */ }
        }
    }

    fun showSnackbar(message: String) {
        viewModelScope.launch { _snackbarMessage.emit(message) }
    }

    private fun copyUriToTemp(context: Context, uri: Uri): File {
        val tempFile = File.createTempFile("resume", ".tmp", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { input.copyTo(it) }
        }
        return tempFile
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        val context = getApplication<Application>()
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            if (idx >= 0) cursor.getString(idx) else null
        }
    }
}
