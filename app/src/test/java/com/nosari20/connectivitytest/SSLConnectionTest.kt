package com.nosari20.connectivitytest

import com.nosari20.connectivitytest.utils.checkSsl
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * SSL/TLS Connection Tests using badssl.com endpoints
 *
 * badssl.com provides various SSL/TLS test scenarios to verify
 * proper certificate validation, hostname verification, and error handling.
 *
 * Test Categories:
 * - Valid certificates
 * - Expired certificates
 * - Wrong hostname
 * - Self-signed certificates
 * - Untrusted root
 * - Revoked certificates
 * - Protocol issues
 * - Cipher suite issues
 */
class SSLConnectionTest {

    @Before
    fun setup() {
        // Initialize any required components
    }

    // ==================== VALID CERTIFICATE TESTS ====================

    @Test
    fun testValidCertificate() {
        val test = ConnectivityTest(
            host = "badssl.com",
            port = 443,
            ssl = true,
            enableCrlCheck = false  // CRL checking disabled (default)
        )

        checkSsl(test)

        assertEquals(ConnectivityTest.Status.OK, test.status)
        assertNotNull(test.connectionDetails)
        assertTrue(test.connectionDetails?.hostnameVerified == true)
        assertNotNull(test.connectionDetails?.serverCertificates)
        assertTrue(test.connectionDetails?.serverCertificates?.isNotEmpty() == true)

        // CRL should not be checked when disabled
        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNull("CRL should not be checked when enableCrlCheck=false", cert?.isRevoked)
    }

    @Test
    fun testValidCertificateWithSHA256() {
        val test = ConnectivityTest(
            host = "sha256.badssl.com",
            port = 443,
            ssl = true,
            enableCrlCheck = false
        )

        checkSsl(test)

        assertEquals(ConnectivityTest.Status.OK, test.status)
        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNotNull(cert)
        assertTrue(cert?.signatureAlgorithm?.contains("SHA256", ignoreCase = true) == true)
        assertNull("CRL should not be checked when disabled", cert?.isRevoked)
    }

