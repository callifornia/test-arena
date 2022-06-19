import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Flow, Framing, Sink, Source}
import akka.util.ByteString
import domain.{Event, EventInWrongOrder, MatchState, MatchStateAccumulator, ValidationError}

import java.nio.file.Paths
import scala.concurrent.Future
import converter.Json._


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

            case Left(currentEvent: EventInWrongOrder) if accumulator.collectUnordered().size >= 3 =>
              val newAccumulator = resetHeadEvent(accumulator + currentEvent)
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


  def resetHeadEvent(oldAccumulator: MatchStateAccumulator): MatchStateAccumulator =
    oldAccumulator
      .collectUnordered()
      .takeRight(3)
      .foldLeft(MatchStateAccumulator.empty)((acc, el) =>
        acc + EventValidationStreamWay.validate(el, acc.matchState)) match {
      case MatchStateAccumulator(event, error) if event.isLessThanOne() => MatchStateAccumulator(MatchState.empty, error)
      case m@MatchStateAccumulator(_, _)                           => m
    }


  val toJson: Flow[Event, String, NotUsed] = Flow[Event].map(_.toJsonValue.stringify).map(e => Console.GREEN + e + Console.RESET)
  val sink = Sink.foreach(println)


  def main(args: Array[String]): Unit = {
    source("src/main/resources/sample2.txt")
      .via(filterOutCorruptedEvents)
      .via(validateEventConsistency)
      .via(toJson)
      .runWith(sink)
  }
}
