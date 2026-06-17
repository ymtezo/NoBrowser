package com.rickgram.NoBrowser

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import android.os.Build
import android.app.DownloadManager
import android.os.Environment
import android.net.Uri
import android.widget.Toast
import android.content.Context


class MainActivity : AppCompatActivity() {
    private lateinit var myWebView: WebView
    private lateinit var urlTextView: TextView
    private lateinit var toolbarTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up the Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        //Initialize WebView
        myWebView = findViewById(R.id.webview)

        // Security: Disable file and content access
        myWebView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
        }

        // Set up the WebViewClient to update the URL in the Toolbar
        myWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                // Update Toolbar title with the current URL
                supportActionBar?.title = url
            }
        }

        // Set up the DownloadListener
        myWebView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val request = DownloadManager.Request(Uri.parse(url))

            // Setting the download file type
            request.setMimeType(mimeType)
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Downloading file...")
            
            // Security: Sanitize filename to prevent path traversal
            val fileName = contentDisposition.substringAfter("filename=", "downloaded_file")
                .replace("\"", "")
                .substringAfterLast("/")
                .substringAfterLast("\\")

            request.setTitle(fileName)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            
            // On Android 10+, WRITE_EXTERNAL_STORAGE is not required for DIRECTORY_DOWNLOADS using DownloadManager
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(applicationContext, "Downloading File...", Toast.LENGTH_LONG).show()
        }


        // Intent handling for receiving URLs from other apps
        val intent = intent
        val action = intent.action
        val data = intent.data
        
        if (action == Intent.ACTION_VIEW && data != null) {
            val scheme = data.scheme
            // Security: Only allow http and https schemes
            if (scheme == "http" || scheme == "https") {
                myWebView.loadUrl(data.toString())
            } else {
                Toast.makeText(this, "Unsupported URL scheme", Toast.LENGTH_SHORT).show()
                myWebView.loadUrl("https://altl.io/")
            }
        } else {
            // Load a default URL if no intent data is received
            myWebView.loadUrl("https://altl.io/")
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                shareCurrentUrl()
                true
            }
            R.id.action_refresh -> {
                refreshPage()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareCurrentUrl() {
        val currentUrl = myWebView.url
        if (currentUrl != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, currentUrl)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Share URL via"))
        }
    }

    private fun refreshPage() {
        myWebView.reload() // Reloads the current page in the WebView
    }


}
