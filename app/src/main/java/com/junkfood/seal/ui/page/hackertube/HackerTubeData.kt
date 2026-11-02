package com.junkfood.seal.ui.page.hackertube

import android.content.Context
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Data from HackerTube (https://hackertube.org), licensed under ODC-By 1.0. Built from Hacker News
 * comments in the open-index/hacker-news dataset. See scripts/build_hackertube_asset.py.
 */
@Serializable
data class HackerTubeCategory(
    val id: String,
    val name: String,
    val area: String,
    val areaName: String,
)

@Serializable
data class HackerTubeVideo(
    val id: String,
    @SerialName("t") val title: String,
    @SerialName("ch") val channel: String,
    @SerialName("s") val summary: String,
    @SerialName("w") val whyHn: String,
    @SerialName("ct") val contentType: String,
    @SerialName("sl") val shelfLife: String,
    @SerialName("tp") val primaryTopic: String,
    @SerialName("c") val categories: List<String>,
    @SerialName("r") val recommenders: Int,
    @SerialName("y0") val firstYear: Int,
    @SerialName("y1") val lastYear: Int,
    @SerialName("sc") val score: Double,
) {
    val url: String
        get() = "https://www.youtube.com/watch?v=$id"

    val thumbnailUrl: String
        get() = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
}

@Serializable
data class HackerTubeDataset(
    val categories: List<HackerTubeCategory>,
    val videos: List<HackerTubeVideo>,
)

object HackerTubeRepository {
    private const val ASSET_PATH = "hackertube/hackertube.json.gz"
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var cached: HackerTubeDataset? = null

    suspend fun load(context: Context): HackerTubeDataset =
        cached
            ?: withContext(Dispatchers.IO) {
                    context.applicationContext.assets.open(ASSET_PATH).use { raw ->
                        GZIPInputStream(raw).bufferedReader(Charsets.UTF_8).use {
                            json.decodeFromString<HackerTubeDataset>(it.readText())
                        }
                    }
                }
                .also { cached = it }
}
