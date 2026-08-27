package com.arnika.session

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import dev.ffmpegkit.whisper.Whisper
import dev.ffmpegkit.whisper.WhisperConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var transcript: TextInputEditText
    private lateinit var title: TextInputEditText
    private lateinit var selectedFileText: TextView
    private lateinit var status: TextView
    private lateinit var progress: CircularProgressIndicator
    private lateinit var btnLive: MaterialButton
    private lateinit var btnStop: MaterialButton
    private lateinit var btnTranscribeFile: MaterialButton
    private var selectedAudio: Uri? = null
    @Volatile private var live = false

    private val pickAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedAudio = uri
            selectedFileText.text = "فایل انتخاب شد: ${uri.lastPathSegment ?: "Audio"}"
            btnTranscribeFile.isEnabled = true
            setStatus("برای شروع، «تبدیل فایل به متن» را بزنید")
        }
    }

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) startLiveLoop() else AlertDialog.Builder(this).setTitle("مجوز میکروفن").setMessage("برای تبدیل صدای زنده به متن، مجوز میکروفن لازم است.").setPositiveButton("تنظیمات") { _, _ ->
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }.setNegativeButton("بستن", null).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(12), dp(18), dp(18)); setBackgroundColor(Color.rgb(245,247,247)) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(14)) }
        val logo = ImageView(this).apply { setImageResource(com.arnika.session.R.drawable.ic_arnika); layoutParams = LinearLayout.LayoutParams(dp(58), dp(58)) }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12),0,0,0) }
        brand.addView(TextView(this).apply { text = "ARNIKA SESSION"; textSize = 23f; setTextColor(Color.rgb(8,47,44)); setTypeface(typeface, 1) })
        brand.addView(TextView(this).apply { text = "Local multilingual voice intelligence"; textSize = 12f; setTextColor(Color.DKGRAY) })
        header.addView(logo); header.addView(brand, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)); root.addView(header)

        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body); root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))

        body.addView(card("جلسه و متن") { box ->
            title = field("عنوان جلسه"); box.addView(title.parent as View)
            transcript = field("متن تشخیص داده‌شده").apply { minLines = 9; gravity = Gravity.TOP or Gravity.START }
            (transcript.parent as TextInputLayout).layoutParams = LinearLayout.LayoutParams(-1, dp(250)).apply { topMargin = dp(10) }
            box.addView(transcript.parent as View)
        })

        body.addView(card("صدای زنده") { box ->
            val hint = TextView(this).apply { text = "هر چند ثانیه یک قطعه ضبط و با Whisper محلی تبدیل می‌شود؛ زبان به‌صورت خودکار تشخیص داده می‌شود."; setTextColor(Color.DKGRAY) }
            box.addView(hint)
            val row = row()
            btnLive = actionButton("🎙 شروع صدای زنده", true) { ensureMicAndStart() }
            btnStop = actionButton("■ توقف", false) { live = false; setStatus("متوقف شد") }
            row.addView(btnLive, weight()); row.addView(btnStop, weight()); box.addView(row)
        })

        body.addView(card("فایل صوتی → متن") { box ->
            selectedFileText = TextView(this).apply { text = "هنوز فایلی انتخاب نشده"; setTextColor(Color.DKGRAY); setPadding(0,0,0,dp(8)) }
            box.addView(selectedFileText)
            val select = actionButton("🎵 انتخاب فایل صوتی", false) { pickAudio.launch(arrayOf("audio/wav","audio/x-wav","audio/mpeg","audio/mp3","audio/flac","audio/*")) }
            btnTranscribeFile = actionButton("✨ تبدیل فایل به متن", true) { transcribeSelectedFile() }.apply { isEnabled = false }
            box.addView(select); box.addView(btnTranscribeFile, LinearLayout.LayoutParams(-1,-2).apply { topMargin = dp(8) })
        })

        body.addView(card("ابزارها") { box ->
            val row1 = row(); row1.addView(actionButton("🌐 ترجمه", false) { showTranslateDialog() }, weight()); row1.addView(actionButton("↗ اشتراک", false) { shareText() }, weight()); box.addView(row1)
            val row2 = row(); row2.addView(actionButton("TXT", false) { exportTxt() }, weight()); row2.addView(actionButton("🔊 خواندن متن", false) { speakText() }, weight()); box.addView(row2)
            box.addView(actionButton("⚙ تنظیمات ظاهر", false) { showThemeDialog() }, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(8) })
        })

        val foot = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(0,dp(10),0,0) }
        progress = CircularProgressIndicator(this).apply { visibility = View.GONE; layoutParams = LinearLayout.LayoutParams(dp(28),dp(28)) }
        status = TextView(this).apply { text="آماده"; setTextColor(Color.rgb(15,118,110)); setPadding(dp(10),0,0,0) }
        foot.addView(progress); foot.addView(status); root.addView(foot)
        return root
    }

    private fun ensureMicAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startLiveLoop()
        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startLiveLoop() {
        if (live) return
        live = true
        setStatus("صدای زنده فعال شد")
        lifecycleScope.launch {
            while (live) {
                val wav = File(cacheDir, "live_${System.currentTimeMillis()}.wav")
                showBusy(true, "در حال شنیدن…")
                val recorded = runCatching { withContext(Dispatchers.IO) { WavRecorder.record(wav, 4) } }.isSuccess
                if (!recorded) { showBusy(false, "خطا در ضبط میکروفن"); live=false; break }
                if (!live) { wav.delete(); break }
                transcribeFile(wav, append = true)
                wav.delete()
            }
            showBusy(false, if (live) "آماده" else "متوقف شد")
        }
    }

    private fun transcribeSelectedFile() {
        val uri = selectedAudio ?: return
        lifecycleScope.launch {
            val f = withContext(Dispatchers.IO) { copyUriToCache(uri) }
            if (f == null) { setStatus("خواندن فایل ناموفق بود"); return@launch }
            transcribeFile(f, append = false)
            f.delete()
        }
    }

    private suspend fun transcribeFile(file: File, append: Boolean) {
        showBusy(true, "Whisper در حال تبدیل صدا به متن…")
        try {
            val text = withContext(Dispatchers.Default) {
                val model = Whisper.loadModelFromAsset(this@MainActivity, "models/ggml-tiny.bin")
                try { Whisper.transcribe(model, file.absolutePath, WhisperConfig(language = "auto")).text.trim() }
                finally { Whisper.releaseModel(model) }
            }
            if (text.isBlank()) setStatus("متنی تشخیص داده نشد")
            else {
                if (append && !transcript.text.isNullOrBlank()) transcript.append("\n$text") else transcript.setText(text)
                setStatus("تبدیل با موفقیت انجام شد")
            }
        } catch (e: Exception) {
            setStatus("خطای تبدیل: ${e.message ?: e.javaClass.simpleName}")
        } finally { showBusy(false, status.text.toString()) }
    }

    private fun copyUriToCache(uri: Uri): File? = try {
        val mime = contentResolver.getType(uri).orEmpty()
        val ext = when { mime.contains("mpeg") || mime.contains("mp3") -> ".mp3"; mime.contains("flac") -> ".flac"; else -> ".wav" }
        val out = File(cacheDir, "import_${System.currentTimeMillis()}$ext")
        contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(out).use { input.copyTo(it) } } ?: return null
        out
    } catch (_: Exception) { null }

    private fun showTranslateDialog() {
        val text = transcript.text?.toString()?.trim().orEmpty(); if (text.isBlank()) { setStatus("ابتدا متن ایجاد کنید"); return }
        val names = arrayOf("فارسی","English","العربية","Français","Italiano","Español","Deutsch","Türkçe","中文","日本語","한국어","Русский")
        val codes = arrayOf("fa","en","ar","fr","it","es","de","tr","zh","ja","ko","ru")
        AlertDialog.Builder(this).setTitle("ترجمه متن").setItems(names) { _, which -> translate(text, codes[which]) }.setNegativeButton("بستن", null).show()
    }

    private fun translate(text: String, target: String) {
        showBusy(true, "در حال تشخیص زبان و ترجمه…")
        val id = LanguageIdentification.getClient()
        id.identifyLanguage(text).addOnSuccessListener { source ->
            val s = TranslateLanguage.fromLanguageTag(source)
            val t = TranslateLanguage.fromLanguageTag(target)
            if (s == null || t == null) { id.close(); showBusy(false,"این زوج زبانی پشتیبانی نمی‌شود"); return@addOnSuccessListener }
            if (s == t) { id.close(); showBusy(false,"متن از قبل همین زبان است"); return@addOnSuccessListener }
            val tr = Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(s).setTargetLanguage(t).build())
            tr.downloadModelIfNeeded(DownloadConditions.Builder().build()).addOnSuccessListener {
                tr.translate(text).addOnSuccessListener { out -> transcript.setText(out); tr.close(); id.close(); showBusy(false,"ترجمه انجام شد") }
                    .addOnFailureListener { e -> tr.close(); id.close(); showBusy(false,"ترجمه ناموفق: ${e.message}") }
            }.addOnFailureListener { e -> tr.close(); id.close(); showBusy(false,"دانلود مدل ترجمه ناموفق: ${e.message}") }
        }.addOnFailureListener { e -> id.close(); showBusy(false,"تشخیص زبان ناموفق: ${e.message}") }
    }

    private fun shareText() {
        val t = transcript.text?.toString().orEmpty(); if (t.isBlank()) return
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_SUBJECT, title.text?.toString().orEmpty()); putExtra(Intent.EXTRA_TEXT,t) }, "اشتراک متن"))
    }

    private fun exportTxt() {
        val t = transcript.text?.toString().orEmpty(); if (t.isBlank()) return
        val i = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="text/plain"; putExtra(Intent.EXTRA_TITLE, (title.text?.toString()?.ifBlank { "ARNIKA_SESSION" } ?: "ARNIKA_SESSION") + ".txt") }
        createTxt.launch(i)
    }

    private val createTxt = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val uri = r.data?.data ?: return@registerForActivityResult
        runCatching { contentResolver.openOutputStream(uri)?.use { it.write(transcript.text?.toString().orEmpty().toByteArray(Charsets.UTF_8)) } }.onSuccess { setStatus("TXT ذخیره شد") }.onFailure { setStatus("ذخیره ناموفق") }
    }

    private fun speakText() {
        val t = transcript.text?.toString().orEmpty(); if (t.isBlank()) return
        val i = Intent("android.speech.tts.engine.CHECK_TTS_DATA")
        runCatching { startActivityForResult(i, 909) }.onFailure { setStatus("موتور TTS دستگاه در دسترس نیست") }
        android.speech.tts.TextToSpeech(this) { code -> if (code == android.speech.tts.TextToSpeech.SUCCESS) android.speech.tts.TextToSpeech(this) {} }
        val tts = android.speech.tts.TextToSpeech(this) { code -> if (code == android.speech.tts.TextToSpeech.SUCCESS) { ttsHolder?.speak(t, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "arnika") } }
        ttsHolder = tts
    }
    private var ttsHolder: android.speech.tts.TextToSpeech? = null

    private fun showThemeDialog() {
        AlertDialog.Builder(this).setTitle("پوسته ARNIKA").setSingleChoiceItems(arrayOf("کلاسیک روشن","کلاسیک تیره"), if (resources.configuration.uiMode and 0x30 == 0x20) 1 else 0) { d, which ->
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(if (which==1) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO); d.dismiss()
        }.setNegativeButton("بستن",null).show()
    }

    private fun field(hint: String): TextInputEditText {
        val layout = TextInputLayout(this).apply { this.hint=hint; boxBackgroundMode=TextInputLayout.BOX_BACKGROUND_OUTLINE }
        val edit = TextInputEditText(layout.context).apply { textSize=16f; setPadding(dp(12),dp(10),dp(12),dp(10)) }
        layout.addView(edit); return edit
    }
    private fun card(caption: String, fill: (LinearLayout)->Unit): View {
        val card = MaterialCardView(this).apply { radius=dp(18).toFloat(); cardElevation=dp(2).toFloat(); setCardBackgroundColor(Color.WHITE); strokeColor=Color.rgb(222,229,228); strokeWidth=1; useCompatPadding=true }
        val box = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(16),dp(15),dp(16),dp(15)) }
        box.addView(TextView(this).apply { text=caption; textSize=18f; setTypeface(typeface,1); setTextColor(Color.rgb(8,47,44)); setPadding(0,0,0,dp(10)) })
        fill(box); card.addView(box); card.layoutParams=LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(12) }; return card
    }
    private fun row() = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(0,dp(8),0,0) }
    private fun weight() = LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd=dp(6) }
    private fun actionButton(text: String, primary: Boolean, click: ()->Unit) = MaterialButton(this).apply { this.text=text; isAllCaps=false; cornerRadius=dp(14); if (primary) { setBackgroundColor(Color.rgb(15,118,110)); setTextColor(Color.WHITE) } else { setTextColor(Color.rgb(8,47,44)) }; setOnClickListener { click() } }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun showBusy(b:Boolean, message:String) { runOnUiThread { progress.visibility=if(b) View.VISIBLE else View.GONE; status.text=message; btnTranscribeFile.isEnabled=!b && selectedAudio!=null; btnLive.isEnabled=!b || live } }
    private fun setStatus(s:String) { runOnUiThread { status.text=s } }

    override fun onDestroy() { live=false; ttsHolder?.shutdown(); super.onDestroy() }
}
