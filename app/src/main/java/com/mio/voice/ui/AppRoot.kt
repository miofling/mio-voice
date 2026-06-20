@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.mio.voice.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mio.voice.BuildConfig
import com.mio.voice.R
import com.mio.voice.data.EmotionPreset
import com.mio.voice.director.OpenAiCompatibleDirectorProvider
import com.mio.voice.data.SegmentStatus
import com.mio.voice.data.VoiceLibrary
import com.mio.voice.provider.MiniMaxVoiceModify
import com.mio.voice.provider.OfficialVoice
import com.mio.voice.provider.OfficialVoiceClassifier
import com.mio.voice.data.VoiceProfile
import com.mio.voice.data.generation.AudioCollection
import com.mio.voice.data.generation.CollectionFormLogic
import com.mio.voice.data.generation.CollectionSummary
import com.mio.voice.data.generation.GeneratedAudioGroup
import com.mio.voice.data.generation.GenerationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_TEXT_LENGTH = 5000
private const val AVATAR_PREVIEW_MAX_EDGE = 512

/**
 * 首页统一设计 token：圆角 / 高度 / 图标尺寸 / 阴影。
 * 所有按钮、切换控件、配置卡片与导航复用这套尺度，避免逐个硬编码。
 */
private object MioStyle {
    val PrimaryButtonHeight = 54.dp
    val PrimaryButtonRadius = 18.dp

    val SegmentHeight = 40.dp
    val SegmentOuterRadius = 14.dp
    val SegmentThumbRadius = 10.dp
    val SegmentPadding = 4.dp

    val CardRadius = 14.dp
    val RowHeight = 58.dp
    val IconBadgeSize = 30.dp
    val RowIconSize = 18.dp
    val ChevronSize = 18.dp

    val LightShadow = 2.dp
    val CardShadow = 1.dp
}

// 轻量顶层导航：主页面（带底部导航）/ 历史列表 / 语音详情。仅在状态中携带 groupId。
// 路由编码与出栈逻辑由 NavBackStack（纯逻辑，可单测）统一提供。
private const val SCREEN_HISTORY = NavBackStack.SCREEN_HISTORY
private const val SCREEN_DETAIL = NavBackStack.SCREEN_DETAIL
private const val SCREEN_SETTINGS_TTS = NavBackStack.SCREEN_SETTINGS_TTS
private const val SCREEN_SETTINGS_AI = NavBackStack.SCREEN_SETTINGS_AI
private const val SCREEN_SETTINGS_PROMPT = NavBackStack.SCREEN_SETTINGS_PROMPT
private const val SCREEN_VOICE_DETAIL = NavBackStack.SCREEN_VOICE_DETAIL
private const val SCREEN_VOICE_NEW = NavBackStack.SCREEN_VOICE_NEW
private const val SCREEN_VOICE_EDIT = NavBackStack.SCREEN_VOICE_EDIT
private const val SCREEN_PRESET_NEW = NavBackStack.SCREEN_PRESET_NEW
private const val SCREEN_PRESET_EDIT = NavBackStack.SCREEN_PRESET_EDIT
private const val SCREEN_COLLECTION_DETAIL = NavBackStack.SCREEN_COLLECTION_DETAIL
private const val SCREEN_COLLECTION_PICKER = NavBackStack.SCREEN_COLLECTION_PICKER
private const val SCREEN_ABOUT = NavBackStack.SCREEN_ABOUT
private const val SCREEN_ABOUT_LICENSE = NavBackStack.SCREEN_ABOUT_LICENSE
private const val SCREEN_ABOUT_PRIVACY = NavBackStack.SCREEN_ABOUT_PRIVACY

@Composable
fun AppRoot(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf("home") }
    // 轻量返回栈：保存每个非主页面的路由（详情页编码为 "detail/{groupId}"）。
    // 栈为空 = 主页面；栈顶 = 当前页面。系统返回键与左上角返回都执行 popBackStack（出栈一层）。
    val backStack = rememberSaveable(saver = navBackStackSaver) { mutableStateListOf<String>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun toast(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    /** 出栈一层；已位于主页面时返回 false（交回系统默认逻辑退出 App）。 */
    fun popBackStack(): Boolean {
        val result = NavBackStack.pop(backStack)
        if (!result.handled) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    fun navigateToHistory() = backStack.add(NavBackStack.historyRoute())
    fun openDetail(groupId: String) = backStack.add(NavBackStack.detailRoute(groupId))
    fun openVoiceDetail(voiceProfileId: String) = backStack.add(NavBackStack.voiceDetailRoute(voiceProfileId))
    fun openVoiceNew() = backStack.add(NavBackStack.voiceNewRoute())
    fun openVoiceEdit(voiceProfileId: String) = backStack.add(NavBackStack.voiceEditRoute(voiceProfileId))
    fun openPresetNew(voiceProfileId: String) = backStack.add(NavBackStack.presetNewRoute(voiceProfileId))
    fun openPresetEdit(voiceProfileId: String, presetId: String) =
        backStack.add(NavBackStack.presetEditRoute(voiceProfileId, presetId))
    fun openCollectionDetail(collectionId: String) =
        backStack.add(NavBackStack.collectionDetailRoute(collectionId))
    fun openCollectionPicker(collectionId: String) =
        backStack.add(NavBackStack.collectionPickerRoute(collectionId))

    val currentScreen = NavBackStack.currentScreen(backStack)
    val currentDetailGroupId = NavBackStack.currentDetailGroupId(backStack)
    val currentVoiceProfileId = NavBackStack.currentVoiceProfileId(backStack)
    val currentPresetId = NavBackStack.currentPresetId(backStack)
    val currentCollectionId = NavBackStack.currentCollectionId(backStack)
    val currentCollectionPickerId = NavBackStack.currentCollectionPickerId(backStack)

    // 页面切换过渡：以返回栈深度判断 push（进入子页，从右滑入）/ pop（返回，向右滑出）。
    val navDepth = backStack.size

    // 导出语音：观察 pendingExport 事件 -> 拉起系统「保存到...」选择器（SAF）。
    // 记住当前导出的源文件路径，供选择器回调拷贝用。
    var exportSourcePath by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/*")
    ) { uri ->
        val src = exportSourcePath
        if (uri != null && src != null) viewModel.performExport(src, uri)
        exportSourcePath = null
    }
    LaunchedEffect(state.pendingExport) {
        state.pendingExport?.let { pending ->
            exportSourcePath = pending.sourcePath
            viewModel.clearPendingExport()
            runCatching { exportLauncher.launch(pending.suggestedName) }
                .onFailure { exportSourcePath = null }
        }
    }

    MioVoiceTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = navDepth to currentScreen,
                transitionSpec = {
                    val forward = targetState.first >= initialState.first
                    val durationMs = 280
                    if (forward) {
                        // push：新页从右滑入，旧页向左微移淡出。
                        (slideInHorizontally(animationSpec = tween(durationMs)) { it } + fadeIn(tween(durationMs)))
                            .togetherWith(
                                slideOutHorizontally(animationSpec = tween(durationMs)) { -it / 4 } + fadeOut(tween(durationMs))
                            )
                    } else {
                        // pop：旧页向右滑出，下层页从左微移淡入。
                        (slideInHorizontally(animationSpec = tween(durationMs)) { -it / 4 } + fadeIn(tween(durationMs)))
                            .togetherWith(
                                slideOutHorizontally(animationSpec = tween(durationMs)) { it } + fadeOut(tween(durationMs))
                            )
                    }.using(SizeTransform(clip = false))
                },
                label = "navTransition"
            ) { (_, screen) ->
            when (screen) {
                SCREEN_HISTORY -> HistoryScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { popBackStack() },
                    onOpenDetail = { openDetail(it) }
                )
                SCREEN_DETAIL -> AudioDetailScreen(
                    groupId = currentDetailGroupId,
                    state = state,
                    viewModel = viewModel,
                    onBack = { popBackStack() }
                )
                SCREEN_SETTINGS_TTS -> TtsSettingsScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { popBackStack() }
                )
                SCREEN_SETTINGS_AI -> AiSettingsScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { popBackStack() }
                )
                SCREEN_SETTINGS_PROMPT -> PromptSettingsScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { popBackStack() }
                )
                SCREEN_VOICE_DETAIL -> VoiceDetailScreen(
                    voiceProfileId = currentVoiceProfileId,
                    state = state,
                    viewModel = viewModel,
                    onBack = { popBackStack() },
                    onEditVoice = { openVoiceEdit(it) },
                    onAddPreset = { openPresetNew(it) },
                    onOpenPreset = { voiceId, presetId -> openPresetEdit(voiceId, presetId) }
                )
                SCREEN_VOICE_NEW -> VoiceProfileEditorScreen(
                    voiceProfileId = null,
                    state = state,
                    viewModel = viewModel,
                    onBack = { popBackStack() }
                )
                SCREEN_VOICE_EDIT -> VoiceProfileEditorScreen(
                    voiceProfileId = currentVoiceProfileId,
                    state = state,
                    viewModel = viewModel,
                    onBack = { popBackStack() }
                )
                SCREEN_PRESET_NEW -> PresetEditorScreen(
                    voiceProfileId = currentVoiceProfileId,
                    presetId = null,
                    state = state,
                    viewModel = viewModel,
                    onBack = { popBackStack() }
                )
                SCREEN_PRESET_EDIT -> PresetEditorScreen(
                    voiceProfileId = currentVoiceProfileId,
                    presetId = currentPresetId,
                    state = state,
                    viewModel = viewModel,
                    onBack = { popBackStack() }
                )
                SCREEN_COLLECTION_DETAIL -> CollectionDetailScreen(
                    collectionId = currentCollectionId,
                    state = state,
                    viewModel = viewModel,
                    onBack = { popBackStack() },
                    onOpenDetail = { openDetail(it) },
                    onAddRecords = { openCollectionPicker(it) }
                )
                SCREEN_COLLECTION_PICKER -> CollectionPickerScreen(
                    collectionId = currentCollectionPickerId,
                    state = state,
                    viewModel = viewModel,
                    onBack = { popBackStack() }
                )
                SCREEN_ABOUT -> AboutScreen(
                    onBack = { popBackStack() },
                    onOpenLicense = { backStack.add(NavBackStack.aboutLicenseRoute()) },
                    onOpenPrivacy = { backStack.add(NavBackStack.aboutPrivacyRoute()) }
                )
                SCREEN_ABOUT_LICENSE -> AboutLicenseScreen(onBack = { popBackStack() })
                SCREEN_ABOUT_PRIVACY -> AboutPrivacyScreen(onBack = { popBackStack() })
                else -> Scaffold(
                    topBar = {
                        MioTopBar(
                            onHistory = { navigateToHistory() },
                            onReserved = { toast("该功能开发中，敬请期待。") }
                        )
                    },
                    bottomBar = {
                        MioBottomBar(current = tab, onSelect = { tab = it })
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        state.credentialMessage?.let { InfoBanner(it, isError = true) }
                        when (tab) {
                            "home" -> HomeScreen(
                                state = state,
                                viewModel = viewModel,
                                onOpenDetail = { openDetail(it) },
                                onViewAll = { navigateToHistory() }
                            )
                            "voices" -> VoiceLibraryScreen(
                                state = state,
                                viewModel = viewModel,
                                onOpenVoice = { openVoiceDetail(it) },
                                onAddVoice = { openVoiceNew() },
                                onAddOfficial = { viewModel.fetchOfficialVoices() }
                            )
                            "library" -> AudioLibraryScreen(
                                state = state,
                                viewModel = viewModel,
                                onOpenCollection = { openCollectionDetail(it) }
                            )
                            else -> SettingsScreen(
                                onOpenTts = { backStack.add(NavBackStack.settingsTtsRoute()) },
                                onOpenAi = { backStack.add(NavBackStack.settingsAiRoute()) },
                                onOpenPrompt = { backStack.add(NavBackStack.settingsPromptRoute()) },
                                onOpenAbout = { backStack.add(NavBackStack.aboutRoute()) }
                            )
                        }
                    }
                }
            }
            }
            // 全局顶部弹窗：覆盖所有页面（首页 / 音色库 / 设置 / 子页面），从状态栏下方滑入，不随内容滚动。
            AutoDismissToast(
                message = state.statusMessage,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
          }
        }
    }
}

// 让 mutableStateListOf 的返回栈能在配置变更 / 进程恢复后存活。
private val navBackStackSaver = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { mutableStateListOf<String>().apply { addAll(it) } }
)

@Composable
private fun MioTopBar(onHistory: () -> Unit, onReserved: () -> Unit) {
    // 自定义顶栏：左右图标贴边，logo 居中并按屏幕宽度比例放大（不受标准 TopAppBar 高度裁切）
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .height(80.dp)
            .padding(horizontal = 4.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.logo_title),
            contentDescription = "Mio Voice",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.44f)
                .heightIn(max = 64.dp)
        )
        IconButton(
            onClick = onHistory,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(Icons.Default.History, contentDescription = "历史记录", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(
            onClick = onReserved,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "更多功能", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun MioBottomBar(current: String, onSelect: (String) -> Unit) {
    val items = listOf(
        Triple("home", "首页", Icons.Default.Home),
        Triple("voices", "音色库", Icons.Default.Headset),
        Triple("library", "语音库", Icons.Default.LibraryMusic),
        Triple("settings", "设置", Icons.Default.Settings)
    )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        items.forEach { (key, label, icon) ->
            val selected = current == key
            // 选中图标轻微放大，克制不夸张
            val iconScale by animateFloatAsState(
                targetValue = if (selected) 1.08f else 1f,
                animationSpec = tween(160),
                label = "navIconScale"
            )
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(key) },
                icon = {
                    Icon(
                        icon,
                        contentDescription = label,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(iconScale)
                    )
                },
                label = {
                    Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    // 轻量椭圆底座（浅绿），不像独立按钮
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            )
        }
    }
}

@Composable
private fun InfoBanner(message: String, isError: Boolean) {
    val container = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val content = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(message, color = content, style = MaterialTheme.typography.bodyMedium)
    }
}

/** 根据消息文案粗略判断是否属于错误/失败提示，用于选择配色与图标。 */
private fun isErrorMessage(message: String): Boolean {
    val keywords = listOf("失败", "错误", "请先", "不可用", "为空", "不存在", "HTTP", "超时", "限流", "请检查", "已放弃", "已停止")
    return keywords.any { message.contains(it) }
}

/**
 * 全局自动消失轻提示（从顶部弹出）。
 * 监听 [message]，每次变化为非空时从顶部滑入显示约 2 秒后自动滑出，不阻塞操作，也不改动 ViewModel 状态。
 * 贴合 MioVoice 黄绿主题：成功用浅绿容器 + 勾选图标，错误用错误容器 + 警示图标；圆角卡片带轻阴影。
 * 由调用方放在 Box 内并对齐顶部（已自带状态栏安全区内边距）。
 */
@Composable
private fun AutoDismissToast(message: String?, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf<String?>(null) }
    // 记录进入页面时已存在的消息，避免把上一个操作残留的提示再弹一次；只响应进入后的新变化。
    var lastSeen by remember { mutableStateOf(message) }
    LaunchedEffect(message) {
        if (message != lastSeen && !message.isNullOrBlank()) {
            shown = message
            visible = true
            delay(2200)
            visible = false
        }
        lastSeen = message
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        val text = shown.orEmpty()
        val error = isErrorMessage(text)
        val container = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
        val content = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
        val icon = if (error) Icons.Default.ErrorOutline else Icons.Default.CheckCircle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(16.dp), clip = false)
                .background(container, RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                // 点一下立即消失；与 2.2 秒自动消失并存（谁先到先生效），不改 ViewModel 状态。
                .clickable { visible = false }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            Text(
                text,
                color = content,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HomeScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    onOpenDetail: (String) -> Unit,
    onViewAll: () -> Unit
) {
    // 顶部分段切换：普通文本 / 单词列表
    SegmentedToggle(
        options = listOf(
            HomeMode.Text to "普通文本",
            HomeMode.Words to "单词列表"
        ),
        selected = state.homeMode,
        onSelect = viewModel::updateMode
    )

    if (state.homeMode == HomeMode.Text) {
        TextInputCard(
            value = state.textInput,
            onValueChange = { viewModel.updateText(it.take(MAX_TEXT_LENGTH)) },
            placeholder = "输入要生成的文本..."
        )
        TextActionRow(
            value = state.textInput,
            onValueChange = { viewModel.updateText(it) }
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("生成方式", style = MaterialTheme.typography.titleSmall)
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
        SegmentedToggle(
            options = listOf(
                TextGenerationMode.FixedPreset to "固定预设朗读",
                TextGenerationMode.AiDirector to "AI 配音导演"
            ),
            selected = state.textGenerationMode,
            onSelect = viewModel::updateTextGenerationMode
        )
    } else {
        TextInputCard(
            value = state.wordInput,
            onValueChange = { viewModel.updateWordInput(it.take(MAX_TEXT_LENGTH)) },
            placeholder = "每行一个单词或短语"
        )
        TextActionRow(
            value = state.wordInput,
            onValueChange = { viewModel.updateWordInput(it) }
        )
        NumberRow(
            repeatCount = state.repeatCount,
            repeatPauseMs = state.repeatPauseMs,
            wordPauseMs = state.wordPauseMs,
            onRepeatCount = viewModel::updateRepeatCount,
            onRepeatPause = viewModel::updateRepeatPause,
            onWordPause = viewModel::updateWordPause
        )
    }

    GenerationControls(state, viewModel)
    if (state.homeMode == HomeMode.Text && state.textGenerationMode == TextGenerationMode.AiDirector) {
        DirectorPreview(state, viewModel)
    }
    RecentGenerationsSection(state, viewModel, onOpenDetail = onOpenDetail, onViewAll = onViewAll)
}

@Composable
private fun TextInputCard(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        shape = RoundedCornerShape(16.dp),
        // 固定高度：文本超出后在框内部独立滚动，而不是把整页撑长。
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        supportingText = {
            Text(
                "${value.length} / $MAX_TEXT_LENGTH",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    )
}

/**
 * 文本框下方的操作行：粘贴剪贴板 / 清空内容。供普通文本框与单词列表框复用。
 */
@Composable
private fun TextActionRow(value: String, onValueChange: (String) -> Unit) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = {
                val pasted = clipboard.getText()?.text
                if (!pasted.isNullOrEmpty()) {
                    onValueChange((value + pasted).take(MAX_TEXT_LENGTH))
                }
            }
        ) {
            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(6.dp))
            Text("粘贴")
        }
        OutlinedButton(
            onClick = { onValueChange("") },
            enabled = value.isNotEmpty()
        ) {
            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(6.dp))
            Text("清空")
        }
    }
}

@Composable
private fun <T> SegmentedToggle(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    val count = options.size
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val outerShape = RoundedCornerShape(MioStyle.SegmentOuterRadius)
    val thumbShape = RoundedCornerShape(MioStyle.SegmentThumbRadius)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(MioStyle.SegmentHeight)
            .clip(outerShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, outerShape)
            .padding(MioStyle.SegmentPadding)
    ) {
        val thumbWidth = maxWidth / count
        val offsetX by animateDpAsState(
            targetValue = thumbWidth * selectedIndex,
            animationSpec = tween(durationMillis = 220),
            label = "segmentedThumb"
        )
        // 内部绿色滑块（极轻底部阴影，区分层级，不发光）
        Box(
            modifier = Modifier
                .offset(x = offsetX)
                .width(thumbWidth)
                .fillMaxHeight()
                .shadow(MioStyle.LightShadow, thumbShape, clip = false)
                .background(MaterialTheme.colorScheme.primary, thumbShape)
        )
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(thumbShape)
                        .clickable { onSelect(value) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private enum class PrimaryActionState { Idle, Loading, Retry }

/**
 * 首页唯一主操作按钮。统一封装可用 / 禁用 / 加载 / 失败重试四态：
 * - 主题绿实心，轻微底部阴影 + 顶部内侧高光（材质感）。
 * - 按下轻微下沉缩放并加深颜色，松开自然回弹。
 * - 加载态显示文案 + 简洁声波动画；禁用态用低饱和灰绿仍保持可辨识。
 */
@Composable
private fun PrimaryActionButton(
    text: String,
    icon: ImageVector,
    state: PrimaryActionState,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.98f else 1f,
        animationSpec = tween(140),
        label = "primaryButtonScale"
    )
    val shape = RoundedCornerShape(MioStyle.PrimaryButtonRadius)
    val base = MaterialTheme.colorScheme.primary
    // 按下时颜色略微加深
    val container = if (pressed && enabled) base.darken(0.08f) else base
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        interactionSource = interaction,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = MioStyle.LightShadow,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            // 禁用态：低饱和灰绿，降低对比但仍可辨识
            disabledContainerColor = base.desaturate(0.55f).copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(MioStyle.PrimaryButtonHeight)
            .scale(scale)
            // 顶部内侧高光：极淡白色上边描边，增加材质感
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = if (enabled) 0.18f else 0f)),
                shape
            )
    ) {
        if (state == PrimaryActionState.Loading) {
            WaveLoadingIndicator(color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(10.dp))
        } else {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** 简洁声波加载动画：三根柱子轻微上下起伏，克制不夸张。 */
@Composable
private fun WaveLoadingIndicator(color: Color) {
    val transition = rememberInfiniteTransition(label = "wave")
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(20.dp)
    ) {
        repeat(3) { index ->
            val h by transition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(520, delayMillis = index * 130),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "waveBar$index"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .graphicsLayer { scaleY = h }
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}

/** 配置卡片内分隔线：低对比度，仅作轻量分隔。 */
@Composable
private fun SettingDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 58.dp)
    )
}

