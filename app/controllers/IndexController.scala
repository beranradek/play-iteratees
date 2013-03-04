package controllers

import play.api._
import play.api.mvc._
import play.api.libs._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import models._

object IndexController extends Controller {

  def index(showLineNumbers: Boolean, upperCase: Boolean) = Action { request =>
    Ok(views.html.index(showLineNumbers, upperCase)(request))
  }

  def feed(showLineNumbers: Boolean, upperCase: Boolean) = WebSocket.using[String] { request =>
    import language.reflectiveCalls

    // filtering produced data
    val filterEvents: Enumeratee[Event, Event] = Enumeratee.collect[Event] {
      case l @ Line(text) => if (text.contains("ACT ")) Line("----- " + text + " -----") else l
      case ln @ LineNumber(_) if showLineNumbers => ln
      case e @ EndSignal() => e
    }

    // transforming produced data
    val toText: Enumeratee[Event, String] = Enumeratee.mapInput[Event] {
      case Input.El(Line(text)) => Input.El(if (upperCase) text.toUpperCase() else text)
      case Input.El(LineNumber(lineNumber)) => Input.El("(" + (if (upperCase) "LINES PRODUCED: " + lineNumber else "lines produced: " + lineNumber) + ")")
      case Input.El(EndSignal()) => Input.EOF // end of stream
    }

    // composed adaptation of produced data
    // ><> is the same as compose
    val adaptation: Enumeratee[Event, String] = filterEvents ><> toText

    // Log events (inputs from web browser) to the console
    val in = Iteratee.foreach[String](Logger.info(_)).mapDone { _ =>
      Logger.info("Disconnected")
    }

    val out: Enumerator[String] = HamletStream.events(showLineNumbers) &> adaptation

    (in, out)
  }
}