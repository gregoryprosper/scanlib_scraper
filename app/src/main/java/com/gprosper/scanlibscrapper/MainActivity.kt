package com.gprosper.scanlibscrapper

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gprosper.scanlibscrapper.model.Page
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import pl.droidsonroids.jspoon.HtmlAdapter
import pl.droidsonroids.jspoon.Jspoon
import java.io.IOException
import java.net.URL
import java.util.concurrent.CountDownLatch


class MainActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupListeners()

        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(this, "HTMLOUT")
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false;
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadClipboardUrl()
    }

    @JavascriptInterface
    fun processHTML(html: String) {
        runOnUiThread {
            scrapeButton.isEnabled = false
            scrapeSite(html) {
                progressBar.visibility = View.GONE
                scrapeButton.isEnabled = true
            }
        }
    }

    private fun loadClipboardUrl() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip()){
            clipboard.primaryClip?.let { clipData ->
                clipData.getItemAt(0)?.let { item ->
                    if (item.text.startsWith("https")){
                        urlEditText.setText(item.text)
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        goButton.setOnClickListener {
            webView.loadUrl(urlEditText.text.toString());
        }

        scrapeButton.setOnClickListener {
            webView.loadUrl("javascript:window.HTMLOUT.processHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');")
        }

        resultsEditText.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ScanlibText", resultsEditText.text.toString())
            clipboard!!.setPrimaryClip(clip)
            resultsEditText.text.clear()
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_LONG).show();
            return@setOnLongClickListener true
        }
    }

    private fun scrapeSite(html: String, completed: () -> Unit) {
        progressBar.progress = 0
        progressBar.visibility = View.VISIBLE

        val url = URL(urlEditText.text.toString().let {
            when {
                it.endsWith("/") -> it
                else -> "${it}/"
            }
        })
        val jspoon = Jspoon.create()
        val htmlAdapter: HtmlAdapter<Page> = jspoon.adapter(Page::class.java)
        val client = OkHttpClient()

        Thread {
            val page = htmlAdapter.fromHtml(html)
            val countdownLatch = CountDownLatch(page.linkList.size)

            progressBar.max = page.linkList.size

            page.linkList.forEach {
                val linkUrl = "${url}${it}"
                val request = Request.Builder()
                        .url(linkUrl)
                        .addHeader("Referer", url.toString())
                        .build()

                client.newCall(request).enqueue(object: Callback {
                    override fun onFailure(call: Call?, e: IOException?) {
                        this@MainActivity.runOnUiThread {
                            progressBar.progress++
                            Toast.makeText(this@MainActivity, "Error occured.", Toast.LENGTH_LONG).show()
                        }
                        countdownLatch.countDown()
                    }

                    override fun onResponse(call: Call?, response: Response?) {
                        Log.d("GREG", response.toString());
                        response?.priorResponse()?.let {
                            val redirectUrl: String? = it.header("Location")?.trimStart('/')?.trimStart('/')
                            redirectUrl?.let rd@{
                                if (it.startsWith("https://scanlibs.com"))
                                    return@rd

                                if (it.startsWith("http")) {
                                    runOnUiThread {
                                        resultsEditText.text.append("${it}\n\n")
                                    }
                                } else {
                                    val fixedUrl = "http://" + it
                                    runOnUiThread {
                                        resultsEditText.text.append("${fixedUrl}\n\n")
                                    }
                                }
                            }
                        }

                        progressBar.progress++
                        countdownLatch.countDown()
                    }
                })
                Thread.sleep(1000)
            }

            countdownLatch.await()
            runOnUiThread {
                completed()
            }
        }.start()
    }
}
