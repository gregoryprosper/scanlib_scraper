package com.gprosper.scanlibscrapper

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.goButton
import kotlinx.android.synthetic.main.activity_main.resultsEditText
import kotlinx.android.synthetic.main.activity_main.urlEditText
import kotlinx.android.synthetic.main.activity_main.webView
import java.util.HashSet

class MainActivity : AppCompatActivity() {

    private val set = HashSet<String>()

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupListeners()

        webView.settings.javaScriptEnabled = true
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

        resultsEditText.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ScanlibText", resultsEditText.text.toString())
            clipboard!!.setPrimaryClip(clip)
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
                    if (!set.contains(request.url.toString())) {
                        set.add(request.url.toString())
                        resultsEditText.text.append("${request.url}\n\n")
                    }
                } else if (request?.url?.host?.contains("turbobit.net") == true) {
                    if (!set.contains(request.url.toString())) {
                        set.add(request.url.toString())
                        resultsEditText.text.append("${request.url}\n\n")
                    }
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
}
