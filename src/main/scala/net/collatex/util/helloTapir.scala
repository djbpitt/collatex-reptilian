package net.collatex.util
// https://tapir.softwaremill.com/en/latest/tutorials/01_hello_world.html

import sttp.shared
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.netty.sync.NettySyncServer

@main def helloWorldTapir(): Unit =
  val helloWorldEndpoint: ServerEndpoint[Any, shared.Identity] = endpoint.get
    .in("hello" / "world")
    .in(query[String]("name"))
    .out(stringBody)
    .handleSuccess(name => s"Hello, $name!")
  val goodbyeWorldEndpoint = endpoint.get
    .in("goodbye" / "world")
    .in(query[String]("name"))
    .out(stringBody)
    .handleSuccess(name => s"Goodbye, $name!")

  NettySyncServer()
    .port(8082)
    .addEndpoint(helloWorldEndpoint)
    .addEndpoint(goodbyeWorldEndpoint)
    .startAndWait()

  // http://localhost:8082/hello/world?name=%22ronald%22
  // println(helloWorldEndpoint.show)
