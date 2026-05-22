package com.chronoswing.buddydash.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.chronoswing.buddydash.ui.findActivity
import kotlinx.coroutines.launch

/**
 * Displays Bambuddy's multipart MJPEG stream (OpenAPI: GET …/camera/stream?token=&fps=).
 * Uses a [WebView] with an &lt;img&gt; tag, matching the Bambuddy web UI pattern.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PrinterCameraMjpegStream(
    streamUrl: String,
    modifier: Modifier = Modifier,
    onStreamFailed: () -> Unit = {},
    onLoadingChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(streamUrl) {
        onLoadingChanged(true)
    }

    Box(
        modifier = modifier.background(Color.Black),
    ) {
        key(streamUrl) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { _ ->
                    val webContext = context.findActivity() ?: context
                    try {
                        WebView(webContext).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            setBackgroundColor(AndroidColor.BLACK)
                            settings.javaScriptEnabled = false
                            settings.domStorageEnabled = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.cacheMode = WebSettings.LOAD_NO_CACHE
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    view?.post { onLoadingChanged(false) }
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?,
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        view?.post {
                                            scope.launch { onStreamFailed() }
                                        }
                                    }
                                }
                            }
                            loadCameraStreamHtml(streamUrl)
                        }
                    } catch (_: Exception) {
                        scope.launch { onStreamFailed() }
                        View(webContext).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            setBackgroundColor(AndroidColor.BLACK)
                        }
                    }
                },
                update = { /* Resize only; do not reload on recomposition. */ },
                onRelease = { view ->
                    runCatching {
                        if (view is WebView) {
                            view.stopLoading()
                            view.loadUrl("about:blank")
                            (view.parent as? ViewGroup)?.removeView(view)
                            view.destroy()
                        }
                    }
                },
            )
        }
    }
}

private fun WebView.loadCameraStreamHtml(streamUrl: String) {
    val safeUrl = streamUrl.htmlAttributeEscaped()
    val html = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
          html, body {
            margin: 0;
            padding: 0;
            width: 100%;
            height: 100%;
            background: #000;
            overflow: hidden;
          }
          img {
            width: 100%;
            height: 100%;
            object-fit: contain;
            display: block;
            background: #000;
          }
        </style>
        </head>
        <body>
          <img src="$safeUrl" alt="camera stream"/>
        </body>
        </html>
    """.trimIndent()
    val baseUrl = streamUrl.substringBefore('?')
    loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
}

private fun String.htmlAttributeEscaped(): String = buildString(length) {
    for (ch in this@htmlAttributeEscaped) {
        when (ch) {
            '&' -> append("&amp;")
            '"' -> append("&quot;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            else -> append(ch)
        }
    }
}
