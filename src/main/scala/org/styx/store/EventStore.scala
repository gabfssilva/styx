package org.styx.store

import org.styx.model.Event
import org.styx.state.State
import org.styx.state.State.AggregationId
import org.styx.store.EventStore.WriteStatus

import scala.concurrent.Future

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
object EventStore {
  abstract class WriteStatus[S <: State](val acknowledged: Boolean, event: Event[S])

  case class SuccessfulWrite[S <: State](event: Event[S]) extends WriteStatus[S](true, event)
  case class FailureWrite[S <: State](reason: Throwable, event: Event[S]) extends WriteStatus[S](false, event)
}

trait EventStore[S <: State] {
  def add(aggregationId: AggregationId, event: Event[S]): Future[WriteStatus[S]]
  def get(aggregationId: AggregationId): Future[Seq[Event[S]]]
}