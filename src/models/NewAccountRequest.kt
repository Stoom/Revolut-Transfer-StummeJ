package uk.stumme.models

data class NewAccountRequest(val countryCode: String, val initialDeposit: Double = 0.00)