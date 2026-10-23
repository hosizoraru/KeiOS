package os.kei.feature.github.data.apk

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal object ZipRangeTestFixtures {
    fun zipWithEntries(vararg entries: Pair<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (entryName, bytes) ->
                zip.putNextEntry(ZipEntry(entryName))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    fun zipWithStoredEntry(
        entryName: String,
        bytes: ByteArray
    ): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            val crc = CRC32().apply { update(bytes) }.value
            val entry = ZipEntry(entryName).apply {
                method = ZipEntry.STORED
                size = bytes.size.toLong()
                compressedSize = bytes.size.toLong()
                this.crc = crc
            }
            zip.putNextEntry(entry)
            zip.write(bytes)
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    fun rangeDispatcher(bytes: ByteArray): Dispatcher {
        return object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = rangeResponse(request, bytes)
        }
    }

    fun rangeResponse(request: RecordedRequest, bytes: ByteArray): MockResponse {
        val range = Regex("""bytes=(\d+)-(\d+)""").matchEntire(request.getHeader("Range").orEmpty())
            ?: return MockResponse().setResponseCode(400)
        val start = range.groupValues[1].toInt()
        val end = range.groupValues[2].toInt().coerceAtMost(bytes.lastIndex)
        if (start !in bytes.indices || end < start) return MockResponse().setResponseCode(416)
        return MockResponse()
            .setResponseCode(206)
            .addHeader("Content-Range", "bytes $start-$end/${bytes.size}")
            .setBody(Buffer().write(bytes.copyOfRange(start, end + 1)))
    }
}