/** 颜色微调工具：用于按下加深 / 禁用降饱和。 */
private fun Color.darken(fraction: Float): Color = Color(
    red = red * (1f - fraction),
    green = green * (1f - fraction),
    blue = blue * (1f - fraction),
    alpha = alpha
)

private fun Color.desaturate(fraction: Float): Color {
    val gray = 0.299f * red + 0.587f * green + 0.114f * blue
    return Color(
        red = red + (gray - red) * fraction,
        green = green + (gray - green) * fraction,
        blue = blue + (gray - blue) * fraction,
        alpha = alpha
    )
}

@Composable
private fun GenerationControls(state: AppUiState, viewModel: AppViewModel) {
    var showVoiceSheet by remember { mutableStateOf(false) }
    var showPresetSheet by remember { mutableStateOf(false) }
    var showModelSheet by remember { mutableStateOf(false) }

    val selectedVoice = state.settings.voices.firstOrNull { it.id == state.selectedVoiceProfileId }
    val selectedPreset = selectedVoice?.presets?.firstOrNull { it.id == state.selectedPresetId }
    val showPresetRow = state.homeMode != HomeMode.Text || state.textGenerationMode == TextGenerationMode.FixedPreset

    val cardShape = RoundedCornerShape(MioStyle.CardRadius)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(MioStyle.CardShadow, cardShape, clip = false)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            SettingRow(
                icon = Icons.Default.Mic,
                label = "音色",
                value = selectedVoice?.displayName ?: "请先添加父音色",
                onClick = { showVoiceSheet = true }
            )
            if (showPresetRow) {
                SettingDivider()
                SettingRow(
                    icon = Icons.Default.Mood,
                    label = "情绪",
                    value = selectedPreset?.label ?: "选择预设",
                    onClick = { showPresetSheet = true }
                )
            }
            SettingDivider()
            SettingRow(
                icon = Icons.Default.GraphicEq,
                label = "模型",
                value = state.modelInput.ifBlank { "选择模型" },
                onClick = { showModelSheet = true }
            )
        }
    }

    val hasInput = if (state.homeMode == HomeMode.Text) state.textInput.isNotBlank() else state.wordInput.isNotBlank()
    val isBusy = state.isGenerating || state.isAnalyzingDirector
    val isAiDirector = state.homeMode == HomeMode.Text && state.textGenerationMode == TextGenerationMode.AiDirector
    // 失败态：未在忙、没有可播放片段、且至少有一段失败 → “重新生成”
    val hasFailed = !isBusy &&
        state.segments.any { it.status == SegmentStatus.Failed } &&
        state.segments.none { it.status == SegmentStatus.Ready }
    val actionState = when {
        isBusy -> PrimaryActionState.Loading
        hasFailed -> PrimaryActionState.Retry
        else -> PrimaryActionState.Idle
    }
    val actionLabel = when {
        isBusy -> if (state.isAnalyzingDirector) "分析中…" else "生成中…"
        isAiDirector -> if (state.directorSegments.isEmpty()) "AI 分析" else "确认并生成"
        hasFailed -> "重新生成"
        else -> "开始生成"
    }
    val actionIcon = when {
        isBusy -> Icons.Default.Stop
        hasFailed -> Icons.Default.Refresh
        else -> Icons.Default.GraphicEq
    }
    PrimaryActionButton(
        text = actionLabel,
        icon = actionIcon,
        state = actionState,
        enabled = isBusy || hasInput,
        onClick = { if (isBusy) viewModel.stopGeneration() else viewModel.generate() }
    )
    if (state.isGenerating || state.totalCount > 0) {
        val progress = if (state.totalCount == 0) 0f else state.generatedCount.toFloat() / state.totalCount
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text("${state.generatedCount}/${state.totalCount}", style = MaterialTheme.typography.bodySmall)
    }

    if (showVoiceSheet) {
        VoicePickerSheet(
            voices = state.settings.voices,
            selectedId = state.selectedVoiceProfileId,
            onSelect = { viewModel.selectVoice(it); showVoiceSheet = false },
            onDismiss = { showVoiceSheet = false }
        )
    }
    if (showPresetSheet && selectedVoice != null) {
        PresetPickerSheet(
            presets = viewModel.getAvailablePresets(selectedVoice.id),
            selectedId = state.selectedPresetId,
            onSelect = { viewModel.selectPreset(it); showPresetSheet = false },
            onPreview = { viewModel.previewPreset(selectedVoice.id, it) },
            onDismiss = { showPresetSheet = false }
        )
    }
    if (showModelSheet) {
        ModelPickerSheet(
            models = state.fetchedModels,
            selected = state.modelInput,
            onSelect = { viewModel.updateModel(it); showModelSheet = false },
            onDismiss = { showModelSheet = false }
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // 仅该行按下时出现浅绿色反馈，不联动整卡
    val rowBackground = if (pressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MioStyle.RowHeight)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .background(rowBackground)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(MioStyle.IconBadgeSize)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(MioStyle.RowIconSize))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(MioStyle.ChevronSize)
        )
    }
}

