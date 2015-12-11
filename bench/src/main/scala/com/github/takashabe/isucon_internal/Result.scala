package com.github.takashabe.isucon_internal

class Result(
              valid: Boolean,
              response: Response,
              requests: Long,
              var elapsed_time: Long,
              var done: String,
              var violations: List[Violation]) {
  def this() {
    this(valid = true, requests = 0, elapsed_time = 0, done = "", response = new Response)
  }

  def addResponse(r: Response): Unit = {
    response.success += r.success
    response.redirect += r.redirect
    response.failure += r.failure
    response.error += r.error
    response.timeout += r.timeout
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
case class Response(success: Long, redirect: Long, failure: Long, error: Long, timeout: Long) {
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
