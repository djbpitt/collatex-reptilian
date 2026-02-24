package net.collatex.util
// https://tapir.softwaremill.com/en/latest/tutorials/01_hello_world.html

import sttp.shared
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

@main def helloWorldTapir(): Unit =
  val helloWorldEndpoint: ServerEndpoint[Any, shared.Identity] = endpoint.get
    .in("hello" / "world")
    .in(query[String]("name"))
    .out(stringBody)
    .handleSuccess(name => s"Hello, $name!")

  println(helloWorldEndpoint.show)
