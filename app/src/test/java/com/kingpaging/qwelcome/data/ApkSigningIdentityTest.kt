package com.kingpaging.qwelcome.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkSigningIdentityTest {
    @Test
    fun `single signer accepts unchanged key and forward verified rotation`() {
        val installed = ApkSigningIdentity(setOf("a"))

        assertTrue(installed.acceptsUpdate(ApkSigningIdentity(setOf("a"))))
        assertTrue(installed.acceptsUpdate(ApkSigningIdentity(setOf("b"), setOf("a", "b"))))
        assertFalse(installed.acceptsUpdate(ApkSigningIdentity(setOf("b"))))
    }

    @Test
    fun `rotation cannot go back to a historical key`() {
        val installed = ApkSigningIdentity(setOf("b"), setOf("a", "b"))

        assertFalse(installed.acceptsUpdate(ApkSigningIdentity(setOf("a"))))
        assertTrue(installed.acceptsUpdate(ApkSigningIdentity(setOf("c"), setOf("a", "b", "c"))))
    }

    @Test
    fun `multiple simultaneous signers require exact current sets`() {
        val installed = ApkSigningIdentity(setOf("a", "b"))

        assertTrue(installed.acceptsUpdate(ApkSigningIdentity(setOf("b", "a"))))
        assertFalse(installed.acceptsUpdate(ApkSigningIdentity(setOf("a"))))
        assertFalse(installed.acceptsUpdate(ApkSigningIdentity(setOf("a", "b", "c"))))
        assertFalse(ApkSigningIdentity(setOf("a")).acceptsUpdate(ApkSigningIdentity(setOf("a", "b"))))
    }

    @Test
    fun `missing signer information fails closed`() {
        assertFalse(ApkSigningIdentity(emptySet()).acceptsUpdate(ApkSigningIdentity(setOf("a"))))
        assertFalse(ApkSigningIdentity(setOf("a")).acceptsUpdate(ApkSigningIdentity(emptySet())))
    }
}
