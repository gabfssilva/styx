package org.styx.bank.example.store

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.common.collect.{Multimaps, TreeMultimap}
import org.bson.RawBsonDocument
import org.mongodb.scala.{Completed, MongoCollection}
import org.styx.model.Event
import org.styx.state.State
import org.styx.state.State.AggregationId
import org.styx.store.EventStore
import org.mongodb.scala.bson._
import org.styx.bank.example.state.BankAccount
import org.styx.bank.example.store.MongoDB.{MongoDBEvent, convertFromMongoDBEvent}

import scala.collection._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure => FutureFailure, Success => FutureSuccess}

class MongoDBEventStore[S <: State](implicit val executionContext: ExecutionContext) extends EventStore[S] {
  override def add(aggregationId: AggregationId, event: Event[S]): Future[WriteStatus] = {
    val collection: MongoCollection[MongoDB.MongoDBEvent] = MongoDB.collection

    collection
      .insertOne(MongoDBEvent(
        eventType = event.getClass.getSimpleName,
        aggregationId = aggregationId,
        data = RawBsonDocument.parse(MongoDB.objectMapper.writeValueAsString(event.data)),
        eventDate = event.eventDate
      )).head()
      .map({
        case Completed() => Success(event)
      })
  }

  override def get(aggregationId: AggregationId): Future[Seq[Event[S]]] = {
    val collection: MongoCollection[MongoDBEvent] = MongoDB.collection

    val eventualEvents: Future[Seq[MongoDBEvent]] = collection
      .find(Document("aggregationId" -> aggregationId))
      .sort(Document("eventDate" -> 1))
      .collect()
      .head()

    eventualEvents.map[Seq[Event[S]]] {
      events => events.map(e => convertFromMongoDBEvent[S](e))
    }
  }
}