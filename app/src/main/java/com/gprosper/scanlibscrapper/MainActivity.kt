package com.gprosper.scanlibscrapper

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gprosper.scanlibscrapper.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Dispatcher

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val set = HashSet<String>()
    private var scrapeJob: Job? = null

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupListeners()

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.addJavascriptInterface(this, "HTMLOUT")
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                if (request?.url?.toString()?.contains("goto") == true) {
                    open(request.url.toString(), binding.urlEditText.text.toString())
                    return true
                }
                return false
            }
        }

        processIntent(intent)
    }

    private fun processIntent(intent: Intent?) {
        intent?.let {
            when {
                it.action == Intent.ACTION_SEND -> {
                    it.clipData?.takeIf { it.itemCount > 0 }?.let {
                        it.getItemAt(0)?.text?.let {
                            if (it.startsWith("https") || it.startsWith("http")) {
                                binding.urlEditText.setText(it)
                                binding.webView.loadUrl(it.toString())
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    private fun setupListeners() {
        binding.goButton.setOnClickListener {
            binding.webView.loadUrl(binding.urlEditText.text.toString())
        }
        binding.scrapeButton.setOnClickListener {
            startScraping()
        }

        binding.resultsEditText.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "ScanlibText",
                binding.resultsEditText.text.toString()
            )
            clipboard!!.setPrimaryClip(clip)
            set.clear()
            binding.resultsEditText.text.clear()
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_LONG).show()
            return@setOnLongClickListener true
        }
    }

    fun open(url: String, from: String) {
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                if (request?.url?.host?.contains("rapidgator.net") == true) {
                    addUrl(request.url.toString())
                } else if (request?.url?.host?.contains("turbobit.net") == true) {
                    addUrl(request.url.toString())
                }
                return false
            }
        }
        webView.loadUrl(
            url,
            mapOf(
                "Referer" to from
            )
        )
    }

    @Synchronized
    fun addUrl(url: String) {
        if (set.contains(url)) return
        set.add(url)
        binding.countTxtView.text = "Found: ${set.size} links"
        binding.resultsEditText.setText(set.joinToString(separator = "\n\n"))
    }

    private fun startScraping() {
        scrapeJob?.cancel()
        scrapeJob = lifecycleScope.launch(Dispatchers.IO) {
            var index = 0
            while(index < 50 && isActive) {
                runOnUiThread {
                    binding.webView.loadUrl(getScrapingJavaScript(index))
                }
                index++
                Thread.sleep(1000)
            }
            Log.d("Scraping", "Finished scraping")
        }
    }

    private fun getScrapingJavaScript(index: Int): String {
        return """
            javascript:(function() {
                const elements = document.getElementsByClassName('rewlink');
                const elementsArray = [].slice.call(elements);
                elementsArray[$index].click();
            })()
        """.trimIndent()
    }
}
