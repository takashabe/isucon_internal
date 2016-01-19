package com.github.takashabe.isucon_internal.scenario

import com.github.takashabe.isucon_internal._

/**
  * 初期コンテンツチェック
  */
class Init extends Scenario {
  override def finishHook(result: Result): Result = {
    // Checkerレスポンスに不正や違反があれば失格
    if (result.responses.exception > 0 || result.violations.nonEmpty) {
      result.fail
    }
    result
  }

  /**
    * 実際にシナリオでチェックすべき項目を書く
    * @param sessions シナリオで使用するセッション
    */
  override def scenario(sessions: List[Session]): Unit = {
    // 初期化
    getAndCheck(new Session(null), "/initialize", "INITIALIZE DATA", (check) => {
      check.isStatus(200)
      check.respondUntil(60L * 1000)
    })
  }
}
