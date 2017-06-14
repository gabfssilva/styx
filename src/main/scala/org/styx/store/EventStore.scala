package org.styx.store

import org.styx.model.Event
import org.styx.state.State
import org.styx.state.State.AggregationId

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
trait EventStore[S <: State] {
  def add(aggregationId: AggregationId, event: Event[S])
  def get(aggregationId: AggregationId): Seq[Event[S]]
}