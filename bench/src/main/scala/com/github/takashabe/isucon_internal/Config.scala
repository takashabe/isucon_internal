package com.github.takashabe.isucon_internal

/**
  * ベンチマーク対象の設定を管理する
  */
class Config(
  val scheme: String,
  val host: String,
  port: Int,
  agent: String,
  runningTime: Int)
{
  val MaxRunningTime: Int = 3 * 60 * 1000

  // GET/POSTリクエストのタイムアウト値
  val GetTimeout: Int = 30 * 1000
  val PostTimeout: Int = 30 * 1000

  val DefaultUserAgent: String = "Isucon internal"

  def this() = {
    this(scheme = "http", host = "localhost", port = 0, agent = "Isucon internal", runningTime = 3 * 60 * 1000)
  }

  def uri(path: String): String = {
    if (port == 0) {
      uriDefaultPort(path)
    } else {
      String.format("%s://%s:%s%s", scheme, host, port.toString, path)
    }
  }

  def uriDefaultPort(path: String): String = {
    String.format("%s://%s%s", scheme, host, path)
  }
}
