package net.collatex.util

import ox.*
import ox.flow.*

import sttp.tapir.*
import sttp.tapir.server.netty.sync.*

import scala.concurrent.duration.*
import java.time.LocalDate
import java.util.concurrent.TimeUnit

// ─── Domain ────────────────────────────────────────────────────────────────

case class Product(
                    id: Long,
                    name: String,
                    price: BigDecimal,
                    availableSince: LocalDate
                  )

// ─── XML Streaming ─────────────────────────────────────────────────────────

def productToXmlFragment(p: Product): String =
  s"""  <product id="${p.id}">
     |    <name>${xml.Utility.escape(p.name)}</name>
     |    <price>${p.price}</price>
     |    <available>${p.availableSince}</available>
     |  </product>
     |""".stripMargin

val xmlHeader =
  """<?xml version="1.0" encoding="UTF-8"?>
    |<products>
    |""".stripMargin

val xmlFooter = "\n</products>\n"

// ─── Data Source (simulated slow database / external API) ──────────────────

def fetchProducts(): Flow[Product] =
  Flow
    .fromIterator(
      (1 to 120).iterator.map { i =>
        Product(
          id = i,
          name = s"Product #$i",
          price = BigDecimal(19.99 + i * 1.25),
          availableSince = LocalDate.now().minusDays(i % 365)
        )
      }
    )
    .throttle(10, FiniteDuration.apply(80, TimeUnit.MILLISECONDS))     // simulate slow streaming source, 10 items, 80ms delay

// ─── Tapir Endpoint ────────────────────────────────────────────────────────

val streamingXmlEndpoint: Endpoint[Unit, Unit, String, Flow[Chunk[Byte]], OxStreams] =
  endpoint.get
    .in("products" / "stream" / "xml")
    .out(streamTextBody(OxStreams)(CodecFormat.Xml()))
    // .outHeader(HeaderNames.CacheControl, "no-cache")
    .errorOut(stringBody)

def streamingXmlLogic(u: Unit)(using Ox): Either[String, Flow[Chunk[Byte]]] = {
  try {
    val stringFlow: Flow[String] = Flow.usingEmit { emit =>
      emit(xmlHeader)
      fetchProducts().runForeach { product =>
        emit(productToXmlFragment(product))
      }
      emit(xmlFooter)
    }

    val byteFlow = stringFlow.map(s =>
      Chunk.fromArray(s.getBytes(java.nio.charset.StandardCharsets.UTF_8))
    )

    Right(byteFlow)
  } catch {
    case t: Throwable => Left(s"Streaming failed: ${t.getMessage}")
  }
}

// ─── Server ─────────────────────────────────────────────────────────────────

@main def runStreamingXmlServer(): Unit =
  supervised {
    val serverBinding = NettySyncServer()
      .port(8083)
      .host("localhost")
      .addEndpoint(
        streamingXmlEndpoint.handle { unitInput =>
          // summon[Ox] provides the scope for the Flow logic
          streamingXmlLogic(unitInput)(using summon[Ox])
        }
      )
      .start()

    // Correct way to access the port in NettySyncServerBinding
    val boundPort = serverBinding.port
    val boundHost = serverBinding.hostName

    println(s"Server started → http://$boundHost:$boundPort/products/stream/xml")
    println("Try: curl -N http://localhost:8081/products/stream/xml")

    // Keep the supervised scope alive until process termination
    never
  }