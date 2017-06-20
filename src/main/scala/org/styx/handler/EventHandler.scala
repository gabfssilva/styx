package org.styx.handler

import org.styx.model.Event
import org.styx.state.State
import org.styx.state.State.AggregationId
import org.styx.handler.EventHandler.WriteStatus

import scala.concurrent.Future

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
object EventHandler {
  abstract class WriteStatus[S <: State](val acknowledged: Boolean, event: Event[S])

  case class SuccessfulWrite[S <: State](event: Event[S]) extends WriteStatus[S](true, event)
  case class FailureWrite[S <: State](reason: Throwable, event: Event[S]) extends WriteStatus[S](false, event)
}

trait EventHandler[S <: State] {
  def add(aggregationId: AggregationId, event: Event[S], actualState: S): Future[WriteStatus[S]]
  def get(aggregationId: AggregationId): Future[Seq[Event[S]]]
}