package org.styx.exceptions

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
case class InvalidExecutionException(message: String) extends RuntimeException(message)
