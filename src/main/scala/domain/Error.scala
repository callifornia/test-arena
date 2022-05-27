package domain

sealed trait Error
case class ReadDataError(ex: Exception) extends Error
case class FailedToDecodeEvent(ex: Exception) extends Error
case class BitFormatTransformationFailed(ex: Exception) extends Error
case class InconsistentBitPattern(ex: Exception, event: EventInBitFormat) extends Error
case class RawEventDataCorrupted(value: String, ex: Exception) extends Error

sealed trait ValidationError extends Error
case class EventInWrongOrder(event: Event, latestEvent: Option[Event]) extends ValidationError
case class EventWithZeroScored(event: Event, latestEvent: Option[Event]) extends ValidationError
case class DuplicatedEvent(event: Event, latestEvent: Option[Event]) extends ValidationError
case class EventMatchTimeInPast(event: Event, latestEvent: Option[Event]) extends ValidationError
case class WrongPointScoredCalculation(event: Event, latestEvent: Option[Event]) extends ValidationError
case class InconsistentEvent(event: Event) extends ValidationError