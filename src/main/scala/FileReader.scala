import domain.ReadDataError
import scala.io.Source
import domain.Error
import scala.util.{Failure, Success, Try}


trait FileReader {
  val readFile: String => Either[Error, Seq[String]] =
    fileName =>
      Try(
        Source
          .fromResource(fileName)
          .getLines()
          .toSeq
          .filterNot(_.isEmpty)) match {
        case Success(value) => Right(value)
        case Failure(ex: Exception) => Left(ReadDataError(ex))
        case Failure(th: Throwable) => throw th
      }
}

object FileReader extends FileReader
