package com.github.takashabe.isucon_internal

import java.net.{URI, URISyntaxException}
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import com.github.takashabe.isucon_internal.scenario._
import com.typesafe.scalalogging.LazyLogging
import org.jsoup.Jsoup
import org.jsoup.nodes._

import scala.collection.JavaConverters._
import scala.util.matching.Regex
import scalaj.http._

/**
  * 複数Scenarioを管理する
  */
class ScenarioManager extends LazyLogging {
  /**
    * ParameterのリストからSessionのリストを生成する
    *
    * @param params Parameterのリスト
    * @return Sessionのリスト
    */
  def createSession(params: List[Parameter]): List[Session] = {
    params.map(p => new Session(p))
  }

  /**
    * 複数Scenarioをどんな順番で実行するか定義する
    */
  def orders(): List[Scenario] = {
    List(
      new Init,
      new Bootstrap,
      new Load
    )
  }

  /**
    * Scenarioを実行する
    *
    * @param params Parameterのリスト
    */
  def run(params: List[Parameter]): Result = {
    // TODO どのクラスを実行するか選択可能にする
    val scenarios = orders()
    val sessions = createSession(params)
    val config = new Config("http", "192.168.33.10", 80, "isucon", 3*60*1000)

    val results = scenarios.map(s => s.execute(config, sessions))
    val mergeResult = Result.merge(results)

    results.map(r => r.valid match {
      case true  => // 正常終了
      case false => // r.violations.map(v => logger.debug(v.toString()))
    })
    logger.debug("Merge結果 = ")
    logger.debug(mergeResult.toString())

    mergeResult
  }
}

/**
  * 1シナリオを表す
  */
class Scenario extends LazyLogging {
  var started_at: LocalDateTime = _
  var stored_result: Result = _
  var config: Config = _
  var state: State = _

  val BlockInterval = 3

  /**
    * 実際にシナリオでチェックすべき項目を書く
    * @param sessions シナリオで使用するセッション
    */
  def scenario(sessions: List[Session]): Unit = {
    throw new AbstractMethodError
  }

  /**
    * シナリオ実行後にResultを返す
    * 一定以上レスポンスエラーが存在した場合にFailさせるなど
    * @param result 使い回しのResult
    * @return
    */
  def finishHook(result: Result): Result = {
    result
  }

  def execute(config: Config, sessions: List[Session]): Result = {
    this.config = config
    initExecute()
    try {
      scenario(sessions)
    } catch {
      case e: ScenarioAbortException => logger.info(e.toString)
    }
    finishHook(stored_result)
  }

  def initExecute(): Unit = {
    started_at = LocalDateTime.now()
    stored_result = new Result()
    stored_result.done = this.getClass.getSimpleName
    state = new State
  }

  def result(): Result = {
    stored_result.elapsed_time = started_at.until(LocalDateTime.now(), ChronoUnit.MILLIS)
    stored_result
  }

  def block(): Unit = {
    while(state.isRunning()) {
      try {
        Thread.sleep(BlockInterval)
      } catch {
        case e: InterruptedException => // Ignore
      }
    }
  }

  def sleep(timeoutMs: Long): Unit = {
    var now = LocalDateTime.now()
    val destTime = now.plus(timeoutMs, ChronoUnit.MILLIS)
    while (destTime.isAfter(now)) {
      try {
        Thread.sleep(now.until(destTime, ChronoUnit.MILLIS))
      } catch {
        case e: InterruptedException => // Ignore
      }
      now = LocalDateTime.now()
    }
  }

  def get(session: Session, path: String): Unit = {
    getAndCheck(session, path, null, null)
  }

  def post(session: Session, path: String, params: Seq[(String, String)]): Unit = {
    postAndCheck(session, path, params, null, null)
  }

  /**
    * GETリクエストでCheckerを走らせる
    *
    * @param session リクエストに使うSession
    * @param path リクエスト先URI
    * @param requestType リクエスト種別
    * @param checkerCallback Checker本体
    */
  def getAndCheck(
     session: Session,
     path: String,
     requestType: String,
     checkerCallback: Checker => Unit): Unit =
  {
    val req = Http(config.uri(path))
      .timeout(connTimeoutMs = 1000, readTimeoutMs = config.GetTimeout)
      .cookies(session.getCookies)

    requestAndCheck(req, session, path, requestType, checkerCallback)
  }

  /**
    * POSTリクエストでCheckerを走らせる
    *
    * @param session リクエストに使うSession
    * @param path リクエスト先URI
    * @param params POSTパラメータ
    * @param requestType リクエスト種別
    * @param checkerCallback Checker本体
    */
  def postAndCheck(
     session: Session,
     path: String,
     params: Seq[(String, String)],
     requestType: String,
     checkerCallback: Checker => Unit): Unit =
  {
    val req = Http(config.uri(path))
      .timeout(connTimeoutMs = 1000, readTimeoutMs = config.GetTimeout)
      .cookies(session.getCookies)
      .postForm(params)

    requestAndCheck(req, session, path, requestType, checkerCallback)
  }

