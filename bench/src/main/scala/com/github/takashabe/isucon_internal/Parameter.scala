package com.github.takashabe.isucon_internal

import java.io._

import org.json4s._
import org.json4s.native.JsonMethods._

/**
  * パラメータJSONをマッピングするためのクラス
  */
case class UserSchema(name: String, email: String, password: String) extends Parameter
case class UserSchemas(parameters: List[UserSchema])

/**
  * マッピングクラスと他クラスとのインタフェースを提供する
  */
class Parameter {
  def generate(path: String): List[UserSchema] = {
    // パラメータJSONの読み込み
    val reader = new BufferedReader(new InputStreamReader(getClass.getResourceAsStream(path), "UTF-8"))
    val list = try {
      Iterator.continually(reader.readLine()).takeWhile(_ != null).toList
    } finally {
      reader.close()
    }

    // パラメータJSONをcase classにマッピングする
    implicit val defaultFormat = DefaultFormats
    val json = list.mkString("")
    val userSchemas = parse(json).extract[UserSchemas]
    userSchemas.parameters
  }
}
