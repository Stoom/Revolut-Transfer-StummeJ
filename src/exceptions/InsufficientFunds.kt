package exceptions

import java.lang.RuntimeException

class InsufficientFunds(message: String? = null) : RuntimeException(message)