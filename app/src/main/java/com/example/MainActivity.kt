package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Note
import com.example.ui.theme.*
import com.example.ui.viewmodel.NoteViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NotepadAppEntry()
            }
        }
    }
}

@Composable
fun NotepadAppEntry() {
    val viewModel: NoteViewModel = viewModel()
    
    // We wrap inside our beautiful Scaffold to support proper bottom & top bar Window insets
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Check if tablet or phone wide screen layout dynamically
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isTablet = maxWidth >= 600.dp
                DualPaneNotepadLayout(viewModel = viewModel, isTablet = isTablet)
            }
        }
    }
}

@Composable
fun DualPaneNotepadLayout(viewModel: NoteViewModel, isTablet: Boolean) {
    val activeNote by viewModel.activeNote.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val stats by viewModel.notesStats.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    
    var showSyncDialog by remember { mutableStateOf(false) }

    if (isTablet) {
        // Multi-pane Screen Layout for tablets and spacious foldables
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Column: Note List controls (40% width)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.4f)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(0.dp)
                    )
            ) {
                NoteListView(
                    notes = notes,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    stats = stats,
                    isSyncing = isSyncing,
                    syncStatus = syncStatus,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onCategorySelect = { viewModel.selectCategory(it) },
                    onNoteSelect = { viewModel.setActiveNote(it) },
                    onAddClick = { viewModel.createNewNote() },
                    onSyncClick = { showSyncDialog = true }
                )
            }

            // Right Column: Live note details editor (60% width)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.6f)
                    .background(Color(0xFFFFFDFA)) // Cream light clean sheet
            ) {
                if (activeNote != null) {
                    NoteEditorView(
                        note = activeNote!!,
                        onSave = { title, content, color, cat, isPinned ->
                            viewModel.saveActiveNote(title, content, color, cat, isPinned)
                        },
                        onDelete = { viewModel.deleteNoteById(it) },
                        onDismiss = { viewModel.setActiveNote(null) }
                    )
                } else {
                    // Placeholder when no active note is loaded
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Empty editor",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Note Selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select a sticky note on the left or tap '+' to jot down a new entry.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.widthIn(max = 280.dp),
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }
    } else {
        // Standard Compact Layout for typical single-screen phones
        if (activeNote != null) {
            // Show full screen Editor Mode
            NoteEditorView(
                note = activeNote!!,
                onSave = { title, content, color, cat, isPinned ->
                    viewModel.saveActiveNote(title, content, color, cat, isPinned)
                },
                onDelete = { viewModel.deleteNoteById(it) },
                onDismiss = { viewModel.setActiveNote(null) }
            )
        } else {
            // Show full screen Explorer list overview
            NoteListView(
                notes = notes,
                searchQuery = searchQuery,
                selectedCategory = selectedCategory,
                stats = stats,
                isSyncing = isSyncing,
                syncStatus = syncStatus,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onCategorySelect = { viewModel.selectCategory(it) },
                onNoteSelect = { viewModel.setActiveNote(it) },
                onAddClick = { viewModel.createNewNote() },
                onSyncClick = { showSyncDialog = true }
            )
        }
    }

    // Telemetry Sync Dialog Console
    if (showSyncDialog) {
        SyncTelemetryDialog(
            viewModel = viewModel,
            onDismiss = { showSyncDialog = false }
        )
    }
}

