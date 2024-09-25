package com.junkfood.seal.ui.page.settings.network

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.google.android.material.R
import com.junkfood.seal.util.PreferenceUtil.updateString
import com.junkfood.seal.util.USER_AGENT_STRING
import com.junkfood.seal.util.connectWithDelimiter
import org.koin.androidx.compose.koinViewModel


private const val TAG = "WebViewPage"

data class Cookie(
    val domain: String = "",
    val name: String = "",
    val value: String = "",
    val includeSubdomains: Boolean = true,
    val path: String = "/",
    val secure: Boolean = true,
    val expiry: Long = 0L,
) {
    constructor(
        url: String,
        name: String,
        value: String
    ) : this(domain = url.toDomain(), name = name, value = value)

    fun toNetscapeCookieString(): String {
        return connectWithDelimiter(
            domain,
            includeSubdomains.toString().uppercase(),
            path,
            secure.toString().uppercase(),
            expiry.toString(),
            name,
            value,
            delimiter = "\u0009"
        )
    }
}


private val domainRegex = Regex("""http(s)?://(\w*(www|m|account|sso))?|/.*""")
private fun String.toDomain(): String {
    return this.replace(domainRegex, "")
}

private fun makeCookie(url: String, cookieString: String): Cookie {
    cookieString.split("=").run {
        return Cookie(url = url, name = first(), value = last())
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewPage(
    cookiesViewModel: CookiesViewModel,
    onDismissRequest: () -> Unit
) {

    val state by cookiesViewModel.stateFlow.collectAsStateWithLifecycle()
    Log.d(TAG, state.editingCookieProfile.url)


    val cookieManager = CookieManager.getInstance()
    val cookieSet = remember { mutableSetOf<Cookie>() }
    val websiteUrl = state.editingCookieProfile.url
    val webViewState = rememberWebViewState(websiteUrl)

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        TopAppBar(
            title = { Text(webViewState.pageTitle.toString(), maxLines = 1) },
            navigationIcon = {
                IconButton(
                    onClick = { onDismissRequest() }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        stringResource(id = androidx.appcompat.R.string.abc_action_mode_done)
                    )
                }
            }, actions = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(id = R.string.abc_action_mode_done))
                }
            }
        )
    }) { paddingValues ->
        val webViewClient = remember {
            object : AccompanistWebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    super.onPageFinished(view, url)
                    if (url.isNullOrEmpty()) return
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    return if (request?.url?.scheme?.contains("http") == true)
                        super.shouldOverrideUrlLoading(view, request)
                    else true
                }
            }

        }
        val webViewChromeClient = remember {
            object : AccompanistWebChromeClient() {
            }
        }
        WebView(
            state = webViewState, client = webViewClient, chromeClient = webViewChromeClient,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            captureBackPresses = true, factory = { context ->
                WebView(context).apply {
                    settings.run {
                        javaScriptCanOpenWindowsAutomatically = true
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        USER_AGENT_STRING.updateString(userAgentString)
                    }
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                }
            }
        )


    }
}