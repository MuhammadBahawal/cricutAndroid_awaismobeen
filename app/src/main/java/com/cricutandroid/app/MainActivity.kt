package com.cricutandroid.app

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cricutandroid.app.models.AppDataItem
import com.cricutandroid.app.models.Craft
import com.cricutandroid.app.models.DraftProject
import com.cricutandroid.app.ui.screens.*
import com.cricutandroid.app.ui.theme.CriCutAndroidTheme
import com.cricutandroid.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File

sealed class Screen {
    object Splash : Screen()
    object MainTabs : Screen()
    data class Editor(val initialSvgFile: File? = null, val initialDraft: DraftProject? = null) : Screen()
    data class Save(val previewBitmap: Bitmap, val svgString: String) : Screen()
    data class Mockup(val designBitmap: Bitmap) : Screen()
    object Drafts : Screen()
    object Crafts : Screen()
    data class CraftDetails(val craft: Craft, val isPremium: Boolean) : Screen()
    data class AllCategories(val type: CurrentDataType) : Screen()
    data class DataList(val type: CurrentDataType, val category: AppDataItem) : Screen()
}

enum class NavigationTab(val title: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Create("Create", Icons.Default.AutoAwesome),
    Setting("Setting", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemNavigationBar()
        setContent {
            CriCutAndroidTheme {
                MainAppHost()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemNavigationBar()
        }
    }

    private fun hideSystemNavigationBar() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.navigationBars())
    }
}

