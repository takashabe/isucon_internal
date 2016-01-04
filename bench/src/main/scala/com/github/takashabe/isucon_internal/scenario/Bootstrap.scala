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
    // 0..2    -> Bootstrap
    // 3..9    -> Checker
    // 10....  -> Load
    val session = sessions.head
    val param = session.getParam.asInstanceOf[UserSchema]

    val session2 = sessions(1)
    val param2 = session.getParam.asInstanceOf[UserSchema]

    val session3 = sessions(2)
    val param3 = session.getParam.asInstanceOf[UserSchema]

    // 2ndユーザでログイン出来るかどうか
    {
      getAndCheck(null, "/login", "LOGIN GET 2ND USER", (check) => {
        check.isStatus(200)
      })

      val loginParams = Seq("email" -> param2.email, "password" -> param2.password)

      postAndCheck(session2, "/login", loginParams, "LOGIN POST 2ND USER", (check) => {
        check.isRedirect("/")
        if (check.hasViolation) {
          check.fatal("ログイン操作に対して正しいレスポンスが返りませんでした")
        }
      })
    }
  }
}
