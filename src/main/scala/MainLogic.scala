import akka.NotUsed
import akka.stream.{ClosedShape, IOResult, Materializer}
import akka.stream.scaladsl.{FileIO, Flow, Framing, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.scaladsl.GraphDSL.Implicits.SourceShapeArrow
import akka.util.ByteString
import domain.{Event, EventInWrongOrder, MatchState, MatchStateAccumulator, ValidationError}
import converter.Json._

import java.nio.file.Paths
import scala.concurrent.Future


trait MainLogic {


  private val amountsEventsToReset = 3


  def flowWay(sourceFileName: String)(implicit m: Materializer) =
    source(sourceFileName)
      .via(parseEvents)
      .via(validateEventConsistency)
      .via(toJson)
      .runWith(sink)


  def runGraph(sourceFileName: String): RunnableGraph[NotUsed] =
    RunnableGraph.fromGraph(
      GraphDSL.create() {implicit builder =>
        val readRawEvent = builder.add(source(sourceFileName))
        val parseAndFilterOutCorrupted = builder.add(parseEvents)
        val filterOutNonConsistent = builder.add(validateEventConsistency)
        val convertToJson = builder.add(toJson)
        val output = builder.add(sink)

        readRawEvent ~> parseAndFilterOutCorrupted ~> filterOutNonConsistent ~> convertToJson ~> output
        ClosedShape
      }
    )


  private val source: String => Source[String, Future[IOResult]] =
    fileName =>
      FileIO
        .fromPath(Paths.get(fileName))
        .via(Framing.delimiter(ByteString("\n"), Int.MaxValue))
        .map(_.utf8String)


  val parseEvents: Flow[String, Event, NotUsed] =
    Flow[String]
      .map(EventParser.parseEvent)
      .mapConcat(_.event match {
        case Left(value) =>
          println("Failed to parse event: " + value)
          Nil
        case Right(value) => value :: Nil
      })


  private val validateEventConsistency: Flow[Event, Event, NotUsed] =
    Flow[Event]
      .statefulMapConcat {() =>
        var accumulator = MatchStateAccumulator.empty
        event =>
          EventValidationStreamWay.validate(event, accumulator.matchState) match {

            case Left(currentEvent: EventInWrongOrder) if accumulator.collectUnordered().size >= amountsEventsToReset =>
              val newAccumulator = resetHeadEvent(accumulator + currentEvent, amountsEventsToReset)
              accumulator = accumulator ++ newAccumulator
              newAccumulator.matchState.allEvents()

            case Left(e: ValidationError) =>
              accumulator = accumulator + e
              Nil

            case Right(consistentEvent) =>
              accumulator = accumulator + consistentEvent
              consistentEvent :: Nil
          }
      }


  private def resetHeadEvent(oldAccumulator: MatchStateAccumulator, amountsEventsToReset: Int): MatchStateAccumulator =
    oldAccumulator
      .collectUnordered(amountsEventsToReset)
      .foldLeft(MatchStateAccumulator.empty)((acc, el) =>
        acc + EventValidationStreamWay.validate(el, acc.matchState)) match {
      case MatchStateAccumulator(event, error) if event.isLessThanOne() => MatchStateAccumulator(MatchState.empty, error)
      case m@MatchStateAccumulator(_, _) => m
    }


  val toJson: Flow[Event, String, NotUsed] =
    Flow[Event].map(_.toJsonValue.stringify).map(e => Console.GREEN + e + Console.RESET)


  private val sink = Sink.foreach(println)
}

object MainLogic extends MainLogic
