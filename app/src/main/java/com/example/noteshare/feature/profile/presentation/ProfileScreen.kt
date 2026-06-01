package com.example.noteshare.feature.profile.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.noteshare.core.network.resolveMediaUrl
import com.example.noteshare.shared.ui.AvatarImage
import com.example.noteshare.shared.ui.NoteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.errorShown()
        }
    }

    // Load more
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            lastVisibleItemIndex > (totalItemsNumber - 3)
        }.collect { shouldLoadMore ->
            if (shouldLoadMore) {
                viewModel.loadNotes(isRefresh = false)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isMyProfile) "我的资料" else "他人资料") },
                navigationIcon = {
                    if (!uiState.isMyProfile) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    if (uiState.isMyProfile) {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "刷新")
                        }
                        TextButton(onClick = onLogout) {
                            Text("退出")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.profile != null) {
                val profile = uiState.profile!!
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AvatarImage(
                                model = resolveMediaUrl(profile.avatarUrl)
                                    ?: "https://api.dicebear.com/7.x/avataaars/png?seed=${profile.username}",
                                contentDescription = "头像",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = profile.nickname ?: profile.username,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "@${profile.username}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (!profile.bio.isNullOrBlank()) {
                                Text(
                                    text = profile.bio,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "${profile.followingCount}", fontWeight = FontWeight.Bold)
                                    Text(text = "关注", style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.width(32.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "${profile.followerCount}", fontWeight = FontWeight.Bold)
                                    Text(text = "粉丝", style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.width(32.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "${profile.noteCount}", fontWeight = FontWeight.Bold)
                                    Text(text = "笔记", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            if (uiState.isMyProfile) {
                                OutlinedButton(onClick = onNavigateToEditProfile) {
                                    Text("编辑资料")
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.toggleFollow() },
                                    enabled = !uiState.isFollowLoading
                                ) {
                                    if (uiState.isFollowLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(if (profile.isFollowing == true) "已关注" else "关注")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                        }
                    }

                    // Notes
                    if (uiState.notes.isEmpty() && !uiState.notesLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("暂无笔记", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(uiState.notes) { note ->
                            NoteCard(
                                title = note.title,
                                content = note.content,
                                authorName = note.author.nickname ?: note.author.username,
                                authorAvatarUrl = note.author.avatarUrl,
                                imageUrl = note.images.firstOrNull()?.url,
                                likeCount = note.likeCount,
                                commentCount = note.commentCount,
                                onClick = { onNavigateToDetail(note.id) }
                            )
                        }

                        if (uiState.notesLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
