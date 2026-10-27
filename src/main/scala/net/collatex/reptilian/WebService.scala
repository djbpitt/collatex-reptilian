package net.collatex.reptilian

import ox.*
import ox.flow.*
import sttp.model.{HeaderNames, MediaType}
import sttp.tapir.*
import sttp.tapir.server.netty.sync.*
import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.generic.auto.*
import sttp.tapir.json.upickle.*
import com.typesafe.scalalogging.{Logger, StrictLogging}
import net.collatex.reptilian.GtaBuilder.{BuildConfig, JsonDataForAlignment, buildFromWitnessData, jsonToWitnessData}
import net.collatex.reptilian.TokenEnum.{Token, TokenSep}
import net.collatex.reptilian.display.DisplayFunctions.*
import sttp.shared.Identity
import sttp.tapir.Schema
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.netty.sync
implicit val ujsonValueSchema: Schema[ujson.Value] = Schema.any
import sttp.tapir.CodecFormat
import sttp.model.MediaType

import scala.collection.mutable.ListBuffer

// Declarative APIs for endpoints
val jsonPostData: EndpointIO[JsonDataForAlignment] = jsonBody[JsonDataForAlignment]

val xmlEndpoint =
  endpoint.post
    .in("xml") // URL ends in .../xml
    .in(jsonPostData)
    .out(streamTextBody(OxStreams)(CodecFormat.Xml())) // returned mime type application/xml (not for svg, etc.)
    .out(header(HeaderNames.CacheControl, "no-cache"))
    .errorOut(stringBody)

val jsonEndpoint =
  endpoint.post
    .in("json")
    .in(jsonPostData)
    .out(streamTextBody(OxStreams)(CodecFormat.Json())) // returned mime type application/json
    .out(header(HeaderNames.CacheControl, "no-cache"))
    .errorOut(stringBody)

object CustomCodecs:
  val tei: CodecFormat = new CodecFormat {
    override def mediaType: MediaType = MediaType.unsafeParse("application/tei+xml")
  }
  val graphml: CodecFormat = new CodecFormat {
    override def mediaType: MediaType = MediaType.unsafeParse("application/graphml+xml")
  }
  val xhtml: CodecFormat = new CodecFormat {
    override def mediaType: MediaType = MediaType.ApplicationXhtml
  }
  val svg: CodecFormat = new CodecFormat {
    override def mediaType: MediaType = MediaType.unsafeParse("image/svg+xml")
  }
  val dot: CodecFormat = new CodecFormat {
    override def mediaType: MediaType = MediaType.unsafeParse("text/vnd.graphviz")
  }

val teiEndpoint =
  endpoint.post
    .in("tei") // URL ends in .../tei
    .in(jsonPostData)
    .out(streamTextBody(OxStreams)(CustomCodecs.tei))
    .out(header(HeaderNames.CacheControl, "no-cache"))
    .errorOut(stringBody)

val graphMlEndpoint =
  endpoint.post
    .in("graphml") // URL ends in .../tei
    .in(jsonPostData)
    .out(streamTextBody(OxStreams)(CustomCodecs.graphml))
    .out(header(HeaderNames.CacheControl, "no-cache"))
    .errorOut(stringBody)

val svgEndpoint =
  endpoint.post
    .in("svg") // URL ends in .../tei
    .in(jsonPostData)
    .out(streamTextBody(OxStreams)(CustomCodecs.svg))
    .out(header(HeaderNames.CacheControl, "no-cache"))
    .errorOut(stringBody)

val svgrichEndpoint =
  endpoint.post
    .in("svg-rich") // URL ends in .../tei
    .in(jsonPostData)
    .out(streamTextBody(OxStreams)(CustomCodecs.svg))
    .out(header(HeaderNames.CacheControl, "no-cache"))
    .errorOut(stringBody)

val ribbonEndpoint =
  endpoint.post
    .in("ribbon") // URL ends in .../tei
    .in(jsonPostData)
    .out(streamTextBody(OxStreams)(CustomCodecs.xhtml))
    .out(header(HeaderNames.CacheControl, "no-cache"))
    .errorOut(stringBody)

val htmlHEndpoint =
  endpoint.post
    .in("html-h")
    .in(jsonPostData)
    .out(streamTextBody(OxStreams)(CustomCodecs.xhtml))
    .out(header(HeaderNames.CacheControl, "no-cache"))
    .errorOut(stringBody)

val htmlVEndpoint =
  endpoint.post
    .in("html-v")
    .in(jsonPostData)
    .out(streamTextBody(OxStreams)(CustomCodecs.xhtml))
    .out(header(HeaderNames.CacheControl, "no-cache"))
    .errorOut(stringBody)

val dotEndpoint =
  endpoint.post
    .in("dot") // URL ends in .../dot
    .in(jsonPostData)
    .out(streamTextBody(OxStreams)(CustomCodecs.dot)) // mime type text/plain
    .out(header(HeaderNames.CacheControl, "no-cache"))
    .errorOut(stringBody)

