package com.iltatrainer.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionTest {

    @Test
    fun `detects newer dotted versions`() {
        assertTrue(isNewerVersion("2.4.0", "2.3.0"))
        assertTrue(isNewerVersion("2.10.1", "2.9.9"))
        assertTrue(isNewerVersion("3.0", "2.99.99"))
        assertFalse(isNewerVersion("2.4.0", "2.4.0"))
        assertFalse(isNewerVersion("2.3.9", "2.4.0"))
        assertFalse(isNewerVersion("", "2.4.0"))
        assertFalse(isNewerVersion("v", "2.4.0"))
    }
}
