package com.example.activitytron.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.activitytron.data.local.ActivityItem
import com.example.activitytron.ui.viewmodel.ActivityViewModel

import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActivityListScreen(
    viewModel: ActivityViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activities by viewModel.activitiesState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showRandomDialog by remember { mutableStateOf(false) }
    var showLibraryDialog by remember { mutableStateOf(false) }
    var randomActivity by remember { mutableStateOf<ActivityItem?>(null) }
    var detailActivity by remember { mutableStateOf<ActivityItem?>(null) }
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    
    // Expanded states for categories
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }
    var completedExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Activitytron",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onThemeToggle) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = if (isDarkTheme) Color(0xFFFFD700) else Color(0xFF757575)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showLibraryDialog = true }) {
                            Icon(
                                Icons.Default.AutoAwesomeMotion,
                                contentDescription = "Quest Library",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(onClick = {
                            if (activities.isNotEmpty()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                randomActivity = activities.random()
                                showRandomDialog = true
                            }
                        }) {
                            Icon(
                                Icons.Default.Casino,
                                contentDescription = "Pick Random",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search activities...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAddDialog = true 
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Activity") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = activities.isEmpty(),
                label = "ListVisibility"
            ) { isEmpty ->
                if (isEmpty) {
                    if (searchQuery.isEmpty()) {
                        EmptyState(onOpenLibrary = { showLibraryDialog = true })
                    } else {
                        NoSearchResultsState()
                    }
                } else {
                    val pendingActivities = remember(activities) { activities.filter { !it.isDone } }
                    val completedActivities = remember(activities) { activities.filter { it.isDone } }
                    
                    val pendingByCategory = remember(pendingActivities) {
                        pendingActivities.groupBy { it.category }.toSortedMap()
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (pendingActivities.isNotEmpty()) {
                                pendingByCategory.forEach { (category, categoryActivities) ->
                                    val isExpanded = expandedCategories.getOrDefault(category, true)
                                    
                                    item(key = "header_$category") {
                                        SectionHeader(
                                            title = category,
                                            isExpanded = isExpanded,
                                            onToggle = { expandedCategories[category] = !isExpanded },
                                            count = categoryActivities.size,
                                            modifier = Modifier.padding(top = if (pendingByCategory.firstKey() == category) 0.dp else 8.dp)
                                        )
                                    }
                                    
                                    if (isExpanded) {
                                        items(
                                            items = categoryActivities,
                                            key = { it.id }
                                        ) { activity ->
                                            ActivityListItem(
                                                activity = activity,
                                                onToggleDone = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.toggleActivityDone(activity)
                                                },
                                                onDelete = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.deleteActivity(activity)
                                                },
                                                onClick = { detailActivity = activity },
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }
                                }
                            }

                            if (completedActivities.isNotEmpty()) {
                                item(key = "completed_header") {
                                    SectionHeader(
                                        title = "Completed",
                                        isExpanded = completedExpanded,
                                        onToggle = { completedExpanded = !completedExpanded },
                                        count = completedActivities.size,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }
                                if (completedExpanded) {
                                    items(
                                        items = completedActivities,
                                        key = { it.id }
                                    ) { activity ->
                                        ActivityListItem(
                                            activity = activity,
                                            onToggleDone = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.toggleActivityDone(activity)
                                            },
                                            onDelete = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.deleteActivity(activity)
                                            },
                                            onClick = { detailActivity = activity },
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(80.dp)) // FAB space
                            }
                        }

                        InteractiveScrollbar(
                            state = listState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(vertical = 16.dp)
                        )
                    }
                }

            }
        }
    }

    if (showAddDialog) {
        AddActivityDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, category ->
                viewModel.addActivity(title, desc, category = category)
                showAddDialog = false
            }
        )
    }

    if (showLibraryDialog) {
        val availableQuests = remember(activities, viewModel.preMadeQuests) {
            val currentQuests = activities.map { it.title + it.description }.toSet()
            viewModel.preMadeQuests.filter { (it.title + it.description) !in currentQuests }
        }

        LibraryDialog(
            quests = availableQuests,
            onDismiss = { showLibraryDialog = false },
            onAddQuest = { viewModel.addPreMadeQuest(it) },
            onAddAll = { 
                viewModel.addAllPreMadeQuests(availableQuests)
                showLibraryDialog = false
            }
        )
    }

    if (showRandomDialog && randomActivity != null) {
        RandomSelectionDialog(
            activity = randomActivity!!,
            onDismiss = { showRandomDialog = false },
            onToggleDone = {
                viewModel.toggleActivityDone(randomActivity!!)
                showRandomDialog = false
            }
        )
    }

    if (detailActivity != null) {
        QuestDetailDialog(
            activity = detailActivity!!,
            onDismiss = { detailActivity = null },
            onToggleDone = {
                viewModel.toggleActivityDone(detailActivity!!)
                detailActivity = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SectionHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Badge(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(count.toString())
        }
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun InteractiveScrollbar(
    state: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    val layoutInfo by remember { derivedStateOf { state.layoutInfo } }
    val totalItemsCount by remember { derivedStateOf { layoutInfo.totalItemsCount } }
    
    if (totalItemsCount == 0) return

    val visibleItemsCount by remember { derivedStateOf { layoutInfo.visibleItemsInfo.size } }
    
    val scrollbarHeightFraction by remember { 
        derivedStateOf { 
            if (totalItemsCount > 0) {
                (visibleItemsCount.toFloat() / totalItemsCount).coerceIn(0.1f, 1f)
            } else 1f
        }
    }
    
    val scrollbarOffsetFraction by remember { 
        derivedStateOf { 
            if (totalItemsCount > visibleItemsCount) {
                val firstItemOffset = state.firstVisibleItemScrollOffset.toFloat()
                val firstItemHeight = state.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 1f
                (state.firstVisibleItemIndex + firstItemOffset / firstItemHeight) / totalItemsCount
            } else 0f
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .width(32.dp) // Wider touch area
            .pointerInput(totalItemsCount, visibleItemsCount) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, dragAmount ->
                    change.consume()
                    val maxHeight = size.height.toFloat()
                    val scrollbarHeight = maxHeight * scrollbarHeightFraction
                    val deltaFraction = dragAmount.y / (maxHeight - scrollbarHeight)
                    val totalScrollableHeight = state.layoutInfo.viewportSize.height * (totalItemsCount.toFloat() / visibleItemsCount.coerceAtLeast(1))
                    val scrollDelta = deltaFraction * totalScrollableHeight
                    
                    coroutineScope.launch {
                        state.scrollBy(scrollDelta)
                    }
                }
            }
    ) {
        val maxHeight = constraints.maxHeight.toFloat()
        val scrollbarHeight = maxHeight * scrollbarHeightFraction
        val scrollbarOffset = (maxHeight - scrollbarHeight) * scrollbarOffsetFraction

        Box(
            modifier = Modifier
                .offset(y = with(density) { scrollbarOffset.toDp() })
                .padding(end = 4.dp)
                .width(if (isDragging) 8.dp else 4.dp) // Widens when dragging
                .height(with(density) { scrollbarHeight.toDp() })
                .align(Alignment.TopEnd)
                .background(
                    color = if (isDragging) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                )
        )
    }
}

@Composable
fun NoSearchResultsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No results found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "We couldn't find any quests matching your search. Try different keywords!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyState(onOpenLibrary: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Explore,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Adventure awaits!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Start by adding your own activity or browse our quest library.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onOpenLibrary,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.AutoAwesomeMotion, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Quest Library")
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryDialog(
    quests: List<ActivityItem>,
    onDismiss: () -> Unit,
    onAddQuest: (ActivityItem) -> Unit,
    onAddAll: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember(quests) { 
        listOf("All") + quests.map { it.category }.distinct().sorted() 
    }
    
    val filteredQuests = remember(selectedCategory, quests) {
        if (selectedCategory == "All") quests else quests.filter { it.category == selectedCategory }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quest Library")
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 450.dp)) {
                ScrollableTabRow(
                    selectedTabIndex = categories.indexOf(selectedCategory),
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {}
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AnimatedContent(
                    targetState = filteredQuests,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "LibraryListAnimation",
                    modifier = Modifier.weight(1f)
                ) { filteredList ->
                    if (filteredList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "All quests in this category added!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(filteredList, key = { it.title + it.category }) { quest ->
                                OutlinedCard(
                                    onClick = { onAddQuest(quest) },
                                    modifier = Modifier.fillMaxWidth().animateItem(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(quest.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                            if (quest.description.isNotBlank()) {
                                                Text(quest.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(
                                                quest.category, 
                                                style = MaterialTheme.typography.labelSmall, 
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                        Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAddAll,
                enabled = quests.isNotEmpty()
            ) {
                Text("Add All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActivityListItem(
    activity: ActivityItem,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    else -> Color.Transparent
                }, label = "SwipeBackground"
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        enableDismissFromStartToEnd = false,
        modifier = modifier
    ) {
        ActivityRow(
            activity = activity,
            onToggleDone = onToggleDone,
            onDelete = onDelete,
            onClick = onClick
        )
    }
}

@Composable
fun ActivityRow(
    activity: ActivityItem,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scale by animateFloatAsState(
        targetValue = if (activity.isDone) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ScaleAnimation"
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .animateContentSize()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (activity.isDone) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleDone,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (activity.isDone) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
            ) {
                Icon(
                    imageVector = if (activity.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle Done",
                    tint = if (activity.isDone) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (activity.isDone) TextDecoration.LineThrough else null,
                        color = if (activity.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (activity.isCustom) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "CUSTOM",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                if (activity.description.isNotBlank()) {
                    Text(
                        text = activity.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            IconButton(onClick = {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Hey! Let's do this activity: ${activity.title}${if(activity.description.isNotBlank()) " - ${activity.description}" else ""}")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            }) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline, 
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun QuestDetailDialog(
    activity: ActivityItem,
    onDismiss: () -> Unit,
    onToggleDone: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onToggleDone,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    if (activity.isDone) Icons.AutoMirrored.Filled.Undo else Icons.Default.Check, 
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (activity.isDone) "Mark as Not Done" else "Complete Quest!")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (activity.isCustom) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "CUSTOM QUEST",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    activity.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (activity.description.isNotBlank()) {
                    Column {
                        Text(
                            "Mission Description",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            activity.description,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lightbulb, 
                                contentDescription = null, 
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Completion Tip",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            getCompletionTip(activity.category),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

fun getCompletionTip(category: String): String {
    return when (category) {
        "Physical" -> "Stay hydrated and remember to stretch before and after high-energy quests!"
        "Food & Cooking" -> "Don't be afraid to experiment. The best flavors often come from the weirdest combos!"
        "Funny" -> "The goal is to laugh and make others smile. Keep it harmless and have fun!"
        "Adventures" -> "Keep your phone charged and stay aware of your surroundings while exploring."
        "Creative" -> "Progress over perfection. The act of creating is more important than the final result."
        "Gaming" -> "Take breaks between sessions and remember: it's just a game (until it's a speedrun)!"
        "Mind Games" -> "Focus is key. Try to minimize distractions for the best mental workout."
        "Chill" -> "Slow down and breathe. This is your time to recharge and find some peace."
        else -> "Every small step counts towards finishing the quest. You've got this!"
    }
}
@Composable
fun RandomSelectionDialog(
    activity: ActivityItem,
    onDismiss: () -> Unit,
    onToggleDone: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onToggleDone,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Done")
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Maybe later")
                }
            }
        },
        icon = {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "Your Pick!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                if (activity.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = activity.description,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Custom") }
    val categories = ActivityViewModel.CATEGORIES

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Adventure") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What's the plan?") },
                    placeholder = { Text("e.g. Picnic at the park") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
                
                Text("Category", style = MaterialTheme.typography.labelLarge)
                ScrollableTabRow(
                    selectedTabIndex = categories.indexOf(category),
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {}
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title, description, category) },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}
