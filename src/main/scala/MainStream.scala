import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.GraphDSL


object MainStream {

  implicit val system = ActorSystem(Behaviors.empty, "foo")



  /* https://github.com/akka/alpakka-samples */
  /* https://www.confluent.io/blog/kafka-streams-tables-part-2-topics-partitions-and-storage-fundamentals/?_ga=2.103295491.2060132106.1657800359-1733407541.1653506075 */
  def main(args: Array[String]): Unit = {
    println("Broker version Going to start an application...")
//    readFromKafka
//      .via(parseEvents)
//      .via(toJson)
//      .via(writeToKafka)
//      .runWith(kafkaSink)


    //    runGraph("src/main/resources/sample2.txt").run()
    //  flowWay("src/main/resources/sample2.txt")
  }


  def graphFlow() =
    GraphDSL.create() { implicit builder =>



      ???
    }
}
