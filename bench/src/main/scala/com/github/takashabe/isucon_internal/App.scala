package com.github.takashabe.isucon_internal

/**
  * エントリポイント
  */
object App {
  def main(args: Array[String]): Unit = {
    // CLIパース
    val cliParser = new CliParser
    val cliOption = cliParser.parse(args)

    // ベンチ用パラメータを取得
    val parameter = new Parameter
    val benchParameter = parameter.generate("/param.json")

    println(benchParameter)

    // TODO: ScenarioManagerのキック
  }
}
