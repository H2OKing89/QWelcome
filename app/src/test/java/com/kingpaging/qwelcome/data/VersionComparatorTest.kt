package com.kingpaging.qwelcome.data

import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [VersionComparator] — SemVer comparison with pre-release support.
 *
 * Covers:
 * - Basic major/minor/patch bumps
 * - Pre-release vs stable precedence
 * - Pre-release to pre-release ordering (the gap this fixes)
 * - Numeric vs string identifiers
 * - Build metadata stripping per SemVer 2.0
 * - Edge cases (empty strings, different-length version parts)
 */
class VersionComparatorTest {

    @Before
    fun setup() {
        AppViewModelProvider.resetForTesting()
    }

    @After
    fun teardown() {
        AppViewModelProvider.resetForTesting()
    }

    // ========== Basic version bumps ==========

    @Test
    fun `major bump is newer`() {
        assertTrue(VersionComparator.isNewerVersion("2.0.0", "1.0.0"))
    }

    @Test
    fun `minor bump is newer`() {
        assertTrue(VersionComparator.isNewerVersion("1.1.0", "1.0.0"))
    }

    @Test
    fun `patch bump is newer`() {
        assertTrue(VersionComparator.isNewerVersion("1.0.1", "1.0.0"))
    }

    @Test
    fun `older major is not newer`() {
        assertFalse(VersionComparator.isNewerVersion("1.0.0", "2.0.0"))
    }

    @Test
    fun `older minor is not newer`() {
        assertFalse(VersionComparator.isNewerVersion("1.0.0", "1.1.0"))
    }

    @Test
    fun `equal versions are not newer`() {
        assertFalse(VersionComparator.isNewerVersion("1.2.3", "1.2.3"))
    }

    @Test
    fun `different-length parts - 1_0 equals 1_0_0`() {
        assertFalse(VersionComparator.isNewerVersion("1.0", "1.0.0"))
    }

    @Test
    fun `different-length parts - 1_0_1 is newer than 1_0`() {
        assertTrue(VersionComparator.isNewerVersion("1.0.1", "1.0"))
    }

    // ========== Pre-release vs stable ==========

    @Test
    fun `stable is newer than same-base prerelease`() {
        assertTrue(VersionComparator.isNewerVersion("1.0.0", "1.0.0-beta"))
    }

    @Test
    fun `prerelease is not newer than same-base stable`() {
        assertFalse(VersionComparator.isNewerVersion("1.0.0-beta", "1.0.0"))
    }

    @Test
    fun `prerelease with higher base is newer than lower stable`() {
        assertTrue(VersionComparator.isNewerVersion("2.0.0-beta", "1.9.9"))
    }

    @Test
    fun `stable with lower base is not newer than higher prerelease base`() {
        assertFalse(VersionComparator.isNewerVersion("1.9.9", "2.0.0-beta"))
    }

    // ========== Pre-release to pre-release (the fix) ==========

    @Test
    fun `beta_2 is newer than beta_1`() {
        assertTrue(VersionComparator.isNewerVersion("1.0.0-beta.2", "1.0.0-beta.1"))
    }

    @Test
    fun `beta_1 is not newer than beta_2`() {
        assertFalse(VersionComparator.isNewerVersion("1.0.0-beta.1", "1.0.0-beta.2"))
    }

    @Test
    fun `beta_10 is newer than beta_2 - numeric comparison`() {
        assertTrue(VersionComparator.isNewerVersion("1.0.0-beta.10", "1.0.0-beta.2"))
    }

    @Test
    fun `rc is newer than beta - lexicographic`() {
        assertTrue(VersionComparator.isNewerVersion("1.0.0-rc", "1.0.0-beta"))
    }

    @Test
    fun `alpha is not newer than beta - lexicographic`() {
        assertFalse(VersionComparator.isNewerVersion("1.0.0-alpha", "1.0.0-beta"))
    }

    @Test
    fun `equal prereleases are not newer`() {
        assertFalse(VersionComparator.isNewerVersion("1.0.0-beta.1", "1.0.0-beta.1"))
    }

    @Test
    fun `rc_1 is newer than beta_99 - first identifier wins`() {
        assertTrue(VersionComparator.isNewerVersion("1.0.0-rc.1", "1.0.0-beta.99"))
    }

    // ========== Numeric vs string identifier precedence ==========

    @Test
    fun `string identifier beats numeric - per SemVer`() {
        // SemVer: numeric has lower precedence than string
        assertTrue(VersionComparator.isNewerVersion("1.0.0-alpha", "1.0.0-1"))
    }

    @Test
    fun `numeric identifier is lower than string`() {
        assertFalse(VersionComparator.isNewerVersion("1.0.0-1", "1.0.0-alpha"))
    }

    // ========== Different-length pre-releases ==========

    @Test
    fun `longer prerelease is newer when prefix matches`() {
        // beta.1 < beta.1.1 (shorter is lower)
        assertTrue(VersionComparator.isNewerVersion("1.0.0-beta.1.1", "1.0.0-beta.1"))
    }

    @Test
    fun `shorter prerelease is not newer when prefix matches`() {
        assertFalse(VersionComparator.isNewerVersion("1.0.0-beta.1", "1.0.0-beta.1.1"))
    }

    // ========== Edge cases ==========

    @Test
    fun `empty remote returns false`() {
        assertFalse(VersionComparator.isNewerVersion("", "1.0.0"))
    }

    @Test
    fun `empty current returns false`() {
        assertFalse(VersionComparator.isNewerVersion("1.0.0", ""))
    }

    @Test
    fun `both empty returns false`() {
        assertFalse(VersionComparator.isNewerVersion("", ""))
    }

    @Test
    fun `non-numeric base parts returns false`() {
        assertFalse(VersionComparator.isNewerVersion("abc", "1.0.0"))
    }

    @Test
    fun `non-numeric in multi-part base returns false`() {
        assertFalse(VersionComparator.isNewerVersion("1.x.0", "1.0.0"))
    }

    // ========== Build metadata handling (SemVer 2.0) ==========

    @Test
    fun `build metadata is ignored - stable versions`() {
        // 1.2.3+001 and 1.2.3+002 should be considered equal (build metadata ignored)
        assertFalse(VersionComparator.isNewerVersion("1.2.3+002", "1.2.3+001"))
        assertFalse(VersionComparator.isNewerVersion("1.2.3+001", "1.2.3+002"))
    }

    @Test
    fun `build metadata is ignored - pre-release versions`() {
        // 1.2.3-alpha+001 and 1.2.3-alpha+002 should be considered equal
        assertFalse(VersionComparator.isNewerVersion("1.2.3-alpha+002", "1.2.3-alpha+001"))
        assertFalse(VersionComparator.isNewerVersion("1.2.3-alpha+001", "1.2.3-alpha+002"))
    }

    @Test
    fun `build metadata does not affect comparison`() {
        // Version comparison should work the same with or without build metadata
        assertTrue(VersionComparator.isNewerVersion("1.2.4+001", "1.2.3+999"))
        assertTrue(VersionComparator.isNewerVersion("1.2.3-beta.2+001", "1.2.3-beta.1+999"))
    }
}
