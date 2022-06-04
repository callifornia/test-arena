import domain._
import converter.Json.JsonConverterSyntax


object Main {

  /*
  * -> run main method and all valid events which are going to be produced down to the stream is going to be printed
  *   into the console in json format.
  *
  * -> `matchStateSample1` and `matchStateSample2` - holds match state based on events consumed from the files
  *   `simple1.txt` and `simple2.txt`. `simple1.txt` and `simple2.txt` files can be found in `src/main/resources`
  *
  * -> `MatchStateAccumulator` - container which hold `matchState` and `inconsistentEvents`
  *    MatchStateAccumulator.matchState.latest()        -> latest Event
  *    MatchStateAccumulator.matchState.latest(n: Int)  -> latest n Events
  *    MatchStateAccumulator.matchState.allEvents()     -> all valid events
  *    MatchStateAccumulator.matchState.+(event: Event) -> add an Event
  *
  *    MatchStateAccumulator.inconsistentEvents         -> all inconsistency events collected during processing
  *
  * -> start point to discover: `processEvents()`
  * */
  def main(array: Array[String]): Unit = {
    println("Start processing event from [simple1.txt]")
    val matchStateSample1: MatchStateAccumulator = processEvents("sample1.txt")
    println("Finished processing event from [simple1.txt]\n")

    println("Start processing event from [simple2.txt]")
    val matchStateSample2: MatchStateAccumulator = processEvents("sample2.txt")
    println("Finished processing event from [simple2.txt]")
  }


  val processEvents: String => MatchStateAccumulator =
    fileName =>
      FileReader
        .readFile(fileName)
        .map(EventParser.toEvents)
        .map(logParseEventErrors)
        .fold(logReadEventError, events => events)
        .foldLeft(MatchStateAccumulator.empty)(process)


  private def process(accumulator: MatchStateAccumulator, event: Event): MatchStateAccumulator =
    EventValidation.validate(event, accumulator.matchState) match {
      case Right(event) if accumulator.containsUnordered() =>
        produceEvent(event)
        accumulator.collectUnordered().map(e => process(accumulator + event, e)).last

      case Right(event) =>
        produceEvent(event)
        accumulator + event

      case Left(_: EventWithZeroScored) => accumulator + event
      case Left(_: DuplicatedEvent) => accumulator
      case Left(validationError) => accumulator + (event, validationError)
    }


  private val logParseEventErrors =
    (parsedEvent: ParsedEventsResult) => {
      if (parsedEvent.errors.nonEmpty) println(s"Failed to parse matchState: ${parsedEvent.errors}")
      parsedEvent.events
    }


  private val logReadEventError =
    (error: Error) => {
      println(s"Failed to read matchState: $error")
      Seq.empty[Event]
    }

  private val produceEvent: Event => Unit =
    event => println(s"Produced event: " + event.toJsonValue.stringify)
}
