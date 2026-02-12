package com.example.peterbondra

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.peterbondra.ui.CreateTaskDialog
import com.example.peterbondra.ui.DoneScreen
import com.example.peterbondra.ui.PeterBondraTheme
import com.example.peterbondra.ui.SettingsScreen
import com.example.peterbondra.ui.TodoScreen

private enum class AppTab {
    TODO,
    DONE,
}

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by appViewModel.themeMode.collectAsState()

            PeterBondraTheme(themeMode = themeMode) {
                RequestNotificationPermission()
                ObserveAppForeground {
                    appViewModel.onAppForeground()
                }
                MainScreen(appViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(viewModel: AppViewModel) {
    val todoTasks by viewModel.todoTasks.collectAsState()
    val doneTasks by viewModel.doneTasks.collectAsState()
    val showBibleQuotes by viewModel.showBibleQuotes.collectAsState()
    val bibleQuote by viewModel.bibleQuote.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(AppTab.TODO.ordinal) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("PETERBONDRAJAKCINKA") },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                )

                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == AppTab.TODO.ordinal,
                        onClick = { selectedTab = AppTab.TODO.ordinal },
                        text = { Text("TODO") },
                    )
                    Tab(
                        selected = selectedTab == AppTab.DONE.ordinal,
                        onClick = { selectedTab = AppTab.DONE.ordinal },
                        text = { Text("DONE") },
                    )
                }

                AnimatedVisibility(visible = showBibleQuotes && bibleQuote != null) {
                    Text(
                        text = bibleQuote?.let { "\u201C${it.text}\u201D (${it.reference})" }.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == AppTab.TODO.ordinal) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add task") },
                )
            }
        },
    ) { innerPadding ->
        val colors = MaterialTheme.colorScheme
        val gradient = Brush.verticalGradient(
            colors = listOf(
                colors.surface,
                colors.surfaceVariant.copy(alpha = 0.35f),
            ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(innerPadding),
        ) {
            if (selectedTab == AppTab.TODO.ordinal) {
                TodoScreen(
                    tasks = todoTasks,
                    onMarkDone = viewModel::markDone,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                DoneScreen(
                    tasks = doneTasks,
                    onDelete = viewModel::deleteDoneTask,
                    onRestore = viewModel::markTodo,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateTaskDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { text, intensity ->
                viewModel.addTask(text, intensity)
                showCreateDialog = false
            },
        )
    }

    if (showSettings) {
        SettingsScreen(
            themeMode = themeMode,
            onThemeModeChange = viewModel::setThemeMode,
            showBibleQuotes = showBibleQuotes,
            onBibleQuotesToggle = viewModel::setBibleQuotesEnabled,
            onRefreshQuote = viewModel::refreshQuote,
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun ObserveAppForeground(onForeground: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                onForeground()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {},
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

