package org.styx.bank.example.store

import org.styx.model.Event
import org.styx.state.State
import org.styx.state.State.AggregationId
import org.styx.store.EventStore
import org.styx.store.EventStore.{FailureWrite, SuccessfulWrite, WriteStatus}

import scala.collection._
import scala.concurrent.{ExecutionContext, Future}

class InMemoryEventStore[S <: State](implicit val executionContext: ExecutionContext) extends EventStore[S] {
  val events =
    new collection.mutable.HashMap[String, collection.mutable.Set[Event[S]]]
      with SortedSetMultiMap

  trait SortedSetMultiMap extends collection.mutable.MultiMap[String, Event[S]] {
    override def makeSet = scala.collection.mutable.SortedSet[Event[S]]()((x: Event[S], y: Event[S]) => x.eventDate.compareTo(y.eventDate))
  }

  override def add(aggregationId: AggregationId, event: Event[S]): Future[WriteStatus[S]] = {
    def add = {
      events.synchronized {
        events.addBinding(aggregationId, event)
        SuccessfulWrite(event)
      }
    }

    Future.successful {
      events.get(aggregationId) match {
        case Some(eventList) =>
          if (eventList.exists(e => e.version == event.version)) {
            FailureWrite(new RuntimeException("optimistic lock exception"), event)
          } else {
            add
          }
        case None => add
      }
    }
  }

  override def get(aggregationId: AggregationId): Future[Seq[Event[S]]] = Future {
    events(aggregationId).toSeq
  }
}