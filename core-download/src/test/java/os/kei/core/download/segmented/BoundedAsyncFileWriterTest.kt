package os.kei.core.download.segmented

import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoundedAsyncFileWriterTest {
    @Test
    fun `close drains queued positioned writes`() = runBlocking(Dispatchers.IO) {
        val target = ByteArray(8)
        coroutineScope {
            val writer = BoundedAsyncFileWriter(
                scope = this,
                capacity = 2,
                writeAt = { position, bytes ->
                    bytes.copyInto(target, destinationOffset = position.toInt())
                },
            )

            writer.enqueue(position = 4, source = byteArrayOf(5, 6, 7, 8), byteCount = 4)
            writer.enqueue(position = 0, source = byteArrayOf(1, 2, 3, 4), byteCount = 4)
            writer.closeAndJoin()
        }

        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8), target)
    }

    @Test
    fun `bounded queue suspends producer until writer makes room`() = runBlocking(Dispatchers.IO) {
        val firstWriteStarted = CountDownLatch(1)
        val releaseWriter = CountDownLatch(1)
        coroutineScope {
            val writer = BoundedAsyncFileWriter(
                scope = this,
                capacity = 1,
                writeAt = { _, _ ->
                    firstWriteStarted.countDown()
                    releaseWriter.await(5, TimeUnit.SECONDS)
                },
            )

            writer.enqueue(0, byteArrayOf(1), 1)
            assertTrue(firstWriteStarted.await(5, TimeUnit.SECONDS))
            writer.enqueue(1, byteArrayOf(2), 1)
            val third = async { writer.enqueue(2, byteArrayOf(3), 1) }
            yield()

            assertFalse(third.isCompleted)

            releaseWriter.countDown()
            third.await()
            writer.closeAndJoin()
        }
    }

    @Test
    fun `writer failure is reported by close`() = runBlocking {
        // Keep the consumer on this event loop: enqueue fills the buffer before close lets it run.
        // On Dispatchers.IO the consumer can fail first, so enqueue correctly throws outside the assertion.
        coroutineScope {
            val writer = BoundedAsyncFileWriter(
                scope = this,
                capacity = 1,
                writeAt = { _, _ -> throw IOException("disk failed") },
            )

            writer.enqueue(0, byteArrayOf(1), 1)
            val error = assertFailsWith<IOException> { writer.closeAndJoin() }

            assertTrue(error.message.orEmpty().contains("disk failed"))
        }
    }

    @Test
    fun `progress callback failure has a distinct fatal type`() = runBlocking {
        coroutineScope {
            val writer = BoundedAsyncFileWriter(
                scope = this,
                capacity = 1,
                writeAt = { _, _ -> },
                onBytesWritten = { throw IllegalStateException("progress failed") },
            )

            writer.enqueue(0, byteArrayOf(1), 1)

            assertFailsWith<BoundedAsyncProgressException> {
                writer.closeAndJoin()
            }
            Unit
        }
    }
}
