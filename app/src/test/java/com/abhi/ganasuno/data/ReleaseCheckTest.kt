package com.abhi.ganasuno.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseCheckTest {
    private val apk = "https://github.com/Abhii-08/GANASUNO/releases/download/v1.1/GANASUNO.apk"

    @Test
    fun updateRequiresStrictlyNewerVersionCode() {
        val release = ReleaseCheck.Release(112, "1.1", apk)
        assertTrue(release.isNewerThan(111))
        assertFalse(release.isNewerThan(112))
        assertFalse(release.isNewerThan(113))
        assertFalse(release.isNewerThan(Int.MAX_VALUE.toLong() + 1))
    }

    @Test
    fun stableDownloadUsesExactGitHubAsset() {
        val assets = sequenceOf("SHA256SUMS.txt" to "other", "GANASUNO.apk" to apk)
        assertEquals(apk, ReleaseCheck.officialApkUrl("v1.1", assets))
    }

    @Test
    fun stableDownloadRejectsNameHostTagAndQueryMismatches() {
        val invalidAssets = listOf(
            "GANASUNO-v1.1-universal.apk" to apk,
            "GANASUNO.apk" to apk.replace("github.com", "example.org"),
            "GANASUNO.apk" to apk.replace("/v1.1/", "/v1.2/"),
            "GANASUNO.apk" to "$apk?redirect=other",
        )
        invalidAssets.forEach { asset ->
            assertThrows(IllegalArgumentException::class.java) {
                ReleaseCheck.officialApkUrl("v1.1", sequenceOf(asset))
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReleaseCheck.officialApkUrl("v1.1-preview", sequenceOf("GANASUNO.apk" to apk))
        }
    }
}
