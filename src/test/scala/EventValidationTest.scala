import domain._
import org.scalatest.{Matchers, WordSpec}


class EventValidationTest extends WordSpec with Matchers {
  "Event Validation" should {
    "return an EventInWrongOrder" in {
      val matchState = MatchState(EventParser.toEvent("0x478385e").toSeq)
      val event = EventParser.toEvent("0x618406e").toOption.get

      validator().validate(event, matchState) shouldEqual
        Left(EventInWrongOrder(event, matchState.latest()))
    }


    "return an EventWithZeroScored" in {
      val matchState = MatchState(EventParser.toEvent("0xe01016").toSeq)
      val event = EventParser.toEvent("0x1081014").toOption.get

      validator().validate(event, matchState) shouldEqual
        Left(EventWithZeroScored(event, matchState.latest()))
    }


    "return an DuplicatedEvent" in {
      val matchState = MatchState(EventParser.toEvent("0xe01016").toSeq)
      val event = EventParser.toEvent("0xe01016").toOption.get

      validator().validate(event, matchState) shouldEqual
        Left(DuplicatedEvent(event, matchState.latest()))
    }
  }

  def validator(): EventValidation = new EventValidation {}

}
