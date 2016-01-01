package com.github.takashabe.isucon_internal

import java.net.HttpCookie

import scalaj.http.HttpResponse

/**
  * HTTP Cookie、Parameterを扱う
  * 一連のシナリオ内で前回のレスポンスに含まれるCookieを再利用出来るようにする
  */
class Session(param: Parameter) {
  var cookies: List[HttpCookie] = List()

  /**
    * Responseとローカルに保存してあるクッキーを比較し、差分を更新する
    * @param res Response
    */
  def updateCookieWithResponse(res: HttpResponse[String]) = {
    for(resCookie <- res.cookies) {
      for(localCookie <- cookies) {
        if(localCookie.getName == resCookie.getName) {
          localCookie.setValue(resCookie.getValue)
          resCookie :: cookies
        }
      }
    }
  }

  // TODO: http client(cookie)を使い回す方法を検討する
  //         java版の方ではjettyのHttpClientがcookieを保持しており、それを使い回すことで
  //         リクエスト毎に前回のレスポンスで得られたcookieとリクエストに使うcookieを比較、更新出来るっぽい？
  def writeCookie() = {

  }
}
