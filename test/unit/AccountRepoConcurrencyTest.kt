package test.unit

import assertk.assert
import assertk.assertions.isEqualTo
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import test.initializeDatabase
import test.stageAccount
import uk.stumme.account.AccountRepo
import uk.stumme.models.database.Account
import uk.stumme.models.domain.Iban
import java.sql.Connection
import java.util.*
import java.util.concurrent.Semaphore
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlin.test.Test

class AccountRepoConcurrencyTest {
    private var repo: AccountRepo

    init {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        this.repo = AccountRepo()

        initializeDatabase()
    }

    @Test
    fun testCreateAccounts_ShouldAllowMultipleAccountsToBeCreated() = runBlocking {
        val jobCount = 500

        val accounts = GlobalScope.massiveRun<Unit>(jobCount) {
            repo.createAccount(generateAccountNumber("UT"), Random.nextDouble(10.00, 50.00))
        }

        assert(accounts.size).isEqualTo(jobCount)
    }

    @Test
    fun testGetBalance_ShouldGetTheBalancesConcurrently() = runBlocking {
        val accountNumber1 = generateAccountNumber("UT")
        stageAccount(accountNumber1, 50.00)
        val jobCount = 500

        val results = GlobalScope.massiveRun<Double>(jobCount) {
            val balance = repo.getBalance(accountNumber1)

            assert(balance).isEqualTo(50.00)
        }

        assert(results.size).isEqualTo(jobCount)
    }

    @Test
    fun testTransfer_ShouldLockAndTransferCorrectly() = runBlocking {
        val accountNumber1 = generateAccountNumber("UT")
        val accountNumber2 = generateAccountNumber("UT")
        stageAccount(accountNumber1, 1000.00)
        stageAccount(accountNumber2, 0.00)
        val jobCount =  1000

        val results = GlobalScope.massiveRun<UUID>(jobCount) {
            repo.transfer(accountNumber1, accountNumber2, 1.00)
        }

        transaction {
            val accounts1 = Account.select { Account.id eq "$accountNumber1" }.single()[Account.balance]
            val accounts2 = Account.select { Account.id eq "$accountNumber2" }.single()[Account.balance]

            assert(accounts1.toDouble()).isEqualTo(0.00)
            assert(accounts2.toDouble()).isEqualTo(1000.00)
        }

        assert(results.size).isEqualTo(jobCount)
    }

    @Test
    fun testTransfer_ShouldHandleMultipleAccountTransfers() = runBlocking {
        val accounts = listOf(
            generateAccountNumber("AA"),
            generateAccountNumber("BB"),
            generateAccountNumber("DD"),
            generateAccountNumber("EE"),
            generateAccountNumber("FF"),
            generateAccountNumber("GG")
            )
        accounts.forEach { stageAccount(it, 1000.00) }
        val jobCount = 500

        val results = GlobalScope.massiveRun<UUID>(jobCount) {
            var srcIdx = -1
            var dstIdx = -1
            while(srcIdx == dstIdx) {
                srcIdx = Random.nextInt(0,3)
                dstIdx = Random.nextInt(0,3)
            }
            val src = accounts[srcIdx]
            val dst = accounts[dstIdx]

            println("Transferring from $src ===> $dst")
            repo.transfer(src, dst, 1.00)
        }

        assert(results.size).isEqualTo(jobCount)
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> CoroutineScope.massiveRun(jobCount: Int, action: suspend () -> Unit): List<T> {
        val results = mutableListOf<T>()
        val time = measureTimeMillis {
            val sem = Semaphore(0)
            val jobs = List(jobCount) {
                async {
                    sem.acquire()
                    delay(Random.nextLong(0, 5))

                    action()
                }
            }
            sem.release(jobCount)
            jobs.forEach {
                val result = it.await() as T
                if (result != null)
                    results.add(result)
            }
        }
        println("Completed $jobCount actions in $time ms")
        return results
    }

    private fun generateAccountNumber(countryCode: String): Iban {
        val accountNumber = (1..26)
            .map { Random.nextInt(0, 9) }
            .joinToString("")
        return Iban("${countryCode}00$accountNumber")
            .calculateChecksum()
    }
}