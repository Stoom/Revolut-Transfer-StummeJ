package uk.stumme.models.domain

import exceptions.InvalidArgumentException
import java.math.BigInteger

class Iban(private var accountNumber: String) {
    init {
        if (accountNumber.isEmpty() || accountNumber.length > 30)
            throw InvalidArgumentException("accountNumber")

        accountNumber = accountNumber.replace(" ", "")
    }

    val countryCode: String get() = accountNumber.substring(0 until 2)
    val checksum: String get() = accountNumber.substring(2 until 4)
    val number: String get() = accountNumber.substring(4)
    private val countryCodeChecksum: String get() = accountNumber.substring(0 until 4)

    override fun toString(): String {
        return accountNumber
    }

    fun isValid(): Boolean {
        if (checksum.any { it !in '0'..'9' })
            return false

        val checksumBigInt = BigInteger("$number$countryCodeChecksum".toIbanIntString())
        return checksumBigInt.mod(97.toBigInteger()) == 1.toBigInteger()
    }

    fun calculateChecksum(): Iban {
        val checksumBigInt = BigInteger("${number}${countryCode}00".toIbanIntString())
        val checksumNumber = checksumBigInt.mod(97.toBigInteger()).toInt()
        val checksum = (98 - checksumNumber).toString().padStart(2, '0')

        accountNumber = "$countryCode$checksum$number"

        return this
    }

    private fun Char.toIbanInt(): Int {
        if (this !in 'A'..'Z')
            return this.toString().toInt()

        return (this.dec() - 54).toInt()
    }

    private fun String.toIbanIntString(): String {
        return this.map { it.toIbanInt() }.joinToString("")
    }
}