package net.collatex.util

import cats.effect
import cats.effect.implicits.genTemporalOps
import org.http4s.MediaType
import sttp.shared
import sttp.tapir.*
import sttp.tapir.CodecFormat.Xml
import sttp.tapir.generic.auto.*
import upickle.default.*
import sttp.tapir.json.upickle.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.netty.sync.NettySyncServer
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.server.ServerEndpoint.Full

case class Book(author: String, title: String, year: Int)

object Book:
  given ReadWriter[Book] = macroRW

def getXmlStream: fs2.Stream[fs2.Pure, Byte] =
  val header = fs2.Stream.emit("<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>").through(fs2.text.utf8.encode)
  val footer = fs2.Stream.emit("</root>").through(fs2.text.utf8.encode)
  // Simulated data stream
  val data = fs2.Stream.emits(Seq("<item>1</item>", "<item>2</item>"))
    .through(fs2.text.utf8.encode)
  header ++ data ++ footer

@main def jsonTapir(): Unit =

  val bookBody: EndpointIO[Book] = jsonBody[Book]

  val newEndpoint: ServerEndpoint[Any, shared.Identity] = endpoint.post
    .in("hello" / "world")
    .in(bookBody)
    .out(bookBody)
    .handleSuccess(book => Book(book.author, title="Different title", book.year))

  val xmlEndpoint: ServerEndpoint[Fs2Streams[effect.IO], effect.IO] = endpoint.post // 1. Ensure effect is IO
    .in("hello" / "xml")
    .in(bookBody)
    .out(
      streamBinaryBody(Fs2Streams[cats.effect.IO])(CodecFormat.Xml())
    )
    .serverLogicSuccess { _ =>
      // 2. Wrap the stream in IO.pure
      // 3. Use .covary[IO] to convert Stream[Pure, ...] to Stream[IO, ...]
      cats.effect.IO.pure(getXmlStream.covary[cats.effect.IO])
    }

  // 2026-02-26 RESUME HERE:
  // Need to switch to OxStreams because netty doesn't work with cats.effect streams
  
  NettySyncServer()
    .port(8082)
    .addEndpoint(newEndpoint)
    // .addEndpoint(xmlEndpoint)
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