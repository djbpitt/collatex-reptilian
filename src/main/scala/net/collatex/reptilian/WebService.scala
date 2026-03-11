package net.collatex.reptilian

import ox.*
import ox.flow.*
import sttp.model.HeaderNames
import sttp.tapir.*
import sttp.tapir.server.netty.sync.*
import sttp.tapir.server.interceptor.log.DefaultServerLog
import com.typesafe.scalalogging.{Logger, StrictLogging}
import net.collatex.reptilian.TokenEnum.{Token, TokenSep}
import net.collatex.reptilian.display.DisplayFunctions.*
import sttp.shared.Identity

import scala.collection.mutable.ListBuffer

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
          // Temporarily fake plain-text input
          val inputString = s"Hi, Ronald!"
          /* Test with XML-invalid character data
          TODO: Once this accepts real input, write real tests
          val inputString = s"Hi, Ronald!${1.toChar}"
           */
          if XmlValidator.isValid(inputString) then
            val gTa = Vector[TokenEnum](
              Token("The ", "the", 0, 0, Map()),
              Token("red ", "red", 0, 1, Map()),
              Token("cat", "cat", 0, 2, Map()),
              TokenSep("sep0", "sep0", 0, 3),
              Token("The ", "the", 1, 4, Map()),
              Token("black ", "black", 1, 5, Map()),
              Token("cat", "cat", 1, 6, Map())
            )
            val aps: List[AlignmentPoint] = List(
              AlignmentPoint(gTa, Map(0 -> TokenRange(0, 1, gTa), 1 -> TokenRange(4, 5, gTa))),
              AlignmentPoint(gTa, Map(0 -> TokenRange(1, 2, gTa), 1 -> TokenRange(5, 6, gTa))),
              AlignmentPoint(gTa, Map(0 -> TokenRange(2, 3, gTa), 1 -> TokenRange(6, 7, gTa)))
            )
            val ar: AlignmentRibbon = AlignmentRibbon(
              ListBuffer.from(aps)
            )
            val xmlResult = displayDispatch(
              ar,
              gTa,
              List(Siglum("Ronald"), Siglum("David")),
              List(),
              List(),
              Map("--format" -> Set("xml")))
            xmlResult
          else Left("Input data cannot be expressed as XML (invalid characters)")
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
