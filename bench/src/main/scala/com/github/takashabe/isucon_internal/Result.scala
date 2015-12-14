package com.github.takashabe.isucon_internal

import com.github.takashabe.isucon_internal.ResponseType._

class Result(
  valid: Boolean,
  response: Responses,
  var requests: Long,
  var elapsed_time: Long,
  var done: String,
  var violations: List[Violation])
{
  def this() {
    this(valid = true, requests = 0, elapsed_time = 0, done = "", response = new Responses, violations = List[Violation]())
  }

  def addResponse(r: ResponseType): Unit = {
    requests += 1
    r match {
      case SUCCESS => response.success += 1
      case REDIRECT => response.redirect += 1
      case FAILURE => response.failure += 1
      case ERROR => response.error += 1
      case TIMEOUT => response.timeout += 1
    }
  }

  def addViolation(requestType: String, description: String): Unit = {
    new Violation(requestType, description, 0) :: violations
  }
}

/**
  * レスポンス結果数
  *
  * @param success 2xx
  * @param redirect 3xx
  * @param failure 4xx
  * @param error 5xx
  * @param timeout リクエストタイムアウト
  */
case class Responses(
  var success: Long,
  var redirect: Long,
  var failure: Long,
  var error: Long,
  var timeout: Long)
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
class Violation(requestType: String, description: String, number: Long)
