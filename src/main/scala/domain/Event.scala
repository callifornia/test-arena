package domain

import scala.concurrent.duration.{Duration, SECONDS}
import scala.util.{Failure, Success, Try}


case class TeamOne(amount: Amount)
case class TeamTwo(amount: Amount)
case class PointScored(amount: Amount)
case class ElapsedMatchTime(duration: Duration) extends AnyVal {
  def gt(another: ElapsedMatchTime): Boolean =
    duration.gt(another.duration)
}
case class RawEvent(event: String)


sealed trait TeamScored
case object FirstTeam extends TeamScored
case object SecondTeam extends TeamScored

object TeamScored {
  val TeamOne: Int = 0
  val TeamTwo: Int = 1
}

case class Amount(value: Int) extends AnyVal {
  def -(other: Amount): Amount = Amount(value - other.value)
  def !=(other: Amount): Boolean = value != other.value
  def ==(other: Int): Boolean = value == other
}

case class MatchState(events: Seq[Event]) {
  def ++(anotherState: MatchState): MatchState = copy(events = (events ++ anotherState.events).distinct)
  def +(event: Event): MatchState = copy(events :+ event)
  def latest(): Option[Event] = events.lastOption
  def latest(n: Int): MatchState = copy(events.takeRight(n))
  def allEvents(): Seq[Event] = events
  def isLessThanOne(): Boolean = events.size <= 1
}


object MatchState {
  val empty: MatchState = MatchState(Seq.empty[Event])
}


case class Event (teamOne: TeamOne,
                  teamTwo: TeamTwo,
                  pointScored: PointScored,
                  elapsedMatchTime: ElapsedMatchTime,
                  teamScored: TeamScored,
                  rawEvent: RawEvent)


object Event {

  private val convert: (EventInBitFormat, Int, Int) => Int =
    (event, startIndex, endIndex) =>
      Integer.parseInt(event.value.substring(startIndex, endIndex), 2)


  def apply(event: EventInBitFormat, rawEvent: String): Either[Error, Event] =
    Try(
      new Event(
          teamOne = TeamOne(Amount(convert(event, 13, 21))),
          teamTwo = TeamTwo(Amount(convert(event, 21, 29))),
          pointScored = PointScored(Amount(convert(event, 30, 32))),
          elapsedMatchTime = ElapsedMatchTime(Duration(convert(event, 1, 13), SECONDS)),
          teamScored = convert(event, 29, 30) match {
            case TeamScored.TeamOne => FirstTeam
            case TeamScored.TeamTwo => SecondTeam
          },
        rawEvent = RawEvent(rawEvent)
      )
    ) match {
      case Success(value) => Right(value)
      case Failure(ex: Exception) => Left(InconsistentBitPattern(ex, event))
      case Failure(th: Throwable) => throw th
    }

}

case class EventInBitFormat (value: String)

object EventInBitFormat {
  def apply(str: String): Either[Error, EventInBitFormat] =
    for {
      decoded <- decode(str)
      bitFormatted <- toBitFormat(decoded)
    } yield new EventInBitFormat(bitFormatted)


  private val decode: String => Either[Error, String] =
    rawEvent =>
      Try(Integer.decode(rawEvent).toInt.toBinaryString) match {
        case Success(value) => Right(value)
        case Failure(ex: Exception) => Left(FailedToDecodeEvent(ex))
        case Failure(th: Throwable) => throw th
      }


  private val toBitFormat: String => Either[Error, String] =
    decodedEvent =>
      Try(String.format("%" + Integer.SIZE + "s", decodedEvent).replace(' ', '0')) match {
        case Success(value) => Right(value)
        case Failure(ex: Exception) => Left(BitFormatTransformationFailed(ex))
        case Failure(th: Throwable) => throw th
      }
}


case class ParsedEventsResult(events: Seq[Event], errors: Seq[Error]) {
  def +(error: Either[Error, Event]): ParsedEventsResult =
    error match {
      case Left(error) => copy(errors = errors :+ error)
      case Right(event) => copy(events = events :+ event)
    }
}

object ParsedEventsResult {
  def empty: ParsedEventsResult =
    ParsedEventsResult(Seq.empty[Event], Seq.empty[Error])
}


case class MatchStateAccumulator(matchState: MatchState, inconsistentEvents: Seq[ValidationError]) {

  def +(maybeEvent: Either[ValidationError, Event]): MatchStateAccumulator =
    maybeEvent match {
      case Left(inconsistentEvent) => copy(inconsistentEvents = inconsistentEvents :+ inconsistentEvent)
      case Right(event) => copy(matchState = matchState + event)
    }

  def +(inconsistentEvent: ValidationError): MatchStateAccumulator = this.+(Left(inconsistentEvent))
  def +(event: Event): MatchStateAccumulator =
    copy(matchState = matchState + event, inconsistentEvents = inconsistentEvents.filterNot(_.event == event))

  def ++(anotherStateAccumulator: MatchStateAccumulator): MatchStateAccumulator =
    copy(
      matchState = matchState ++ anotherStateAccumulator.matchState,
      inconsistentEvents =
        (inconsistentEvents ++ anotherStateAccumulator.inconsistentEvents)
          .distinct
          .filterNot(e => (anotherStateAccumulator.matchState.events ++ matchState.allEvents()).contains(e.event))
          )


  def containsUnordered(): Boolean = collectUnordered().nonEmpty
  def collectUnordered(amount: Int = Int.MaxValue): Seq[Event] = inconsistentEvents.collect{case e: EventInWrongOrder  => e.event}
}

object MatchStateAccumulator {
  def empty: MatchStateAccumulator =
    MatchStateAccumulator(MatchState.empty, Seq.empty[ValidationError])
}


// akka-stream
case class EventParsed(event: Either[Error, Event])