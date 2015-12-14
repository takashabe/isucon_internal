package com.github.takashabe.isucon_internal

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import scalaj.http._

/**
  * 複数Scenarioを管理する
  */
class ScenarioManager {
}

/**
  * 1シナリオを表す
  */
class Scenario {
  var started_at: LocalDateTime = _
  var stored_result: Result = _
  var config: Config = _
  var state: State = _

  val BlockInterval = 3

  /**
    * 実際にシナリオでチェックすべき項目を書く
    * @param sessions シナリオで使用するセッション
    */
  def scenario(sessions: List[Session]): Unit = {
    throw new AbstractMethodError
  }

  /**
    * シナリオ実行後にResultを返す
    * 一定以上レスポンスエラーが存在した場合にFailさせるなど
    * @param result 使い回しのResult
    * @return
    */
  def finishHook(result: Result): Result = {
    result
  }

  def execute(config: Config, sessions: List[Session]): Result = {
    this.config = config
    finishHook(stored_result)
  }

  def initExecute(): Unit = {
    started_at = LocalDateTime.now()
    stored_result = new Result()
    stored_result.done = this.getClass.getSimpleName
    state = new State
  }

  def result(): Result = {
    stored_result.elapsed_time = started_at.until(LocalDateTime.now(), ChronoUnit.MILLIS)
    stored_result
  }

  def block(): Unit = {
    while(state.isRunning()) {
      try {
        Thread.sleep(BlockInterval)
      } catch {
        case e: InterruptedException => // Ignore
      }
    }
  }

  def sleep(timeoutMs: Long): Unit = {
    var now = LocalDateTime.now()
    val destTime = now.plus(timeoutMs, ChronoUnit.MILLIS)
    while (destTime.isAfter(now)) {
      try {
        Thread.sleep(now.until(destTime, ChronoUnit.MILLIS))
      } catch {
        case e: InterruptedException => // Ignore
      }
      now = LocalDateTime.now()
    }
  }

  def get(session: Session, path: String): Unit = {
    getAndCheck(session, path, null, null)
  }

  def getAndCheck(
     session: Session,
     path: String,
     requestType: String,
     checkerCallback: Checker => Unit): Unit =
  {
    val response = Http(path)
      .timeout(connTimeoutMs = 1000, readTimeoutMs = config.GetTimeout)
      .asString

    // レスポンス数を加算
    if (response.is2xx) {
      stored_result.addResponse(ResponseType.SUCCESS)
    } else if (response.is3xx) {
      stored_result.addResponse(ResponseType.REDIRECT)
    } else if (response.is4xx) {
      stored_result.addResponse(ResponseType.FAILURE)
    } else {
      stored_result.addResponse(ResponseType.ERROR)
    }

    // Checkerコールバックの実行
    val checker = new Checker(stored_result, requestType, path, config, response)
    checkerCallback(checker)
  }
}

/**
  * Scenarioで使用するためのassert群
  */
class Checker(
  result: Result,
  requestType: String,
  path: String,
  config: Config,
  responseTime: Long,
  response: HttpResponse[String]) extends Scenario
{
  def this(result: Result, requestType: String, path: String, config: Config, response: HttpResponse[String]) {
    this(result, requestType, path, config, 0, response)
  }

  def addViolation(description: String): Unit = {
    result.addViolation(requestType, description)
  }

  def isStatus(code: Int): Unit = {
    if (response.code != code) {
      this.addViolation("パス '%s' へのレスポンスコード %d が期待されていましたが %d でした".format(path, response.code, code))
    }
  }
}

/**
  * セッションを使い回すためにクラス化
  */
class Session {

}

/**
  * シナリオの実行状態
  */
class State(var running: Boolean = true) {
  def init(): Unit = {
    running = true
  }

  def isRunning(): Boolean = {
    running
  }

  def finish(): Unit = {
    running = false
  }
}
