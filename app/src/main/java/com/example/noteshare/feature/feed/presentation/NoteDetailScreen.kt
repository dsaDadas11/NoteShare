package com.example.noteshare.feature.feed.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.noteshare.core.common.formatDateTime
import com.example.noteshare.core.network.resolveMediaUrl
import com.example.noteshare.feature.feed.domain.model.CommentResponse
import com.example.noteshare.shared.ui.AvatarImage
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.runtime.DisposableEffect

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (Long) -> Unit,
    onDeleteSuccess: () -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var commentText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onDeleteSuccess()
        }
    }

    LaunchedEffect(uiState.commentSendSuccess) {
        if (uiState.commentSendSuccess) {
            commentText = ""
            viewModel.commentSendSuccessConsumed()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.errorShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.noteDetail?.title ?: "笔记详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val note = uiState.noteDetail
                    if (note?.isAuthorSelf == true) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !uiState.isDeleting
                        ) {
                            if (uiState.isDeleting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Note")
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            // 底部评论输入栏
            CommentInputBar(
                replyTarget = uiState.replyTarget,
                commentText = commentText,
                onCommentTextChange = { commentText = it },
                onSend = {
                    viewModel.sendComment(commentText)
                },
                onCancelReply = { viewModel.clearReplyTarget() },
                isSending = uiState.isSendingComment
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.noteDetail != null) {
            val note = uiState.noteDetail!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Media: Video or Images
                if (note.videoUrl != null) {
                    item {
                        VideoPlayer(
                            videoUrl = resolveMediaUrl(note.videoUrl) ?: "",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .background(Color.Black)
                        )
                    }
                } else if (note.images.isNotEmpty()) {
                    item {
                        val pagerState = rememberPagerState(pageCount = { note.images.size })
                        Box(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                AsyncImage(
                                    model = resolveMediaUrl(note.images[page].url),
                                    contentDescription = "Image $page",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(note.images.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                                    Box(
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .size(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Author Info
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToProfile(note.author.id) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarImage(
                            model = resolveMediaUrl(note.author.avatarUrl)
                                ?: "https://api.dicebear.com/7.x/avataaars/png?seed=${note.author.username}",
                            contentDescription = "Author Avatar",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = note.author.nickname ?: note.author.username,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = formatDateTime(note.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!note.isAuthorSelf) {
                            Button(
                                onClick = { viewModel.toggleAuthorFollow() },
                                enabled = !uiState.isAuthorFollowLoading
                            ) {
                                if (uiState.isAuthorFollowLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(if (note.isAuthorFollowed) "已关注" else "关注")
                                }
                            }
                        }
                    }
                }

                // Content
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Interactions
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.toggleLike() }) {
                            Icon(
                                imageVector = if (note.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (note.isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${note.likeCount}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(24.dp))
                        Text(
                            text = "评论 ${note.commentCount}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    HorizontalDivider()
                }

                // Comments Section Header
                item {
                    Text(
                        text = "全部评论",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Comments List
                items(uiState.comments, key = { it.id }) { comment ->
                    CommentItem(
                        comment = comment,
                        onReply = { viewModel.setReplyTarget(comment) },
                        onLike = { viewModel.toggleCommentLike(comment.id) },
                        onDelete = { viewModel.deleteComment(comment.id) },
                        onLongPress = { viewModel.showCommentMenu(comment) },
                        onNavigateToProfile = onNavigateToProfile,
                        deletingCommentId = uiState.deletingCommentId,
                        // 回复相关
                        expandedReplies = uiState.expandedReplies[comment.id],
                        isExpanded = uiState.expandedReplies.containsKey(comment.id),
                        isLoadingReplies = uiState.loadingReplies == comment.id,
                        onExpandReplies = { viewModel.expandReplies(comment.id) },
                        onReplyLike = { replyId -> viewModel.toggleCommentLike(replyId) },
                        onReplyLongPress = { reply -> viewModel.showCommentMenu(reply) },
                        onReplyReply = { reply -> viewModel.setReplyTarget(reply) },
                        onReplyDelete = { replyId -> viewModel.deleteComment(replyId) }
                    )
                }

                if (uiState.commentsLoading) {
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

                // 底部留白，避免被 bottomBar 遮挡
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // 删除笔记确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除笔记") },
            text = { Text("删除后无法恢复，确定要删除这篇笔记吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteNote()
                    },
                    enabled = !uiState.isDeleting
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !uiState.isDeleting
                ) {
                    Text("取消")
                }
            }
        )
    }

    // 评论长按菜单（BottomSheet）
    uiState.longPressedComment?.let { comment ->
        CommentContextMenu(
            comment = comment,
            onDismiss = { viewModel.dismissCommentMenu() },
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("comment", comment.content))
                viewModel.dismissCommentMenu()
            },
            onReply = {
                viewModel.setReplyTarget(comment)
                viewModel.dismissCommentMenu()
            },
            onDelete = {
                viewModel.deleteComment(comment.id)
            }
        )
    }
}

// ==================== 视频播放器 ====================

@Composable
fun VideoPlayer(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = modifier
    )
}

// ==================== 评论输入栏 ====================

@Composable
private fun CommentInputBar(
    replyTarget: ReplyTarget?,
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancelReply: () -> Unit,
    isSending: Boolean
) {
    Surface(tonalElevation = 3.dp) {
        Column {
            // 回复提示条
            if (replyTarget != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "回复 ${replyTarget.authorName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onCancelReply,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "取消回复",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            // 输入框
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = onCommentTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(if (replyTarget != null) "回复 ${replyTarget.authorName}..." else "说点什么...")
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSend,
                    enabled = commentText.isNotBlank() && !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "发送评论")
                    }
                }
            }
        }
    }
}

// ==================== 单条评论（支持楼中楼） ====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommentItem(
    comment: CommentResponse,
    onReply: () -> Unit,
    onLike: () -> Unit,
    onDelete: () -> Unit,
    onLongPress: () -> Unit,
    onNavigateToProfile: (Long) -> Unit,
    deletingCommentId: Long?,
    // 回复相关
    expandedReplies: List<CommentResponse>?,
    isExpanded: Boolean,
    isLoadingReplies: Boolean,
    onExpandReplies: () -> Unit,
    onReplyLike: (Long) -> Unit,
    onReplyLongPress: (CommentResponse) -> Unit,
    onReplyReply: (CommentResponse) -> Unit,
    onReplyDelete: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // 评论主体
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onReply,
                    onLongClick = onLongPress
                )
                .padding(vertical = 4.dp)
        ) {
            AvatarImage(
                model = resolveMediaUrl(comment.user.avatarUrl)
                    ?: "https://api.dicebear.com/7.x/avataaars/png?seed=${comment.user.username}",
                contentDescription = "Commenter Avatar",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onNavigateToProfile(comment.user.id) }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.user.nickname ?: comment.user.username,
                        modifier = Modifier
                            .clickable { onNavigateToProfile(comment.user.id) },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (comment.parentId != null) {
                        // 显示回复标记
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp).padding(horizontal = 2.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // 删除按钮
                    if (comment.isMine) {
                        IconButton(
                            onClick = onDelete,
                            enabled = deletingCommentId == null,
                            modifier = Modifier.size(32.dp)
                        ) {
                            if (deletingCommentId == comment.id) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "删除评论",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDateTime(comment.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    // 点赞按钮
                    IconButton(
                        onClick = onLike,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (comment.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "点赞",
                            modifier = Modifier.size(16.dp),
                            tint = if (comment.liked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (comment.likeCount > 0) {
                        Text(
                            text = "${comment.likeCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    // 回复按钮
                    IconButton(
                        onClick = onReply,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "回复",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 预览回复（最多显示3条）
        if (comment.replies.isNotEmpty() && !isExpanded) {
            Column(
                modifier = Modifier
                    .padding(start = 48.dp, top = 2.dp, bottom = 2.dp)
            ) {
                comment.replies.forEach { reply ->
                    ReplyPreviewItem(
                        reply = reply,
                        replyToAuthorFallback = comment.user.nickname ?: comment.user.username,
                        onLongPress = { onReplyLongPress(reply) },
                        onReply = { onReplyReply(reply) },
                        onLike = { onReplyLike(reply.id) },
                        onDelete = { onReplyDelete(reply.id) }
                    )
                }
                // 展开更多回复
                if (comment.replyCount > 3) {
                    TextButton(
                        onClick = onExpandReplies,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "展开全部 ${comment.replyCount} 条回复",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 展开的全部回复
        if (isExpanded && expandedReplies != null) {
            Column(
                modifier = Modifier.padding(start = 48.dp, top = 2.dp)
            ) {
                expandedReplies.forEach { reply ->
                    ReplyPreviewItem(
                        reply = reply,
                        replyToAuthorFallback = comment.user.nickname ?: comment.user.username,
                        onLongPress = { onReplyLongPress(reply) },
                        onReply = { onReplyReply(reply) },
                        onLike = { onReplyLike(reply.id) },
                        onDelete = { onReplyDelete(reply.id) }
                    )
                }
                // 收起回复
                if (expandedReplies.isNotEmpty()) {
                    TextButton(
                        onClick = onExpandReplies,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.ExpandLess,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "收起回复",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 加载回复中
        if (isLoadingReplies) {
            Box(
                modifier = Modifier
                    .padding(start = 48.dp, top = 4.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }

        // 无展开回复时的"展开回复"入口（只有预览也没有时才显示）
        if (comment.replies.isEmpty() && comment.replyCount > 0 && !isExpanded) {
            TextButton(
                onClick = onExpandReplies,
                modifier = Modifier.padding(start = 48.dp),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
            ) {
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "展开 ${comment.replyCount} 条回复",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

// ==================== 回复预览项（楼中楼内） ====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReplyPreviewItem(
    reply: CommentResponse,
    replyToAuthorFallback: String?,
    onLongPress: () -> Unit,
    onReply: () -> Unit,
    onLike: () -> Unit,
    onDelete: () -> Unit
) {
    val replyToAuthor = reply.replyToAuthor?.takeIf { it.isNotBlank() }
        ?: replyToAuthorFallback?.takeIf { it.isNotBlank() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onReply,
                onLongClick = onLongPress
            )
            .padding(vertical = 4.dp)
    ) {
        AvatarImage(
            model = resolveMediaUrl(reply.user.avatarUrl)
                ?: "https://api.dicebear.com/7.x/avataaars/png?seed=${reply.user.username}",
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = reply.user.nickname ?: reply.user.username,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatDateTime(reply.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildAnnotatedString {
                    if (replyToAuthor != null) {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append("回复")
                        }
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append(replyToAuthor)
                        }
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append("：")
                        }
                    }
                    append(reply.content)
                },
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 点赞
                IconButton(
                    onClick = onLike,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (reply.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "点赞",
                        modifier = Modifier.size(14.dp),
                        tint = if (reply.liked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (reply.likeCount > 0) {
                    Text(
                        text = "${reply.likeCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                // 回复
                IconButton(
                    onClick = onReply,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        contentDescription = "回复",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // 删除按钮（自己的回复）
        if (reply.isMine) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==================== 评论长按菜单 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentContextMenu(
    comment: CommentResponse,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            // 复制评论
            ListItem(
                headlineContent = { Text("复制评论内容") },
                leadingContent = {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                },
                modifier = Modifier.clickable { onCopy() }
            )
            // 回复评论
            ListItem(
                headlineContent = { Text("回复") },
                leadingContent = {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null)
                },
                modifier = Modifier.clickable { onReply() }
            )
            // 删除（仅自己的评论）
            if (comment.isMine) {
                ListItem(
                    headlineContent = {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable { onDelete() }
                )
            }
        }
    }
}
