package org.styx.mongo

import com.fasterxml.jackson.databind.ObjectMapper
import org.bson.RawBsonDocument
import org.mongodb.scala.{Completed, MongoCollection}
import org.styx.handler.EventHandler
import org.styx.handler.EventHandler.{SuccessfullyHandled, UnsuccessfullyHandled}
import org.styx.model.Event
import org.styx.state.State
import org.styx.state.State.AggregationId

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
trait MongoDBEventHandler[S <: State] extends EventHandler[S] {
  val collection: MongoCollection[MongoDBEvent]
  val objectMapper: ObjectMapper

  override def handle(aggregationId: AggregationId, event: Event[S], actualState: S)(implicit executionContext: ExecutionContext): Future[EventHandler.HandleStatus[S]] = {
    collection
      .insertOne {
        MongoDBEvent(
          eventType = event.getClass.getSimpleName,
          aggregationId = aggregationId,
          data = RawBsonDocument.parse(objectMapper.writeValueAsString(event.data)),
          eventDate = event.eventDate,
          version = event.version
        )
      }.head
      .map { case Completed() => SuccessfullyHandled(event) }
      .recover { case e: Exception => UnsuccessfullyHandled(e, event) }
  }
}

object MongoDBEventHandler {
  def apply[S <: State](mongoCollection: MongoCollection[MongoDBEvent], mapper: ObjectMapper): MongoDBEventHandler[S] = new MongoDBEventHandler[S](){
    override val collection: MongoCollection[MongoDBEvent] = mongoCollection
    override val objectMapper: ObjectMapper = mapper
  }
}