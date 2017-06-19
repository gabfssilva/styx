package org.styx.bank.example.store

import org.styx.model.Event
import org.styx.state.State
import org.styx.state.State.AggregationId
import org.styx.store.EventStore

import scala.collection._
import scala.concurrent.{ExecutionContext, Future}

class InMemoryEventStore[S <: State](implicit val executionContext: ExecutionContext) extends EventStore[S] {
  val events =
    new collection.mutable.HashMap[String, collection.mutable.Set[Event[S]]]
      with SortedSetMultiMap

  trait SortedSetMultiMap extends collection.mutable.MultiMap[String, Event[S]] {
    override def makeSet = scala.collection.mutable.SortedSet[Event[S]]()((x: Event[S], y: Event[S]) => x.eventDate.compareTo(y.eventDate))
  }

  override def add(aggregationId: AggregationId, event: Event[S]): Future[WriteStatus] = {
    synchronized {
      events.addBinding(aggregationId, event)
      Future.successful(Success(event))
    }
  }

  override def get(aggregationId: AggregationId): Future[Seq[Event[S]]] = Future {
    events(aggregationId).toSeq
  }
}