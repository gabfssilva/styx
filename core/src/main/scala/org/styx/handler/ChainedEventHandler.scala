package org.styx.handler

import org.styx.handler.EventHandler.{SuccessfullyHandled, UnsuccessfullyHandled}
import org.styx.model.Event
import org.styx.state.State
import org.styx.state.State.AggregationId

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
class ChainedEventHandler[S <: State](val handlers: List[EventHandler[S]]) extends EventHandler[S] {
  def handle(aggregationId: AggregationId, event: Event[S], actualState: S, handlers: List[EventHandler[S]])(implicit executionContext: ExecutionContext): Future[EventHandler.HandleStatus[S]] = {
    handlers match {
      case handler :: Nil => handler.handle(aggregationId, event, actualState)
      case handler :: tail =>
        handler
          .handle(aggregationId, event, actualState)
          .flatMap {
            case SuccessfullyHandled(_) => handle(aggregationId, event, actualState, tail)
            case e: UnsuccessfullyHandled[S] => Future.successful(e)
          }
    }
  }

  override def handle(aggregationId: AggregationId, event: Event[S], actualState: S)(implicit executionContext: ExecutionContext): Future[EventHandler.HandleStatus[S]] = {
    handle(aggregationId, event, actualState, handlers)
  }
}