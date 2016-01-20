package com.github.takashabe.isucon_internal.scenario

import com.github.takashabe.isucon_internal._

import scala.util.Random

/**
  * 初期コンテンツチェック
  */
class Bootstrap extends Scenario {
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
    // 0..2    -> Bootstrap
    // 3..9    -> Checker
    // 10....  -> Load
    val session = sessions.head
    val param = session.getParam.asInstanceOf[UserSchema]

    val session2 = sessions(1)
    val param2 = session2.getParam.asInstanceOf[UserSchema]

    val session3 = sessions(2)
    val param3 = session3.getParam.asInstanceOf[UserSchema]

    // 2ndユーザでログイン出来るか
    {
      getAndCheck(session2, "/login", "LOGIN GET 2ND USER", (check) => {
        check.isStatus(200)
      })

      val loginParams = Seq("email" -> param2.email, "password" -> param2.password)

      postAndCheck(session2, "/login", loginParams, "LOGIN POST 2ND USER", (check) => {
        check.isRedirect("/")
        if (check.hasViolations) {
          check.fatal("ログイン操作に対して正しいレスポンスが返りませんでした")
        }
      })
    }

    // 3rdユーザでログイン出来るか
    {
      getAndCheck(session3, "/login", "LOGIN GET 3RD USER", (check) => {
        check.isStatus(200)
      })

      val loginParams = Seq("email" -> param3.email, "password" -> param3.password)

      postAndCheck(session3, "/login", loginParams, "LOGIN POST 3RD USER", (check) => {
        check.isRedirect("/")
        if (check.hasViolations) {
          check.fatal("ログイン操作に対して正しいレスポンスが返りませんでした")
        }
      })
    }

    // 未ログイン状態で"/"のリダイレクトが正しいか
    getAndCheck(session, "/", "SHOULD LOGIN AT FIRST", (check) => {
        check.isRedirect("/login")
        if (check.hasViolations) {
          check.fatal("未ログインでトップページへのアクセスが正しいリダイレクトになっていません")
        }
    })

    // ログインフォームの表示が正しいか
    getAndCheck(session, "/login", "LOGIN PAGE", (check) => {
      check.isStatus(200)

      check.someExist("form input[type=text]", 1)
      check.someExist("form input[type=password]", 1)
      check.someExist("form input[type=submit]", 1)
      if (check.hasViolations) {
        check.fatal("ログインフォームが正常に表示されていません")
      }
    })

    // 1stユーザでログイン出来るか
    {
      val loginParams = Seq("email" -> param.email, "password" -> param.password)

      postAndCheck(session, "/login", loginParams, "LOGIN POST", (check) => {
        check.isRedirect("/")
        if (check.hasViolations) {
          check.fatal("ログイン処理に対して正しいレスポンスが返りませんでした")
        }
      })
    }

    // 1stユーザでログイン後indexページのコンテンツチェック
    getAndCheck(session, "/", "INDEX AFTER LOGIN", (check) => {
      check.isStatus(200)

      check.hasStyleSheet("/css/bootstrap.min.css")

      check.content("dd#prof-name", param.name)
      check.content("dd#prof-email", param.email)

      check.someExist("dd#prof-following a", 1)
      check.attribute("dd#prof-following a", "href", "/following")
      check.someExist("dd#prof-followers a", 1)
      check.attribute("dd#prof-followers a", "href", "/followers")

      check.contentMatch("dd#prof-following a", """\d""".r)
      check.contentMatch("dd#prof-followers a", """\d""".r)

      // TODO timelineのコンテンツチェック

      if (check.hasViolations) {
        check.fatal("トップページが正しく表示されていません")
      }
    })

    // スタイルシートが正しいか
    getAndCheck(session, "/css/bootstrap.min.css", "STYLE SHEET CHECK", (check) => {
      check.isStatus(200)
      check.isContentLength(122540)
      if (check.hasViolations) {
        check.fatal("スタイルシートが取得できません")
      }
    })

    // 2ndユーザでログイン後のindexページコンテンツチェック
    getAndCheck(session2, "/", "INDEX AFTER LOGIN 2ND USER", (check) => {
      check.isStatus(200)
      check.hasStyleSheet("/css/bootstrap.min.css")
      check.content("dd#prof-name", param2.name)

      check.exist("dd#prof-email")
    })

    // 3rdユーザでログイン後のindexページコンテンツチェック
    getAndCheck(session3, "/", "INDEX AFTER LOGIN 3RD USER", (check) => {
      check.isStatus(200)
      check.hasStyleSheet("/css/bootstrap.min.css")
      check.content("dd#prof-name", param3.name)

        check.exist("dd#prof-email")
    })

    // tweet後のリダイレクトが正しいか
    {
      val params = Seq("content" -> Random.nextInt(Integer.MAX_VALUE).toString)
      postAndCheck(session, "/tweet", params, "POST NEW TWEET", (check) => {
        check.isRedirect("/")
      })
    }

    // 他ユーザのページを閲覧した際に内容が正しいか
    {
      // フォロー済みのユーザページが正しいか
      getAndCheck(session, "/user/%d".format(param3.id), "PROFILE FROM FOLLOW USER", (check) => {
        check.content("dd#prof-name", param3.name)
        check.content("dd#prof-email", param3.email)

        check.missing("form#follow-form")
      })

      // 未フォローのユーザページが正しいか
      getAndCheck(session, "/user/%d".format(param2.id), "PROFILE FROM NON-FOLLOW USER", (check) => {
        check.content("dd#prof-name", param2.name)
        check.content("dd#prof-email", param2.email)

        check.exist("form#follow-form")
      })
    }

    // TODO: プロフィール変更テスト ※webapp側でuserテーブルにprofile入れる必要アリ

    // TODO: フォローしていないがフォローされているユーザのuserページでツイートが表示されるかどうか

    // TODO: フォローしたユーザがfollowingページに表示されるかどうか

    // TODO: フォローしたユーザが呟いたツイートがindexに表示されるかどうか

    // TODO: フォローされているユーザがfollowersページに表示されるかどうか

    // ログアウト後、"/"のリダイレクトが正しいか
    {
      getAndCheck(session, "/logout", "LOGOUT 1ST USER", (check) => {
        check.isRedirect("/login")
        check.isRedirect("/")
      })

      getAndCheck(session, "/", "INDEX AFTER LOGOUT", (check) => {
        check.isRedirect("/login")
      })
    }
  }
}
