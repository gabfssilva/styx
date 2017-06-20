package org.styx.bank.example.store

import org.bson.RawBsonDocument
import org.mongodb.scala.bson._
import org.mongodb.scala.{Completed, MongoCollection}
import org.styx.bank.example.store.MongoDB.{MongoDBEvent, convertFromMongoDBEvent}
import org.styx.model.Event
import org.styx.state.State
import org.styx.state.State.AggregationId
import org.styx.handler.EventHandler
import org.styx.handler.EventHandler.{FailureWrite, SuccessfulWrite, WriteStatus}

import scala.collection._
import scala.concurrent.{ExecutionContext, Future, Promise}

class MongoDBEventHandler[S <: State](implicit val executionContext: ExecutionContext) extends EventHandler[S] {
  override def add(aggregationId: AggregationId, event: Event[S], actualStatus: S): Future[WriteStatus[S]] = {
    val collection: MongoCollection[MongoDB.MongoDBEvent] = MongoDB.collection

    collection
      .insertOne {
        MongoDBEvent(
          eventType = event.getClass.getSimpleName,
          aggregationId = aggregationId,
          data = RawBsonDocument.parse(MongoDB.objectMapper.writeValueAsString(event.data)),
          eventDate = event.eventDate,
          version = event.version
        )
      }.head
      .map { case Completed() => SuccessfulWrite(event) }
      .recover { case e: Exception => FailureWrite(e, event) }
  }

  override def get(aggregationId: AggregationId): Future[Seq[Event[S]]] = {
    val collection: MongoCollection[MongoDBEvent] = MongoDB.collection

    val eventualEvents: Future[Seq[MongoDBEvent]] = collection
      .find(Document("aggregationId" -> aggregationId))
      .sort(Document("eventDate" -> 1))
      .collect()
      .head()

    eventualEvents.map[Seq[Event[S]]] {
      events => events.map(e => convertFromMongoDBEvent[S](e)).distinct
    }
  }
}