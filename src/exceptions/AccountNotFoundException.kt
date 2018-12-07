package exceptions

import java.lang.RuntimeException

class AccountNotFoundException(message: String? = null) : RuntimeException(message)