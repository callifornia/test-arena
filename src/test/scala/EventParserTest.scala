import domain._
import org.scalatest.FutureOutcome.failed
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration.{Duration, SECONDS}
import scala.util.Random

class EventParserTest extends WordSpec with Matchers {

  "EventParser" when {
    "valid event is supplied" should {
      "parse event correctly (matchState from .pdf)" in {
        EventParser.toEvent("0x781002") shouldEqual
          Right(new Event(TeamOne(Amount(2)), TeamTwo(Amount(0)), PointScored(Amount(2)), ElapsedMatchTime(Duration(15, SECONDS)), FirstTeam))

        EventParser.toEvent("0xf0101f") shouldEqual
          Right(new Event(TeamOne(Amount(2)), TeamTwo(Amount(3)), PointScored(Amount(3)), ElapsedMatchTime(Duration(30, SECONDS)), SecondTeam))

        EventParser.toEvent("0x1310c8a1") shouldEqual
          Right(new Event(TeamOne(Amount(25)), TeamTwo(Amount(20)), PointScored(Amount(1)), ElapsedMatchTime(Duration(610, SECONDS)), FirstTeam))

        EventParser.toEvent("0x29f981a2") shouldEqual
          Right(new Event(TeamOne(Amount(48)), TeamTwo(Amount(52)), PointScored(Amount(2)), ElapsedMatchTime(Duration(1343, SECONDS)), FirstTeam))

        EventParser.toEvent("0x48332327") shouldEqual
          Right(new Event(TeamOne(Amount(100)), TeamTwo(Amount(100)), PointScored(Amount(3)), ElapsedMatchTime(Duration(2310, SECONDS)), SecondTeam))
      }
    }

    "invalid event is supplied" should {
      "return a corresponding error" in {
        val corruptedEvent = Random.nextString(Random.nextInt())
        EventParser.toEvent(corruptedEvent) match {
          case Right(_) => failed()
          case Left(RawEventDataCorrupted(event, _)) =>
            event shouldEqual corruptedEvent
          case Left(_) => failed()
        }
      }
    }
  }
}
