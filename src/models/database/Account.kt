package uk.stumme.models.database

import org.jetbrains.exposed.sql.Table

object Account : Table() {
    val id = varchar("account_number", 30).primaryKey()
    val balance = decimal("balance", Int.MAX_VALUE, 2)
    val dateOpened = datetime("date_opened")
}