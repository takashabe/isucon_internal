package com.github.takashabe.isucon_internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.github.takashabe.isucon_internal.ResponseType._
import com.typesafe.scalalogging.LazyLogging

class Result (
  var valid: Boolean,
  val responses: Responses,
  var requests: Long,
  var elapsed_time: Long,
  var done: String,
  var violations: List[Violation])
{
  def this() {
    this(valid = true, responses = new Responses, requests = 0, elapsed_time = 0, done = "", violations = List[Violation]())
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

  /**
    * 違反情報を追加する。追加する際に既存と同じ違反であれば回数をインクリメントする
    *
    * @param requestType
    * @param description
    */
  def addViolation(requestType: String, description: String): Unit = {
    val violationType = (requestType, description)
    val vs = violations.collect {case v if v.getType() == violationType => v}
    vs.isEmpty match {
      case true  => violations = new Violation(requestType, description, 1) :: violations
      case false => vs.foreach(v => v.number += 1)
    }
  }

  def fail: Unit = {
    valid = false
  }

  /**
    * ResultをJSON化して返す
    */
  def toJson(): String = {
    val mapper = new ObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
  }


  override def toString(): String = {
    "valid:%s, responses:%s, requests: %s, elapsed: %s, done: %s, violations: %s".format(
      valid, responses, requests, elapsed_time, done, violations
    )
  }
}

object Result {
  /**
    * Resultの結果を足した新たなResultを返す
    *
    * @param results
    * @return
    */
  def merge(results: List[Result]): Result = {
    var valid = true
    var requests = 0L
    var elapsed = 0L
    var res = new Responses()
    var violations = List[Violation]()
    for(r <- results) {
      valid = valid && r.valid
      elapsed += r.elapsed_time
      requests += r.requests
      res = mergeResponses(res, r.responses)
      violations = updateViolations(violations, r.violations)
    }
    new Result(valid, res, requests, elapsed, "", violations)
  }

  /**
    * Responses同士のパラメータを足した新たなResponsesを返す
    *
    * @param r1 Responses
    * @param r2 Responses
    * @return
    */
  def mergeResponses(r1: Responses, r2: Responses): Responses = {
    new Responses(
      r1.success + r2.success,
      r1.redirect + r2.redirect,
      r1.clientError + r2.clientError,
      r1.serverError + r2.serverError,
      r1.exception + r2.exception
    )
  }

  /**
    * 同一の違反情報があるものをまとめ、数をカウントする
    * @param origin Violations
    * @param compare Violations
    * @return
    */
  def updateViolations(origin: List[Violation], compare: List[Violation]): List[Violation] = {
    // 両方に存在するものを返り値に寄せて返す
    var res = origin
    for (c <- compare) {
      if (res.isEmpty) {
        res = c :: res
      } else {
        var notfound = true
        for (r <- res) {
          if (c.getType() == r.getType()) {
            r.number += 1
            notfound = false
          }
        }
        if (notfound) {
          res = c :: res
        }
      }
    }
    res
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
case class Violation(requestType: String, description: String, var number: Long) {
  def getType(): (String, String) = {
    (requestType, description)
  }

  override def toString(): String = {
    //"requesttype:%s, description:%s, num:%d".format(requestType, description, number)
    "requesttype:%s, num:%d".format(requestType, number)
  }
}
