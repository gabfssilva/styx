package org.styx.command

import org.styx.command.Command.ExecutionRequest
import org.styx.exceptions.InvalidExecutionException
import org.styx.model.Event
import org.styx.state.State
import org.styx.store.EventStore

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
object Command{
  type ExecutionRequest[R, S <: State] = R => S => S
}

abstract class Command[Request, S <: State](implicit val eventStore: EventStore[S]) extends (((Request, S)) => (S)) {
  type ExecutionProduce = (Request) => (S) => Event[S]
  type ValidationProduce = (Request) => (S) => Valid

  case class Valid(value: Boolean, message: () => String)

  def validation(validExpression: Boolean, message: => String) = Valid(validExpression, () => message)

  def execute: ExecutionProduce
  def validate: ValidationProduce = (_) => (_) => validation(validExpression = true, "")

  implicit class CommandTupleImplicit(val tuple: (Request, S)) {
    lazy val request: Request = tuple._1
    lazy val actualState: S = tuple._2
  }

  implicit def asFunc: ExecutionRequest[Request, S] = { r => s => this(r, s)}
  implicit def asFunc(command: Command[Request, S]): ExecutionRequest[Request, S] = { r => s => command(r, s)}

  override def apply(parameters: (Request, S)): S = {
    val valid: Valid = validate(parameters.request)(parameters.actualState)

    if(!valid.value) {
      throw InvalidExecutionException(s"you cannot apply the command to the current state: ${valid.message()}")
    }

    val aggregationId = parameters.actualState.aggregationId
    val event: Event[S] = execute(parameters.request)(parameters.actualState)
    eventStore.add(aggregationId, event)
    event applyTo parameters.actualState
  }
}