  def requestAndCheck(
    request: HttpRequest,
    session: Session,
    path: String,
    requestType: String,
    checkerCallback: Checker => Unit): Unit =
  {
    val response = request.asString

    logger.info("リクエスト種別:%s, URI:%s, ステータス:%d".format(requestType, path, response.code))

    // レスポンス数を加算
    if (response.is2xx) {
      stored_result.addResponse(ResponseType.SUCCESS)
    } else if (response.is3xx) {
      stored_result.addResponse(ResponseType.REDIRECT)
    } else if (response.is4xx) {
      stored_result.addResponse(ResponseType.FAILURE)
    } else {
      stored_result.addResponse(ResponseType.ERROR)
    }

    // Checkerコールバックの実行
    if (checkerCallback != null) {
      val checker = new Checker(stored_result, requestType, path, config, response)
      checkerCallback(checker)
    }

    session.updateCookieWithResponse(response.cookies)
  }
}

/**
  * シナリオ中に期待するレスポンスが帰ってこなかった
  */
class ScenarioAbortException extends RuntimeException

/**
  * Scenarioで使用するためのassert群
  */
class Checker(
  result: Result,
  requestType: String,
  path: String,
  config: Config,
  responseTime: Long,
  response: HttpResponse[String]) extends Scenario
{
  def this(result: Result, requestType: String, path: String, config: Config, response: HttpResponse[String]) {
    this(result, requestType, path, config, 0, response)
  }

  def document(): Document = {
    Jsoup.parse(response.body)
  }

  def hasViolations: Boolean = {
    result.violations.nonEmpty
  }

  def fatal(message: String): Unit = {
    addViolation(message)
    throw new ScenarioAbortException
  }

  /**
    * Resultに違反情報を追加する
    *
    * @param description 違反情報詳細
    */
  def addViolation(description: String): Unit = {
    result.addViolation(requestType, description)
  }

  /**
    * レスポンスに一定時間以上かかっているか
    *
    * @param millis 制限時間
    */
  def respondUntil(millis: Long): Unit = {
    if (responseTime > millis) {
      addViolation("アプリケーションが %d ミリ秒以内に応答しませんでした".format(millis))
    }
  }

  /**
    * HTTPステータスコードが一致しているかどうか
    *
    * @param code 期待するステータスコード
    */
  def isStatus(code: Int): Unit = {
    if (response.code != code) {
      addViolation("パス '%s' へのレスポンスコード %d が期待されていましたが %d でした".format(path, code, response.code))
    }
  }

  /**
    * リダイレクトが正しく行われているか
    *
    * @param path 本来のリダイレクト先
    */
  def isRedirect(path: String): Unit = {
    // HTTPステータスコードは一時リダイレクトのものしか認めない
    val tempRedirectCode = List(302, 303, 307)
    if (!tempRedirectCode.contains(response.code)) {
      addViolation("レスポンスコードが一時リダイレクトのもの(302, 303, 307)ではなく %d でした".format(response.code))
      return
    }

    // Locationヘッダがないものは認めない
    val location = response.location
    location match {
      case Some(l) if l.equals(config.uri(path)) || l.equals(config.uriDefaultPort(path)) =>
        // OK
        return
      case Some(_) =>
        // 次の判定に進む
      case None =>
        addViolation("Locationヘッダがありません")
        return
    }

    try {
      val uri = new URI(location.get)
      val h = uri.getHost
      val p = if (uri.getPath != null) uri.getPath else "/"
      if (h==null || h.equals(config.host) && p.equals(path)) {
        // OK
        return
      }
    } catch {
      case e: URISyntaxException => // invalid syntax
    }
    addViolation("リダイレクト先が %s でなければなりませんが %s でした".format(path, location.get))
  }

  /**
    * ContentLengthの長さが一致しているか
    *
    * @param expectedLength 期待する長さ
    */
  def isContentLength(expectedLength: Int): Unit = {
    val length = response.headers("Content-Length")

    response.headers.get("Content-Length") match {
      case Some(l) if Integer.parseInt(l.head) == expectedLength => // OK
      case Some(l) => addViolation("パス %s に対するレスポンスのサイズが正しくありません: %s bytes".format(path, l.head))
      case None    => addViolation("リクエストパス %s に対して Content-Length がありませんでした".format(path))
    }
  }

  /**
    * 特定のスタイルシートへのパスが存在するか
    *
    * @param expectedPath 期待するパス
    */
  def hasStyleSheet(expectedPath: String): Unit = {
    val es = document().head().getElementsByTag("link").asScala
    if (!es.exists(p => p.attr("rel") == "stylesheet" && p.attr("href") == expectedPath)) {
      addViolation("スタイルシートのパス %s への参照がありません".format(expectedPath))
    }
  }

  /**
    * 指定のDOM要素が存在するか
    *
    * @param selector 期待するDOM要素
    */
  def exist(selector: String): Unit = {
    if (document().select(selector).size() == 0) {
      addViolation("指定のDOM要素 '%s' が見付かりません".format(selector))
    }
  }

  /**
    * 指定のDOM要素要素がn回出現するか
    *
    * @param selector 期待するDOM要素
    * @param num 期待する出現回数
    */
  def someExist(selector: String, num: Int): Unit = {
    if (document().select(selector).size() != num) {
      addViolation("指定のDOM要素 '%s' が %d 回表示されるはずですが、正しくありません".format(selector, num))
    }
  }

  /**
    * 指定のDOM要素が存在しないか
    *
    * @param selector 期待するDOM要素
    */
  def missing(selector: String): Unit = {
    if (document().select(selector).size() > 0) {
      addViolation("DOM要素 '%s' は存在しないはずですが、表示されています".format(selector))
    }
  }

  /**
    * DOM内に任意の文字列が存在するか
    *
    * @param selector DOM要素
    * @param text 期待する文字列
    */
  def content(selector: String, text: String): Unit = {
    val es = document().select(selector).asScala
    if (!es.exists(e => e.hasText && e.text().trim.equals(text))) {
      if (es.isEmpty) {
        addViolation("DOM要素 '%s' で文字列 '%s' を持つものが見付かりません".format(selector, text))
      } else {
        addViolation("DOM要素 '%s' に文字列 '%s' がセットされているはずですが、'%s' となっています".format(selector, text, es.head.text()))
      }
    }
  }

  /**
    * DOM内に任意の文字列が存在しないか
    *
    * @param selector DOM要素
    * @param text 期待する文字列
    */
  def contentMissing(selector: String, text: String): Unit = {
    val es = document().select(selector).asScala
    if (es.exists(e => e.hasText && e.text().trim.equals(text))) {
      addViolation("DOM要素 '%s' に文字列 '%s' をもつものは表示されないはずですが、表示されています".format(selector, text))
    }
  }

  /**
    * DOM内に任意の文字列が存在するか
    * ※ 改行を含む大きいコンテンツが対象
    *
    * @param selector DOM要素
    * @param text 期待する文字列
    */
  def contentLongText(selector: String, text: String): Unit = {
    // 改行(\n, <br />)を取り払った上で比較
    val shrink = text.split("\n").map(s => s.trim).mkString
    val es = document().select(selector).asScala
    for (e <- es) {
      val fullText = e.html().trim.split("<(br|BR|Br|bR) */?>").map(s => s.trim).mkString
      if (fullText.equals(shrink)) {
        // OK
        return
      }
    }

    addViolation("入力されたはずのテキストがDOM要素 '%s' に表示されていません".format(selector))
  }

  /**
    * DOM内に任意の正規表現にマッチする文字列が存在するか
    *
    * @param selector DOM要素
    * @param regex 期待する正規表現
    */
  def contentMatch(selector: String, regex: Regex): Unit = {
    val es = document().select(selector).asScala
    if (es.size == 1) {
      regex.findFirstIn(es.head.text()) match {
        case Some(_) => // Nothing
        case None    => addViolation("DOM要素 '%s' のテキストが正規表現 '%s' にマッチしません".format(selector, regex.toString()))
      }
    } else {
      // DOM内を再帰的にチェックする
      for (e <- es) {
        regex.findFirstIn(e.text()) match {
          case Some(s) => return // OK
          case None    => // Nothing
        }
      }
      addViolation("DOM要素 '%s' の中に、テキストが正規表現 '%s' にマッチするものが見つかりません".format(selector, regex.toString()))
    }
  }

  /**
    * 汎用的なコンテンツチェック
    * ※ 呼び出し側のチェック関数に依存
    *
    * @param selector DOM要素
    * @param message Violationに格納する文字列
    * @param check チェック関数
    */
  def contentCheck(selector: String, message: String, check: Element => Boolean): Unit = {
    val es = document().select(selector).asScala
    for (e <- es) {
      // 一度でもOKなら通過
      if (check(e)) {
        return
      }
    }
    addViolation(message)
  }

  /**
    * 任意の属性の内容が正しいか
    *
    * @param selector DOM要素
    * @param attributeName 属性名
    * @param text 期待する文字列
    */
  def attribute(selector: String, attributeName: String, text: String): Unit = {
    val es = document().select(selector).asScala
    if (!es.exists(e => e.attr(attributeName).equals(text))) {
      addViolation("DOM要素 '%s' のattribute %s の内容が %s になっていません".format(selector, attributeName, text))
    }
  }
}

/**
  * シナリオの実行状態
  */
class State(var running: Boolean = true) {
  def init(): Unit = {
    running = true
  }

  def isRunning(): Boolean = {
    running
  }

  def finish(): Unit = {
    running = false
  }
}
