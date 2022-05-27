import domain._

trait EventParser {

  val toEvents: Seq[String] => ParsedEventsResult =
    _.map(toEvent).foldLeft(ParsedEventsResult.empty)(_ + _)

  val toEvent: String => Either[Error, Event] =
    rawEvent =>
      for {
        bitFormatted            <- EventInBitFormat(rawEvent)
        event                   <- Event(bitFormatted, rawEvent)
      } yield event
}

object EventParser extends EventParser
