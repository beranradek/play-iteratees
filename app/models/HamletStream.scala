package models

sealed trait Event

// Some other members of case classes are possible...
case class Line(text: String) extends Event
case class LineNumber(lineNumber: Int) extends Event
case class EndSignal() extends Event

object HamletStream {

  import play.api.Play.current
  import play.api.Play
  import play.api.libs.iteratee._
  import play.api.libs.concurrent._
  import scala.concurrent.ExecutionContext.Implicits.global

  private val lines: Enumerator[Event] = Enumerator.generateM[Event] {
    // data generated each 3000 milliseconds
    val line: Option[String] = nextLine()
    Promise.timeout(Some(if (line.isDefined) Line(line.get) else endSignal()), 3000)
  }

  private val lineNumbers: Enumerator[Event] = Enumerator.generateM[Event] {
    Promise.timeout(Some(if (hasNextLine()) LineNumber(lineNumber - 1) else endSignal()), 9500)
  }

  // >- is the same as interleave; andThen etc. can be used
  def events(showLineNumbers: Boolean): Enumerator[Event] = {
    // Loading from conf/hamlet.xml
    fileLines = Some(io.Source.fromInputStream(Play.classloader.getResourceAsStream("hamlet.txt")).getLines())
    lineNumber = 0
    if (showLineNumbers) lines interleave lineNumbers
    else lines
  }

  private def endSignal(): EndSignal = {
    lineNumber = 0
    fileLines = None
    EndSignal()
  }

  private var fileLines: Option[Iterator[String]] = None
  private var lineNumber: Int = 0

  private def hasNextLine() = fileLines.isDefined && fileLines.get.hasNext
  private def nextLine(): Option[String] =
    if (hasNextLine()) {
      val line = fileLines.get.next
      if (line != null && !line.trim().isEmpty()) {
        lineNumber = lineNumber + 1
        Some(line)
      } else nextLine()
    } else None

}