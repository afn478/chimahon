package tachiyomi.domain.backup.model

interface NovelBackupStatistic {
    val dateKey: String
    val lastStatisticModified: Long
}

interface NovelBackupRecord<Stat : NovelBackupStatistic> {
    val id: String
    val author: String?
    val cover: String?
    val lastModified: Long
    val stats: List<Stat>
    val categoryIds: List<String>
    val lang: String?
}
