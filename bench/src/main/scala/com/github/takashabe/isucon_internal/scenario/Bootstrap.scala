package com.github.takashabe.isucon_internal.scenario

import com.github.takashabe.isucon_internal._

/**
  * 初期コンテンツチェック
  */
class Bootstrap extends Scenario {
  /**
    * 実際にシナリオでチェックすべき項目を書く
    * @param sessions シナリオで使用するセッション
    */
  override def scenario(sessions: List[Session]): Unit = {
    {
      getAndCheck(null, "/login", "LOGIN GET 2ND USER", (check) => {
        check.isStatus(200)
      })
    }
  }
}
