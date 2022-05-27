import domain._


trait EventValidation {

  private val isCorrectPointScoredCalculated: (Event, MatchState) => Either[ValidationError, Unit] =
    (event, matchState) => Either.cond(
      matchState.latest().exists(latestEvent =>
        event.teamScored match {
          case FirstTeam =>
            event.teamTwo.amount == latestEvent.teamTwo.amount &&
              event.teamOne.amount - latestEvent.teamOne.amount == event.pointScored.amount
          case SecondTeam =>
            event.teamOne.amount == latestEvent.teamOne.amount &&
              event.teamTwo.amount - latestEvent.teamTwo.amount == event.pointScored.amount
        }),
      (), WrongPointScoredCalculation(event, matchState.latest()))


  private val isNotInThePast: (Event, MatchState) => Either[ValidationError, Unit] =
    (event, matchState) =>
      Either.cond(
        matchState.latest().exists(latestEvent =>
          event.elapsedMatchTime.gt(latestEvent.elapsedMatchTime)),
        (),
        EventMatchTimeInPast(event, matchState.latest()))


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


  private val isCorrectOrderByPoints: (Event, MatchState) => Either[ValidationError, Unit] =
    (event, matchState) => Either.cond(
      matchState.latest().exists(latestEvent =>
        !(event.teamOne.amount != latestEvent.teamOne.amount &&
          event.teamTwo.amount != latestEvent.teamTwo.amount)),
      (),
      EventInWrongOrder(event, matchState.latest()))



  def validate(event: Event, matchState: MatchState): Either[ValidationError, Event] =
    matchState.latest() match {
      case None => Right(event)
      case Some(_) =>
        for {
          _ <- isCorrectOrderByPoints(event, matchState)
          _ <- isZeroScorePointOnly(event, matchState)
          _ <- isDuplicated(event, matchState)
          _ <- isNotInThePast(event, matchState)
          _ <- isCorrectPointScoredCalculated(event, matchState)
        } yield event
    }
}

object EventValidation extends EventValidation
