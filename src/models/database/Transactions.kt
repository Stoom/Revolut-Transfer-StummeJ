package uk.stumme.models.database

import org.jetbrains.exposed.sql.Table
import java.util.*

object Transactions: Table() {
    val id = uuid("id").default(UUID.randomUUID())
    val sourceAccount = varchar("source", 30)
    val destinationAccount = varchar("destination", 30)
    val amount = decimal("amount", Int.MAX_VALUE - 2, 2)
}