package net.collatex.util
// https://tapir.softwaremill.com/en/latest/tutorials/01_hello_world.html

import sttp.tapir.*

@main def helloWorldTapir(): Unit =
  val helloWorldEndpoint = endpoint
    .get
    .in("hello" / "world")
    .in(query[String]("name"))

  println(helloWorldEndpoint.show)