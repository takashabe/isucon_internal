package com.github.takashabe.isucon_internal

import java.net.HttpCookie

import com.typesafe.scalalogging.LazyLogging

/**
  * HTTP Cookie、Parameterを扱う
  * 一連のシナリオ内で前回のレスポンスに含まれるCookieを再利用出来るようにする
  */
class Session(param: Parameter) extends LazyLogging {
  var cookies: List[HttpCookie] = List()

  def getParam: Parameter = {
    param
  }

  /**
    * Responseとローカルに保存してあるクッキーを比較し、差分を更新する
    * @param compare Response
    */
  def updateCookieWithResponse(compare: IndexedSeq[HttpCookie]) = {
    if (cookies.isEmpty) {
      cookies = compare.toList
    }

    for(compareCookie <- compare; localCookie <- cookies) {
      if(compareCookie.getName == localCookie.getName) {
        localCookie.setValue(compareCookie.getValue)
//        cookies = compareCookie :: cookies
      }
    }
  }

  /**
    * ローカルのcookiesに保存されているcookieを返す
    *   リクエスト送信時に前回のレスポンスで得られたcookieで初期化する場合、
    *   シーケンシャルにリクエストを組み立てる必要があるので注意
    */
  def getCookies: List[HttpCookie] = {
    cookies
  }
}
