package test.unit

import exceptions.InvalidArgumentException
import uk.stumme.models.domain.Iban
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IbanTest {
    @Test
    fun testToString_ShouldReturnTheAccountNumber() {
        val accountNumber = "GB25123456789012345678"
        val iban = Iban(accountNumber)

        assertEquals(accountNumber, iban.toString())
    }

    @Test(expected = InvalidArgumentException::class)
    fun testCtor_ShouldThrowWhenLengthIsGreaterThan30() {
        Iban("GB00ThisIsAVeryLongAccountNumberThatIsNotValid")
    }

    @Test(expected = InvalidArgumentException::class)
    fun testCtor_ShouldThrowWhenEmptyAccountNumber() {
        Iban("")
    }

    @Test
    fun testCtor_ShouldRemoveWhitespace() {
        val accountNumber = "GB82 WEST 1234 5698 7654 32"
        val expected = "GB82WEST12345698765432"

        val iban = Iban(accountNumber)

        assertEquals(expected, iban.toString())
    }

    @Test
    fun testCountryCode_ShouldReturnCountryCode() {
        val accountNumber = "GB98MIDL07009312345678"
        val expected = "GB"

        val iban = Iban(accountNumber)

        assertEquals(expected, iban.countryCode)
    }

    @Test
    fun testChecksum_ShouldReturnChecksum() {
        val accountNumber = "GB98MIDL07009312345678"
        val expected = "98"

        val iban = Iban(accountNumber)

        assertEquals(expected, iban.checksum)
    }

    @Test
    fun testNumber_ShouldReturnNumber() {
        val accountNumber = "GB98MIDL07009312345678"
        val expected = "MIDL07009312345678"

        val iban = Iban(accountNumber)

        assertEquals(expected, iban.number)
    }

    @Test
    fun testIsValid_ShouldBeTrueWhenValidChecksum() {
        val accountNumber = "GB98MIDL07009312345678"
        val iban = Iban(accountNumber)

        val actual = iban.isValid()

        assertTrue(actual)
    }

    @Test
    fun testIsValid_ShouldBeFalseWhenInvalidChecksum() {
        val accountNumber = "GB24MIDL07009312345678"
        val iban = Iban(accountNumber)

        val actual = iban.isValid()

        assertFalse(actual)
    }

    @Test
    fun testCalculateChecksum_ShouldCalculateChecksumIfDoubleZero() {
        val accountNumber = "GB00MIDL07009312345678"
        val expected = "98"

        val iban = Iban(accountNumber).calculateChecksum()

        assertEquals(expected, iban.toString().substring(2 until 4))
    }
}