package com.github.takashabe.isucon_internal

/**
  * ベンチマーク対象の設定を管理する
  */
class Config(scheme: String, host: String, port: Int, agent: String, runningTime: Long) {
  val MaxRunningTime: Long = 3 * 60 * 1000

  // GET/POSTリクエストのタイムアウト値
  val GetTimeout: Long = 30 * 1000
  val PostTimeout: Long = 30 * 1000

  val DefaultUserAgent: String = "Isucon internal"

  def this() = {
    this(scheme = "http", host = "localhost", port = 0, agent = DefaultUserAgent, runningTime = MaxRunningTime)
  }

  def uri(path: String): String = {
    if (port == 0) {
      uriDefaultPort(path)
    } else {
      String.format("%s://%s:%d%s", scheme, host, port, path)
    }
  }

  def uriDefaultPort(path: String): String = {
    String.format("%s://%s%s", scheme, host, path)
  }
}