@Composable
fun MainAppHost(viewModel: MainViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var selectedTab by remember { mutableStateOf(NavigationTab.Home) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Navigation backstack tracking
    val screenStack = remember { mutableStateListOf<Screen>() }

    fun navigateTo(screen: Screen) {
        screenStack.add(currentScreen)
        currentScreen = screen
    }

    fun navigateBack() {
        if (screenStack.isNotEmpty()) {
            currentScreen = screenStack.removeAt(screenStack.lastIndex)
        } else if (currentScreen != Screen.MainTabs) {
            currentScreen = Screen.MainTabs
        }
    }

    // Intercept system back button
    BackHandler(enabled = currentScreen != Screen.Splash && (currentScreen != Screen.MainTabs || drawerState.isOpen)) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            navigateBack()
        }
    }

    // Paywall Modal
    if (viewModel.showPaywall) {
        SubscriptionPaywallDialog(
            onDismiss = { viewModel.showPaywall = false },
            onSuccess = {
                viewModel.showPaywall = false
                viewModel.refreshPremiumStatus()
            }
        )
    }

    // Main Navigation Drawer wrapping the application
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentScreen == Screen.MainTabs,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFF8F9FA),
                modifier = Modifier.width(310.dp)
            ) {
                LeftMenuDrawerContent(
                    isProUser = viewModel.isProUser,
                    onClose = { coroutineScope.launch { drawerState.close() } },
                    onOpenPaywall = { viewModel.showPaywall = true },
                    onOpenImportGuide = {
                        // Managed inside LeftMenuDrawer
                    },
                    onOpenDrafts = {
                        coroutineScope.launch { drawerState.close() }
                        navigateTo(Screen.Drafts)
                    }
                )
            }
        }
    ) {
        Crossfade(targetState = currentScreen, label = "ScreenTransition") { targetScreen ->
            when (targetScreen) {
                is Screen.Splash -> {
                    SplashScreen(
                        onFinished = {
                            currentScreen = Screen.MainTabs
                        }
                    )
                }

                is Screen.MainTabs -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Main screen content based on tab
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            when (selectedTab) {
                                NavigationTab.Home -> {
                                    HomeScreen(
                                        viewModel = viewModel,
                                        onOpenDrawer = {
                                            coroutineScope.launch { drawerState.open() }
                                        },
                                        onOpenPaywall = { viewModel.showPaywall = true },
                                        onOpenCrafts = { navigateTo(Screen.Crafts) },
                                        onOpenCraftDetail = { craft, isPrem ->
                                            navigateTo(Screen.CraftDetails(craft, isPrem))
                                        },
                                        onOpenCategory = { catType ->
                                            navigateTo(Screen.AllCategories(catType))
                                        },
                                        onOpenAiGenerator = {
                                            selectedTab = NavigationTab.Create
                                        },
                                        onEditSvg = { file ->
                                            navigateTo(Screen.Editor(initialSvgFile = file))
                                        }
                                    )
                                }

                                NavigationTab.Create -> {
                                    AIEditingScreen(
                                        viewModel = viewModel,
                                        onOpenPaywall = { viewModel.showPaywall = true },
                                        onDesignGenerated = { bitmap, svgString ->
                                            navigateTo(Screen.Save(bitmap, svgString))
                                        }
                                    )
                                }

                                NavigationTab.Setting -> {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onOpenPaywall = { viewModel.showPaywall = true },
                                        onOpenDrafts = { navigateTo(Screen.Drafts) }
                                    )
                                }
                            }
                        }

                        // Floating iPhone Glassy Rounded Tab Bar
                        IPhoneGlassyBottomTabBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 20.dp)
                        )
                    }
                }

                is Screen.Editor -> {
                    EditorScreen(
                        initialSvgFile = targetScreen.initialSvgFile,
                        initialDraft = targetScreen.initialDraft,
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onExport = { bitmap, svgStr ->
                            navigateTo(Screen.Save(bitmap, svgStr))
                        },
                        onOpenPaywall = { viewModel.showPaywall = true }
                    )
                }

                is Screen.Save -> {
                    SaveScreen(
                        previewBitmap = targetScreen.previewBitmap,
                        svgString = targetScreen.svgString,
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onOpenMockup = { bitmap ->
                            navigateTo(Screen.Mockup(bitmap))
                        },
                        onOpenPaywall = { viewModel.showPaywall = true }
                    )
                }

                is Screen.Mockup -> {
                    MockupScreen(
                        designBitmap = targetScreen.designBitmap,
                        onBack = { navigateBack() }
                    )
                }

                is Screen.Drafts -> {
                    DraftsScreen(
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onOpenProject = { draft ->
                            navigateTo(Screen.Editor(initialDraft = draft))
                        },
                        onNewProject = {
                            navigateTo(Screen.Editor())
                        }
                    )
                }

                is Screen.Crafts -> {
                    CraftsScreen(
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onSelectCraft = { craft, isPrem ->
                            navigateTo(Screen.CraftDetails(craft, isPrem))
                        }
                    )
                }

                is Screen.CraftDetails -> {
                    CraftDetailsScreen(
                        craft = targetScreen.craft,
                        isPremium = targetScreen.isPremium,
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onOpenPaywall = { viewModel.showPaywall = true },
                        onEditSvg = { file ->
                            navigateTo(Screen.Editor(initialSvgFile = file))
                        }
                    )
                }

                is Screen.AllCategories -> {
                    AllCategoriesScreen(
                        type = targetScreen.type,
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onSelectCategory = { type, category ->
                            navigateTo(Screen.DataList(type, category))
                        },
                        onOpenPaywall = { viewModel.showPaywall = true },
                        onEditSvg = { file ->
                            navigateTo(Screen.Editor(initialSvgFile = file))
                        }
                    )
                }

                is Screen.DataList -> {
                    DataListScreen(
                        type = targetScreen.type,
                        category = targetScreen.category,
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onOpenPaywall = { viewModel.showPaywall = true },
                        onEditSvg = { file ->
                            navigateTo(Screen.Editor(initialSvgFile = file))
                        }
                    )
                }
            }
        }
    }
}

/**
 * Modern iPhone-style floating greenish glassy rounded tab bar with icons only (no text labels).
 * Uses reduced transparency with a rich frosted mint/emerald glass finish and specular highlight.
 */
@Composable
fun IPhoneGlassyBottomTabBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 42.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(27.dp),
                    ambientColor = Color(0xFF08B67D).copy(alpha = 0.16f),
                    spotColor = Color(0xFF048344).copy(alpha = 0.24f)
                )
                .clip(RoundedCornerShape(27.dp))
                // Subtle greenish frosted glass gradient with low transparency (~96% opacity)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF0FAF5).copy(alpha = 0.97f),
                            Color(0xFFD8F3E6).copy(alpha = 0.95f)
                        )
                    )
                )
                // Specular greenish glass border
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF08B67D).copy(alpha = 0.40f),
                            Color(0xFF08B67D).copy(alpha = 0.15f)
                        )
                    ),
                    shape = RoundedCornerShape(27.dp)
                )
                // Crisp hairline reflection
                .border(
                    width = 0.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(27.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val inactiveColor = Color(0xFF3F6355)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onTabSelected(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF08B67D),
                                                Color(0xFF059A68)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color.Transparent
                                            )
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) Color.White else inactiveColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}