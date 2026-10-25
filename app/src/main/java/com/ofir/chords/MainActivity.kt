package com.ofir.chords

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var titleView: TextView
    private lateinit var query: EditText
    private lateinit var chordsOnly: CheckBox
    private lateinit var progress: ProgressBar
    private lateinit var status: TextView
    private lateinit var resultsList: ListView
    private lateinit var songScroll: ScrollView
    private lateinit var songText: TextView

    private var results: List<SongResult> = emptyList()
    private var songLines: List<SongLine> = emptyList()
    private var textSizeSp = 16f

    private val backToResults = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = showResults()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        titleView = findViewById(R.id.title)
        query = findViewById(R.id.query)
        chordsOnly = findViewById(R.id.chordsOnly)
        progress = findViewById(R.id.progress)
        status = findViewById(R.id.status)
        resultsList = findViewById(R.id.results)
        songScroll = findViewById(R.id.songScroll)
        songText = findViewById(R.id.songText)

        findViewById<Button>(R.id.searchBtn).setOnClickListener { doSearch() }
        query.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch(); true
            } else false
        }
        resultsList.setOnItemClickListener { _, _, pos, _ -> openSong(results[pos]) }
        chordsOnly.setOnCheckedChangeListener { _, _ -> renderSong() }
        findViewById<Button>(R.id.smaller).setOnClickListener { changeTextSize(-2f) }
        findViewById<Button>(R.id.bigger).setOnClickListener { changeTextSize(2f) }

        onBackPressedDispatcher.addCallback(this, backToResults)
        songText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
    }

    private fun doSearch() {
        val q = query.text.toString().trim()
        if (q.isEmpty()) return
        hideKeyboard()
        showResults()
        setLoading(true)
        lifecycleScope.launch {
            try {
                val found = withContext(Dispatchers.IO) { Tab4uClient.search(q) }
                results = found
                resultsList.adapter =
                    ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, found)
                when (found.size) {
                    0 -> showStatus(getString(R.string.no_results))
                    1 -> openSong(found[0])
                    else -> showStatus(null)
                }
            } catch (e: Exception) {
                showStatus(getString(R.string.error, e.message ?: e.javaClass.simpleName))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun openSong(song: SongResult) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                songLines = withContext(Dispatchers.IO) { Tab4uClient.fetchSong(song.url) }
                titleView.text = song.title
                renderSong()
                showSong()
            } catch (e: Exception) {
                showStatus(getString(R.string.error, e.message ?: e.javaClass.simpleName))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun renderSong() {
        val lines = songLines.filter { !chordsOnly.isChecked || it.isChord }
        val text = lines.joinToString("\n") { it.text }
        songText.textDirection =
            if (HEBREW.containsMatchIn(text)) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR
        songText.text = text
    }

    private fun showSong() {
        resultsList.visibility = View.GONE
        status.visibility = View.GONE
        songScroll.visibility = View.VISIBLE
        songScroll.scrollTo(0, 0)
        backToResults.isEnabled = true
    }

    private fun showResults() {
        songScroll.visibility = View.GONE
        resultsList.visibility = View.VISIBLE
        titleView.setText(R.string.app_name)
        backToResults.isEnabled = false
    }

    private fun showStatus(msg: String?) {
        status.text = msg ?: ""
        status.visibility = if (msg == null) View.GONE else View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) status.visibility = View.GONE
    }

    private fun changeTextSize(delta: Float) {
        textSizeSp = (textSizeSp + delta).coerceIn(10f, 32f)
        songText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(query.windowToken, 0)
    }

    companion object {
        private val HEBREW = Regex("[֐-׿]")
    }
}
