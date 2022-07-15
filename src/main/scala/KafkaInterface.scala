import KafkaInterface.bootStrapServer
import akka.NotUsed
import akka.actor.ClassicActorSystemProvider
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.scaladsl.Flow
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.concurrent.duration.DurationInt


trait KafkaInterface {

  private val bootStrapServer = "broker:29092"
  val rawEventsReadTopic = "events_raw"
  val allEventsTopic = "events_all"
  val consistentEventTopic = "event_consistent"


  def readFromKafka(implicit system: ClassicActorSystemProvider) =
    Consumer
      .sourceWithOffsetContext(
        ConsumerSettings.apply(
          system = system,
          keyDeserializer = new StringDeserializer,
          valueDeserializer = new StringDeserializer)
          .withBootstrapServers(bootStrapServer)
          .withProperty(ConsumerConfig.GROUP_ID_CONFIG, "custom_application_consumer_id")
          .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
          .withProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000")
          .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
          .withStopTimeout(0.seconds), Subscriptions.topics(rawEventsReadTopic))
      .asSource
      .map(_._1.value())


  val writeToKafka: (String, String) => Flow[String, ProducerRecord[String, String], NotUsed] =
    (topicName, key) =>
      Flow[String]
        .map {el =>
          println(s"Going to write into the [$topicName] with a key [$key] element: [$el]")
          new ProducerRecord[String, String](topicName, key, el)
        }


  def kafkaSink(implicit system: ClassicActorSystemProvider) =
    Producer.plainSink(
      ProducerSettings.apply(
        system = system,
        keySerializer = new StringSerializer,
        valueSerializer = new StringSerializer)
        .withBootstrapServers(bootStrapServer))

}

object KafkaInterface extends KafkaInterface
