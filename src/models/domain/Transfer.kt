package uk.stumme.models.domain

import java.util.*

data class Transfer(val id: UUID, val source: String, val destination: String, val amount: Double)