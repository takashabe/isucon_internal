package com.github.takashabe.isucon_internal

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
  * 1シナリオを表す
  */
abstract class Scenario {
  var started_at: LocalDateTime
  var stored_result: Result
  var config: Config
  var state: State

  val BlockInterval = 3

  /**
    * 実際にシナリオでチェックすべき項目を書く
    * @param sessions シナリオで使用するセッション
    */
  def scenario(sessions: List[Session])

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

  def getAndCheck(session: Session, path: String, value: Null, b: Boolean, value1: Null, value2: Null): Unit = {

  }

  def get(session: Session, path: String): Unit = {
    getAndCheck(session, path, null, b = false, null, null)
  }
}

/**
  * Scenarioで使用するためのassert群
  */
abstract class Checker {
  def scenario(scenario: Scenario)
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
  def isRunning(): Boolean = {
    running
  }

  def finish(): Unit = {
    running = false
  }
}
