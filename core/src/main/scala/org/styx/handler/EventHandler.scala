package org.styx.handler

import org.styx.handler.EventHandler.HandleStatus
import org.styx.model.Event
import org.styx.state.State
import org.styx.state.State.AggregationId

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
object EventHandler {
  abstract class HandleStatus[S <: State](val acknowledged: Boolean, event: Event[S])

  case class SuccessfullyHandled[S <: State](event: Event[S]) extends HandleStatus[S](true, event)
  case class UnsuccessfullyHandled[S <: State](reason: Throwable, event: Event[S]) extends HandleStatus[S](false, event)
}

trait EventHandler[S <: State] {
  def handle(aggregationId: AggregationId, event: Event[S], actualState: S)(implicit executionContext: ExecutionContext): Future[HandleStatus[S]]
}