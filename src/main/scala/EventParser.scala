import domain._

trait EventParser {

  val toEvents: Seq[String] => ParsedEventsResult =
    _.map(toEvent).foldLeft(ParsedEventsResult.empty)(_ + _)

  val parseEvent: String => EventParsed =
    rawEvent => EventParsed(toEvent(rawEvent))

  val toEvent: String => Either[Error, Event] =
    rawEvent =>
      for {
        bitFormatted            <- EventInBitFormat(rawEvent)
        event                   <- Event(bitFormatted, rawEvent)
      } yield event
}

object EventParser extends EventParser
