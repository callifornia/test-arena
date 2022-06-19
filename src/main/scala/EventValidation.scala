import domain._

trait EventValidationStreamWay {


  private val isDuplicated: (Event, MatchState) => Either[ValidationError, Unit] =
    (event, matchState) =>
      Either.cond(!matchState.latest().contains(event), (), DuplicatedEvent(event, matchState.latest()))


  private val isZeroScorePointOnly: (Event, MatchState) => Either[ValidationError, Unit] =
    (event, matchState) => Either.cond(
      matchState.latest().exists(latestEvent =>
        !(event.pointScored.amount == 0 &&
          event.teamOne.amount == latestEvent.teamOne.amount &&
          event.teamTwo.amount == latestEvent.teamTwo.amount)),
      (), EventWithZeroScored(event, matchState.latest()))


  private val isZeroScorePointWithWrongCalculation: (Event, MatchState) => Either[ValidationError, Unit] =
    (event, matchState) => Either.cond(
      matchState.latest().exists(latestEvent =>
        !(event.pointScored.amount == 0 &&
          (event.teamOne.amount != latestEvent.teamOne.amount ||
          event.teamTwo.amount != latestEvent.teamTwo.amount))),
      (), EventWithZeroAndWrongCalculation(event, matchState.latest()))


  private val isCorrectOrder: (Event, MatchState) => Either[ValidationError, Unit] =
    (event, matchState) => Either.cond(
      matchState.latest().exists(latestEvent =>
        event.teamScored match {
          case FirstTeam =>
            event.teamTwo.amount == latestEvent.teamTwo.amount &&
              event.teamOne.amount - latestEvent.teamOne.amount == event.pointScored.amount &&
              event.pointScored.amount.value != 0
          case SecondTeam =>
            event.teamOne.amount == latestEvent.teamOne.amount &&
              event.teamTwo.amount - latestEvent.teamTwo.amount == event.pointScored.amount &&
              event.pointScored.amount.value != 0
        }),
      (),
      EventInWrongOrder(event, matchState.latest()))



  def validate(event: Event, matchState: MatchState): Either[ValidationError, Event] =
    matchState.latest() match {
      case None => Right(event)
      case Some(_) =>
        for {
          _ <- isZeroScorePointOnly(event, matchState)
          _ <- isZeroScorePointWithWrongCalculation(event, matchState)
          _ <- isDuplicated(event, matchState)
          _ <- isCorrectOrder(event, matchState)
        } yield event
    }
}

object EventValidationStreamWay extends EventValidationStreamWay