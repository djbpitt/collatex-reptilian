package net.collatex.util

import sttp.shared
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import upickle.default.*
import sttp.tapir.json.upickle.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.netty.sync.NettySyncServer

case class Book(author: String, title: String, year: Int)

/*
{
  "author" : "Ronald",
  "title" : "stuff",
  "year" : 1
}
* */

object Book:
  given ReadWriter[Book] = macroRW

@main def jsonTapir(): Unit =

  val bookQuery: EndpointInput.Query[Book] = jsonQuery[Book]("book")

  // val bookEndpoint: EndpointIO[Book] = jsonBody[Book]

  val newEndpoint: ServerEndpoint[Any, shared.Identity] = endpoint.get
    .in("hello" / "world")
    .in(bookQuery)
    .out(stringBody)
    .handleSuccess(book => s"Hello, $book!")

  NettySyncServer()
    .port(8082)
    .addEndpoint(newEndpoint)
    .startAndWait()

/*
  curl
  -G
  -v
  --data-urlencode
  book@/Users/djb/IdeaProjects/collatex-reptilian/src/main/resources/sampleBook.json
  http://localhost:8082/hello/world
* */