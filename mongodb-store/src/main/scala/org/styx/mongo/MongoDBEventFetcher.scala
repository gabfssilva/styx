package org.styx.mongo

import com.fasterxml.jackson.databind.ObjectMapper
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.Document
import org.styx.handler.EventFetcher
import org.styx.model.Event
import org.styx.state.State
import org.styx.state.State.AggregationId

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
trait MongoDBEventFetcher[S <: State] extends EventFetcher[S] {
  val collection: MongoCollection[MongoDBEvent]
  val objectMapper: ObjectMapper
  val converter: (MongoDBEvent => Event[S])

  override def get(aggregationId: AggregationId)(implicit executionContext: ExecutionContext): Future[Seq[Event[S]]] = {
    val eventualEvents: Future[Seq[MongoDBEvent]] = collection
      .find(Document("aggregationId" -> aggregationId))
      .sort(Document("eventDate" -> 1))
      .collect()
      .head()

    eventualEvents.map[Seq[Event[S]]] {
      events => events.map(e => converter(e)).distinct
    }
  }
}

object MongoDBEventFetcher {
  def apply[S <: State](collection: MongoCollection[MongoDBEvent],
                        objectMapper: ObjectMapper,
                        converter: (MongoDBEvent => Event[S])): MongoDBEventFetcher[S] = new MongoDBEventFetcher[S]() {
    override val collection: MongoCollection[MongoDBEvent] = collection
    override val objectMapper: ObjectMapper = objectMapper
    override val converter: (MongoDBEvent) => Event[S] = converter
  }
}
