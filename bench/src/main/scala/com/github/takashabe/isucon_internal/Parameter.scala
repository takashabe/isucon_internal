package com.github.takashabe.isucon_internal

import spray.json._
import spray.json.DefaultJsonProtocol._

/**
  * マッピングクラスと他クラスとのインタフェースを提供する
  */
trait Parameter {
  // TODO: リフレクションでCliOption.paramJsonのクラスを取り回す
//  def generate(className: String): Parameter = {
//
//  }
}

/**
  * パラメータJSONをマッピングするためのクラス
  */
case class IsuconBenchUserSchema(name: String, email: String) extends Parameter
