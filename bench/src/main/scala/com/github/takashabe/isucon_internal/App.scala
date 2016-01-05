package com.github.takashabe.isucon_internal

import com.typesafe.scalalogging.LazyLogging

/**
  * エントリポイント
  */
object App extends LazyLogging {
  def main(args: Array[String]): Unit = {
    // CLIパース
    val cliParser = new CliParser
    val cliOption = cliParser.parse(args)

    // ベンチ用パラメータを取得
    // TODO: CLIからjsonパスを受け取るようにする
    val parameter = new Parameter
    val benchParameter = parameter.generate("/param.json")

    // TODO: ScenarioManagerのキック
    val manager = new ScenarioManager
    manager.run(benchParameter)
  }
}
