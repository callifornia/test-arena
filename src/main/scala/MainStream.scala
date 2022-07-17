import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.{Broadcast, GraphDSL, RunnableGraph}
import KafkaInterface._
import LogicInterface._
import akka.stream.ClosedShape
import akka.stream.scaladsl.GraphDSL.Implicits.{SourceShapeArrow, port2flow}
import domain.Event

object MainStream {

  implicit val system = ActorSystem(Behaviors.empty, "foo")



  /* https://github.com/akka/alpakka-samples */
  /* https://www.confluent.io/blog/kafka-streams-tables-part-2-topics-partitions-and-storage-fundamentals/?_ga=2.103295491.2060132106.1657800359-1733407541.1653506075 */
  def main(args: Array[String]): Unit = {
    println("Application started ...")
    RunnableGraph.fromGraph(graphFlow()).run()
  }


  def graphFlow() =
    GraphDSL.create() { implicit builder =>
      val input = builder.add(readFromKafka)
      val parseEvent = builder.add(parseEvents)
      val validateEvent = builder.add(validateEventConsistency)
      val writeToGeneralEventTopic = builder.add(writeToKafka(allEventsTopic, eventKey))
      val writeToConsistentEventTopic = builder.add(writeToKafka(consistentEventTopic, eventKey))

      val (convertToJson, convertToJson2) = (builder.add(toJson), builder.add(toJson))
      val (output, output2)  = (builder.add(kafkaSink), builder.add(kafkaSink))

      val broadcast = builder.add(Broadcast[Event](2))



      input ~> parseEvent ~> broadcast
      broadcast.out(0) ~> convertToJson ~> writeToGeneralEventTopic ~> output
      broadcast.out(1) ~> validateEvent ~> convertToJson2  ~> writeToConsistentEventTopic ~> output2

      ClosedShape
    }
}
