package net.collatex.util

import sttp.shared
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import upickle.default.*
import sttp.tapir.json.upickle.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.netty.sync.NettySyncServer

case class Book(author: String, title: String, year: Int)

object Book:
  given ReadWriter[Book] = macroRW

@main def jsonTapir(): Unit =

  val bookBody: EndpointIO[Book] = jsonBody[Book]

  val newEndpoint: ServerEndpoint[Any, shared.Identity] = endpoint.post
    .in("hello" / "world")
    .in(bookBody)
    .out(bookBody)
    .handleSuccess(book => Book(book.author, title="Different title", book.year))

  NettySyncServer()
    .port(8082)
    .addEndpoint(newEndpoint)
    .startAndWait()

/*
  curl -X POST
  -v
  -d @/Users/djb/IdeaProjects/collatex-reptilian/src/main/resources/sampleBook.json http://localhost:8082/hello/world

  For GET, use, change method in newEndpoint to get
  and in() value as bookQuery, defined as
  val bookQuery: EndpointInput.Query[Book] = jsonQuery[Book]("book")
  and use:

  curl
  -G
  -v
  --data-urlencode
  book@/Users/djb/IdeaProjects/collatex-reptilian/src/main/resources/sampleBook.json
  http://localhost:8082/hello/world

* */