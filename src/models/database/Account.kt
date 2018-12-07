package uk.stumme.models.database

import org.jetbrains.exposed.sql.Table

object Account : Table() {
    val id = varchar("account_number", 30).primaryKey()
    val dateOpened = datetime("date_opened")
}