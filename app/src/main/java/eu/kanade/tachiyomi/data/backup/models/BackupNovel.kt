package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.backup.model.NovelBackupRecord
import tachiyomi.domain.backup.model.NovelBackupStatistic

@Serializable
data class BackupNovel(
    @ProtoNumber(1) override val id: String,
    @ProtoNumber(2) val title: String,
    @ProtoNumber(3) override val author: String? = null,
    @ProtoNumber(4) override val cover: String? = null,
    @ProtoNumber(5) val chapterIndex: Int = 0,
    @ProtoNumber(6) val progress: Double = 0.0,
    @ProtoNumber(7) val characterCount: Int = 0,
    @ProtoNumber(8) override val lastModified: Long = 0L,
    @ProtoNumber(9) override val stats: List<BackupStatEntry> = emptyList(),
    @ProtoNumber(10) override val categoryIds: List<String> = emptyList(),
    @ProtoNumber(11) override val lang: String? = null,
) : NovelBackupRecord<BackupStatEntry>

@Serializable
data class BackupNovelCategory(
    @ProtoNumber(1) val id: String,
    @ProtoNumber(2) val name: String,
    @ProtoNumber(3) val order: Long = 0,
    @ProtoNumber(4) val flags: Long = 0,
)

@Serializable
data class BackupStatEntry(
    @ProtoNumber(1) override val dateKey: String,
    @ProtoNumber(2) val charactersRead: Int,
    @ProtoNumber(3) val readingTime: Double,
    @ProtoNumber(4) val minReadingSpeed: Int,
    @ProtoNumber(5) val altMinReadingSpeed: Int,
    @ProtoNumber(6) val lastReadingSpeed: Int,
    @ProtoNumber(7) val maxReadingSpeed: Int,
    @ProtoNumber(8) override val lastStatisticModified: Long,
) : NovelBackupStatistic
