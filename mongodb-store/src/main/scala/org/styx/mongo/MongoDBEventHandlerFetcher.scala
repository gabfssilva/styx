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
class MongoDBEventHandlerFetcher[S <: State](override val collection: MongoCollection[MongoDBEvent],
                                             override val objectMapper: ObjectMapper,
                                             override val converter: (MongoDBEvent) => Event[S])
  extends MongoDBEventHandler[S]
    with MongoDBEventFetcher[S]

object MongoDBEventHandlerFetcher {
  def apply[S <: State](collection: MongoCollection[MongoDBEvent],
                        objectMapper: ObjectMapper,
                        converter: (MongoDBEvent) => Event[S]): MongoDBEventHandlerFetcher[S] =
    new MongoDBEventHandlerFetcher[S](collection, objectMapper, converter)
}