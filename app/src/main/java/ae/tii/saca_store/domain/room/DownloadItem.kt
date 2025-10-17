package ae.tii.saca_store.domain.room

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
@Keep
data class DownloadItem(
    @PrimaryKey val id: String, // uuid
    val url: String,
    val fileName: String,
    val destPath: String,       // absolute path where final file will live
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val lastError: String? = null,
    val retries: Int = 0
)

enum class DownloadStatus { PENDING, RUNNING, PAUSED, FAILED, COMPLETED, INSTALLING }