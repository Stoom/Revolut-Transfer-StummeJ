package uk.stumme.account

import java.lang.RuntimeException

class AccountNotFoundException(message: String? = null) : RuntimeException(message)