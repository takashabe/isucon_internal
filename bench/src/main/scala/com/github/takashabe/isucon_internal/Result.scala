package com.github.takashabe.isucon_internal

class Result {
  var response: Response = Response()

  /**
    * Response結果を加算する
    * Scenario走査後にレスポンスを加算することを期待する
    */
  def addResponse(r: Response): Unit = {
    response.success += r.success
    response.redirect += r.redirect
    response.failure += r.failure
    response.error += r.error
    response.timeout += r.timeout
  }
}

case class Response(
                     success: Long = 0,
                     redirect: Long = 0,
                     failure: Long = 0,
                     error: Long = 0,
                     timeout: Long = 0
                   )
