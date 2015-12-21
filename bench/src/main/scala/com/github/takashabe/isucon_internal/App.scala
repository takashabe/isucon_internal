package com.github.takashabe.isucon_internal

/**
  * エントリポイント
  */
object App {
  def main(args: Array[String]): Unit = {
    val cliParser = new CliParser
    val cliOption = cliParser.parse(args)

    // TODO: JSONとクラス文字列から動的にParameterインスタンスを生成する
    val parameter = new Parameter()
    val param = parameter.generate("com.github.takashabe.isucon_internal.IsuconBenchUserSchema", "Alice", "alice@example.com")

    println(parameter)
    println(param)

    // TODO: ScenarioManagerのキック
  }
}