@Composable
private fun VoicePickerSheet(
    voices: List<VoiceProfile>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("选择音色", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
            if (voices.isEmpty()) {
                Text("还没有音色，请先到音色库添加。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
            }
            voices.forEach { voice ->
                PickerRow(
                    title = voice.displayName,
                    subtitle = voice.voiceId,
                    selected = voice.id == selectedId,
                    onClick = { onSelect(voice.id) }
                )
            }
        }
    }
}

@Composable
private fun PresetPickerSheet(
    presets: List<EmotionPreset>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onPreview: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("选择情绪预设", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
            if (presets.isEmpty()) {
                Text("当前音色没有可用预设。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
            }
            presets.forEach { preset ->
                PickerRow(
                    title = preset.label,
                    subtitle = "${preset.emotion} / speed ${"%.2f".format(preset.speed)}",
                    selected = preset.id == selectedId,
                    onClick = { onSelect(preset.id) },
                    trailing = {
                        TextButton(onClick = { onPreview(preset.id) }) { Text("试听") }
                    }
                )
            }
        }
    }
}

@Composable
private fun ModelPickerSheet(
    models: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("选择模型", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
            models.forEach { model ->
                PickerRow(
                    title = model,
                    subtitle = null,
                    selected = model == selected,
                    onClick = { onSelect(model) }
                )
            }
        }
    }
}

@Composable
private fun PickerRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        trailing?.invoke()
    }
}

