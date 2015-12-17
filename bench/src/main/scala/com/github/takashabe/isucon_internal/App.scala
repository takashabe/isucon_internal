package com.github.takashabe.isucon_internal

/**
  * エントリポイント
  */
object App {
  def main(args: Array[String]): Unit = {
    val cliParser = new CliParser
    val cliOption = cliParser.parse(args)

    // TODO: JSONとクラス文字列から動的にParameterインスタンスを生成する

    // TODO: ScenarioManagerのキック
  }
}
