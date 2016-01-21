package com.github.takashabe.isucon_internal.scenario

import com.github.takashabe.isucon_internal._

import scala.util.Random

trait ScenarioUtil {
  val LoadDurationMills = 5L * 1000

  def getParam(session: Session): UserSchema = {
    session.getParam.asInstanceOf[UserSchema]
  }

  def getLoginForm(session: Session): Seq[(String, String)] = {
    val param = session.getParam.asInstanceOf[UserSchema]
    Seq("email" -> param.email, "password" -> param.password)
  }

  def genTweet(): Seq[(String, String)] = {
    Seq("content" -> Random.nextInt(Integer.MAX_VALUE).toString)
  }
}