@Composable
private fun DirectorPreview(state: AppUiState, viewModel: AppViewModel) {
    val voice = state.settings.voices.firstOrNull { it.id == (state.directorVoiceProfileId ?: state.selectedVoiceProfileId) }
    if (state.isAnalyzingDirector) {
        Text("AI 导演分析中...")
    }
    state.directorValidationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    state.directorWarnings.forEach { warning -> Text(warning, color = MaterialTheme.colorScheme.error) }
    if (voice == null || state.directorSegments.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("导演预览", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::analyzeDirector, enabled = !state.isAnalyzingDirector && !state.isGenerating) {
                    Text("重新分析")
                }
                OutlinedButton(onClick = viewModel::discardDirectorResult) {
                    Text("放弃")
                }
            }
        }
        state.directorSegments.forEachIndexed { index, segment ->
            val preset = voice.presets.firstOrNull { it.id == segment.presetId }
                ?: voice.presets.firstOrNull { it.id == voice.defaultPresetId }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 头部：序号徽章 + 「片段 N」标题 + 右侧情绪 chip（可点开下拉改情绪）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            "片段 ${index + 1}",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        PresetDropdown(
                            presets = voice.presets,
                            selectedId = segment.presetId,
                            onSelected = { it?.let { presetId -> viewModel.updateDirectorSegmentPreset(segment.id, presetId) } }
                        )
                    }
                    // 正文
                    Text(
                        segment.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                    )
                    // 预设信息（弱化为次要色）
                    preset?.let {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "预设：${it.label}  ·  ${it.description.ifBlank { "未填写描述" }}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "emotion ${it.emotion} · speed ${"%.2f".format(it.speed)} · pitch ${it.pitch}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                    segment.warnings.forEach { warning ->
                        Text(warning, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                    if (index < state.directorSegments.lastIndex) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { viewModel.mergeDirectorSegmentWithNext(segment.id) }) {
                                Text("与下一段合并")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 设置主页：简洁入口页。仅展示分组卡片入口（TTS / AI / 提示词），不直接呈现任何 URL/Key/模型输入框。
 * 渲染于带底部导航的主 Scaffold 内（tab == "settings"）。
 */
@Composable
private fun SettingsScreen(
    onOpenTts: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenPrompt: () -> Unit,
    onOpenAbout: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsEntryRow(
            icon = Icons.Default.GraphicEq,
            title = "TTS 语音配置",
            subtitle = "配置语音生成服务、模型与默认参数",
            onClick = onOpenTts
        )
        SettingsEntryRow(
            icon = Icons.Default.AutoAwesome,
            title = "AI 分析配置",
            subtitle = "配置文本分析与情绪导演模型",
            onClick = onOpenAi
        )
        SettingsEntryRow(
            icon = Icons.AutoMirrored.Filled.Notes,
            title = "提示词配置",
            subtitle = "查看并编辑 AI 导演的系统提示词",
            onClick = onOpenPrompt
        )
        SettingsEntryRow(
            icon = Icons.Default.Info,
            title = "关于 Mio Voice",
            subtitle = "版本信息、开源许可与项目反馈",
            onClick = onOpenAbout
        )
    }
}

/** 设置主页入口行：图标 + 标题 + 副标题 + 右侧箭头，绿色主题卡片样式。 */
@Composable
private fun SettingsEntryRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(MioStyle.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = MioStyle.CardShadow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 子页面分组容器：小标题 + 内容。 */
@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

/** 密码输入框：默认隐藏，尾部图标可切换显示/隐藏。TTS / AI 的 API Key 共用。 */
@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { placeholder?.let { Text(it) } },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "隐藏" else "显示"
                )
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * 测试连接按钮：空闲时显示播放图标 + 文案；测试中切换为旋转的进度环 + “测试中...”并禁用，
 * 让点击后的等待过程有动态反馈，不再“看着太死”。
 */
@Composable
private fun TestConnectionButton(
    testing: Boolean,
    label: String,
    onClick: () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        enabled = !testing,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (testing) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("测试中...")
        } else {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    }
}

/**
 * TTS 语音配置子页（全屏，不含底部导航）。
 * 顶部返回 + 标题；系统返回键与左上角返回执行一致逻辑（出栈回到设置主页）。
 */
@Composable
private fun TtsSettingsScreen(state: AppUiState, viewModel: AppViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var baseUrl by rememberSaveable { mutableStateOf(state.settings.baseUrl) }
    var endpoint by rememberSaveable { mutableStateOf(state.settings.endpointPath) }
    var model by rememberSaveable { mutableStateOf(state.settings.model) }

    LaunchedEffect(state.settings) {
        baseUrl = state.settings.baseUrl
        endpoint = state.settings.endpointPath
        model = state.settings.model
    }

    // 默认参数（语速 / 单次安全长度 / 情绪）改由音色库预设配置承担，TTS 页不再呈现；保存时透传旧值。
    // 兼容旧字段：defaultVoiceId 不再有 UI，仅透传旧值给 testConnection；useFakeProvider 固定 false（已移除开关）。
    fun persist() = viewModel.saveSettings(
        baseUrl = baseUrl,
        endpointPath = endpoint,
        model = model,
        defaultVoiceId = state.settings.defaultVoiceId,
        defaultSpeed = state.settings.defaultSpeed,
        defaultEmotion = state.settings.defaultEmotion,
        maxCharsPerRequest = state.settings.maxCharsPerRequest,
        useFakeProvider = false,
        directorBaseUrl = state.settings.directorBaseUrl,
        directorEndpointPath = state.settings.directorEndpointPath,
        directorModel = state.settings.directorModel
    )

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(title = "TTS 语音配置", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            state.credentialMessage?.let { InfoBanner(it, isError = true) }

            SettingsSection("服务连接") {
                OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(endpoint, { endpoint = it }, label = { Text("Endpoint Path") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                PasswordField(
                    state.apiKeyInput,
                    viewModel::updateApiKey,
                    "API Key",
                    placeholder = if (state.hasSavedTtsKey) "••••••••（已保存）" else null
                )
            }

            SettingsSection("模型设置") {
                ModelDropdown(
                    models = state.fetchedModels,
                    selected = model,
                    onSelected = { model = it },
                    label = "默认模型"
                )
                OutlinedButton(
                    onClick = { viewModel.fetchModels(baseUrl = baseUrl, apiKey = state.apiKeyInput) },
                    enabled = !state.isFetchingModels,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isFetchingModels) "拉取中..." else "拉取模型")
                }
            }

            SettingsSection("数据管理") {
                Button(
                    onClick = { persist() },
                    modifier = Modifier.fillMaxWidth().height(MioStyle.PrimaryButtonHeight),
                    shape = RoundedCornerShape(MioStyle.PrimaryButtonRadius)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("保存")
                }
                TestConnectionButton(
                    testing = state.isTestingConnection,
                    label = "测试连接",
                    onClick = {
                        viewModel.testConnection(
                            baseUrl = baseUrl,
                            endpointPath = endpoint,
                            model = model,
                            defaultVoiceId = state.settings.defaultVoiceId,
                            defaultSpeed = state.settings.defaultSpeed,
                            defaultEmotion = state.settings.defaultEmotion,
                            useFakeProvider = false
                        )
                    }
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = viewModel::clearCredentials) { Text("清除凭证") }
                    TextButton(onClick = viewModel::clearAudioCache) { Text("清理缓存") }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * AI 分析配置子页（全屏，不含底部导航）。
 * 复用现有 director 配置保存与连接测试逻辑，不改变接口调用格式。
 */
@Composable
private fun AiSettingsScreen(state: AppUiState, viewModel: AppViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var directorBaseUrl by rememberSaveable { mutableStateOf(state.settings.directorBaseUrl) }
    var directorEndpoint by rememberSaveable { mutableStateOf(state.settings.directorEndpointPath) }
    var directorModel by rememberSaveable { mutableStateOf(state.settings.directorModel) }

    LaunchedEffect(state.settings) {
        directorBaseUrl = state.settings.directorBaseUrl
        directorEndpoint = state.settings.directorEndpointPath
        directorModel = state.settings.directorModel
    }

    // TTS 字段透传旧值；defaultVoiceId 透传、useFakeProvider 固定 false（与 TTS 子页一致）。
    fun persist() = viewModel.saveSettings(
        baseUrl = state.settings.baseUrl,
        endpointPath = state.settings.endpointPath,
        model = state.settings.model,
        defaultVoiceId = state.settings.defaultVoiceId,
        defaultSpeed = state.settings.defaultSpeed,
        defaultEmotion = state.settings.defaultEmotion,
        maxCharsPerRequest = state.settings.maxCharsPerRequest,
        useFakeProvider = false,
        directorBaseUrl = directorBaseUrl,
        directorEndpointPath = directorEndpoint,
        directorModel = directorModel
    )

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(title = "AI 分析配置", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            state.credentialMessage?.let { InfoBanner(it, isError = true) }

            SettingsSection("服务连接") {
                OutlinedTextField(directorBaseUrl, { directorBaseUrl = it }, label = { Text("AI Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(directorEndpoint, { directorEndpoint = it }, label = { Text("AI Endpoint") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                PasswordField(
                    state.aiApiKeyInput,
                    viewModel::updateAiApiKey,
                    "AI API Key",
                    placeholder = if (state.hasSavedDirectorKey) "••••••••（已保存）" else null
                )
            }

            SettingsSection("模型设置") {
                ModelDropdown(
                    models = state.directorFetchedModels,
                    selected = directorModel,
                    onSelected = { directorModel = it },
                    label = "AI 模型名"
                )
                OutlinedButton(
                    onClick = { viewModel.fetchDirectorModels(baseUrl = directorBaseUrl, endpointPath = directorEndpoint) },
                    enabled = !state.isFetchingDirectorModels,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isFetchingDirectorModels) "拉取中..." else "拉取模型")
                }
            }

            SettingsSection("数据管理") {
                Button(
                    onClick = { persist() },
                    modifier = Modifier.fillMaxWidth().height(MioStyle.PrimaryButtonHeight),
                    shape = RoundedCornerShape(MioStyle.PrimaryButtonRadius)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("保存 AI 配置")
                }
                TestConnectionButton(
                    testing = state.isTestingDirector,
                    label = "测试 AI 连接",
                    onClick = { viewModel.testDirectorConnection(directorBaseUrl, directorEndpoint, directorModel) }
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = viewModel::clearDirectorCredentials) { Text("清除 AI 凭证") }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * 提示词配置子页（全屏，不含底部导航）。
 * 本期展示 AI 导演的系统提示词（角色设定），可编辑、保存生效，并支持一键恢复内置默认。
 * 分组结构为后续新增其他提示词预留扩展空间。
 */
@Composable
private fun PromptSettingsScreen(state: AppUiState, viewModel: AppViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val default = OpenAiCompatibleDirectorProvider.DEFAULT_SYSTEM_PROMPT
    // 空字符串表示沿用内置默认；编辑框始终展示有效内容（自定义或默认）。
    var prompt by rememberSaveable {
        mutableStateOf(state.settings.directorSystemPrompt.ifBlank { default })
    }
    LaunchedEffect(state.settings.directorSystemPrompt) {
        prompt = state.settings.directorSystemPrompt.ifBlank { default }
    }
    val isCustom = state.settings.directorSystemPrompt.isNotBlank()

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(title = "提示词配置", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsSection("AI 导演 · 系统提示词") {
                Text(
                    if (isCustom) "当前使用自定义提示词。" else "当前使用内置默认提示词，可直接编辑后保存为自定义。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("系统提示词") },
                    minLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "提示：此提示词作为对话的 system 角色发送给 AI 导演；用户提示词（含原文、可用预设等动态内容）由系统自动拼接，暂不在此编辑。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection("AI 导演 · 自动表演标记") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("让 AI 自动添加表演标记", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "实验性：开启后 AI 会在合适处插入笑声/叹气等语气词与停顿标记，仅 MiniMax speech-2.8 支持。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.settings.directorAutoTags,
                        onCheckedChange = { viewModel.setDirectorAutoTags(it) }
                    )
                }
            }

            SettingsSection("数据管理") {
                Button(
                    onClick = { viewModel.saveDirectorSystemPrompt(prompt) },
                    modifier = Modifier.fillMaxWidth().height(MioStyle.PrimaryButtonHeight),
                    shape = RoundedCornerShape(MioStyle.PrimaryButtonRadius)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("保存提示词")
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            prompt = default
                            viewModel.saveDirectorSystemPrompt("")
                        }
                    ) { Text("恢复默认提示词") }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ============================================================================
// 音色库三级结构：
//   音色库主页（父音色列表）→ 音色详情页（子情绪预设分支）→ 预设编辑页（占位）
// 父音色卡片停留在 voices Tab 主页面（保留底栏）；详情 / 预设编辑是返回栈子页面。
// ============================================================================

/** 音色库主页：仅展示父音色列表（不含任何子情绪预设 / 统计 / 默认音色概念）。 */
@Composable
private fun VoiceLibraryScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    onOpenVoice: (String) -> Unit,
    onAddVoice: () -> Unit,
    onAddOfficial: () -> Unit
) {
    if (state.showOfficialVoicePicker) {
        OfficialVoicePickerDialog(
            voices = state.officialVoices,
            onConfirm = { selected -> viewModel.importOfficialVoices(selected) },
            onDismiss = { viewModel.dismissOfficialVoicePicker() }
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("音色库", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (state.isFetchingOfficialVoices) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    TextButton(onClick = onAddOfficial) {
                        Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("官方音色")
                    }
                }
                IconButton(onClick = onAddVoice) {
                    Icon(Icons.Default.Add, contentDescription = "新增音色", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (state.settings.voices.isEmpty()) {
            VoiceLibraryEmptyState(onAddVoice = onAddVoice)
        } else {
            state.settings.voices.forEach { profile ->
                VoiceProfileCard(profile = profile, onOpen = { onOpenVoice(profile.id) })
            }
        }
    }
}

/** 音色库空状态。 */
@Composable
private fun VoiceLibraryEmptyState(onAddVoice: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        VoiceAvatar(name = "", size = 72.dp)
        Text("还没有音色", style = MaterialTheme.typography.titleMedium)
        Text(
            "添加你的第一个音色后，就可以为它创建子情绪预设。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Button(onClick = onAddVoice) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("添加第一个音色")
        }
    }
}

/** 官方音色多选弹窗：按语言/性别筛选（命名推断）+ 美化行，勾选后批量加入音色库。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfficialVoicePickerDialog(
    voices: List<OfficialVoice>,
    onConfirm: (List<OfficialVoice>) -> Unit,
    onDismiss: () -> Unit
) {
    var checked by remember(voices) { mutableStateOf<Set<String>>(emptySet()) }
    var selectedLanguage by remember(voices) { mutableStateOf<OfficialVoiceClassifier.Language?>(null) }
    var selectedGender by remember(voices) { mutableStateOf<OfficialVoiceClassifier.Gender?>(null) }

    // 预计算每个音色的语言/性别，避免重复推断。
    data class Tagged(val voice: OfficialVoice, val language: OfficialVoiceClassifier.Language, val gender: OfficialVoiceClassifier.Gender)
    val tagged = remember(voices) {
        // 入参已是 confidentVoices 过滤后的集合，性别/语言强信号必然非空；展示标签即过滤依据。
        voices.map {
            Tagged(
                it,
                OfficialVoiceClassifier.confidentLanguage(it) ?: OfficialVoiceClassifier.language(it),
                OfficialVoiceClassifier.confidentGender(it) ?: OfficialVoiceClassifier.gender(it)
            )
        }
    }
    // 仅展示数据中实际出现过的语言筛选项（保持枚举顺序）。
    val availableLanguages = remember(tagged) {
        OfficialVoiceClassifier.Language.entries.filter { lang -> tagged.any { it.language == lang } }
    }
    val availableGenders = remember(tagged) {
        OfficialVoiceClassifier.Gender.entries.filter { g -> tagged.any { it.gender == g } }
    }

    val visible = tagged.filter {
        (selectedLanguage == null || it.language == selectedLanguage) &&
            (selectedGender == null || it.gender == selectedGender)
    }
    val visibleIds = visible.map { it.voice.voiceId }.toSet()
    val allVisibleSelected = visibleIds.isNotEmpty() && checked.containsAll(visibleIds)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加官方音色") },
        text = {
            if (voices.isEmpty()) {
                Text("没有可添加的官方音色。")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 语言筛选
                    if (availableLanguages.size > 1) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            FilterChip(
                                selected = selectedLanguage == null,
                                onClick = { selectedLanguage = null },
                                label = { Text("全部语言") }
                            )
                            availableLanguages.forEach { lang ->
                                FilterChip(
                                    selected = selectedLanguage == lang,
                                    onClick = { selectedLanguage = if (selectedLanguage == lang) null else lang },
                                    label = { Text(lang.label) }
                                )
                            }
                        }
                    }
                    // 性别筛选
                    if (availableGenders.size > 1) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            FilterChip(
                                selected = selectedGender == null,
                                onClick = { selectedGender = null },
                                label = { Text("全部性别") }
                            )
                            availableGenders.forEach { g ->
                                FilterChip(
                                    selected = selectedGender == g,
                                    onClick = { selectedGender = if (selectedGender == g) null else g },
                                    label = { Text(g.label) }
                                )
                            }
                        }
                    }

                    // 全选（作用于当前可见列表）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                checked = if (allVisibleSelected) checked - visibleIds else checked + visibleIds
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(checked = allVisibleSelected, onCheckedChange = { on ->
                            checked = if (on) checked + visibleIds else checked - visibleIds
                        })
                        Text(
                            "${if (allVisibleSelected) "取消全选" else "全选"}（${visible.size}）",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider()

                    if (visible.isEmpty()) {
                        Text(
                            "该筛选下没有音色。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            visible.forEach { item ->
                                val voice = item.voice
                                val isOn = voice.voiceId in checked
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            checked = if (isOn) checked - voice.voiceId else checked + voice.voiceId
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    VoiceAvatar(name = voice.voiceName, size = 36.dp)
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                voice.voiceName,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (item.language != OfficialVoiceClassifier.Language.Other) {
                                                MiniTag(item.language.label)
                                            }
                                            if (item.gender != OfficialVoiceClassifier.Gender.Other) {
                                                MiniTag(item.gender.label)
                                            }
                                        }
                                        if (voice.description.isNotBlank()) {
                                            Text(
                                                voice.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            voice.voiceId,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Checkbox(checked = isOn, onCheckedChange = { on ->
                                        checked = if (on) checked + voice.voiceId else checked - voice.voiceId
                                    })
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = checked.isNotEmpty(),
                onClick = { onConfirm(voices.filter { it.voiceId in checked }) }
            ) { Text("添加（${checked.size}）") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

/** 小标签：圆角浅底，用于语言/性别。 */
@Composable
private fun MiniTag(text: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * 从本地文件路径解码圆形头像位图；文件不存在或解码失败返回 null（由调用方回退占位）。
 * 以 path + 文件修改时间为 key 做 remember 缓存，避免列表滚动反复解码。
 */
@Composable
private fun rememberAvatarBitmap(path: String?): ImageBitmap? {
    if (path.isNullOrBlank()) return null
    val file = remember(path) { File(path) }
    val stamp = remember(path) { if (file.exists()) file.lastModified() else 0L }
    return remember(path, stamp) {
        if (!file.exists()) null
        else runCatching {
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        }.getOrNull()
    }
}

/**
 * 统一的圆形头像：有有效 avatarPath 则展示自定义图片（圆形裁切、居中裁切），
 * 否则回退到首字母 / 麦克风占位。坏路径自动回退占位，不崩溃。
 */
@Composable
private fun VoiceAvatar(
    name: String,
    size: Dp,
    avatarPath: String? = null,
    onClick: (() -> Unit)? = null
) {
    val bitmap = rememberAvatarBitmap(avatarPath)
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString()
    val base = Modifier
        .size(size)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer)
    val modifier = if (onClick != null) base.clickable(onClick = onClick) else base
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape)
            )
            initial != null -> Text(
                initial,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            else -> Icon(
                Icons.Default.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.45f)
            )
        }
    }
}

/**
 * 头像选择区（新增 / 编辑音色页共用）。
 * - 圆形预览优先级：本次新选图片(pendingUri) > 当前已保存头像(currentAvatarPath，未标记移除时) > 占位。
 * - 点击头像或角标打开系统相册（PickVisualMedia，仅图片，无需存储权限）。取消选择不改动既有状态。
 * - 已有头像或本次已选图片时，显示低干扰“移除头像”文本按钮。
 * 注意：此处不落盘，仅维护表单状态；真正写入/清理在保存音色时由 ViewModel 处理。
 */
@Composable
private fun AvatarPickerField(
    name: String,
    currentAvatarPath: String?,
    pendingUri: String?,
    avatarRemoved: Boolean,
    onPick: (Uri?) -> Unit,
    onRemove: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> onPick(uri) } // 取消时 uri 为 null，onPick 内部不会清掉既有头像。

    fun open() = launcher.launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    )

    // 预览：本次新选 > 既有（未移除）> 占位。
    val pendingBitmap = rememberUriBitmap(pendingUri)
    val effectivePath = if (avatarRemoved) null else currentAvatarPath
    val showRemove = pendingUri != null || (!avatarRemoved && !currentAvatarPath.isNullOrBlank())

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box {
            val base = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { open() }
            Box(modifier = base, contentAlignment = Alignment.Center) {
                when {
                    pendingBitmap != null -> Image(
                        bitmap = pendingBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(88.dp).clip(CircleShape)
                    )
                    else -> VoiceAvatar(name = name, size = 88.dp, avatarPath = effectivePath)
                }
            }
            FilledTonalIconButton(
                onClick = { open() },
                modifier = Modifier.align(Alignment.BottomEnd).size(28.dp)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "选择头像", modifier = Modifier.size(15.dp))
            }
        }
        TextButton(onClick = { open() }) { Text("选择头像") }
        if (showRemove) {
            TextButton(onClick = onRemove) {
                Text("移除头像", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** 从 content Uri 解码预览位图（用于头像选择即时预览）；失败返回 null。 */
@Composable
private fun rememberUriBitmap(uriString: String?): ImageBitmap? {
    if (uriString.isNullOrBlank()) return null
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uriString, context) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriString)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, bounds)
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

                var sampleSize = 1
                while (
                    bounds.outWidth / sampleSize > AVATAR_PREVIEW_MAX_EDGE * 2 ||
                    bounds.outHeight / sampleSize > AVATAR_PREVIEW_MAX_EDGE * 2
                ) {
                    sampleSize *= 2
                }
                val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    return bitmap
}

/** 父音色卡片：整卡可点进入详情页。不显示预设数 / 默认标签 / 编辑删除按钮。 */
@Composable
private fun VoiceProfileCard(profile: VoiceProfile, onOpen: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(MioStyle.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = MioStyle.CardShadow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VoiceAvatar(name = profile.displayName, size = 48.dp, avatarPath = profile.avatarPath)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    profile.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    profile.voiceId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // VoiceProfile 暂无独立描述字段；存在时再显示，不写死占位。
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "进入",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ----------------------------------------------------------------------------
// 音色详情页（返回栈子页面）：顶部父音色信息 + 头像引出的分支线 + 子情绪预设
// ----------------------------------------------------------------------------

private object BranchStyle {
    val RailWidth = 36.dp       // 左侧分支线列宽度
    val LineWidth = 2.dp        // 线条粗细（轻盈）
    val NodeRadius = 5.dp       // 节点圆半径
    val RootStubHeight = 20.dp  // 头像底部引出的根部短线高度
}

@Composable
private fun VoiceDetailScreen(
    voiceProfileId: String?,
    state: AppUiState,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onEditVoice: (String) -> Unit,
    onAddPreset: (String) -> Unit,
    onOpenPreset: (String, String) -> Unit
) {
    // 系统返回键与左上角返回执行一致逻辑（出栈一层，回到音色库主页，不退出 App）。
    BackHandler(onBack = onBack)

    // 离开详情页（含返回/进入子页）时停止试听，避免后台继续播放。
    DisposableEffect(Unit) {
        onDispose { viewModel.stopPreview() }
    }

    val profile = state.settings.voices.firstOrNull { it.id == voiceProfileId }
    var showVoiceMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (profile == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackTopBar(title = "音色详情", onBack = onBack)
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("音色不存在或已被删除。", style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(onClick = onBack) { Text("返回") }
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(
            title = "音色详情",
            onBack = onBack,
            actions = {
                Box {
                    IconButton(onClick = { showVoiceMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    DropdownMenu(expanded = showVoiceMenu, onDismissRequest = { showVoiceMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("编辑音色") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showVoiceMenu = false
                                onEditVoice(profile.id)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("删除音色", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showVoiceMenu = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            VoiceDetailHeader(
                profile = profile,
                viewModel = viewModel,
                onEditInfo = { onEditVoice(profile.id) }
            )
            PresetBranchSection(
                profile = profile,
                viewModel = viewModel,
                preview = state.preview,
                onAddPreset = { onAddPreset(profile.id) },
                onOpenPreset = { presetId -> onOpenPreset(profile.id, presetId) }
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            title = "删除音色？",
            message = "将删除音色「${profile.displayName}」，其下所有子情绪预设也会被一并删除。此操作目前不可撤销。",
            onCancel = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteVoiceProfile(profile.id) { success ->
                    if (success) onBack() // 删除成功后回音色库主页，不停留在已不存在的详情路由。
                }
            }
        )
    }
}

/** 详情页顶部父音色信息区：轻盈、留白，左侧大头像（含相机占位），右侧基础信息。整块可点进入编辑。 */
@Composable
private fun VoiceDetailHeader(
    profile: VoiceProfile,
    viewModel: AppViewModel,
    onEditInfo: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier
            .clickable(onClick = onEditInfo)
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 头像 + 边缘小相机角标：点击进入编辑页更换头像（不在详情页直接弹选择器，避免重复入口）。
        Box {
            VoiceAvatar(
                name = profile.displayName,
                size = 72.dp,
                avatarPath = profile.avatarPath,
                onClick = onEditInfo
            )
            FilledTonalIconButton(
                onClick = onEditInfo,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(26.dp)
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = "更换头像",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                profile.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    profile.voiceId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false)
                )
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(profile.voiceId))
                        viewModel.notify("voice_id 已复制。")
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "复制 voice_id",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            // 真实字段：仅在非空时显示，空字段整行隐藏（不显示 "null"）。
            if (profile.description.isNotBlank()) {
                Text(
                    profile.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            VoiceMetaLine(label = "语言", value = profile.language)
            VoiceMetaLine(label = "风格", value = profile.style)
            if (profile.createdAt > 0L) {
                VoiceMetaLine(label = "创建时间", value = formatCreatedAt(profile.createdAt))
            }
        }
    }
}

/** 详情页信息区的一行 "标签：值"；值为空则整行隐藏。 */
@Composable
private fun VoiceMetaLine(label: String, value: String) {
    if (value.isBlank()) return
    Text(
        "$label：$value",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** 统一可读的本地创建时间格式（yyyy-MM-dd HH:mm）。 */
private fun formatCreatedAt(millis: Long): String =
    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date(millis))

/**
 * 头像 → 主线 → 子情绪预设的连续分支结构 + 预设列表。
 *
 * 连续性实现：整段（根部短线 + 区标题 + 所有预设）放在同一个 Column 中，
 * 该 Column 用 drawBehind 在左侧 rail 中心画一条贯穿竖线，y 从顶部 0 一直到
 * 最后一个节点的中心；每个预设行左侧 rail 内画节点圆 + 横向短线连到卡片。
 * 这样主线与各预设属于同一条线，且随内容高度 / 预设数量 / 滚动自适应（无绝对坐标）。
 */
@Composable
private fun PresetBranchSection(
    profile: VoiceProfile,
    viewModel: AppViewModel,
    preview: PreviewUiState,
    onAddPreset: () -> Unit,
    onOpenPreset: (String) -> Unit
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val railCenterPx = with(density) { BranchStyle.RailWidth.toPx() / 2f }
    val lineWidthPx = with(density) { BranchStyle.LineWidth.toPx() }

    // 记录最后一个节点中心的 y（相对容器顶部），主线绘制到此为止。
    var lastNodeCenterY by remember(profile.id) { mutableStateOf<Float?>(null) }
    // 待删除预设（确认对话框）。
    var presetPendingDelete by remember(profile.id) { mutableStateOf<EmotionPreset?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val endY = lastNodeCenterY ?: size.height
                drawLine(
                    color = lineColor,
                    start = Offset(railCenterPx, 0f),
                    end = Offset(railCenterPx, endY),
                    strokeWidth = lineWidthPx,
                    cap = StrokeCap.Round
                )
            }
    ) {
        // 根部短线：从头像底部正下方引出，视觉上承接头像与下方分支。
        Spacer(Modifier.height(BranchStyle.RootStubHeight))

        // 区标题 + 简洁圆形“+”新增预设按钮。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = BranchStyle.RailWidth, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("子情绪预设", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            FilledIconButton(
                onClick = onAddPreset,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增预设", modifier = Modifier.size(18.dp))
            }
        }

        if (profile.presets.isEmpty()) {
            // 空预设：仍展示从头像引出的根节点 + 空状态说明 + 创建按钮。
            EmptyPresetState(
                lineColor = lineColor,
                onAddPreset = onAddPreset,
                onNodePlaced = { lastNodeCenterY = it }
            )
        } else {
            profile.presets.forEachIndexed { index, preset ->
                val isLast = index == profile.presets.lastIndex
                // 仅当试听目标是本预设时显示其活动态，保证同时只有一张卡处于活动态。
                val cardStatus = if (preview.targetKey == preset.id) preview.status else null
                PresetBranchRow(
                    preset = preset,
                    lineColor = lineColor,
                    onOpen = { onOpenPreset(preset.id) },
                    onPreview = { viewModel.previewPreset(profile.id, preset.id) },
                    onStopPreview = { viewModel.stopPreview() },
                    onEdit = { onOpenPreset(preset.id) },
                    onDelete = { presetPendingDelete = preset },
                    previewStatus = cardStatus,
                    onNodePlaced = if (isLast) { y -> lastNodeCenterY = y } else null
                )
            }
        }
    }

    presetPendingDelete?.let { target ->
        DeleteConfirmDialog(
            title = "删除预设？",
            message = "将删除情绪预设「${target.label}」。取消不会做任何修改，此操作不可撤销。",
            onCancel = { presetPendingDelete = null },
            onConfirm = {
                presetPendingDelete = null
                viewModel.deletePreset(profile.id, target.id)
            }
        )
    }
}

/**
 * 单个预设行：左侧 rail（节点圆 + 横向短线）+ 右侧预设卡片。
 * 节点按本行卡片实际高度居中绘制，适配不同卡片高度；onNodePlaced 上报节点中心 y 供主线收尾。
 */
@Composable
private fun PresetBranchRow(
    preset: EmotionPreset,
    lineColor: Color,
    onOpen: () -> Unit,
    onPreview: () -> Unit,
    onStopPreview: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    previewStatus: PreviewStatus?,
    onNodePlaced: ((Float) -> Unit)?
) {
    val density = LocalDensity.current
    val railCenterPx = with(density) { BranchStyle.RailWidth.toPx() / 2f }
    val lineWidthPx = with(density) { BranchStyle.LineWidth.toPx() }
    val nodeRadiusPx = with(density) { BranchStyle.NodeRadius.toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            // 本 Row 是分支区 Column 的直接子项；节点在 Row 内垂直居中，
            // 故节点中心在 Column 坐标系中的 y = 本 Row 在父中的中心 y。
            .onGloballyPositioned { coords ->
                onNodePlaced?.invoke(coords.boundsInParent().center.y)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // rail：高度跟随本行（卡片）高度，节点垂直居中；横线从主线连到卡片左缘。
        Box(
            modifier = Modifier
                .width(BranchStyle.RailWidth)
                .fillMaxHeight()
                .drawBehind {
                    val cy = size.height / 2f
                    // 横向短线：主线 → 卡片左缘
                    drawLine(
                        color = lineColor,
                        start = Offset(railCenterPx, cy),
                        end = Offset(size.width, cy),
                        strokeWidth = lineWidthPx,
                        cap = StrokeCap.Round
                    )
                    // 节点圆
                    drawCircle(
                        color = lineColor,
                        radius = nodeRadiusPx,
                        center = Offset(railCenterPx, cy)
                    )
                }
        )
        PresetCard(
            preset = preset,
            onOpen = onOpen,
            onPreview = onPreview,
            onStopPreview = onStopPreview,
            onEdit = onEdit,
            onDelete = onDelete,
            previewStatus = previewStatus,
            modifier = Modifier.weight(1f)
        )
    }
}

/** 预设卡片：整卡可点进入编辑页；试听 / 省略号为独立点击区，不触发卡片跳转。 */
@Composable
private fun PresetCard(
    preset: EmotionPreset,
    onOpen: () -> Unit,
    onPreview: () -> Unit,
    onStopPreview: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    previewStatus: PreviewStatus? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(MioStyle.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = MioStyle.CardShadow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(preset.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    EmotionOptions.labelFor(preset.emotion) + " · " + preset.emotion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "speed ${"%.2f".format(preset.speed)} / pitch ${formatPitch(preset.pitch)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (preset.description.isNotBlank()) {
                    Text(
                        preset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // 试听：独立点击区，按状态机切换 播放/生成中/停止；保持 IconButton 固定尺寸，
            // 不改变卡片高度以维持分支线节点对齐。
            IconButton(
                onClick = {
                    when (previewStatus) {
                        PreviewStatus.Generating, PreviewStatus.Playing -> onStopPreview()
                        else -> onPreview()
                    }
                }
            ) {
                when (previewStatus) {
                    PreviewStatus.Generating -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    PreviewStatus.Playing -> Icon(
                        Icons.Default.Stop,
                        contentDescription = "停止试听",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    else -> Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "试听",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            // 省略号：编辑 / 删除菜单（独立点击区）。
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/** 空预设状态：附带从头像引出的根节点（上报节点 y 让主线收束到此）。 */
@Composable
private fun EmptyPresetState(
    lineColor: Color,
    onAddPreset: () -> Unit,
    onNodePlaced: (Float) -> Unit
) {
    val density = LocalDensity.current
    val railCenterPx = with(density) { BranchStyle.RailWidth.toPx() / 2f }
    val lineWidthPx = with(density) { BranchStyle.LineWidth.toPx() }
    val nodeRadiusPx = with(density) { BranchStyle.NodeRadius.toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .onGloballyPositioned { coords ->
                onNodePlaced(coords.boundsInParent().center.y)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(BranchStyle.RailWidth)
                .fillMaxHeight()
                .drawBehind {
                    val cy = size.height / 2f
                    drawLine(
                        color = lineColor,
                        start = Offset(railCenterPx, cy),
                        end = Offset(size.width, cy),
                        strokeWidth = lineWidthPx,
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = lineColor, radius = nodeRadiusPx, center = Offset(railCenterPx, cy))
                }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("还没有情绪预设", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "为这个音色创建不同情绪、语速和音调的表达方式。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAddPreset) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("创建第一个预设")
            }
        }
    }
}

/** 把 pitch 格式化为带正负号显示（0 / +1 / -2）。 */
private fun formatPitch(pitch: Int): String = if (pitch > 0) "+$pitch" else pitch.toString()

// ----------------------------------------------------------------------------
// 父音色编辑页（新增 / 编辑共用 VoiceProfileForm）
// ----------------------------------------------------------------------------

@Composable
private fun VoiceProfileEditorScreen(
    voiceProfileId: String?,
    state: AppUiState,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val isNew = voiceProfileId == null
    val existing = state.settings.voices.firstOrNull { it.id == voiceProfileId }

    // 编辑模式但音色已不存在：安全提示并返回。
    if (!isNew && existing == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackTopBar(title = "编辑音色", onBack = onBack)
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("音色不存在或已被删除。", style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(onClick = onBack) { Text("返回") }
                }
            }
        }
        return
    }

    // 表单状态：仅在 profile id 变化时初始化，避免每次重组重置用户输入。
    var name by rememberSaveable(voiceProfileId) { mutableStateOf(existing?.displayName.orEmpty()) }
    var vid by rememberSaveable(voiceProfileId) { mutableStateOf(existing?.voiceId.orEmpty()) }
    var description by rememberSaveable(voiceProfileId) { mutableStateOf(existing?.description.orEmpty()) }
    var language by rememberSaveable(voiceProfileId) { mutableStateOf(existing?.language.orEmpty()) }
    var style by rememberSaveable(voiceProfileId) { mutableStateOf(existing?.style.orEmpty()) }
    var saving by rememberSaveable(voiceProfileId) { mutableStateOf(false) }
    // 头像表单状态：仅在保存时才落盘。pendingAvatarUri 为本次新选图片（字符串以便 rememberSaveable）。
    var pendingAvatarUri by rememberSaveable(voiceProfileId) { mutableStateOf<String?>(null) }
    var avatarRemoved by rememberSaveable(voiceProfileId) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(title = if (isNew) "新增音色" else "编辑音色", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 头像选择区（新增 / 编辑共用）。
            AvatarPickerField(
                name = name,
                currentAvatarPath = existing?.avatarPath,
                pendingUri = pendingAvatarUri,
                avatarRemoved = avatarRemoved,
                onPick = { uri ->
                    pendingAvatarUri = uri?.toString()
                    if (uri != null) avatarRemoved = false
                },
                onRemove = {
                    pendingAvatarUri = null
                    avatarRemoved = true
                }
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(VoiceFormLimits.MAX_VOICE_NAME_LENGTH) },
                label = { Text("音色名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = vid,
                onValueChange = { vid = it.take(VoiceFormLimits.MAX_VOICE_ID_LENGTH) },
                label = { Text("voice_id") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(VoiceFormLimits.MAX_DESCRIPTION_LENGTH) },
                label = { Text("音色描述（可选）") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = language,
                onValueChange = { language = it.take(VoiceFormLimits.MAX_SHORT_FIELD_LENGTH) },
                label = { Text("语言（可选）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = style,
                onValueChange = { style = it.take(VoiceFormLimits.MAX_SHORT_FIELD_LENGTH) },
                label = { Text("风格（可选）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (saving) return@Button
                    saving = true
                    viewModel.saveVoiceProfile(
                        editingId = voiceProfileId,
                        displayName = name,
                        voiceId = vid,
                        description = description,
                        language = language,
                        style = style,
                        avatarUri = pendingAvatarUri,
                        removeAvatar = avatarRemoved && pendingAvatarUri == null
                    ) { success ->
                        saving = false
                        if (success) onBack()
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(MioStyle.PrimaryButtonHeight),
                shape = RoundedCornerShape(MioStyle.PrimaryButtonRadius)
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("保存音色")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ----------------------------------------------------------------------------
// 子情绪预设编辑页（新增 / 编辑共用同一表单）
// ----------------------------------------------------------------------------

@Composable
private fun PresetEditorScreen(
    voiceProfileId: String?,
    presetId: String?,
    state: AppUiState,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    // 离开编辑页时停止试听。
    DisposableEffect(Unit) {
        onDispose { viewModel.stopPreview() }
    }
    val isNew = presetId == null
    val profile = state.settings.voices.firstOrNull { it.id == voiceProfileId }
    val existing = profile?.presets?.firstOrNull { it.id == presetId }

    // 路由参数无效（音色或预设不存在）：安全提示并返回，不崩溃。
    if (profile == null || (!isNew && existing == null)) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackTopBar(title = if (isNew) "新增情绪预设" else "编辑情绪预设", onBack = onBack)
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (profile == null) "音色不存在或已被删除。" else "预设不存在或已被删除。",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    OutlinedButton(onClick = onBack) { Text("返回") }
                }
            }
        }
        return
    }

    // initKey 必须随已存预设的内容变化：否则保存后退出再进同一预设（presetId 不变），
    // rememberSaveable 会从缓存恢复编辑前的旧 UI 状态，而不读刚写盘的 existing（表现为“重进显示旧值”）。
    // 加 existing 的内容指纹后，保存改变数据 → key 变 → 重新按新数据初始化。新建时 existing 为 null，指纹稳定。
    val initKey = "${voiceProfileId}_${presetId}_${existing?.hashCode() ?: 0}"
    var label by rememberSaveable(initKey) { mutableStateOf(existing?.label.orEmpty()) }
    var emotion by rememberSaveable(initKey) { mutableStateOf(existing?.emotion ?: "calm") }
    var speed by rememberSaveable(initKey) { mutableStateOf(existing?.speed ?: 1.0f) }
    var pitch by rememberSaveable(initKey) { mutableStateOf(existing?.pitch ?: 0) }
    var description by rememberSaveable(initKey) { mutableStateOf(existing?.description.orEmpty()) }
    var previewText by rememberSaveable(initKey) { mutableStateOf(existing?.previewText ?: VoiceLibrary.DEFAULT_PREVIEW_TEXT) }
    var saving by rememberSaveable(initKey) { mutableStateOf(false) }

    // voice_modify（嗓音改造）四项状态：用基元而非 Map，规避 rememberSaveable 无 Map Saver 的问题。
    // 初值从预设私有袋回填；只在 save/preview 边界折成 providerExtras。
    val initVoiceModify = remember(initKey) {
        MiniMaxVoiceModify.fromExtras(existing?.providerExtras ?: emptyMap())
    }
    var vmPitch by rememberSaveable(initKey) { mutableStateOf(initVoiceModify.pitch) }
    var vmIntensity by rememberSaveable(initKey) { mutableStateOf(initVoiceModify.intensity) }
    var vmTimbre by rememberSaveable(initKey) { mutableStateOf(initVoiceModify.timbre) }
    var vmSound by rememberSaveable(initKey) { mutableStateOf(initVoiceModify.soundEffect) }
    // 启用开关：开了才展开 + 才生效（折成 extras）。初值=已存预设里本就有非默认 voice_modify。
    var vmEnabled by rememberSaveable(initKey) {
        mutableStateOf(initVoiceModify.run { pitch != 0 || intensity != 0 || timbre != 0 || soundEffect != null })
    }
    // 关闭开关时不发送 voice_modify（保留滑块值但对外当未启用）。
    val voiceModifyExtras = if (vmEnabled) {
        MiniMaxVoiceModify.toExtras(vmPitch, vmIntensity, vmTimbre, vmSound)
    } else {
        emptyMap()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(title = if (isNew) "新增情绪预设" else "编辑情绪预设", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(VoiceFormLimits.MAX_PRESET_LABEL_LENGTH) },
                label = { Text("预设名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            EmotionSelector(selected = emotion, onSelect = { emotion = it })

            SpeedControl(value = speed, onChange = { speed = it })

            PitchControl(value = pitch, onChange = { pitch = it })

            VoiceModifySection(
                enabled = vmEnabled,
                onEnabledChange = { vmEnabled = it },
                pitch = vmPitch,
                intensity = vmIntensity,
                timbre = vmTimbre,
                soundEffect = vmSound,
                onPitchChange = { vmPitch = it },
                onIntensityChange = { vmIntensity = it },
                onTimbreChange = { vmTimbre = it },
                onSoundEffectChange = { vmSound = it }
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(VoiceFormLimits.MAX_DESCRIPTION_LENGTH) },
                label = { Text("描述（可选）") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = previewText,
                onValueChange = { previewText = it.take(VoiceFormLimits.MAX_PREVIEW_TEXT_LENGTH) },
                label = { Text("试听文本") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // 试听当前效果：使用表单上的未保存参数试听，不会触发保存。
            val editorPreviewStatus = if (state.preview.targetKey == PreviewUiState.EDITOR_TARGET) {
                state.preview.status
            } else null
            OutlinedButton(
                onClick = {
                    when (editorPreviewStatus) {
                        PreviewStatus.Generating, PreviewStatus.Playing -> viewModel.stopPreview()
                        else -> viewModel.previewDraft(
                            voiceProfileId = profile.id,
                            presetId = presetId,
                            emotion = emotion,
                            speed = speed,
                            pitch = pitch,
                            previewText = previewText,
                            providerExtras = voiceModifyExtras
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                when (editorPreviewStatus) {
                    PreviewStatus.Generating -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("停止")
                    }
                    PreviewStatus.Playing -> {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("停止")
                    }
                    else -> {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("试听当前效果")
                    }
                }
            }

            Button(
                onClick = {
                    if (saving) return@Button
                    saving = true
                    viewModel.savePresetForm(
                        voiceProfileId = voiceProfileId,
                        editingPresetId = presetId,
                        label = label,
                        emotion = emotion,
                        speed = speed,
                        pitch = pitch,
                        description = description,
                        previewText = previewText,
                        providerExtras = voiceModifyExtras
                    ) { success ->
                        saving = false
                        if (success) onBack()
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(MioStyle.PrimaryButtonHeight),
                shape = RoundedCornerShape(MioStyle.PrimaryButtonRadius)
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("保存预设")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 情绪选择器：FilterChip 多选一，展示中文名，保存英文枚举值。 */
@Composable
private fun EmotionSelector(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("情绪", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EmotionOptions.ordered.forEach { (value, cnLabel) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(cnLabel) }
                )
            }
        }
    }
}

/** 语速控件：滑块 + 实时数值。范围与 VoiceLibrary 归一化一致（0.5~2.0）。 */
@Composable
private fun SpeedControl(value: Float, onChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("语速", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("%.2f".format(value), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = { onChange((it * 100).toInt() / 100f) }, // 量化到 0.01
            valueRange = VoiceFormLimits.SPEED_MIN..VoiceFormLimits.SPEED_MAX
        )
    }
}

/** 音高控件：滑块 + 实时数值（整数步进）。范围与 VoiceLibrary 一致（-12~12）。 */
@Composable
private fun PitchControl(value: Int, onChange: (Int) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("音高 pitch", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(formatPitch(value), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = VoiceFormLimits.PITCH_MIN.toFloat()..VoiceFormLimits.PITCH_MAX.toFloat(),
            steps = (VoiceFormLimits.PITCH_MAX - VoiceFormLimits.PITCH_MIN) - 1
        )
    }
}

/**
 * voice_modify（嗓音改造）开关区：由一个启用开关驱动——关着不展开也不生效，开了才展开参数并发送。
 * 三个 ±100 滑块（pitch/intensity/timbre）+ 音效单选（含"无"）。0 视为默认（不改造）。
 */
@Composable
private fun VoiceModifySection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    pitch: Int,
    intensity: Int,
    timbre: Int,
    soundEffect: String?,
    onPitchChange: (Int) -> Unit,
    onIntensityChange: (Int) -> Unit,
    onTimbreChange: (Int) -> Unit,
    onSoundEffectChange: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onEnabledChange(!enabled) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    "高级：嗓音改造（voice_modify）",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "变声器式音色改造，与上面的语调 pitch（±12）不同。开启后生效。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
        if (enabled) {
            VoiceModifySlider("音高 pitch（低沉↔明亮）", pitch, onPitchChange)
            VoiceModifySlider("力度 intensity（力量↔柔和）", intensity, onIntensityChange)
            VoiceModifySlider("音质 timbre（浑厚↔清脆）", timbre, onTimbreChange)

            Text("音效 sound_effects", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = soundEffect == null,
                    onClick = { onSoundEffectChange(null) },
                    label = { Text("无") }
                )
                VoiceModifySoundEffects.forEach { (value, cnLabel) ->
                    FilterChip(
                        selected = soundEffect == value,
                        onClick = { onSoundEffectChange(value) },
                        label = { Text(cnLabel) }
                    )
                }
            }
        }
    }
}

/** voice_modify 数值滑块：±100 整数步进，0 居中显示"默认"。 */
@Composable
private fun VoiceModifySlider(title: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (value == 0) "默认" else (if (value > 0) "+$value" else value.toString()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = MiniMaxVoiceModify.VALUE_MIN.toFloat()..MiniMaxVoiceModify.VALUE_MAX.toFloat()
        )
    }
}

/** 音效枚举值 → 中文名（与 MiniMaxVoiceModify.ALLOWED_SOUND_EFFECTS 一一对应）。 */
private val VoiceModifySoundEffects = listOf(
    "spacious_echo" to "空旷回响",
    "auditorium_echo" to "礼堂广播",
    "lofi_telephone" to "电话失真",
    "robotic" to "电音机器人"
)

/** 统一的危险操作确认对话框（取消 / 删除）。 */
@Composable
private fun DeleteConfirmDialog(
    title: String,
    message: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { Text(message) },
        dismissButton = { TextButton(onClick = onCancel) { Text("取消") } },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}
@Composable
private fun NumberRow(
    repeatCount: Int,
    repeatPauseMs: Int,
    wordPauseMs: Int,
    onRepeatCount: (Int) -> Unit,
    onRepeatPause: (Int) -> Unit,
    onWordPause: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField("重复次数", repeatCount, onRepeatCount, Modifier.weight(1f))
        NumberField("重复间停顿 ms", repeatPauseMs, onRepeatPause, Modifier.weight(1f))
        NumberField("单词间停顿 ms", wordPauseMs, onWordPause, Modifier.weight(1f))
    }
}

@Composable
private fun NumberField(label: String, value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { onChange(it.toIntOrNull() ?: 0) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@Composable
private fun FloatField(label: String, value: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = "%.2f".format(value),
        onValueChange = { input -> input.toFloatOrNull()?.let(onChange) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

@Composable
private fun VoiceDropdown(
    voices: List<VoiceProfile>,
    selectedId: String?,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = voices.firstOrNull { it.id == selectedId }
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.displayName ?: "请先在音色库添加父音色")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            voices.forEach { profile ->
                DropdownMenuItem(text = { Text(profile.displayName) }, onClick = {
                    expanded = false
                    onSelected(profile.id)
                })
            }
        }
    }
}

@Composable
private fun PresetDropdown(
    presets: List<EmotionPreset>,
    selectedId: String?,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = presets.firstOrNull { it.id == selectedId }
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.label ?: "选择预设")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            presets.forEach { preset ->
                DropdownMenuItem(text = { Text("${preset.label} / ${preset.emotion}") }, onClick = {
                    expanded = false
                    onSelected(preset.id)
                })
            }
        }
    }
}

@Composable
private fun ModelDropdown(
    models: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = selected,
            onValueChange = onSelected,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("选择模型")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            models.forEach { model ->
                DropdownMenuItem(text = { Text(model) }, onClick = {
                    expanded = false
                    onSelected(model)
                })
            }
        }
    }
}


// ============================================================================
// 最近生成 / 历史 / 详情：统一使用紧凑卡 CompactGenerationCard，复用同一详情页。
// ============================================================================

private val PLAYBACK_SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

/** 首页“最近生成”：最多三条紧凑卡片 + “查看全部”。 */
@Composable
private fun RecentGenerationsSection(
    state: AppUiState,
    viewModel: AppViewModel,
    onOpenDetail: (String) -> Unit,
    onViewAll: () -> Unit
) {
    val groups = state.recentGroups
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("最近生成", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "查看全部",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onViewAll() }
            )
        }
        if (groups.isEmpty()) {
            GenerationEmptyState("生成的语音会出现在这里")
        } else {
            groups.forEach { group ->
                CompactGenerationCard(
                    group = group,
                    state = state,
                    onOpenDetail = { onOpenDetail(group.id) },
                    onTogglePlay = { togglePlayGroup(viewModel, state, group.id) }
                )
            }
        }
    }
}

@Composable
private fun GenerationEmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 紧凑生成卡：两行文本 + 音色·时长·相对时间 + 小播放按钮。首页与历史页共用。 */
@Composable
private fun CompactGenerationCard(
    group: GeneratedAudioGroup,
    state: AppUiState,
    onOpenDetail: () -> Unit,
    onTogglePlay: () -> Unit,
    onAddToCollection: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val playback = state.playback
    val isActive = playback.activeGenerationGroupId == group.id && playback.readyCount > 0
    val isPlaying = isActive && playback.isPlaying
    val previewText = groupDisplayTitle(group)
    val hasMenu = onAddToCollection != null || onRename != null || onDelete != null

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpenDetail() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    compactMetaLine(group),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                    .clickable { onTogglePlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            if (hasMenu) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (onAddToCollection != null) {
                            DropdownMenuItem(
                                text = { Text("加入组…") },
                                leadingIcon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                                onClick = { showMenu = false; onAddToCollection() }
                            )
                        }
                        if (onRename != null) {
                            DropdownMenuItem(
                                text = { Text("重命名") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = { showMenu = false; onRename() }
                            )
                        }
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; onDelete() }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// 历史记录页
// ----------------------------------------------------------------------------

@Composable
private fun HistoryScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit
) {
    // 系统返回键与左上角返回按钮执行完全一致的逻辑（出栈一层）。
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { viewModel.loadHistory() }
    // 当前正在为哪条记录选择「加入组」（null = 弹窗未开）。
    var addToCollectionGroupId by remember { mutableStateOf<String?>(null) }
    // 当前正在重命名/删除哪条记录（null = 弹窗未开）。
    var renameTargetGroup by remember { mutableStateOf<GeneratedAudioGroup?>(null) }
    var deleteTargetGroupId by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(title = "历史记录", onBack = onBack)
        val groups = state.historyGroups
        if (groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                GenerationEmptyState("还没有任何生成记录")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    // 底部安全区：内容滚动到底部时让出系统导航栏空间。
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groups.forEach { group ->
                    CompactGenerationCard(
                        group = group,
                        state = state,
                        onOpenDetail = { onOpenDetail(group.id) },
                        onTogglePlay = { togglePlayGroup(viewModel, state, group.id) },
                        onAddToCollection = { addToCollectionGroupId = group.id },
                        onRename = { renameTargetGroup = group },
                        onDelete = { deleteTargetGroupId = group.id }
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    addToCollectionGroupId?.let { gid ->
        AddToCollectionDialog(
            groupId = gid,
            state = state,
            viewModel = viewModel,
            onDismiss = { addToCollectionGroupId = null }
        )
    }

    renameTargetGroup?.let { target ->
        RenameGroupDialog(
            initial = target.customTitle ?: "",
            onConfirm = { input ->
                viewModel.renameGroup(target.id, input)
                renameTargetGroup = null
            },
            onDismiss = { renameTargetGroup = null }
        )
    }

    deleteTargetGroupId?.let { gid ->
        AlertDialog(
            onDismissRequest = { deleteTargetGroupId = null },
            title = { Text("删除生成记录") },
            text = { Text("确定删除这条生成记录吗？音频文件将一并删除，且无法恢复。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteGenerationGroup(gid); deleteTargetGroupId = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTargetGroupId = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            // 顶部安全区：自适应状态栏 / 刘海 / 挖孔高度，避免标题与系统图标重叠（不写死高度）。
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

// ----------------------------------------------------------------------------
// 关于 Mio Voice
// ----------------------------------------------------------------------------

/** 关于 Mio Voice 主页：顶部信息区（留白）+ 功能入口分组卡片 + 页脚弱辅助文字。 */
@Composable
private fun AboutScreen(
    onBack: () -> Unit,
    onOpenLicense: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    BackHandler(onBack = onBack)
    val uriHandler = LocalUriHandler.current

    // 打开外部链接：失败（无浏览器 / 无法处理）时静默忽略，绝不让 App 崩溃。
    fun openUrl(url: String) {
        runCatching { uriHandler.openUri(url) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(title = "关于 Mio Voice", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 顶部信息区：不套大卡片，靠留白呈现 App 图标 / 名称 / 版本 / 一句话介绍。
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                Image(
                    painter = painterResource(R.mipmap.ic_launcher),
                    contentDescription = "Mio Voice 应用图标",
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
                Text(
                    "Mio Voice",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "版本 ${AboutInfo.formatVersionName(BuildConfig.VERSION_NAME)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "一个支持音色管理、情绪预设与 AI 辅助分析的 Android TTS 客户端。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(4.dp))
            }

            // 功能入口：复用设置页同款圆角卡片行。
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsEntryRow(
                    icon = Icons.Default.Code,
                    title = "GitHub 项目主页",
                    subtitle = "查看源代码、更新与项目说明",
                    onClick = { openUrl(AboutInfo.GITHUB_URL) }
                )
                SettingsEntryRow(
                    icon = Icons.Default.BugReport,
                    title = "反馈问题与建议",
                    subtitle = "前往 GitHub Issues 提交反馈",
                    onClick = { openUrl(AboutInfo.ISSUES_URL) }
                )
                SettingsEntryRow(
                    icon = Icons.Default.Description,
                    title = "开源许可证",
                    subtitle = "本项目使用 MIT License",
                    onClick = onOpenLicense
                )
                SettingsEntryRow(
                    icon = Icons.Default.PrivacyTip,
                    title = "隐私说明",
                    subtitle = "了解配置、文本与音频数据的处理方式",
                    onClick = onOpenPrivacy
                )
            }

            // 页脚：弱辅助文字。
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Mio Voice · Open Source",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    "Made with Kotlin & Jetpack Compose",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 开源许可证本地页面：离线展示项目根目录 LICENSE 的 MIT License 全文。 */
@Composable
private fun AboutLicenseScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(title = "开源许可证", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(MioStyle.CardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = MioStyle.CardShadow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    AboutInfo.LICENSE_TEXT,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 隐私说明本地页面：与当前实现一致的、自然易懂的数据处理说明。 */
@Composable
private fun AboutPrivacyScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(title = "隐私说明", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AboutInfo.PRIVACY_PARAGRAPHS.forEach { paragraph ->
                AboutPrivacyParagraph(paragraph)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 隐私说明段落：与设置页一致的圆角卡片承载单条说明。 */
@Composable
private fun AboutPrivacyParagraph(text: String) {
    Card(
        shape = RoundedCornerShape(MioStyle.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = MioStyle.CardShadow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp)
        )
    }
}

// ----------------------------------------------------------------------------
// 语音库（组 / 收藏集）
// ----------------------------------------------------------------------------

/** 语音库主页（底部导航 tab）：组列表 + 新建组。 */
@Composable
private fun AudioLibraryScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    onOpenCollection: (String) -> Unit
) {
    LaunchedEffect(Unit) { viewModel.loadCollections() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<AudioCollection?>(null) }
    var deleteTarget by remember { mutableStateOf<AudioCollection?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("语音库", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "新建组", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (state.collections.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.LibraryMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Text("还没有任何组", style = MaterialTheme.typography.titleMedium)
                Text(
                    "创建一个组（比如「我的收藏」），把历史里的生成记录归类进来，方便反复收听。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("新建第一个组")
                }
            }
        } else {
            state.collections.forEach { summary ->
                CollectionCard(
                    summary = summary,
                    onOpen = { onOpenCollection(summary.collection.id) },
                    onRename = { renameTarget = summary.collection },
                    onDelete = { deleteTarget = summary.collection }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCreateDialog) {
        CollectionNameDialog(
            title = "新建组",
            initial = "",
            confirmLabel = "创建",
            onConfirm = { name -> viewModel.createCollection(name); showCreateDialog = false },
            onDismiss = { showCreateDialog = false }
        )
    }
    renameTarget?.let { target ->
        CollectionNameDialog(
            title = "重命名组",
            initial = target.name,
            confirmLabel = "保存",
            onConfirm = { name -> viewModel.renameCollection(target.id, name); renameTarget = null },
            onDismiss = { renameTarget = null }
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除组") },
            text = { Text("确定删除组「${target.name}」吗？组内的生成记录与音频不会被删除，仅解除归类。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCollection(target.id); deleteTarget = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

/** 组卡片：组名 + 成员数 + 更新时间 + 溢出菜单（重命名 / 删除）。 */
@Composable
private fun CollectionCard(
    summary: CollectionSummary,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val collection = summary.collection
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    collection.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${summary.itemCount} 条 · ${RecentGenerationsLogic.relativeTime(collection.updatedAt, System.currentTimeMillis())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("重命名") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { showMenu = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text("删除组", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

/** 组名输入弹窗（新建 / 重命名共用）。 */
@Composable
private fun CollectionNameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    val trimmed = name.trim()
    val valid = trimmed.isNotEmpty() && trimmed.length <= CollectionFormLogic.MAX_NAME_LENGTH
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(CollectionFormLogic.MAX_NAME_LENGTH) },
                label = { Text("组名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(trimmed) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

/** 生成记录重命名弹窗：留空提交=清除自定义标题，恢复默认显示原文预览。 */
@Composable
private fun RenameGroupDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(80) },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "留空则恢复默认（显示原文预览）。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

/** 组详情页（栈子页）：连续播放全部 + 成员列表 + 移出本组。 */
@Composable
private fun CollectionDetailScreen(
    collectionId: String?,
    state: AppUiState,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onAddRecords: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    LaunchedEffect(collectionId) { collectionId?.let { viewModel.loadCollectionDetail(it) } }

    val detail = state.collectionDetail
    val collection = detail.collection

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(title = collection?.name ?: "组详情", onBack = onBack)

        if (collection == null) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                if (detail.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(detail.error ?: "组不存在或已被删除。", style = MaterialTheme.typography.bodyLarge)
                        OutlinedButton(onClick = onBack) { Text("返回") }
                    }
                }
            }
            return@Column
        }

        val playback = state.playback
        val isActive = playback.activeGenerationGroupId == collection.id && playback.readyCount > 0
        val isPlaying = isActive && playback.isPlaying

        // 用 Box 承载内容滚动区 + 右下角悬浮「+」（进入历史选择页加记录）。
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (isPlaying) viewModel.pause() else viewModel.playCollection(collection.id)
                    },
                    enabled = detail.memberGroups.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(MioStyle.PrimaryButtonHeight),
                    shape = RoundedCornerShape(MioStyle.PrimaryButtonRadius)
                ) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isPlaying) "暂停" else "连续播放全部")
                }

                if (detail.memberGroups.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                        GenerationEmptyState("这个组还没有内容\n点右下角「+」从历史记录里添加")
                    }
                } else {
                    detail.memberGroups.forEach { group ->
                        CollectionMemberCard(
                            group = group,
                            state = state,
                            onOpenDetail = { onOpenDetail(group.id) },
                            onTogglePlay = { togglePlayGroup(viewModel, state, group.id) },
                            onRemove = { viewModel.removeGroupFromCollection(collection.id, group.id) }
                        )
                    }
                    // 给 FAB 让出底部空间，避免遮住最后一张卡片。
                    Spacer(Modifier.height(88.dp))
                }
            }

            FloatingActionButton(
                onClick = { onAddRecords(collection.id) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "从历史添加")
            }
        }
    }
}

/** 组内成员卡片：复用紧凑卡样式 + 「移出本组」溢出菜单。 */
@Composable
private fun CollectionMemberCard(
    group: GeneratedAudioGroup,
    state: AppUiState,
    onOpenDetail: () -> Unit,
    onTogglePlay: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val playback = state.playback
    val isActive = playback.activeGenerationGroupId == group.id && playback.readyCount > 0
    val isPlaying = isActive && playback.isPlaying
    val previewText = groupDisplayTitle(group)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpenDetail() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    compactMetaLine(group),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                    .clickable { onTogglePlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("移出本组", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onRemove() }
                    )
                }
            }
        }
    }
}

/**
 * 「从历史选记录加入本组」选择页（栈子页）：
 * 列出不在本组的历史记录，每条左侧加号；点一下加入本组并把加号变勾（本次会话内保留显示作反馈）。
 */
@Composable
private fun CollectionPickerScreen(
    collectionId: String?,
    state: AppUiState,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    LaunchedEffect(collectionId) { collectionId?.let { viewModel.loadCollectionPicker(it) } }

    val picker = state.collectionPicker
    val cid = collectionId

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(title = "添加到组", onBack = onBack)

        when {
            cid == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("组不存在。", style = MaterialTheme.typography.bodyLarge)
                }
            }
            picker.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            picker.candidates.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    GenerationEmptyState("没有可添加的记录\n历史里的记录都已在本组，或还没有生成记录")
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    picker.candidates.forEach { group ->
                        PickerRecordCard(
                            group = group,
                            added = group.id in picker.addedIds,
                            onToggle = { viewModel.pickerToggleGroup(cid, group.id) }
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/** 选记录页的单条卡片：左侧加号/勾（再点取消）+ 文本预览 + 元信息。 */
@Composable
private fun PickerRecordCard(
    group: GeneratedAudioGroup,
    added: Boolean,
    onToggle: () -> Unit
) {
    val previewText = groupDisplayTitle(group)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (added) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 加号 / 已加入勾。再点一下取消选择（移出本组）。
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = if (added) 0.0f else 0.14f), RoundedCornerShape(20.dp))
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (added) Icons.Default.CheckCircle else Icons.Default.Add,
                    contentDescription = if (added) "已加入（点击取消）" else "加入本组",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(if (added) 26.dp else 24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    compactMetaLine(group),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (added) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 「加入组」弹窗：列出所有组 + 复选框（预勾当前归属），底部「+ 新建组」。
 * 确认时 diff 出新增/移除并落库。
 */
@Composable
private fun AddToCollectionDialog(
    groupId: String,
    state: AppUiState,
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    // 预勾选：进入弹窗时查询该记录当前所属组。
    var initialIds by remember { mutableStateOf<Set<String>?>(null) }
    var checked by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(groupId, state.collections.size) {
        viewModel.collectionIdsForGroup(groupId) { ids ->
            if (initialIds == null) {
                initialIds = ids
                checked = ids
            }
        }
    }
    LaunchedEffect(Unit) { viewModel.loadCollections() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入组") },
        text = {
            if (state.collections.isEmpty()) {
                Text("还没有任何组，先新建一个吧。")
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    state.collections.forEach { summary ->
                        val id = summary.collection.id
                        val isOn = id in checked
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    checked = if (isOn) checked - id else checked + id
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(checked = isOn, onCheckedChange = { on ->
                                checked = if (on) checked + id else checked - id
                            })
                            Text(
                                summary.collection.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    TextButton(onClick = { showCreate = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("新建组")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = initialIds != null,
                onClick = {
                    val base = initialIds ?: emptySet()
                    (checked - base).forEach { viewModel.addGroupToCollection(it, groupId) }
                    (base - checked).forEach { viewModel.removeGroupFromCollection(it, groupId) }
                    onDismiss()
                }
            ) { Text("完成") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )

    if (showCreate) {
        CollectionNameDialog(
            title = "新建组",
            initial = "",
            confirmLabel = "创建",
            onConfirm = { name -> viewModel.createCollection(name); showCreate = false },
            onDismiss = { showCreate = false }
        )
    }
}

// ----------------------------------------------------------------------------
// 语音详情页
// ----------------------------------------------------------------------------

@Composable
private fun AudioDetailScreen(
    groupId: String?,
    state: AppUiState,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    // 系统返回键与左上角返回按钮执行完全一致的逻辑（出栈一层，回到来源页面）。
    BackHandler(onBack = onBack)
    LaunchedEffect(groupId) { groupId?.let { viewModel.loadAudioDetail(it) } }
    DisposableEffect(groupId) { onDispose { viewModel.clearAudioDetail() } }

    val detail = state.detail
    val group = detail.group

    Column(modifier = Modifier.fillMaxSize()) {
        if (group == null) {
            BackTopBar(title = "语音详情", onBack = onBack)
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                if (detail.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(detail.error ?: "记录不存在或已被删除。", style = MaterialTheme.typography.bodyLarge)
                        OutlinedButton(onClick = onBack) { Text("返回") }
                    }
                }
            }
            return@Column
        }

        var showMoreMenu by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }
        var showAddToCollection by remember { mutableStateOf(false) }
        var showRename by remember { mutableStateOf(false) }
        val clipboard = LocalClipboardManager.current

        BackTopBar(
            title = "语音详情",
            onBack = onBack,
            actions = {
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("复制原始文本") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                clipboard.setText(AnnotatedString(group.originalText))
                                viewModel.notify("原始文本已复制。")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("重命名") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                showRename = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("加入组…") },
                            leadingIcon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                showAddToCollection = true
                            }
                        )
                        run {
                            // 仅单段记录可导出；多段暂不支持，置灰。
                            val canExport = detail.segments.size == 1
                            DropdownMenuItem(
                                text = { Text(if (canExport) "导出语音" else "导出语音（多段暂不支持）") },
                                leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                                enabled = canExport,
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.requestExportCurrentDetail()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("重新生成") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                viewModel.prepareRegenerate(group)
                                onBack()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("删除记录", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMoreMenu = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                // 底部安全区：内容滚动到底部时让出系统导航栏空间。
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DetailPlayerCard(group, state, viewModel)
            DetailOriginalTextCard(group.originalText, clipboard = clipboard, onCopied = { viewModel.notify("原始文本已复制。") })
            DetailInfoCard(group)
            DetailTechCard(group, detail)
            Spacer(Modifier.height(24.dp))
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("删除生成记录") },
                text = { Text("将删除本条历史记录，并同时删除本地音频文件，删除后无法在 App 中恢复。确认删除？") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        viewModel.deleteGenerationGroup(group.id)
                        onBack()
                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
                }
            )
        }

        if (showAddToCollection) {
            AddToCollectionDialog(
                groupId = group.id,
                state = state,
                viewModel = viewModel,
                onDismiss = { showAddToCollection = false }
            )
        }

        if (showRename) {
            RenameGroupDialog(
                initial = group.customTitle ?: "",
                onConfirm = { input ->
                    viewModel.renameGroup(group.id, input)
                    showRename = false
                },
                onDismiss = { showRename = false }
            )
        }
    }
}

/** 详情页顶部整组播放器。 */
@Composable
private fun DetailPlayerCard(
    group: GeneratedAudioGroup,
    state: AppUiState,
    viewModel: AppViewModel
) {
    val playback = state.playback
    val isActive = playback.activeGenerationGroupId == group.id && playback.readyCount > 0
    val isPlaying = isActive && playback.isPlaying
    var showSpeedMenu by remember { mutableStateOf(false) }

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(0f) }

    val durationMs = if (isActive && playback.currentGroupDurationMs > 0L) playback.currentGroupDurationMs else group.totalDurationMs
    val livePositionMs = if (isActive) playback.currentGroupPositionMs else 0L
    val sliderFraction = if (isDragging) dragFraction else RecentGenerationsLogic.positionToFraction(livePositionMs, durationMs)
    val displayPositionMs = if (isDragging) RecentGenerationsLogic.fractionToPositionMs(dragFraction, durationMs) else livePositionMs
    val speedLabel = if (isActive) playback.playbackSpeed else 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                groupDisplayTitle(group),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${group.voiceName} · ${AudioDetailLogic.formatDateTime(group.createdAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Slider(
                    value = sliderFraction,
                    onValueChange = { isDragging = true; dragFraction = it },
                    onValueChangeFinished = {
                        val target = RecentGenerationsLogic.fractionToPositionMs(dragFraction, durationMs)
                        if (isActive) viewModel.seekGenerationGroupTo(target) else viewModel.playGenerationGroup(group.id)
                        isDragging = false
                    },
                    enabled = durationMs > 0L,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(RecentGenerationsLogic.formatDuration(displayPositionMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(RecentGenerationsLogic.formatDuration(durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp))
                            .clickable { showSpeedMenu = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("${formatSpeedLabel(speedLabel)}×", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
                        PLAYBACK_SPEED_OPTIONS.forEach { speed ->
                            val selected = kotlin.math.abs(speedLabel - speed) < 0.001f
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${formatSpeedLabel(speed)}×",
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = { showSpeedMenu = false; viewModel.setPlaybackSpeed(speed) }
                            )
                        }
                    }
                }

                IconButton(onClick = { viewModel.seekGenerationGroupBy(-5000) }) {
                    Icon(Icons.Default.Replay5, contentDescription = "后退5秒", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(28.dp))
                        .clickable {
                            when {
                                !isActive -> viewModel.playGenerationGroup(group.id)
                                isPlaying -> viewModel.pause()
                                else -> viewModel.play()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                IconButton(onClick = { viewModel.seekGenerationGroupBy(5000) }) {
                    Icon(Icons.Default.Forward5, contentDescription = "前进5秒", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // 占位，使中央播放按钮视觉居中（与左侧倍速胶囊平衡）
                Spacer(Modifier.size(40.dp))
            }
        }
    }
}

@Composable
private fun DetailOriginalTextCard(
    originalText: String,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    onCopied: () -> Unit
) {
    val collapsible = originalText.length > 240
    var expanded by rememberSaveable(originalText) { mutableStateOf(!collapsible) }

    DetailSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("原始文本", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = {
                clipboard.setText(AnnotatedString(originalText))
                onCopied()
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "复制", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text(
            originalText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (expanded) Int.MAX_VALUE else 6,
            overflow = TextOverflow.Ellipsis
        )
        if (collapsible) {
            Text(
                if (expanded) "收起" else "展开全文",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { expanded = !expanded }
            )
        }
    }
}

@Composable
private fun DetailInfoCard(group: GeneratedAudioGroup) {
    DetailSectionCard {
        Text("生成信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        InfoRow("使用音色", group.voiceName)
        InfoRow("情绪", AudioDetailLogic.orUnknown(group.emotion))
        InfoRow("生成模型", AudioDetailLogic.orUnknown(group.model))
        InfoRow("生成时间", AudioDetailLogic.formatDateTime(group.createdAt))
        InfoRow("生成语速", AudioDetailLogic.formatSpeed(group.speed))
        InfoRow("音频时长", RecentGenerationsLogic.formatDuration(group.totalDurationMs))
        InfoRow("生成类型", AudioDetailLogic.generationTypeLabel(group.generationType))
        InfoRow("分段数量", group.segmentCount.toString())
    }
}

@Composable
private fun DetailTechCard(group: GeneratedAudioGroup, detail: AudioDetailState) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    DetailSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("技术信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (expanded) {
            InfoRow("服务提供商", AudioDetailLogic.orUnknown(group.provider))
            InfoRow("完整模型名", AudioDetailLogic.orUnknown(group.model))
            InfoRow("voiceId", AudioDetailLogic.orUnknown(group.voiceId))
            InfoRow("音频格式", AudioDetailLogic.orUnknown(group.format))
            InfoRow("文件总大小", AudioDetailLogic.formatFileSize(detail.fileSizeBytes))
            InfoRow("分段数量", group.segmentCount.toString())
            InfoRow("本地文件状态", AudioDetailLogic.localFilesLabel(detail.localFilesOk))
        }
    }
}

@Composable
private fun DetailSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

// ----------------------------------------------------------------------------
// 共用辅助
// ----------------------------------------------------------------------------

private fun togglePlayGroup(viewModel: AppViewModel, state: AppUiState, groupId: String) {
    val playback = state.playback
    val isActive = playback.activeGenerationGroupId == groupId && playback.readyCount > 0
    when {
        !isActive -> viewModel.playGenerationGroup(groupId)
        playback.isPlaying -> viewModel.pause()
        else -> viewModel.play()
    }
}

/** 该生成记录的统一显示标题：自定义标题优先，回退到原文预览、原始文本。 */
private fun groupDisplayTitle(group: GeneratedAudioGroup): String =
    group.customTitle?.takeIf { it.isNotBlank() }
        ?: group.previewText?.takeIf { it.isNotBlank() }
        ?: group.originalText

private fun compactMetaLine(group: GeneratedAudioGroup): String {
    val duration = RecentGenerationsLogic.formatDuration(group.totalDurationMs)
    val relative = RecentGenerationsLogic.relativeTime(group.createdAt, System.currentTimeMillis())
    return "${group.voiceName} · $duration · $relative"
}

private fun formatSpeedLabel(speed: Float): String {
    val rounded = Math.round(speed * 100f) / 100f
    return if (rounded % 1f == 0f) "%.1f".format(rounded) else rounded.toString().trimEnd('0').trimEnd('.')
}
