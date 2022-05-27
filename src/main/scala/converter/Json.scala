package converter

import scala.concurrent.duration.Duration
import domain._
import Json.JsonValue._

object Json {

  trait JsonConverter[T] {
    def toJsonValue(value: T): JsonValue
  }
  /*
  *   {
  *     "teamOne": {
  *       "amount": 123
  *     },
  *     "teamTwo": {
  *       "amount": 234
  *     },
  *     "pointScored": {
  *       "amount": 345
  *     },
  *     "ElapsedMatchTime": 43423,
  *     "teamScored": "FirstTeam"
  *   }
  * */

  implicit class JsonConverterSyntax[T](value: T) {
    def toJsonValue(implicit jsonConverter: JsonConverter[T]): JsonValue =
      jsonConverter.toJsonValue(value)
  }

  object JsonConverter {

    implicit object EventJsonConverter extends JsonConverter[Event] {
      def toJsonValue(event: Event): JsonValue =
        ObjectJsonValue(
          Map(
            "teamOne" -> TeamOneJsonConverter.toJsonValue(event.teamOne),
            "teamTwo" -> TeamTwoJsonConverter.toJsonValue(event.teamTwo),
            "pointScored" -> PointScoredJsonConverter.toJsonValue(event.pointScored),
            "elapsedMatchTime" -> ElapsedMatchTimeJsonConverter.toJsonValue(event.elapsedMatchTime),
            "teamScored" -> TeamScoredJsonConverter.toJsonValue(event.teamScored),
            "rawEvent" -> RawEventConverter.toJsonValue(event.rawEvent)
          ))
    }


    implicit object SeqJsonConverter extends JsonConverter[Seq[Event]] {
      def toJsonValue(value: Seq[Event]): JsonValue =
        ArrayJsonValue(value.map(_.toJsonValue).toArray)
    }


    implicit object RawEventConverter extends JsonConverter[RawEvent] {
      def toJsonValue(value: RawEvent): JsonValue = StringJsonValue(value.event)
    }

    implicit object TeamScoredJsonConverter extends JsonConverter[TeamScored] {
      def toJsonValue(value: TeamScored): JsonValue =
        value match {
          case FirstTeam => FirstTeamScoredJsonConverter.toJsonValue(FirstTeam)
          case SecondTeam => SecondTeamScoredJsonConverter.toJsonValue(SecondTeam)
        }
    }


    implicit object SecondTeamScoredJsonConverter extends JsonConverter[SecondTeam.type] {
      def toJsonValue(value: SecondTeam.type): JsonValue =
        StringJsonValue("SecondTeam")
    }


    implicit object FirstTeamScoredJsonConverter extends JsonConverter[FirstTeam.type] {
      def toJsonValue(value: FirstTeam.type): JsonValue =
        StringJsonValue("FirstTeam")
    }


    implicit object ElapsedMatchTimeJsonConverter extends JsonConverter[ElapsedMatchTime] {
      def toJsonValue(matchTime: ElapsedMatchTime): JsonValue = DurationJsonValue(matchTime.duration)
    }


    implicit object AmountJsonConverter extends JsonConverter[Amount] {
      def toJsonValue(amount: Amount): JsonValue =
        ObjectJsonValue(Map(
          "amount" -> IntJsonValue(amount.value)))
    }


    implicit object PointScoredJsonConverter extends JsonConverter[PointScored] {
      def toJsonValue(pointScored: PointScored): JsonValue = pointScored.amount.toJsonValue
    }


    implicit object TeamTwoJsonConverter extends JsonConverter[TeamTwo] {
      def toJsonValue(teamTwo: TeamTwo): JsonValue = teamTwo.amount.toJsonValue
    }


    implicit object TeamOneJsonConverter extends JsonConverter[TeamOne] {
      def toJsonValue(teamOne: TeamOne): JsonValue = teamOne.amount.toJsonValue

    }
  }

  trait JsonValue {
    def stringify: String
  }

  object JsonValue {

    final case class ArrayJsonValue(array: Array[JsonValue]) extends JsonValue {
      def stringify: String = array.map(_.stringify).mkString("[", ",", "]")
    }

    final case class IntJsonValue(value: Int) extends JsonValue {
      def stringify: String = value.toString
    }

    final case class StringJsonValue(value: String) extends JsonValue {
      def stringify: String = "\"" + value + "\""
    }

    final case class DurationJsonValue(value: Duration) extends JsonValue {
      def stringify: String = "" + value.toSeconds + ""
    }

    final case class ObjectJsonValue(value: Map[String, JsonValue]) extends JsonValue {
      def stringify: String = value map {
        case (key, value) => "\"" + key + "\":" + " " + value.stringify
      } mkString("{", ", ", "}")
    }
  }
}
