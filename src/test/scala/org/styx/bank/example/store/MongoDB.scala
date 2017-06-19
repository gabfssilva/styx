package org.styx.bank.example.store

import java.util.Date

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.{DEFAULT_CODEC_REGISTRY, Macros}
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.{MongoClient, MongoCollection}
import org.styx.bank.example.events._
import org.styx.model.Event
import org.styx.state.State

/**
  * @author Gabriel Francisco <peo_gfsilva@uolinc.com>
  */
object MongoDB {
  val objectMapper = new ObjectMapper
  objectMapper.registerModule(DefaultScalaModule)

  val client = MongoClient(uri = "mongodb://localhost:27017/?waitqueuemultiple=10000")

  def codedProvider: CodecProvider = Macros.createCodecProvider[MongoDBEvent]()
  val registries = fromRegistries(fromProviders(codedProvider), DEFAULT_CODEC_REGISTRY)

  val db = client.getDatabase("eventSourcingSample").withCodecRegistry(registries)

  val collection: MongoCollection[MongoDBEvent] = db.getCollection("bankAccountEvents")

  val indexes = for {
    aggregationId <- collection.createIndex(Document("aggregationId" -> 1), IndexOptions().unique(false))
  } yield aggregationId

  indexes.subscribe((indexes: String) => println("Indexes created:" + indexes))

  case class MongoDBEvent(eventType: String,
                          eventDate: Date,
                          aggregationId: String,
                          data: Document)

  def convertFromMongoDBEvent[S <: State](mongoDBEvent: MongoDBEvent): Event[S] = {
    val data = mongoDBEvent.data
    val eventDate = mongoDBEvent.eventDate

    val event: Event[S] = (mongoDBEvent match {
      case MongoDBEvent("BankAccountCreated", _, _, _) => BankAccountCreated(eventDate)
      case MongoDBEvent("BankAccountClosed", _, _, _) => BankAccountClosed(eventDate)
      case MongoDBEvent("OwnerChanged", _, _, _) => OwnerChanged(eventDate)
      case MongoDBEvent("DepositPerformed", _, _, _) => DepositPerformed(eventDate)
      case MongoDBEvent("WithdrawalPerformed", _, _, _) => WithdrawalPerformed(eventDate)
    }).asInstanceOf[Event[S]]

    event.data = objectMapper.readValue(data.toJson(), classOf[Map[String, Any]])
    event
  }
}
