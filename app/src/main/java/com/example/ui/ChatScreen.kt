package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSessionEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val inputText by viewModel.inputMessageText.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isInspectorOpen by viewModel.isInspectorOpen.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Error Snackbar Host
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(modifier = Modifier.statusBarsPadding())
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Saved Chats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GeoTextPrimary
                    )
                    IconButton(
                        onClick = { viewModel.createSession() },
                        modifier = Modifier.testTag("new_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Conversation",
                            tint = GeoPrimary
                        )
                    }
                }

                HorizontalDivider(color = GeoLightBorder)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(sessions) { session ->
                        val isSelected = session.id == activeSessionId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) GeoPrimary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    viewModel.selectSession(session.id)
                                    coroutineScope.launch { drawerState.close() }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("session_item_${session.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (session.title.contains("Coding")) Icons.Default.Terminal
                                    else if (session.title.contains("Trivia")) Icons.Default.Psychology
                                    else if (session.title.contains("Adventure")) Icons.Default.Casino
                                    else Icons.Default.Chat,
                                    contentDescription = "Session icon",
                                    tint = if (isSelected) GeoPrimary else GeoTextSecondary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = session.title,
                                    color = if (isSelected) GeoPrimary else GeoTextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (sessions.size > 1) {
                                IconButton(
                                    onClick = { viewModel.deleteSession(session.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Session",
                                        tint = Color.Red.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(GeoPrimary, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "L",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Context Flow Lab",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = GeoTextPrimary
                                    )
                                    Text(
                                        text = "Active Session: ${activeSession?.title?.uppercase() ?: "NONE"}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp,
                                            fontSize = 10.sp
                                        ),
                                        color = GeoPrimary
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("session_selector")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Conversations Drawer",
                                    tint = GeoTextPrimary
                                )
                            }
                        },
                        actions = {
                            // Inspector Toggle
                            IconButton(
                                onClick = { viewModel.toggleInspector() },
                                modifier = Modifier.testTag("inspector_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isInspectorOpen) Icons.Default.Tune else Icons.Default.Settings,
                                    contentDescription = "Toggle Context Inspector",
                                    tint = if (isInspectorOpen) GeoPrimary else GeoTextPrimary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.statusBarsPadding()
                    )
                    HorizontalDivider(color = GeoLightBorder)
                }
            },
            modifier = modifier.background(MaterialTheme.colorScheme.background)
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                val isWideScreen = maxWidth > 720.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Column: The Conversation Flow Panel
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Visual Indicator that context is active
                        ContextStateIndicator(
                            messageCount = messages.size,
                            systemPrompt = activeSession?.systemInstruction ?: "",
                            temp = activeSession?.temperature ?: 0.7f,
                            onInspectorToggle = { viewModel.toggleInspector() }
                        )

                        // Message List Pane
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            if (messages.isEmpty()) {
                                OnboardingEmptyState(
                                    onLoadPreset = { title, instr, temp, greet ->
                                        viewModel.loadPreset(title, instr, temp, greet)
                                    }
                                )
                            } else {
                                val listState = rememberLazyListState()
                                LaunchedEffect(messages.size) {
                                    if (messages.isNotEmpty()) {
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                }

                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                                ) {
                                    items(messages) { message ->
                                        MessageBubble(message = message)
                                    }
                                    if (isSending) {
                                        item {
                                            ModelTypingIndicator()
                                        }
                                    }
                                }
                            }
                        }

                        // Presets Row & Input Box
                        Surface(
                            tonalElevation = 2.dp,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(width = 1.dp, color = GeoLightBorder, shape = RoundedCornerShape(0.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .navigationBarsPadding()
                            ) {
                                // Preset shortcuts right above input for easy play
                                if (messages.isNotEmpty()) {
                                    Text(
                                        text = "Switch Context Preset:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GeoTextSecondary,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    ) {
                                        item {
                                            PresetChip(
                                                label = "Socratic Trivia",
                                                icon = Icons.Default.Psychology,
                                                testTag = "preset_trivia_button",
                                                onClick = {
                                                    viewModel.loadPreset(
                                                        title = "Socratic Trivia 🧠",
                                                        systemInstruction = "You are a witty trivia master. Ask the user 3 trivia questions about science and history, one by one. Maintain their running score in each response (e.g., Score: X/3). After they answer, explain the answer briefly, increment the question number, and ask the next question.",
                                                        temperature = 0.6f,
                                                        firstMessage = "Greetings, knowledge seeker! Welcome to the Trivia Lab. I'll test your wits across 3 questions. Let's start with Question 1: What is the only planet in our solar system that rotates clockwise?"
                                                    )
                                                }
                                            )
                                        }
                                        item {
                                            PresetChip(
                                                label = "Coding Tutor",
                                                icon = Icons.Default.Terminal,
                                                testTag = "preset_code_button",
                                                onClick = {
                                                    viewModel.loadPreset(
                                                        title = "Coding Lab 💻",
                                                        systemInstruction = "You are an expert software engineering tutor. Guide the user through refactoring code step-by-step. Break changes into small, logical iterations. Ask for confirmation before moving to the next refactoring step. Keep responses concise and use clean code blocks.",
                                                        temperature = 0.2f,
                                                        firstMessage = "Welcome to the Coding Lab! Send me some messy code, and we'll refactor it together step-by-step. What language or code are we starting with?"
                                                    )
                                                }
                                            )
                                        }
                                        item {
                                            PresetChip(
                                                label = "Adventure RPG",
                                                icon = Icons.Default.Casino,
                                                testTag = "preset_rpg_button",
                                                onClick = {
                                                    viewModel.loadPreset(
                                                        title = "Adventure Engine 🌲",
                                                        systemInstruction = "You are the Game Master of a text-based fantasy RPG. Start an adventure for the user in a mysterious ancient forest. Give them 3 distinct numbered action choices in each turn (e.g. 1. Do X, 2. Do Y, 3. Do Z). Maintain their inventory and health points in a small text box block at the bottom of your response.",
                                                        temperature = 1.0f,
                                                        firstMessage = "The trees whisper secrets of ancient gold... Welcome to the Adventure Engine! You stand at the edge of the Whispering Woods with a rusted sword and 10 HP. What will you do?\n\n1. Venture down the dark, overgrown path.\n2. Examine the glowing mushrooms near the river.\n3. Climb the tall ancient oak to get a better view."
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }

                                // Text input and send
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = inputText,
                                        onValueChange = { viewModel.updateInputText(it) },
                                        placeholder = {
                                            Text(
                                                "Ask follow up question...",
                                                color = GeoTextMuted
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("message_input"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.background,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                            focusedBorderColor = GeoPrimary,
                                            unfocusedBorderColor = GeoLightBorder,
                                            focusedTextColor = GeoTextPrimary,
                                            unfocusedTextColor = GeoTextPrimary
                                        ),
                                        maxLines = 4,
                                        keyboardOptions = KeyboardOptions(
                                            imeAction = ImeAction.Send
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onSend = {
                                                if (inputText.isNotBlank() && !isSending) {
                                                    viewModel.sendMessage()
                                                    focusManager.clearFocus()
                                                }
                                            }
                                        )
                                    )

                                    IconButton(
                                        onClick = {
                                            viewModel.sendMessage()
                                            focusManager.clearFocus()
                                        },
                                        enabled = inputText.isNotBlank() && !isSending,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                if (inputText.isNotBlank() && !isSending) GeoPrimary else GeoLightBg,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .testTag("send_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Send message",
                                            tint = if (inputText.isNotBlank() && !isSending) Color.White else GeoTextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Style Pacing indicators at the bottom
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(20.dp)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(GeoPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(3.dp)
                                            .clip(CircleShape)
                                            .background(GeoLightBorder)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(3.dp)
                                            .clip(CircleShape)
                                            .background(GeoLightBorder)
                                    )
                                }
                            }
                        }
                    }

                    // Right Column: Side Pane Context Inspector (For wide screens only)
                    if (isWideScreen && isInspectorOpen) {
                        Surface(
                            modifier = Modifier
                                .width(340.dp)
                                .fillMaxHeight()
                                .border(
                                    width = 1.dp,
                                    color = GeoLightBorder,
                                    shape = RoundedCornerShape(0.dp)
                                ),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            ContextInspectorPanel(
                                session = activeSession,
                                messages = messages,
                                onUpdateSettings = { systemInstr, temp, tokens ->
                                    viewModel.updateSessionSettings(systemInstr, temp, tokens)
                                },
                                onClearMessages = { viewModel.clearActiveMessages() }
                            )
                        }
                    }
                }

                // Modal Bottom Sheet Context Inspector (For compact screens only)
                if (!isWideScreen && isInspectorOpen) {
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.toggleInspector() },
                        containerColor = MaterialTheme.colorScheme.surface,
                        dragHandle = { BottomSheetDefaults.DragHandle(color = GeoTextMuted) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.75f)
                        ) {
                            ContextInspectorPanel(
                                session = activeSession,
                                messages = messages,
                                onUpdateSettings = { systemInstr, temp, tokens ->
                                    viewModel.updateSessionSettings(systemInstr, temp, tokens)
                                },
                                onClearMessages = { viewModel.clearActiveMessages() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContextStateIndicator(
    messageCount: Int,
    systemPrompt: String,
    temp: Float,
    onInspectorToggle: () -> Unit
) {
    Surface(
        color = GeoInfoBg,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspectorToggle() }
            .border(width = 1.dp, color = GeoInfoBorder, shape = RoundedCornerShape(0.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pulse Green indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (messageCount > 0) GeoAccent else Color.Gray, CircleShape)
                )
                Text(
                    text = if (messageCount > 0) "Maintaining context: active memory stack" else "Awaiting flow initiation...",
                    color = GeoInfoText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(GeoInfoBorder)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                val turnCount = (messageCount + 1) / 2
                Text(
                    text = "$turnCount Turn${if (turnCount == 1) "" else "s"}",
                    color = GeoInfoText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessageEntity) {
    val isUser = message.role == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) GeoPrimary else MaterialTheme.colorScheme.surface
    val strokeColor = if (isUser) GeoPrimary else GeoLightBorder
    val textAlignment = TextAlign.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 4.dp, start = 8.dp, end = 8.dp)
        ) {
            Icon(
                imageVector = if (isUser) Icons.Default.Person else Icons.Default.SmartToy,
                contentDescription = if (isUser) "User Avatar" else "Assistant Avatar",
                tint = if (isUser) GeoSecondary else GeoPrimary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (isUser) "09:42 · YOU" else "09:41 · ASSISTANT",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = if (isUser) GeoTextSecondary else GeoTextMuted,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isUser) 16.dp else 0.dp,
                        topEnd = if (isUser) 0.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                )
                .background(bubbleColor)
                .border(
                    width = 1.dp,
                    color = strokeColor,
                    shape = RoundedCornerShape(
                        topStart = if (isUser) 16.dp else 0.dp,
                        topEnd = if (isUser) 0.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                )
                .padding(14.dp)
        ) {
            Column {
                val isCode = message.text.startsWith("```") && message.text.endsWith("```")
                if (isCode) {
                    val codeContent = message.text.removeSurrounding("```")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = codeContent,
                            color = Color(0xFF10B981),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    Text(
                        text = message.text,
                        color = if (isUser) Color.White else GeoTextPrimary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp
                        ),
                        textAlign = textAlignment
                    )
                }
            }
        }
    }
}

@Composable
fun ModelTypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 4.dp, start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "AI Assistant typing",
                    tint = GeoPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Agent is writing...",
                    style = MaterialTheme.typography.labelSmall,
                    color = GeoTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(width = 1.dp, color = GeoLightBorder, shape = RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = GeoPrimary
                )
            }
        }
    }
}

@Composable
fun PresetChip(
    label: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, GeoLightBorder),
        shadowElevation = 1.dp,
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GeoPrimary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = GeoTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun OnboardingEmptyState(
    onLoadPreset: (title: String, systemInstruction: String, temperature: Float, firstMessage: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Aesthetic blueprint logo block
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(GeoPrimary, GeoSecondary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = "Lab icon",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Context Memory Lab",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = GeoTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Test how multi-turn conversation memory works. Ask follow-up questions, edit system instructions dynamically, and observe the live payload stack.",
            style = MaterialTheme.typography.bodyMedium,
            color = GeoTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "CHOOSE A FLOW SCENARIO TO TEST:",
            style = MaterialTheme.typography.labelSmall,
            color = GeoPrimary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Scenarios Grid/List
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PresetCard(
                title = "1. Trivia Master Flow 🧠",
                desc = "AI asks sequential trivia and tracks score across multi-turn gameplay.",
                icon = Icons.Default.Psychology,
                accent = GeoPrimary,
                testTag = "empty_preset_trivia"
            ) {
                onLoadPreset(
                    "Socratic Trivia 🧠",
                    "You are a witty trivia master. Ask the user 3 trivia questions about science and history, one by one. Maintain their running score in each response (e.g., Score: X/3). After they answer, explain the answer briefly, increment the question number, and ask the next question.",
                    0.6f,
                    "Greetings, knowledge seeker! Welcome to the Trivia Lab. I'll test your wits across 3 questions. Let's start with Question 1: What is the only planet in our solar system that rotates clockwise?"
                )
            }

            PresetCard(
                title = "2. Coding Refactor Flow 💻",
                desc = "AI guides step-by-step code refinement based on past design iterations.",
                icon = Icons.Default.Terminal,
                accent = GeoSecondary,
                testTag = "empty_preset_code"
            ) {
                onLoadPreset(
                    "Coding Lab 💻",
                    "You are an expert software engineering tutor. Guide the user through refactoring code step-by-step. Break changes into small, logical iterations. Ask for confirmation before moving to the next refactoring step. Keep responses concise and use clean code blocks.",
                    0.2f,
                    "Welcome to the Coding Lab! Send me some messy code, and we'll refactor it together step-by-step. What language or code are we starting with?"
                )
            }

            PresetCard(
                title = "3. Fantasy Adventure Game 🌲",
                desc = "A roleplay adventure tracking inventory & health points dynamically.",
                icon = Icons.Default.Casino,
                accent = GeoTertiary,
                testTag = "empty_preset_rpg"
            ) {
                onLoadPreset(
                    "Adventure Engine 🌲",
                    "You are the Game Master of a text-based fantasy RPG. Start an adventure for the user in a mysterious ancient forest. Give them 3 distinct numbered action choices in each turn (e.g. 1. Do X, 2. Do Y, 3. Do Z). Maintain their inventory and health points in a small text box block at the bottom of your response.",
                    1.0f,
                    "The trees whisper secrets of ancient gold... Welcome to the Adventure Engine! You stand at the edge of the Whispering Woods with a rusted sword and 10 HP. What will you do?\n\n1. Venture down the dark, overgrown path.\n2. Examine the glowing mushrooms near the river.\n3. Climb the tall ancient oak to get a better view."
                )
            }
        }
    }
}

@Composable
fun PresetCard(
    title: String,
    desc: String,
    icon: ImageVector,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, GeoLightBorder),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = GeoTextPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    color = GeoTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Load Scenario",
                tint = GeoTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ContextInspectorPanel(
    session: ChatSessionEntity?,
    messages: List<ChatMessageEntity>,
    onUpdateSettings: (String, Float, Int) -> Unit,
    onClearMessages: () -> Unit
) {
    if (session == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = GeoTextMuted,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Active Session",
                color = GeoTextSecondary,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    var sysPrompt by remember(session.id) { mutableStateOf(session.systemInstruction) }
    var tempVal by remember(session.id) { mutableStateOf(session.temperature) }
    var maxTokens by remember(session.id) { mutableStateOf(session.maxOutputTokens) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Inspector Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = GeoPrimary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "Context Inspector",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GeoTextPrimary
            )
        }

        HorizontalDivider(color = GeoLightBorder)

        // System Instruction Editor
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "System Instruction (Persona Prompt):",
                style = MaterialTheme.typography.labelMedium,
                color = GeoPrimary,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = sysPrompt,
                onValueChange = {
                    sysPrompt = it
                    onUpdateSettings(it, tempVal, maxTokens)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .testTag("system_instruction_input"),
                textStyle = MaterialTheme.typography.bodySmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GeoPrimary,
                    unfocusedBorderColor = GeoLightBorder,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedTextColor = GeoTextPrimary,
                    unfocusedTextColor = GeoTextPrimary
                )
            )
        }

        // Sliders & settings
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Temperature Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Temperature (Randomness):",
                    style = MaterialTheme.typography.labelMedium,
                    color = GeoTextSecondary
                )
                Text(
                    text = String.format("%.2f", tempVal),
                    style = MaterialTheme.typography.labelMedium,
                    color = GeoPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = tempVal,
                onValueChange = {
                    tempVal = it
                    onUpdateSettings(sysPrompt, it, maxTokens)
                },
                valueRange = 0.0f..1.5f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("temperature_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = GeoPrimary,
                    activeTrackColor = GeoPrimary,
                    inactiveTrackColor = GeoLightBorder
                )
            )

            // Max Tokens Option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Max Output Tokens:",
                    style = MaterialTheme.typography.labelMedium,
                    color = GeoTextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(500, 1000, 1500).forEach { tokens ->
                        val isSelected = tokens == maxTokens
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) GeoPrimary else GeoLightBg)
                                .clickable {
                                    maxTokens = tokens
                                    onUpdateSettings(sysPrompt, tempVal, tokens)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tokens.toString(),
                                color = if (isSelected) Color.White else GeoTextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = GeoLightBorder)

        // History Stack inspector
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Context Payload Structure:",
                    style = MaterialTheme.typography.labelMedium,
                    color = GeoSecondary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onClearMessages,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Clear conversation context memory",
                        tint = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                        .border(width = 1.dp, color = GeoLightBorder, shape = RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Memory Stack is Empty",
                        color = GeoTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(width = 1.dp, color = GeoLightBorder, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (msg.role == "user") GeoPrimary.copy(alpha = 0.2f) else GeoSecondary.copy(
                                        alpha = 0.2f
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "role: \"${msg.role}\"",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (msg.role == "user") GeoPrimary else GeoSecondary
                                )
                                Text(
                                    text = "${msg.text.length} chars",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = GeoTextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.text,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                color = GeoTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
