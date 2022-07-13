import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import MainLogic._
object MainStream {

  implicit val system = ActorSystem(Behaviors.empty, "foo")


  def main(args: Array[String]): Unit =
    runGraph("src/main/resources/sample2.txt").run()
//  flowWay("src/main/resources/sample2.txt")


}
