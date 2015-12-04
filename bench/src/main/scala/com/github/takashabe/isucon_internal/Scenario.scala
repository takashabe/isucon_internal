package com.github.takashabe.isucon_internal

abstract class Scenario extends Driver {

}

/**
  * Scenarioで使用するためのassert群
  */
abstract class Checker {
  def scenario(scenario: Scenario)
}

class Driver {
  def requestAndCheck(session:Session, path:String): Unit = {
  }
}

class Session {

}