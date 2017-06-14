package org.styx.store

import org.styx.model.Event
import org.styx.state.State

import scala.collection._

class InMemoryEventStore[S <: State] extends EventStore[S] {
  val events =
    new collection.mutable.HashMap[String, collection.mutable.Set[Event[S]]]
      with SortedSetMultiMapMultiMap

  override def add(aggregateId: String, event: Event[S]): Unit = events.synchronized {
    events.addBinding(aggregateId, event)
  }

  override def get(aggregateId: String): Seq[Event[S]] = events(aggregateId).toSeq

  trait SortedSetMultiMapMultiMap extends collection.mutable.MultiMap[String, Event[S]] {
    override def makeSet = scala.collection.mutable.SortedSet[Event[S]]()((x: Event[S], y: Event[S]) => x.eventDate.compareTo(y.eventDate))
  }
}