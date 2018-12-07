package exceptions

import java.lang.RuntimeException

class InvalidArgumentException(val argument: String, message: String? = null)
    : RuntimeException(message)