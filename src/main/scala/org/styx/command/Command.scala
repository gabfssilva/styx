package org.styx.command

import org.styx.command.Command.ExecutionRequest
import org.styx.exceptions.InvalidExecutionException
import org.styx.model.Event
import org.styx.model.Event.Valid
import org.styx.state.State
import org.styx.handler.EventHandler
import org.styx.handler.EventHandler.{FailureWrite, SuccessfulWrite}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
object Command {
  type ExecutionRequest[R, S <: State] = R => S => Future[S]
}

abstract class Command[Request, S <: State](implicit val eventStore: EventHandler[S], implicit val executionContext: ExecutionContext) extends ExecutionRequest[Request, S] {
  type ExecutionProduce = (Event[S]) => (S) => Future[Unit]
  type EventProduce = (Request) => (S) => Future[Event[S]]

  def event: EventProduce

  override def apply(request: Request): (S) => Future[S] = { actualState: S =>
    val eventualEvent = event(request)(actualState)

    eventualEvent.flatMap { e =>
      e canApply actualState match {
        case Valid(true, _) =>
          val state = e applyTo actualState

          execute(e)(state)
            .flatMap { _ =>
              eventStore
                .add(actualState.aggregationId, e, state)
                .map {
                  case SuccessfulWrite(_) => execute(e)(state); state
                  case FailureWrite(ex, _) => throw InvalidExecutionException(s"there was a problem to insert the event into the event store: ${ex.getMessage}")
                }
            }
        case Valid(false, msg) => throw InvalidExecutionException(s"you cannot apply the command to the current state: ${msg()}")
      }
    }
  }

  def execute: ExecutionProduce
}