package org.styx.state

import org.styx.model.Event
import org.styx.state.State.AggregationId

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
object State {
  type AggregationId = String
}

trait State {
  val lastEventVersion: Long
  val aggregationId: AggregationId
}
