package com.example.auraarchive.ui.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.auraarchive.module.AppNavigationItems
import com.example.auraarchive.ui.components.TopBar
import com.example.auraarchive.ui.screens.AboutScreen
import com.example.auraarchive.ui.screens.DocContentScreen
import com.example.auraarchive.ui.screens.HomeScreen
import com.example.auraarchive.ui.screens.DraftReviewScreen
import com.example.auraarchive.ui.screens.ResourceScreen
import com.example.auraarchive.ui.viewModel.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val backstack = remember {
        mutableStateListOf<AppNavigationItems>(AppNavigationItems.Home)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val uiState by homeViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.navigateToDraft.collect { postId ->
            val current = backstack.lastOrNull()
            if (current !is AppNavigationItems.DraftReview || current.postId != postId) {
                backstack.add(AppNavigationItems.DraftReview(postId))
            }
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { homeViewModel.uploadAudio(it) }
    }
//
//    val docPickerLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.GetContent()
//    ) { uri: Uri? ->
//        uri?.let { homeViewModel.uploadDocument(it) }
//    }

    val currentScreen = backstack.lastOrNull() ?: AppNavigationItems.Home
    val isFullscreenScreen =
        currentScreen is AppNavigationItems.DocContent || currentScreen is AppNavigationItems.DraftReview

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Home") },
                    selected = currentScreen is AppNavigationItems.Home,
                    onClick = {
                        scope.launch { drawerState.close() }
                        backstack.clear()
                        backstack.add(AppNavigationItems.Home)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.List, null) },
                    label = { Text("Resources") },
                    selected = currentScreen is AppNavigationItems.Resource,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentScreen !is AppNavigationItems.Resource) {
                            backstack.add(AppNavigationItems.Resource)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Info, null) },
                    label = { Text("About") },
                    selected = currentScreen is AppNavigationItems.About,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentScreen !is AppNavigationItems.About) {
                            backstack.add(AppNavigationItems.About)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (!isFullscreenScreen) {
                    TopBar(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        title = when (currentScreen) {
                            is AppNavigationItems.Home -> "Home"
                            is AppNavigationItems.About -> "About"
                            is AppNavigationItems.Resource -> "Resources"
                            else -> ""
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavDisplay(
                    backStack = backstack,
                    modifier = Modifier.fillMaxSize(),
                    onBack = { if (backstack.size > 1) backstack.removeLastOrNull() },
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(200)) +
                                slideInHorizontally(
                                    animationSpec = tween(200),
                                    initialOffsetX = { it / 10 }))
                            .togetherWith(fadeOut(animationSpec = tween(150)))
                    },
                    popTransitionSpec = {
                        fadeIn(animationSpec = tween(200))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(200),
                                    targetOffsetX = { it / 10 }) +
                                        fadeOut(animationSpec = tween(150))
                            )
                    },
                    entryProvider = entryProvider {
                        entry<AppNavigationItems.Home> {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onCardClick = { id ->
                                    val post = uiState.posts.find { it.id == id }
                                    if (post?.status == "REVIEW_PENDING") {
                                        backstack.add(AppNavigationItems.DraftReview(id))
                                    } else if (post?.status == "PUBLISHED") {
                                        backstack.add(AppNavigationItems.DocContent(id))
                                    }
                                }
                            )
                        }
                        entry<AppNavigationItems.About> {
                            AboutScreen()
                        }
                        entry<AppNavigationItems.Resource> {
                            ResourceScreen(
                                onResourceClick = { id ->
                                    backstack.add(AppNavigationItems.DocContent(id))
                                }
                            )
                        }
                        entry<AppNavigationItems.DraftReview> { item ->
                            DraftReviewScreen(
                                postId = item.postId,
                                onBack = { if (backstack.size > 1) backstack.removeLastOrNull() }
                            )
                        }
                        entry<AppNavigationItems.DocContent> { item ->
                            val post = uiState.posts.find { it.id == item.postId }
                            post?.let {
                                DocContentScreen(
                                    post = it,
                                    onBack = { if (backstack.size > 1) backstack.removeLastOrNull() }
                                )
                            }
                        }
                    }
                )

                if (currentScreen == AppNavigationItems.Home) {
                    PersistentActionButtons(
                        onUploadAudio = { audioPickerLauncher.launch("audio/*") },
                        onUploadDoc = { }
                    )
                }
            }
        }
    }
}

@Composable
fun PersistentActionButtons(
    onUploadAudio: () -> Unit,
    onUploadDoc: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var isExpanded by remember { mutableStateOf(false) }
    val anchorSpacerWidth = 130.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 32.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
                    exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut()
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            onUploadAudio()
                            isExpanded = false
                        },
                        containerColor = colorScheme.secondaryContainer,
                        contentColor = colorScheme.onSecondaryContainer,
                        icon = { Icon(Icons.Default.AudioFile, null) },
                        text = { Text("Audio") },
                        shape = RoundedCornerShape(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(anchorSpacerWidth))

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                    exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            onUploadDoc()
                            isExpanded = false
                        },
                        containerColor = colorScheme.secondaryContainer,
                        contentColor = colorScheme.onSecondaryContainer,
                        icon = { Icon(Icons.Default.Description, null) },
                        text = { Text("Doc") },
                        shape = RoundedCornerShape(24.dp)
                    )
                }
            }

            LargeFloatingActionButton(
                onClick = { isExpanded = !isExpanded },
                shape = RoundedCornerShape(40.dp),
                containerColor = if (isExpanded) colorScheme.errorContainer else colorScheme.primaryContainer,
                contentColor = if (isExpanded) colorScheme.onErrorContainer else colorScheme.onPrimaryContainer,
                modifier = Modifier.zIndex(1f)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}