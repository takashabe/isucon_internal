package com.github.takashabe.isucon_internal

import com.github.takashabe.isucon_internal.ResponseType._
import com.typesafe.scalalogging.LazyLogging

class Result (
  var valid: Boolean,
  val responses: Responses,
  var requests: Long,
  var elapsed_time: Long,
  var done: String,
  var violations: List[Violation]) extends LazyLogging
{
  def this() {
    this(valid = true, requests = 0, elapsed_time = 0, done = "", responses = new Responses, violations = List[Violation]())
  }

  def addResponse(r: ResponseType): Unit = {
    requests += 1
    r match {
      case SUCCESS => responses.success += 1
      case REDIRECT => responses.redirect += 1
      case FAILURE => responses.clientError += 1
      case ERROR => responses.serverError += 1
      case TIMEOUT => responses.exception += 1
    }
  }

  def addViolation(requestType: String, description: String): Unit = {
    violations = new Violation(requestType, description, 0) :: violations
  }

  def fail: Unit = {
    valid = false
  }
}

/**
  * レスポンス結果数
  *
  * @param success 2xx
  * @param redirect 3xx
  * @param clientError 4xx
  * @param serverError 5xx
  * @param exception リクエスト不正(タイムアウトなど)
  */
case class Responses(
  var success: Long,
  var redirect: Long,
  var clientError: Long,
  var serverError: Long,
  var exception: Long)
{
  def this() {
    this(0, 0, 0, 0, 0)
  }
}

/**
  * リクエスト違反情報を持つ
  *
  * @param requestType リクエスト種別
  * @param description 違反原因の詳細
  * @param number 違反数
  */
class Violation(requestType: String, description: String, number: Long) {
  override def toString(): String = {
    "requesttype:%s, description:%s, num:%d".format(requestType, description, number)
  }
}
