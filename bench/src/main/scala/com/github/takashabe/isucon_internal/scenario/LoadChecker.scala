package com.github.takashabe.isucon_internal.scenario

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import com.github.takashabe.isucon_internal._

import scala.util.Random

/**
  * 負荷をかけつつ動作チェック
  */
class LoadChecker extends Scenario with ScenarioUtil {
  // ベンチ時間 10秒
  val DurationMillis = 10L * 1000

  /**
    * 実際にシナリオでチェックすべき項目を書く
    * @param sessions シナリオで使用するセッション
    */
  override def scenario(sessions: List[Session]): Unit = {
    // 0..2    -> Bootstrap
    // 3..9    -> LoadChecker
    // 10....  -> Load

    // セッションの取り出し
    val loadSessions = sessions.slice(3, 10)

    // 指定時間まで無限ループ
    val stopAt = LocalDateTime.now().plus(DurationMillis, ChronoUnit.MILLIS)
    while (true) {
      if (LocalDateTime.now().isAfter(stopAt)) {
        return
      }

      // 2セッションの選定(Random)
      val shuffled = Random.shuffle(loadSessions)
      val s1 = shuffled.head
      val p1 = getParam(s1)
      val s2 = shuffled(2)
      val p2 = getParam(s2)
      if (LocalDateTime.now().isAfter(stopAt)) {
        return
      }

      // s1でログイン
      get(s1, "/logout")
      get(s1, "/login")
      post(s1, "/login", getLoginForm(s1))
      get(s1, "/")
      if (LocalDateTime.now().isAfter(stopAt)) {
        return
      }

      // s1でログイン
      getStatus(s1, "/") match {
        case 200    => // Nothing
        case i: Int =>
          getAndCheck(s1, "/login", "LOGIN PAGE BECAUSE NOT LOGGED IN", (check) => check.isStatus(200))
          postAndCheck(s1, "/login", getLoginForm(s1), "LOGIN POST WHEN LOGGED OUT", check => check.isRedirect("/"))
          getAndCheck(s1, "/", "SHOW INDEX AFTER LOGIN", check => check.isStatus(200))
      }
      if (LocalDateTime.now().isAfter(stopAt)) {
        return
      }

      // s2でログイン
      getStatus(s2, "/") match {
        case 200    => // Nothing
        case i: Int =>
          getAndCheck(s2, "/login", "LOGIN PAGE BECAUSE NOT LOGGED IN", check => check.isStatus(200))
          postAndCheck(s2, "/login", getLoginForm(s2), "LOGIN POST WHEN LOGGED OUT", check => check.isRedirect("/"))
          getAndCheck(s2, "/", "SHOW INDEX AFTER LOGIN", check => check.isStatus(200))
      }
      if (LocalDateTime.now().isAfter(stopAt)) {
        return
      }

      val s2followingPath = "/user/%s".format(p2.id)
      val s2Name = getAndRead(s1, "/following", "#friends dl dd.friend-friend a[href=%s]".format(s2followingPath), 0, e => e.text())
      s2Name.isEmpty match {
        case true =>
          // 未フォローなのでフォローしてみる
          postAndCheck(s1, "/follow/%s".format(p2.id), Seq("" -> ""), "MAKE FOLLOW", check => {
            check.isRedirect("/")
            check.respondUntil(3000)
          })
          if (LocalDateTime.now().isAfter(stopAt)) {
            return
          }

          getAndCheck(s1, "/following", "FOLLOWING LIST AFTER MAKING FOLLOW", check => {
            check.isStatus(200)
            check.contentCheck("#friends dl dd.friend-friend a[href=%s]".format(s2followingPath), "フォローしたばかりのユーザが含まれていません", e => {
              e.attr("href") == s2followingPath
            })
          })

        case false =>
          // フォロー済みなのでフォロー先ユーザにツイートさせてみる
          val tweet = genTweet()
          postAndCheck(s2, "/tweet", tweet, "POST TWEET", check => {
            check.isRedirect("/")
            check.respondUntil(3000)
          })
          if (LocalDateTime.now().isAfter(stopAt)) {
            return
          }

          getAndCheck(s1, "/", "SEE FOLLOWING TWEET", check => {
            check.isStatus(200)
            check.contentCheck("#entry-comments.row.panel.panel-primary div.comment div.tweet", "フォローしているユーザのツイートが含まれていません", e => {
              e.text() == tweet.head._2
            })
          })
      }
    }
  }
}
