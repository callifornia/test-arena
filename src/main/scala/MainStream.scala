import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import MainLogic._
import akka.Done
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.scaladsl.{Flow, Sink}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object MainStream {

  implicit val system = ActorSystem(Behaviors.empty, "foo")
  private val bootStrapServer = "localhost:9092"


  val kafkaConsumerSettings = ConsumerSettings.apply(
    system = system,
    keyDeserializer = new StringDeserializer,
    valueDeserializer = new StringDeserializer)
    .withBootstrapServers(bootStrapServer)
    .withGroupId("app1GroupId")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    .withStopTimeout(0.seconds)


  val kafkaProducerSettings =
    ProducerSettings.apply(
      system = system,
      keySerializer = new StringSerializer,
      valueSerializer = new StringSerializer)
      .withBootstrapServers(bootStrapServer)


  val readFromKafka =
    Consumer
      .sourceWithOffsetContext(kafkaConsumerSettings, Subscriptions.topics("events"))
      .asSource
      .map(_._1.value())


  val writeToKafka =
    Flow[String]
      .map { el =>
        new ProducerRecord[String, String]("parsed_events", el)
      }


  /* https://github.com/akka/alpakka-samples */
  def main(args: Array[String]): Unit =
    readFromKafka
      .via(parseEvents)
      .via(toJson)
      .via(writeToKafka)
      .runWith(Producer.plainSink(kafkaProducerSettings))


//    runGraph("src/main/resources/sample2.txt").run()
//  flowWay("src/main/resources/sample2.txt")

}
