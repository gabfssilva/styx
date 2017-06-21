package org.styx.handler

import org.styx.model.Event
import org.styx.state.State
import org.styx.state.State.AggregationId

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
trait EventFetcher[S <: State] {
  def get(aggregationId: AggregationId)(implicit executionContext: ExecutionContext): Future[Seq[Event[S]]]
}
