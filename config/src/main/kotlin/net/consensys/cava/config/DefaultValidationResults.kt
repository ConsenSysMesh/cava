package net.consensys.cava.config

import net.consensys.cava.config.schema.ValidationResults

class DefaultValidationResults : ValidationResults {

  val errorMessages = mutableListOf<String>()

  override fun getErrors(): List<String> = errorMessages
}
