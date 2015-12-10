package com.github.takashabe.isucon_internal

object ResponseType {
  case object SUCCESS extends ResponseType(1)
  case object REDIRECT extends ResponseType(2)
  case object FAILURE extends ResponseType(3)
  case object ERROR extends ResponseType(4)
  case object EXCEPTION extends ResponseType(5)

  def valueOf(a: Int) = a match {
    case SUCCESS.value   => SUCCESS
    case REDIRECT.value  => REDIRECT
    case FAILURE.value   => FAILURE
    case ERROR.value     => ERROR
    case EXCEPTION.value => EXCEPTION
  }
}

sealed abstract class ResponseType(a: Int) {
  val value = a
}
