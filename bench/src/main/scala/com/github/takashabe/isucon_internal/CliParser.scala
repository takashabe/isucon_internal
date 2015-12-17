package com.github.takashabe.isucon_internal

import java.io.File

import scopt.OptionParser

case class CliOption(
  host: String = "localhost",
  port: Int = 0,
  schema : String = "com.github.takashabe.isucon_internal.IsuconBenchUserParameter",
  paramJson: File = new File("./param.json"),
  runningTime: Int = 3 * 60 * 1000,
  agent: String = "Isucon internal")

class CliParser {
  def parse(args: Array[String]): CliOption = {
    val parser = new OptionParser[CliOption]("isucon internal") {
      head("isucon internal", "1.0")

      opt[String]('h', "host") optional() action { (x, o) =>
        o.copy(host = x)
      } text "target bench IP Addr"

      opt[Int]('p', "port") optional() action { (x, o) =>
        o.copy(port = x)
      } text "target bench port number"

      opt[String]('s', "schema") optional() action { (x, o) =>
        o.copy(schema = x)
      } text "bench parameter schema class name"

      opt[File]('f', "file") optional() action { (x, o) =>
        o.copy(paramJson = x)
      } text "bench parameter json file"

      opt[Int]('t', "time") optional() action { (x, o) =>
        o.copy(runningTime = x)
      } text "running bench time"

      opt[String]('a', "agent") optional() action { (x, o) =>
        o.copy(agent = x)
      } text "user agent"
    }

    parser.parse(args, CliOption()) match {
      case Some(x) => x
      case None    => sys.exit(1)
    }
  }
}
