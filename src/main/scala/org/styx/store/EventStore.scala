package org.styx.store

import org.styx.model.Event
import org.styx.state.State
import org.styx.state.State.AggregationId

import scala.concurrent.Future

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
trait EventStore[S <: State] {
  def add(aggregationId: AggregationId, event: Event[S]): Future[WriteStatus]
  def get(aggregationId: AggregationId): Future[Seq[Event[S]]]

  abstract class WriteStatus(val acknowledged: Boolean, event: Event[S])

  case class Success(event: Event[S]) extends WriteStatus(true, event)
  case class Failure(reason: Throwable, event: Event[S]) extends WriteStatus(false, event)
}