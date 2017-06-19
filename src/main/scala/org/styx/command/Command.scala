package org.styx.command

import org.styx.command.Command.ExecutionRequest
import org.styx.exceptions.InvalidExecutionException
import org.styx.model.Event
import org.styx.state.State
import org.styx.store.EventStore

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
object Command {
  type ExecutionRequest[R, S <: State] = R => S => Future[S]
}

abstract class Command[Request, S <: State](implicit val eventStore: EventStore[S], implicit val executionContext: ExecutionContext) extends ExecutionRequest[Request, S] {
  type ExecutionProduce = (Request) => (S) => Future[Event[S]]
  type ValidationProduce = (Request) => (S) => Valid

  case class Valid(value: Boolean, message: () => String)

  def validation(validExpression: Boolean, message: => String) = Valid(validExpression, () => message)
  def validate: ValidationProduce = (_) => (_) => validation(validExpression = true, "")

  override def apply(request: Request): (S) => Future[S] = { actualState: S =>
    validate(request)(actualState) match {
      case Valid(true, _) => for {
        event: Event[S] <- execute(request)(actualState)
        writeResult <- eventStore.add(actualState.aggregationId, event)
        if writeResult.acknowledged
      } yield event applyTo actualState

      case Valid(false, message) => throw InvalidExecutionException(s"you cannot apply the command to the current state: ${message()}")
    }
  }

  def execute: ExecutionProduce
}