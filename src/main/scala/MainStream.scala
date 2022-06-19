import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Flow, Framing, Sink, Source}
import akka.util.ByteString
import domain.{Event, MatchStateAccumulator, ValidationError}

import java.nio.file.Paths
import scala.concurrent.Future
import converter.Json._

import scala.annotation.tailrec

object MainStream {

  implicit val system = ActorSystem(Behaviors.empty, "foo")


  val source: String => Source[String, Future[IOResult]] =
    fileName =>
      FileIO
        .fromPath(Paths.get(fileName))
        .via(Framing.delimiter(ByteString("\n"), Int.MaxValue))
        .map(_.utf8String)


  val filterOutCorruptedEvents: Flow[String, Event, NotUsed] =
    Flow[String]
      .map(EventParser.parseEvent)
      .mapConcat(_.event match {
        case Left(value) =>
          println("Failed to parse event: " + value)
          Nil
        case Right(value) => value :: Nil
      })


  val validateEventConsistency: Flow[Event, Event, NotUsed] =
    Flow[Event]
      .statefulMapConcat {() =>
        var accumulator = MatchStateAccumulator.empty
        event =>
          EventValidationStreamWay.validate(event, accumulator.matchState) match {
            case Left(e: ValidationError) =>
              println("Event are not consistent: " + e)
              accumulator = accumulator + (event, e)
              Nil

            case Right(consistentEvent) if accumulator.containsUnordered() =>
              val (fixedEvents, accumulatorNewState) = tryToRecoverEventsOrder(accumulator.collectUnordered(), List.empty[Event], accumulator + consistentEvent)
              accumulator = accumulatorNewState
              consistentEvent :: fixedEvents

            case Right(consistentEvent) =>
              accumulator = accumulator + consistentEvent
              consistentEvent :: Nil
          }
      }


  @tailrec
  def tryToRecoverEventsOrder(unorderedEvents: List[Event],
                              fixedEvents: List[Event],
                              accumulator: MatchStateAccumulator): (List[Event], MatchStateAccumulator) =
    unorderedEvents match {
      case Nil => (fixedEvents, accumulator)
      case list =>
        list
          .map(e => EventValidationStreamWay.validate(e, accumulator.matchState))
          .collect {case Right(validEvent) => validEvent} match {
          case Nil => (fixedEvents, accumulator)
          case validEvents =>
            val newStateAccumulator = validEvents.foldLeft(accumulator)(_ + _)
            tryToRecoverEventsOrder(
              newStateAccumulator.collectUnordered(),
              fixedEvents ++ validEvents,
              newStateAccumulator)

        }
    }

  val toJson: Flow[Event, String, NotUsed] = Flow[Event].map(_.toJsonValue.stringify)
  val sink = Sink.foreach(println)


  def main(args: Array[String]): Unit = {
    source("src/main/resources/sample2.txt")
      .via(filterOutCorruptedEvents)
      .via(validateEventConsistency)
      .via(toJson)
      .runWith(sink)
  }
}
