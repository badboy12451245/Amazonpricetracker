package com.amazonpricetracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.amazonpricetracker.ui.theme.AmazonpricetrackerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AmazonpricetrackerTheme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val baseUrl = "https://apricetracker.ooguy.com/"
    var currentUrl by remember { mutableStateOf(baseUrl) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Intercept back button to navigate WebView back history if possible
    BackHandler(enabled = webViewInstance?.canGoBack() == true) {
        webViewInstance?.goBack()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Price Tracker Menu",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Home
                    NavigationDrawerItem(
                        label = { Text("Home") },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        selected = currentUrl == baseUrl,
                        onClick = {
                            currentUrl = baseUrl
                            scope.launch { drawerState.close() }
                        }
                    )

                    // 2. Amazon Alerts
                    NavigationDrawerItem(
                        label = { Text("Amazon Alerts") },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Amazon Alerts") },
                        selected = currentUrl.contains("manage.php") || currentUrl.contains("manage_alerts.php"),
                        onClick = {
                            currentUrl = "${baseUrl}manage_alerts.php"
                            scope.launch { drawerState.close() }
                        }
                    )

                    // 3. Costco Alerts
                    NavigationDrawerItem(
                        label = { Text("Costco Alerts") },
                        icon = { Icon(Icons.Default.Store, contentDescription = "Costco Alerts") },
                        selected = currentUrl.contains("manage_costco.php"),
                        onClick = {
                            currentUrl = "${baseUrl}manage_costco.php"
                            scope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. Open in External Browser (Opens https://apricetracker.ooguy.com)
                    NavigationDrawerItem(
                        label = { Text("Open Website in Browser") },
                        icon = { Icon(Icons.Default.OpenInBrowser, contentDescription = "Open Browser") },
                        selected = false,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://apricetracker.ooguy.com"))
                            context.startActivity(intent)
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Price Tracker") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Toggle Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                WebViewScreen(
                    url = currentUrl,
                    onWebViewCreated = { webViewInstance = it }
                )
            }
        }
    }
}

@Composable
fun WebViewScreen(
    url: String,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val requestUrl = request?.url?.toString() ?: return false

                        // If user clicks a link outside apricetracker.ooguy.com (e.g. Amazon or Costco product page), launch native browser
                        if (!requestUrl.contains("apricetracker.ooguy.com")) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl))
                            context.startActivity(intent)
                            return true
                        }
                        return false
                    }
                }

                onWebViewCreated(this)
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        }
    )
}