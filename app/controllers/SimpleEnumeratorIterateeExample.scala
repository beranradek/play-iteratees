package controllers

import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Enumerator
import scala.concurrent.Future

/**
 * Simple solo example of enumerator and bound iteratee.
 * This is independent on the rest of the project.
 */
object SimpleEnumeratorIterateeExample {
  def main(args: Array[String]) {
    import scala.concurrent.ExecutionContext.Implicits.global
    import language.reflectiveCalls

    val enumerateNumbers: Enumerator[String] = {
      Enumerator("One", "Two", "Three")
    }
    // Simple iteratee that only composes (appends) parts of consumed data
    val consumeIteratee = Iteratee.consume[String]()
    // Apply the enumerator and flatten Future[Iteratee[String, String]] to 
    // Iteratee[String, String] (Future can be viewed as an Iteratee)
    val newIteratee = Iteratee.flatten(enumerateNumbers(consumeIteratee))
    // Run method of iteratee terminates the iteratee by passing Input.EOF to it
    val eventuallyResult: Future[String] = newIteratee.run
    // Eventually print the result
    eventuallyResult.onComplete(s => println(s.getOrElse("Error"))) // prints "OneTwoThree"
  }
}