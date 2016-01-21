package com.github.takashabe.isucon_internal.scenario

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import com.github.takashabe.isucon_internal._

import scala.util.Random

/**
  * ガンガン負荷をかける系
  */
class Load extends Scenario with ScenarioUtil {
  /**
    * 実際にシナリオでチェックすべき項目を書く
    * @param sessions シナリオで使用するセッション
    */
  override def scenario(sessions: List[Session]): Unit = {
    // 指定時間まで無限ループ
    val stopAt = LocalDateTime.now().plus(LoadDurationMills, ChronoUnit.MILLIS)
    while (true) {
      if (LocalDateTime.now().isAfter(stopAt)) {
        return
      }

      // 3セッションの選定(Random)
      val s1 = sessions(Random.nextInt(sessions.size))
      val s2 = sessions(Random.nextInt(sessions.size))
      val s3 = sessions(Random.nextInt(sessions.size))
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

      // s1でtweet
      postAndCheck(s1, "/tweet", genTweet(), "TWEET POST", (check) => {
        check.respondUntil(3000)
      })
      if (LocalDateTime.now().isAfter(stopAt)) {
        return
      }

      // s2でログイン
      get(s2, "/logout")
      get(s2, "/login")
      post(s2, "/login", getLoginForm(s2))
      get(s2, "/")
      if (LocalDateTime.now().isAfter(stopAt)) {
        return
      }

      // s2でfollowingページ
      getAndCheck(s2, "/following", "GET FOLLOWING", (check) => {
        check.isStatus(200)
      })
      if (LocalDateTime.now().isAfter(stopAt)) {
        return
      }

      // s3でログイン
      get(s3, "/logout")
      get(s3, "/login")
      post(s3, "/login", getLoginForm(s3))
      get(s3, "/")
      if (LocalDateTime.now().isAfter(stopAt)) {
        return
      }

      // s3でfollowersページ
      getAndCheck(s3, "/followers", "GET FOLLOWERS", (check) => {
        check.isStatus(200)
      })
      if (LocalDateTime.now().isAfter(stopAt)) {
        return
      }
    }
  }
}