@Composable
fun NoteListView(
    notes: List<Note>,
    searchQuery: String,
    selectedCategory: String,
    stats: NoteViewModel.Stats,
    isSyncing: Boolean,
    syncStatus: String,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onNoteSelect: (Note) -> Unit,
    onAddClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    val categories = listOf("All", "Work", "Personal", "Ideas", "Recipes", "General")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App Title and Header with custom Red/Yellow Clean Minimalist aesthetics
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Amber Notes",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-0.5).sp
                    ),
                    color = CleanMinRed
                )
                Text(
                    text = "Clean Offline Storage System",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B) // Slate-500
                )
            }

            // Sync controller shortcut pill (HTML mock design: light rose pill with red border & syncing indicator)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SoftRoseBg)
                    .border(1.dp, SoftRoseBorder, RoundedCornerShape(50))
                    .clickable { onSyncClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("sync_shortcut_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = CleanMinRed,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SYNCING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanMinRed,
                        letterSpacing = 0.5.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Cloud Status Dashboard",
                        tint = CleanMinRed,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val anyUnsynced = notes.any { !it.isSynced }
                    Text(
                        text = if (anyUnsynced) "UNSYNCED" else "SYNCED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanMinRed,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Ambient Search Bar (styled with translucent amber [#FBC02D15] background and accent borders [#FBC02D40])
        TextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .border(1.dp, Color(0x66FBC02D), RoundedCornerShape(16.dp))
                .testTag("search_input"),
            placeholder = { Text("Search your thoughts...", color = Color(0x90B71C1C)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = CleanMinRed) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = CleanMinRed)
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0x15FBC02D),
                unfocusedContainerColor = Color(0x15FBC02D),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = Color(0xFF1E293B),
                unfocusedTextColor = Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Offline storage badge of Clean Minimalism theme!
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = CleanMinGold, shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "OFFLINE STORAGE ACTIVE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live stats items (placed in simple sleek rows with minimal styling)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(1.dp, SoftRoseBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(label = "Total", valStr = stats.total.toString(), color = CleanMinRed)
            VerticalDivider(modifier = Modifier.height(20.dp), color = SoftRoseBorder.copy(alpha = 0.5f))
            StatItem(label = "Yellow", valStr = stats.yellowNotes.toString(), color = CleanMinYellow)
            VerticalDivider(modifier = Modifier.height(20.dp), color = SoftRoseBorder.copy(alpha = 0.5f))
            StatItem(label = "Red", valStr = stats.redNotes.toString(), color = CleanMinRed)
            VerticalDivider(modifier = Modifier.height(20.dp), color = SoftRoseBorder.copy(alpha = 0.5f))
            StatItem(label = "Pinned", valStr = stats.pinnedNotes.toString(), color = CleanMinGold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Folder Chips matching theme design
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 6.dp)
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelect(category) },
                    label = { Text(category, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CleanMinRed,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = Color(0xFF1E293B)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) CleanMinRed else SoftRoseBorder.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main List Grid view of Sticky Notes!
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Empty Search UI",
                        tint = CleanMinRed.copy(alpha = 0.2f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No records found.",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "Write a new item using the sticky pen below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("notes_grid"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    StickyNoteCard(note = note, onClick = { onNoteSelect(note) })
                }
            }
        }

        // Add Floating Action Button in high-contrast styling (Bright yellow backdrop, blood red icon/text)
        Button(
            onClick = onAddClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 4.dp)
                .testTag("add_note_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = CleanMinYellow,
                contentColor = CleanMinRed
            ),
            shape = RoundedCornerShape(16.dp), // 16dp maps exactly to rounded-2xl
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "New note icon", modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "WRITE MEMO",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun StickyNoteCard(note: Note, onClick: () -> Unit) {
    // Determine the border stripe color based on Note category color index
    val stripeColor = remember(note.colorHex) {
        if (note.colorHex.contains("CDD2", ignoreCase = true) || note.colorHex.contains("21", ignoreCase = true)) {
            BorderRed
        } else if (note.colorHex.contains("F59D", ignoreCase = true) || note.colorHex.contains("15", ignoreCase = true)) {
            BorderYellow
        } else if (note.colorHex.contains("CC80", ignoreCase = true) || note.colorHex.contains("11", ignoreCase = true)) {
            BorderOrange
        } else {
            BorderGreen
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square sticky note ratio!
            .clickable(onClick = onClick)
            .testTag("note_card_${note.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(24.dp) // rounded-3xl equivalent
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Elegant left border style element from the design HTML (border-l-4)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(stripeColor)
            )

            // Note Content Details Box
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Category Tag (HTML styled uppercase)
                        Text(
                            text = note.category.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = stripeColor,
                            letterSpacing = 0.5.sp
                        )

                        // Pin star
                        if (note.isPinned) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Pinned item",
                                tint = CleanMinGold,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Title with overflow management
                    Text(
                        text = if (note.title.isBlank()) "Untitled Record" else note.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A), // Slate-900 (as specified in HTML font color "text-slate-900")
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Memo content paragraph
                    Text(
                        text = note.content,
                        fontSize = 12.sp,
                        color = Color(0xFF475569), // Slate-600 (as specified in HTML body paragraph "text-slate-600")
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )
                }

                // Sync status indicator in bottom right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                ) {
                    if (note.isSynced) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Synced in cloud",
                            tint = Color(0xFF94A3B8), // Slate-400 (just like original yesterday dates)
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CleanMinRed) // Red warning dot for unsynced local changes
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoteEditorView(
    note: Note,
    onSave: (String, String, String, String, Boolean) -> Unit,
    onDelete: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
    var colorHex by remember { mutableStateOf(note.colorHex) }
    var category by remember { mutableStateOf(note.category) }
    var isPinned by remember { mutableStateOf(note.isPinned) }

    val categories = listOf("General", "Personal", "Work", "Ideas", "Recipes")

    // Dynamic light mode color selectors paired to Light or Dark modes
    val colorOptions = listOf(
        Pair("#FFF59D", "Yellow Sticker"),
        Pair("#FFFFCDD2", "Red Sticker"),
        Pair("#FFFFCC80", "Orange Sticker"),
        Pair("#FFE8F5E9", "Sage Cream Sticker")
    )

    // Handle android physical back press to safely return to dashboard
    BackHandler(onBack = { onDismiss() })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CleanMinBackground) // Clean minimalism theme background
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Navigation app row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Return to dashboard")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pin button toggleable
                IconButton(
                    onClick = { isPinned = !isPinned },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Pin toggles",
                        tint = if (isPinned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                    )
                }

                // Delete option for pre-existing items
                if (note.id != 0) {
                    IconButton(
                        onClick = { onDelete(note.id) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_note_button"),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete this Note")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sticky color select interface (Gradients and customized borders)
        Text(
            text = "STICKY COLOR:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            colorOptions.forEach { pair ->
                val optionColor = Color(android.graphics.Color.parseColor(pair.first))
                val isSelected = colorHex.equals(pair.first, ignoreCase = true)

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(optionColor)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { colorHex = pair.first }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected color",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Folders Selection Category Column
        Text(
            text = "CATEGORY FOLDER:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val selected = category == cat
                InputChip(
                    selected = selected,
                    onClick = { category = cat },
                    label = { Text(cat) },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Interactive legal paper divider styling line!
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Clean Modern Fields for Title input
        TextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("note_title_input"),
            placeholder = { Text("Topic Title", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Clean Body text fields
        TextField(
            value = content,
            onValueChange = { content = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 200.dp)
                .testTag("note_content_input"),
            placeholder = { Text("Jot down notes, plans, lists...") },
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Save layout buttons
        Button(
            onClick = { onSave(title, content, colorHex, category, isPinned) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("save_note_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = CleanMinYellow,
                contentColor = CleanMinRed
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = title.isNotBlank() || content.isNotBlank()
        ) {
            Icon(Icons.Default.Check, contentDescription = "Save action")
            Spacer(modifier = Modifier.width(8.dp))
            Text("SAVE MEMO", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun StatItem(label: String, valStr: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = valStr,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = color
        )
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SyncTelemetryDialog(
    viewModel: NoteViewModel,
    onDismiss: () -> Unit
) {
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val rawLogs by viewModel.syncLogs.collectAsStateWithLifecycle()
    
    var editedUrl by remember { mutableStateOf(serverUrl) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Dialog Title Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Panel Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cloud Sync Panel",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close settings")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Connection Configuration Url input
                Text(
                    text = "TARGET HTTP ENDPOINT BASE URL:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(6.dp))

                TextField(
                    value = editedUrl,
                    onValueChange = { editedUrl = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("server_url_input"),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    placeholder = { Text("https://your-server-domain.com") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            viewModel.setServerUrl(editedUrl)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("APPLY SERVER URL", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Console Header Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TERMINAL SYNCHRONIZER METRICS:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    TextButton(
                        onClick = { viewModel.clearLogConsole() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    ) {
                        Text("CLEAR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // The Retro/Cyber Terminal Monitor Log Output!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF131313)) // Deep computer terminal black
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    val terminalState = rememberLazyListState()
                    
                    // Auto-scroll terminal log to bottom on addition!
                    LaunchedEffect(rawLogs.size) {
                        if (rawLogs.isNotEmpty()) {
                            terminalState.animateScrollToItem(rawLogs.size - 1)
                        }
                    }

                    LazyColumn(
                        state = terminalState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(rawLogs) { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                ),
                                color = if (line.contains("SUCCESS") || line.contains("OK")) Color(0xFF4CAF50) // Emerald green success logs
                                        else if (line.contains("WARNING") || line.contains("INITIATING") || line.contains("Handshake") || line.contains("POST")) Color(0xFFFFB300) // Sand yellow handshake headers
                                        else if (line.contains("Error") || line.contains("FAILURE") || line.contains("TIMEOUT")) Color(0xFFEF5350) // Coral red warning logs
                                        else Color(0xFFE0E0E0) // Calm slate text for general logs
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sync button footer actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "STATUS NODE:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Text(
                            text = syncStatus.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (syncStatus.contains("success") || syncStatus.contains("ready") || syncStatus.contains("Synced")) Color(0xFF388E3C) else MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = { viewModel.forceWebSync() },
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Action button sync", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PUSH SYNC", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