val tableHEndpointbase =
  endpoint.post
    .in(jsonPostData)
    .out(streamTextBody(OxStreams)(CodecFormat.TextPlain())) // mime type text/plain
    .out(header(HeaderNames.CacheControl, "no-cache"))
    .errorOut(stringBody)

val tableEndpoint = tableHEndpointbase.in("table")
val tableHEndpoint = tableHEndpointbase.in("table-h")

val tableVEndpoint =
  endpoint.post
    .in("table-v")
    .in(jsonPostData)
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

@main def webServerMain(): Unit = {
  val config = loadResolvedConfig().getOrElse(throw new Exception("Could not read config.yaml"))
  val cfg = BuildConfig.Default // TODO: depends on default config.yaml filename

  def addEndpointTemplate(
      endpointToBind: sttp.tapir.endpoint.EndpointType[
        Unit, // URL endpoint (e.g., .in("xml"))
        JsonDataForAlignment, // JSON input supplied by caller (.in(jsonPostData))
        String, // set codec and mediaType (e.g., .out(streamTextBody(OxStreams)(CodecFormat.Xml())))
        OxStreams.BinaryStream, // fixed header for cache control (.out(header(HeaderNames.CacheControl, "no-cache")))
        OxStreams // fixed error handler (.errorOut(stringBody))
      ],
      dispatcher: String
  ): ServerEndpoint.Full[Unit, Unit, JsonDataForAlignment, String, OxStreams.BinaryStream, OxStreams, Identity] = {
    endpointToBind.handle { jsonInput =>
      if XmlValidator.isUnicodeValid(jsonInput.toString) then // Validates only for Unicode, not for markup
        val results = for { // Gta, List[Siglum]
          witnesses <- jsonToWitnessData(cfg, jsonInput)
          out <- buildFromWitnessData(witnesses, config.defaultColors)
          root = createAlignmentRibbon(out.head, Set(), false)
        } yield (
          root,
          out
        ) // (out = Vector[TokenEnum] (Gta), List[Siglum], List[colors], List[fonts])
        results match {
          case Left(_) =>
            throw new Exception(
              "Computing alignment and Gta failed"
            ) // TODO: Handle correctly for web service
          case Right(x) =>
            val ar = x.head
            val gTa = x._2.head
            val sigla = x._2._2 // TODO: WTF
            val xmlResult =
              displayDispatch(ar, gTa, sigla, config.defaultColors, List(), Map("--format" -> Set(dispatcher)))
            xmlResult
        }
      else Left("Input data cannot be expressed as XML (invalid characters)")
    }
  }

  case class endpointToBind(
      endpointId: sttp.tapir.endpoint.EndpointType[
        Unit, // URL endpoint (e.g., .in("xml"))
        JsonDataForAlignment, // JSON input supplied by caller (.in(jsonPostData))
        String, // set codec and mediaType (e.g., .out(streamTextBody(OxStreams)(CodecFormat.Xml())))
        OxStreams.BinaryStream, // fixed header for cache control (.out(header(HeaderNames.CacheControl, "no-cache")))
        OxStreams // fixed error handler (.errorOut(stringBody))
      ],
      displayString: String
  )

  val endpointsToBind: Seq[endpointToBind] =
    Seq(
      endpointToBind(xmlEndpoint, "xml"),
      endpointToBind(teiEndpoint, "tei"),
      endpointToBind(graphMlEndpoint, "graphml"),
      endpointToBind(svgEndpoint, "svg"),
      endpointToBind(svgrichEndpoint, "svg-rich"),
      endpointToBind(ribbonEndpoint, "ribbon"),
      endpointToBind(htmlHEndpoint, "table-html-h"),
      endpointToBind(htmlVEndpoint, "table-html-v"),
      endpointToBind(dotEndpoint, "dot"),
      endpointToBind(jsonEndpoint, "json"),
      endpointToBind(tableHEndpoint, "table-h"),
      endpointToBind(tableEndpoint, "table"),
      endpointToBind(tableVEndpoint, "table-v")
    )

  supervised {
      val serverBinding =
        endpointsToBind
          .foldLeft(NettySyncServer(WebServer.serverOptions))((acc: NettySyncServer, newItem: endpointToBind) =>
            acc.addEndpoint(addEndpointTemplate(newItem.endpointId, newItem.displayString))
          )
          .port(config.serverPort)
          .host("localhost")
          .start()

      // Access the port in NettySyncServerBinding
      val boundPort = serverBinding.port
      val boundHost = serverBinding.hostName

      // println(s"Server started → http://$boundHost:$boundPort/products/stream/xml")
      WebServer.log.info(s"Server started → http://$boundHost:$boundPort")

      // Keep the supervised scope alive until process termination
      never
  }
}
