package org.styx.bank.example.store

import org.styx.handler.EventHandler.{HandleStatus, SuccessfullyHandled, UnsuccessfullyHandled}
import org.styx.handler.{EventFetcher, EventHandler}
import org.styx.model.Event
import org.styx.state.State
import org.styx.state.State.AggregationId

import scala.collection._
import scala.concurrent.{ExecutionContext, Future}

class InMemoryEventHandler[S <: State]
  extends EventHandler[S]
    with EventFetcher[S] {
  val events =
    new collection.mutable.HashMap[String, collection.mutable.Set[Event[S]]]
      with SortedSetMultiMap

  trait SortedSetMultiMap extends collection.mutable.MultiMap[String, Event[S]] {
    override def makeSet = scala.collection.mutable.SortedSet[Event[S]]()((x: Event[S], y: Event[S]) => x.eventDate.compareTo(y.eventDate))
  }

  override def handle(aggregationId: AggregationId, event: Event[S], actualState: S)(implicit executionContext: ExecutionContext): Future[HandleStatus[S]] = {
    def add = {
      events.addBinding(aggregationId, event)
      SuccessfullyHandled(event)
    }

    Future.successful {
      events.synchronized {
        events.get(aggregationId) match {
          case Some(eventList) =>
            if (eventList.exists(e => e.revision == event.revision)) {
              UnsuccessfullyHandled(new RuntimeException("optimistic lock exception"), event)
            } else {
              add
            }
          case None => add
        }
      }
    }
  }

  override def get(aggregationId: AggregationId)(implicit executionContext: ExecutionContext): Future[Seq[Event[S]]] = Future {
    events(aggregationId).toSeq
  }
}