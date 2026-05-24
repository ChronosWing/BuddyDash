package com.chronoswing.buddydash.util

import org.junit.Assert.assertEquals
import org.junit.Test

class BambuHmsWikiTest {

    @Test
    fun genericHmsWikiUrl_isVerifiedHomePage() {
        assertEquals(
            "https://wiki.bambulab.com/en/hms/home",
            BambuHmsLookup.GENERIC_HMS_WIKI_URL,
        )
    }
}
