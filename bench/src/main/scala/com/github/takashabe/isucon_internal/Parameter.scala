package com.github.takashabe.isucon_internal

import scala.reflect.runtime.universe._

import spray.json._
import spray.json.DefaultJsonProtocol._

/**
  * パラメータJSONをマッピングするためのクラス
  */
case class IsuconBenchUserSchema(name: String, email: String) extends Parameter

/**
  * マッピングクラスと他クラスとのインタフェースを提供する
  */
class Parameter {
  def generate(className: String, args: Any*): Parameter = {
    // リフレクションでSchemaインスタンスを生成して返す
    val mirror = scala.reflect.runtime.currentMirror
    val classSymbol = mirror.staticClass(className)
    val classMirror = mirror.reflectClass(classSymbol)
    val constructorSymbol = classSymbol.typeSignature.decl(termNames.CONSTRUCTOR).asMethod
    val constructorMethodMirror = classMirror.reflectConstructor(constructorSymbol)

    constructorMethodMirror(args: _*).asInstanceOf[Parameter]
  }
}
