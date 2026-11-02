package com.junkfood.seal.ui.page.hackertube

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

private const val PAGE_SIZE_HINT = 200

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HackerTubePage(onNavigateBack: () -> Unit, onVideoClick: (HackerTubeVideo) -> Unit) {
    val context = LocalContext.current
    var dataset by remember { mutableStateOf<HackerTubeDataset?>(null) }
    var failed by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedArea by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var hideNews by rememberSaveable { mutableStateOf(true) }
    var recommendedOnly by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        runCatching { HackerTubeRepository.load(context) }
            .onSuccess { dataset = it }
            .onFailure {
                Log.e("HackerTube", "Failed to load dataset", it)
                failed = true
                errorText = it.toString()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HackerTube") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                    }
                },
            )
        }
    ) { padding ->
        val data = dataset
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                failed ->
                    Text(
                        "Could not load the HackerTube data.\n$errorText",
                        modifier = Modifier.align(Alignment.Center),
                    )
                data == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> {
                    val areas =
                        remember(data) { data.categories.map { it.area to it.areaName }.distinct() }
                    val categoriesInArea =
                        remember(data, selectedArea) {
                            data.categories.filter { it.area == selectedArea }
                        }
                    val results =
                        remember(
                            data,
                            query,
                            selectedArea,
                            selectedCategory,
                            hideNews,
                            recommendedOnly,
                        ) {
                            val areaOfCategory = data.categories.associate { it.id to it.area }
                            val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
                            data.videos.filter { v ->
                                (!hideNews || v.shelfLife != "news") &&
                                    (!recommendedOnly || v.recommenders > 0) &&
                                    (selectedCategory == null || selectedCategory in v.categories) &&
                                    (selectedArea == null ||
                                        v.categories.any { areaOfCategory[it] == selectedArea }) &&
                                    terms.all { t ->
                                        v.title.contains(t, true) ||
                                            v.channel.contains(t, true) ||
                                            v.primaryTopic.contains(t, true) ||
                                            v.summary.contains(t, true)
                                    }
                            }
                        }

                    LazyColumn(
                        state = rememberLazyListState(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        item {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                singleLine = true,
                                placeholder = { Text("Search ${data.videos.size} videos") },
                                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                                trailingIcon = {
                                    if (query.isNotEmpty()) {
                                        IconButton(onClick = { query = "" }) {
                                            Icon(Icons.Outlined.Close, null)
                                        }
                                    }
                                },
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedArea == null,
                                        onClick = {
                                            selectedArea = null
                                            selectedCategory = null
                                        },
                                        label = { Text("All") },
                                    )
                                }
                                items(areas) { (id, name) ->
                                    FilterChip(
                                        selected = selectedArea == id,
                                        onClick = {
                                            selectedArea = id
                                            selectedCategory = null
                                        },
                                        label = { Text(name) },
                                    )
                                }
                            }
                        }
                        if (categoriesInArea.isNotEmpty()) {
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(categoriesInArea) { c ->
                                        FilterChip(
                                            selected = selectedCategory == c.id,
                                            onClick = {
                                                selectedCategory =
                                                    if (selectedCategory == c.id) null else c.id
                                            },
                                            label = { Text(c.name) },
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FilterChip(
                                    selected = hideNews,
                                    onClick = { hideNews = !hideNews },
                                    label = { Text("Hide news") },
                                )
                                FilterChip(
                                    selected = recommendedOnly,
                                    onClick = { recommendedOnly = !recommendedOnly },
                                    label = { Text("Recommended only") },
                                )
                            }
                        }
                        item {
                            Text(
                                "${results.size} videos",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                        items(results.take(PAGE_SIZE_HINT * 5), key = { it.id }) { video ->
                            VideoRow(video, onClick = { onVideoClick(video) })
                        }
                        item {
                            Text(
                                "Data from HackerTube (hackertube.org) and the open-index/hacker-news " +
                                    "dataset, under ODC-By 1.0. Video titles and channel names belong " +
                                    "to their creators.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoRow(video: HackerTubeVideo, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier.width(128.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)),
        )
        Column(Modifier.weight(1f)) {
            Text(
                video.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                video.channel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.padding(top = 2.dp))
            Text(
                video.whyHn,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${video.recommenders} recommenders, ${video.firstYear}-${video.lastYear}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
