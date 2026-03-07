package net.collatex.reptilian

import ox.*
import ox.flow.*
import sttp.model.HeaderNames
import sttp.tapir.*
import sttp.tapir.server.netty.sync.*
import sttp.tapir.server.interceptor.log.DefaultServerLog
import com.typesafe.scalalogging.{Logger, StrictLogging}
import sttp.shared.Identity

// Declarative APIs for endpoints
// TODO: Format (only one for web service) expressed as endpoint, not part of JSON input
val xmlEndpoint: Endpoint[Unit, Unit, String, Flow[Chunk[Byte]], OxStreams] =
  endpoint.get
    .in("xml") // URL ends in .../xml
    .out(streamTextBody(OxStreams)(CodecFormat.Xml())) // mime type application/xml (not for svg)
    .out(header(HeaderNames.CacheControl, "no-cache"))
    .errorOut(stringBody)

val txtEndpoint: Endpoint[Unit, Unit, String, Flow[Chunk[Byte]], OxStreams] =
  endpoint.get
    .in("table") // URL ends in .../table
    .out(streamTextBody(OxStreams)(CodecFormat.TextPlain())) // mime type text/plain
    .out(header(HeaderNames.CacheControl, "no-cache"))
    .errorOut(stringBody)

object WebServer extends StrictLogging:
  val log: Logger = logger // Alias because logger property is protected
  val serverOptions: NettySyncServerOptions = NettySyncServerOptions.customiseInterceptors
    .serverLog(
      DefaultServerLog[Identity](
        doLogWhenReceived = msg => logger.debug(msg),
        doLogWhenHandled = (msg, ex) => ex.fold(logger.debug(msg))(e => logger.error(msg, e)),
        doLogAllDecodeFailures = (msg, _) => logger.warn(msg),
        doLogExceptions = (msg, ex) => logger.error(msg, ex),
        noLog = ()
      )
    )
    .options

// NB: Extend name of main method because
// main method webServer() plus object WebServer confuses the compiler
@main def webServerMain(): Unit =
  supervised {
    val serverBinding = NettySyncServer(WebServer.serverOptions)
      .port(8083)
      .host("localhost")
      .addEndpoint(
        xmlEndpoint.handle { unitInput =>
          val writer = new java.io.StringWriter()
          val e = <p>Hi, Ronald!</p>
          // Parameters: writer, node, encoding, xmlDecl, doctype
          scala.xml.XML.write(writer, e, "UTF-8", true, null)
          val xmlString = writer.toString
          Right(
            Flow.fromValues(
              Chunk.fromArray(xmlString.getBytes(java.nio.charset.StandardCharsets.UTF_8))
            )
          )
        }
      )
      .addEndpoint(
        txtEndpoint.handle { unitInput =>
          val e = "Hi, Ronald!"
          Right(
            Flow.fromValues(
              Chunk.fromArray(
                e.getBytes(java.nio.charset.StandardCharsets.UTF_8)
              )
            )
          )
        }
      )
      .start()

    // Access the port in NettySyncServerBinding
    val boundPort = serverBinding.port
    val boundHost = serverBinding.hostName

    // println(s"Server started → http://$boundHost:$boundPort/products/stream/xml")
    WebServer.log.info(s"Server started → http://$boundHost:$boundPort")

    // Keep the supervised scope alive until process termination
    never
  }
