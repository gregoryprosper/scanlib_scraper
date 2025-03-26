package com.gprosper.scanlibscrapper

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.countTxtView
import kotlinx.android.synthetic.main.activity_main.goButton
import kotlinx.android.synthetic.main.activity_main.resultsEditText
import kotlinx.android.synthetic.main.activity_main.scrapeButton
import kotlinx.android.synthetic.main.activity_main.urlEditText
import kotlinx.android.synthetic.main.activity_main.webView
import java.util.HashSet

class MainActivity : AppCompatActivity() {

    private val set = HashSet<String>()

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupListeners()

        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(this, "HTMLOUT")
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                if (request?.url?.toString()?.contains("goto") == true) {
                    open(request.url.toString(), urlEditText.text.toString())
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
                    if (it.clipData != null && it.clipData!!.itemCount > 0) {
                        it.clipData?.getItemAt(0)?.text?.let {
                            if (it.startsWith("https") || it.startsWith("http")) {
                                urlEditText.setText(it)
                                webView.loadUrl(it.toString())
                            }
                        }
                    }
                }
                else -> {
                }
            }
        }
    }

    private fun setupListeners() {
        goButton.setOnClickListener {
            webView.loadUrl(urlEditText.text.toString())
        }
        scrapeButton.setOnClickListener {
            startScraping()
        }

        resultsEditText.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ScanlibText", resultsEditText.text.toString())
            clipboard!!.setPrimaryClip(clip)
            set.clear()
            resultsEditText.text.clear()
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
        countTxtView.text = "Found: ${set.size} links"
        resultsEditText.setText(set.joinToString(separator = "\n\n"))
    }

    private fun startScraping() {
        Thread {
            var index = 0
            while(index < 50) {
                runOnUiThread {
                    webView.loadUrl(getScrapingJavaScript(index))
                }
                index++
                Thread.sleep(1000)
            }
        }.start()
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