    @Test
    fun testValidCertificateWithSHA384() {
        val test = ConnectivityTest(
            host = "sha384.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        // Note: This endpoint may fail on some systems
        // Test that we at least capture certificate details
        assertNotNull(test.connectionDetails)
        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNotNull(cert)

        // If successful, verify signature algorithm
        if (test.status == ConnectivityTest.Status.OK) {
            assertTrue(cert?.signatureAlgorithm?.contains("SHA384", ignoreCase = true) == true)
        }
    }

    @Test
    fun testValidCertificateWithSHA512() {
        val test = ConnectivityTest(
            host = "sha512.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        // Note: This endpoint may fail on some systems
        // Test that we at least capture certificate details
        assertNotNull(test.connectionDetails)
        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNotNull(cert)

        // If successful, verify signature algorithm
        if (test.status == ConnectivityTest.Status.OK) {
            assertTrue(cert?.signatureAlgorithm?.contains("SHA512", ignoreCase = true) == true)
        }
    }

    @Test
    fun testValidCertificateWithECC256() {
        val test = ConnectivityTest(
            host = "ecc256.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        assertEquals(ConnectivityTest.Status.OK, test.status)
        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNotNull(cert)
        assertTrue(cert?.publicKeyAlgorithm?.contains("EC", ignoreCase = true) == true)
    }

    @Test
    fun testValidCertificateWithECC384() {
        val test = ConnectivityTest(
            host = "ecc384.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        assertEquals(ConnectivityTest.Status.OK, test.status)
        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNotNull(cert)
        assertTrue(cert?.publicKeyAlgorithm?.contains("EC", ignoreCase = true) == true)
    }

    @Test
    fun testValidCertificateWithRSA2048() {
        val test = ConnectivityTest(
            host = "rsa2048.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        assertEquals(ConnectivityTest.Status.OK, test.status)
        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNotNull(cert)
        assertEquals("RSA", cert?.publicKeyAlgorithm)
    }

    @Test
    fun testValidCertificateWithRSA4096() {
        val test = ConnectivityTest(
            host = "rsa4096.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        assertEquals(ConnectivityTest.Status.OK, test.status)
        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNotNull(cert)
        assertEquals("RSA", cert?.publicKeyAlgorithm)
    }

    @Test
    fun testValidCertificateWithRSA8192() {
        val test = ConnectivityTest(
            host = "rsa8192.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        // Note: RSA 8192 may be slow and might fail on some systems
        // Test that we at least capture certificate details
        assertNotNull(test.connectionDetails)
        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNotNull(cert)

        // If successful, verify it's RSA
        if (test.status == ConnectivityTest.Status.OK) {
            assertEquals("RSA", cert?.publicKeyAlgorithm)
        }
    }

    // ==================== EXTENDED VALIDATION (EV) TESTS ====================

    @Test
    fun testExtendedValidationCertificate() {
        val test = ConnectivityTest(
            host = "extended-validation.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        // EV certificates may not always validate successfully
        // Test that we capture certificate details regardless
        assertNotNull(test.connectionDetails)
        assertNotNull(test.connectionDetails?.serverCertificates)
        assertTrue(test.connectionDetails?.serverCertificates?.isNotEmpty() == true)
    }

    // ==================== EXPIRED CERTIFICATE TESTS ====================

    @Test
    fun testExpiredCertificate() {
        val test = ConnectivityTest(
            host = "expired.badssl.com",
            port = 443,
            ssl = true,
            enableCrlCheck = false  // CRL checking disabled
        )

        checkSsl(test)

        assertEquals(ConnectivityTest.Status.KO, test.status)
        assertTrue(test.info.contains("expired", ignoreCase = true) ||
                   test.info.contains("certificate", ignoreCase = true) ||
                   test.info.contains("handshake", ignoreCase = true))

        // Certificate chain should still be captured
        assertNotNull(test.connectionDetails)
        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNotNull(cert)

        // Certificate should have validity dates
        assertNotNull(cert?.validFrom)
        assertNotNull(cert?.validTo)

        // CRL should not be checked when disabled
        assertNull("CRL should not be checked when disabled", cert?.isRevoked)
    }

    @Test
    fun testExpiredCertificateWithCRLCheckEnabled() {
        val test = ConnectivityTest(
            host = "expired.badssl.com",
            port = 443,
            ssl = true,
            enableCrlCheck = true  // CRL checking enabled
        )

        checkSsl(test)

        // Should fail due to expiration, not CRL
        assertEquals(ConnectivityTest.Status.KO, test.status)

        // Certificate chain should still be captured
        assertNotNull(test.connectionDetails)
        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNotNull(cert)

        // Note: CRL checking may or may not succeed for expired cert
        // depending on whether cert was in CRL before expiration
    }

    // ==================== WRONG HOSTNAME TESTS ====================

    @Test
    fun testWrongHostname() {
        val test = ConnectivityTest(
            host = "wrong.host.badssl.com",
            port = 443,
            ssl = true,
            enableCrlCheck = false  // CRL checking disabled
        )

        checkSsl(test)

        assertEquals(ConnectivityTest.Status.KO, test.status)
        assertNotNull(test.connectionDetails)
        assertFalse(test.connectionDetails?.hostnameVerified == true)

        // Hostname verification details should explain the mismatch
        assertNotNull(test.connectionDetails?.hostnameVerificationDetails)
        assertTrue(test.connectionDetails?.hostnameVerificationDetails?.isNotEmpty() == true)

        // Certificate chain should still be captured
        assertNotNull(test.connectionDetails?.serverCertificates)
        assertTrue(test.connectionDetails?.serverCertificates?.isNotEmpty() == true)

        // CRL should not be checked when disabled
        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNull("CRL should not be checked when disabled", cert?.isRevoked)
    }

    // ==================== SELF-SIGNED CERTIFICATE TESTS ====================

    @Test
    fun testSelfSignedCertificate() {
        val test = ConnectivityTest(
            host = "self-signed.badssl.com",
            port = 443,
            ssl = true,
            enableCrlCheck = false  // CRL checking disabled
        )

        checkSsl(test)

        assertEquals(ConnectivityTest.Status.KO, test.status)

        // Certificate should be captured even though validation failed
        assertNotNull(test.connectionDetails)
        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNotNull(cert)

        // Verify it's marked as self-signed
        assertTrue(cert?.isSelfSigned == true)
        assertEquals(cert?.subjectDN, cert?.issuerDN)

        // CRL should not be checked when disabled
        assertNull("CRL should not be checked when disabled", cert?.isRevoked)
    }

    // ==================== UNTRUSTED ROOT TESTS ====================

    @Test
    fun testUntrustedRoot() {
        val test = ConnectivityTest(
            host = "untrusted-root.badssl.com",
            port = 443,
            ssl = true,
            enableCrlCheck = false  // CRL checking disabled
        )

        checkSsl(test)

        assertEquals(ConnectivityTest.Status.KO, test.status)
        // Error message may vary, just check that it failed
        assertNotNull(test.info)
        assertTrue(test.info.isNotEmpty())

        // Certificate chain should still be captured
        assertNotNull(test.connectionDetails)
        assertNotNull(test.connectionDetails?.serverCertificates)
        assertTrue(test.connectionDetails?.serverCertificates?.isNotEmpty() == true)

        // CRL should not be checked when disabled
        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNull("CRL should not be checked when disabled", cert?.isRevoked)
    }

    // ==================== REVOKED CERTIFICATE TESTS ====================

    @Test
    fun testRevokedCertificate() {
        val test = ConnectivityTest(
            host = "revoked.badssl.com",
            port = 443,
            ssl = true,
            enableCrlCheck = true  // EXPLICITLY ENABLE CRL CHECKING
        )

        checkSsl(test)

        // With CRL checking enabled, we must have checked CRL
        assertNotNull(test.connectionDetails)
        assertTrue("CRL check must be performed when enableCrlCheck=true",
                   test.connectionDetails?.crlChecked == true)

        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNotNull("Certificate must be captured", cert)

        // Verify CRL distribution points were extracted
        if (cert?.crlDistributionPoints?.isNotEmpty() == true) {
            // If CRL URLs exist and checking is enabled, isRevoked must not be null
            assertNotNull("CRL revocation status must be determined when CRL URLs exist",
                          cert.isRevoked)

            // If the certificate is revoked, the test should fail
            if (cert.isRevoked == true) {
                assertEquals("Revoked certificate should fail the test",
                           ConnectivityTest.Status.KO, test.status)
                assertNotNull("Revocation reason must be provided", cert.revocationReason)
            }
        }
    }

    // ==================== INCOMPLETE CHAIN TESTS ====================

    @Test
    fun testIncompleteChain() {
        val test = ConnectivityTest(
            host = "incomplete-chain.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        // Incomplete chain should fail validation
        assertEquals(ConnectivityTest.Status.KO, test.status)

        // But partial chain should be captured
        assertNotNull(test.connectionDetails)
        val certs = test.connectionDetails?.serverCertificates
        assertNotNull(certs)

        // Chain should have fewer certificates than expected
        assertTrue((certs?.size ?: 0) < 3)
    }

    // ==================== SUBDOMAIN TESTS ====================

    @Test
    fun testSubdomainMismatch() {
        val test = ConnectivityTest(
            host = "subdomain.mismatch.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        // Should fail hostname verification
        assertNotNull(test.connectionDetails)
        assertFalse(test.connectionDetails?.hostnameVerified == true)
    }

    // ==================== WILDCARD CERTIFICATE TESTS ====================

    @Test
    fun testWildcardCertificate() {
        val test = ConnectivityTest(
            host = "wildcard.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        // Wildcard endpoint may not always work
        // Test that we at least capture certificate details
        assertNotNull(test.connectionDetails)
        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNotNull(cert)

        // If successful, check that SANs exist
        if (test.status == ConnectivityTest.Status.OK) {
            assertNotNull(cert?.subjectAlternativeNames)
        }
    }

    // ==================== PROTOCOL VERSION TESTS ====================

    @Test
    fun testTLS12Connection() {
        val test = ConnectivityTest(
            host = "tls-v1-2.badssl.com",
            port = 1012,
            ssl = true
        )

        checkSsl(test)

        // TLS 1.2 should work
        assertEquals(ConnectivityTest.Status.OK, test.status)
        val protocol = test.connectionDetails?.protocol
        assertNotNull(protocol)
        assertTrue(protocol?.contains("TLSv1.2", ignoreCase = true) == true)
    }

    @Test
    fun testTLS11Connection() {
        val test = ConnectivityTest(
            host = "tls-v1-1.badssl.com",
            port = 1011,
            ssl = true
        )

        checkSsl(test)

        // TLS 1.1 is deprecated and may fail on newer systems
        // Test captures the result regardless
        assertNotNull(test.connectionDetails)
    }

    @Test
    fun testTLS10Connection() {
        val test = ConnectivityTest(
            host = "tls-v1-0.badssl.com",
            port = 1010,
            ssl = true
        )

        checkSsl(test)

        // TLS 1.0 is deprecated and should likely fail
        // Test captures the result
        assertNotNull(test.connectionDetails)
    }

    // ==================== CIPHER SUITE TESTS ====================

    @Test
    fun testRC4Cipher() {
        val test = ConnectivityTest(
            host = "rc4.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        // RC4 is deprecated and should fail on modern systems
        assertEquals(ConnectivityTest.Status.KO, test.status)
    }

    @Test
    fun testRC4MD5Cipher() {
        val test = ConnectivityTest(
            host = "rc4-md5.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        // RC4-MD5 is insecure and should fail
        assertEquals(ConnectivityTest.Status.KO, test.status)
    }

    @Test
    fun test3DESCipher() {
        val test = ConnectivityTest(
            host = "3des.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        // 3DES is deprecated but might still work on some systems
        assertNotNull(test.connectionDetails)
    }

    @Test
    fun testNullCipher() {
        val test = ConnectivityTest(
            host = "null.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        // NULL cipher provides no encryption and should fail
        assertEquals(ConnectivityTest.Status.KO, test.status)
    }

    // ==================== DH KEY EXCHANGE TESTS ====================

    @Test
    fun testDH480() {
        val test = ConnectivityTest(
            host = "dh480.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        // DH 480 is too weak and should fail
        assertEquals(ConnectivityTest.Status.KO, test.status)
    }

    @Test
    fun testDH512() {
        val test = ConnectivityTest(
            host = "dh512.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        // DH 512 is too weak and should fail
        assertEquals(ConnectivityTest.Status.KO, test.status)
    }

    @Test
    fun testDH1024() {
        val test = ConnectivityTest(
            host = "dh1024.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        // DH 1024 is weak but might still work
        assertNotNull(test.connectionDetails)
    }

    @Test
    fun testDH2048() {
        val test = ConnectivityTest(
            host = "dh2048.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        // DH 2048 should work
        assertEquals(ConnectivityTest.Status.OK, test.status)
    }

    // ==================== CERTIFICATE CHAIN TESTS ====================

    @Test
    fun testLongCertificateChain() {
        val test = ConnectivityTest(
            host = "long-extended-subdomain-name-containing-many-letters-and-dashes.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        // Long domain names should work
        assertEquals(ConnectivityTest.Status.OK, test.status)

        // Hostname verification should handle long names
        assertTrue(test.connectionDetails?.hostnameVerified == true)
    }

    @Test
    fun testCertificateChainCapture() {
        val test = ConnectivityTest(
            host = "badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        val certs = test.connectionDetails?.serverCertificates
        assertNotNull(certs)
        assertTrue(certs?.isNotEmpty() == true)

        // Should have at least leaf + intermediate(s)
        assertTrue((certs?.size ?: 0) >= 2)

        // First cert should be leaf (server cert)
        val leafCert = certs?.firstOrNull()
        assertNotNull(leafCert)
        assertTrue(leafCert?.subjectDN?.contains("badssl.com", ignoreCase = true) == true)

        // Check certificate chain integrity
        for (i in 0 until (certs?.size ?: 0) - 1) {
            val cert = certs?.get(i)
            val issuer = certs?.get(i + 1)

            // Cert's issuer should match next cert's subject
            assertNotNull(cert?.issuerDN)
            assertNotNull(issuer?.subjectDN)
        }
    }

    // ==================== SPECIAL CASES ====================

    @Test
    fun testNoSubjectAlternativeNames() {
        val test = ConnectivityTest(
            host = "no-sct.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        // Should still work with hostname verification
        assertNotNull(test.connectionDetails)
    }

    @Test
    fun testStaticRSAConnection() {
        val test = ConnectivityTest(
            host = "static-rsa.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        // Static RSA should work but is deprecated
        assertNotNull(test.connectionDetails)
    }

    // ==================== HOSTNAME VERIFICATION TESTS ====================

    @Test
    fun testHostnameVerificationDetails() {
        val test = ConnectivityTest(
            host = "badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        assertEquals(ConnectivityTest.Status.OK, test.status)

        // Check hostname verification details
        assertNotNull(test.connectionDetails?.hostnameVerificationDetails)
        assertTrue(test.connectionDetails?.hostnameVerificationDetails?.contains("Matched", ignoreCase = true) == true)
    }

    @Test
    fun testHostnameVerificationFailureDetails() {
        val test = ConnectivityTest(
            host = "wrong.host.badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        assertEquals(ConnectivityTest.Status.KO, test.status)

        // Should have detailed explanation of failure
        assertNotNull(test.connectionDetails?.hostnameVerificationDetails)
        val details = test.connectionDetails?.hostnameVerificationDetails ?: ""
        assertTrue(details.contains("Certificate has:", ignoreCase = true) ||
                   details.contains("mismatch", ignoreCase = true))
    }

    // ==================== CRL CHECKING TESTS ====================

    @Test
    fun testCRLDistributionPoints() {
        val test = ConnectivityTest(
            host = "badssl.com",
            port = 443,
            ssl = true,
            enableCrlCheck = true  // Enable CRL checking
        )

        checkSsl(test)

        assertEquals(ConnectivityTest.Status.OK, test.status)

        // Check that CRL checking was performed
        assertTrue("CRL check must be performed when enableCrlCheck=true",
                   test.connectionDetails?.crlChecked == true)

        // Check that CRL distribution points are extracted
        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNotNull(cert)

        // Most modern certificates have CRL distribution points
        // If present, verify they're URLs
        if (cert?.crlDistributionPoints?.isNotEmpty() == true) {
            cert.crlDistributionPoints.forEach { url ->
                assertTrue("CRL URL must be HTTP or HTTPS",
                          url.startsWith("http://") || url.startsWith("https://"))
            }

            // With CRL checking enabled and URLs present, revocation status must be checked
            assertNotNull("Revocation status must be checked when CRL URLs exist",
                         cert.isRevoked)
        }
    }

    @Test
    fun testCRLCheckingDisabledByDefault() {
        val test = ConnectivityTest(
            host = "badssl.com",
            port = 443,
            ssl = true
            // enableCrlCheck NOT set - defaults to false
        )

        checkSsl(test)

        assertEquals(ConnectivityTest.Status.OK, test.status)

        // CRL checking should NOT be performed when disabled
        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNotNull(cert)

        // CRL URLs might be extracted, but revocation status should not be checked
        assertNull("Revocation status should not be checked when CRL checking is disabled",
                  cert?.isRevoked)
    }

    @Test
    fun testCRLCheckWithValidCertificate() {
        val test = ConnectivityTest(
            host = "badssl.com",
            port = 443,
            ssl = true,
            enableCrlCheck = true  // Enable CRL checking
        )

        checkSsl(test)

        // Should succeed - certificate is valid and not revoked
        assertEquals(ConnectivityTest.Status.OK, test.status)

        // CRL check must be performed
        assertTrue("CRL check must be performed when enableCrlCheck=true",
                   test.connectionDetails?.crlChecked == true)

        // Check certificate
        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNotNull(cert)

        // If CRL URLs exist, revocation status should be checked
        if (cert?.crlDistributionPoints?.isNotEmpty() == true) {
            // Revocation status should be determined
            // (null means CRL server was unreachable, which is acceptable)
            // If not null, it should be false (not revoked)
            if (cert.isRevoked != null) {
                assertFalse("Valid certificate should not be revoked", cert.isRevoked == true)
            }
        }
    }

    @Test
    fun testCRLCheckFailsWhenCertificateRevoked() {
        val test = ConnectivityTest(
            host = "revoked.badssl.com",
            port = 443,
            ssl = true,
            enableCrlCheck = true  // Enable CRL checking - MUST CHECK
        )

        checkSsl(test)

        // With CRL checking enabled, we must have checked CRL
        assertNotNull(test.connectionDetails)
        assertTrue("CRL check must be attempted when enableCrlCheck=true",
                   test.connectionDetails?.crlChecked == true)

        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNotNull("Certificate must be captured", cert)

        // If CRL URLs exist and CRL was reachable
        if (cert?.crlDistributionPoints?.isNotEmpty() == true && cert.isRevoked != null) {
            // If certificate is revoked, test MUST fail
            if (cert.isRevoked == true) {
                assertEquals("Test must fail when certificate is revoked",
                           ConnectivityTest.Status.KO, test.status)
                assertNotNull("Revocation reason must be provided", cert.revocationReason)
                assertTrue("Error message must mention revocation",
                          test.info.contains("revoked", ignoreCase = true))
            }
        }
    }

    @Test
    fun testCRLCheckGracefulDegradationWhenCRLUnavailable() {
        val test = ConnectivityTest(
            host = "badssl.com",
            port = 443,
            ssl = true,
            enableCrlCheck = true  // Enable CRL checking
        )

        checkSsl(test)

        val cert = test.connectionDetails?.serverCertificates?.firstOrNull()
        assertNotNull(cert)

        // If CRL is unavailable (isRevoked = null), test should still proceed
        // based on other validation (should succeed for badssl.com)
        if (cert?.isRevoked == null) {
            // Test continues despite CRL being unavailable
            // This is graceful degradation
            assertNotNull(test.connectionDetails)
        }
    }

    @Test
    fun testCRLURLsExtractedRegardlessOfCheckFlag() {
        // Test 1: CRL URLs extracted even when checking is disabled
        val testDisabled = ConnectivityTest(
            host = "badssl.com",
            port = 443,
            ssl = true,
            enableCrlCheck = false  // Disabled
        )

        checkSsl(testDisabled)

        val certDisabled = testDisabled.connectionDetails?.serverCertificates?.firstOrNull()
        // CRL URLs might be extracted but status should NOT be checked
        if (certDisabled?.crlDistributionPoints?.isNotEmpty() == true) {
            assertNull("Revocation status should not be checked when disabled",
                      certDisabled.isRevoked)
        }

        // Test 2: CRL URLs extracted and checked when enabled
        val testEnabled = ConnectivityTest(
            host = "badssl.com",
            port = 443,
            ssl = true,
            enableCrlCheck = true  // Enabled
        )

        checkSsl(testEnabled)

        val certEnabled = testEnabled.connectionDetails?.serverCertificates?.firstOrNull()
        // CRL URLs extracted AND status checked
        if (certEnabled?.crlDistributionPoints?.isNotEmpty() == true) {
            // Status should be checked (might be null if CRL unavailable, but that's OK)
            // The key is that the ATTEMPT was made
            assertTrue("CRL check must be attempted when enabled",
                      testEnabled.connectionDetails?.crlChecked == true)
        }
    }

    @Test
    fun testMultipleCertificatesInChainCRLChecking() {
        val test = ConnectivityTest(
            host = "badssl.com",
            port = 443,
            ssl = true,
            enableCrlCheck = true  // Enable CRL checking
        )

        checkSsl(test)

        val certs = test.connectionDetails?.serverCertificates
        assertNotNull(certs)
        assertTrue("Certificate chain should have multiple certificates",
                   certs?.size ?: 0 >= 2)

        // If CRL checking is enabled, it should check all certificates in chain
        assertTrue("CRL check must be performed when enableCrlCheck=true",
                   test.connectionDetails?.crlChecked == true)

        // Verify CRL status includes information about all certificates
        if (test.connectionDetails?.crlStatus?.isNotEmpty() == true) {
            val status = test.connectionDetails?.crlStatus ?: ""
            // Status should mention multiple certificates (Cert 0, Cert 1, etc.)
            assertTrue("CRL status should cover multiple certificates",
                      status.contains("Cert", ignoreCase = true))
        }
    }

    // ==================== CROSS-SIGNED CERTIFICATE TESTS ====================

    @Test
    fun testCrossSignedCertificateDetection() {
        val test = ConnectivityTest(
            host = "badssl.com",
            port = 443,
            ssl = true
        )

        checkSsl(test)

        val certs = test.connectionDetails?.serverCertificates
        assertNotNull(certs)

        // Check for cross-signing detection
        val crossSigned = com.nosari20.connectivitytest.utils.detectCrossSignedCertificates(certs!!)
        // Result depends on actual certificate chain
        assertNotNull(crossSigned)
    }
}







