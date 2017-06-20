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
import org.styx.bank.example.state.BankAccount
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
    aggregationIdAndVersion <- collection.createIndex(Document("aggregationId" -> 1, "version" -> 1), IndexOptions().unique(true))
  } yield aggregationIdAndVersion

  indexes.subscribe((indexes: String) => println("Indexes created:" + indexes))

  case class MongoDBEvent(eventType: String,
                          eventDate: Date,
                          version: Long,
                          aggregationId: String,
                          data: Document)

  def convertFromMongoDBEvent[S <: State](mongoDBEvent: MongoDBEvent): Event[S] = {
    val event: Event[S] = (mongoDBEvent match {
      case MongoDBEvent("BankAccountCreated", eventDate, version, _, _) => BankAccountCreated(version, eventDate)
      case MongoDBEvent("BankAccountClosed", eventDate, version, _, _) => BankAccountClosed(version, eventDate)
      case MongoDBEvent("OwnerChanged", eventDate, version, _, _) => OwnerChanged(version, eventDate)
      case MongoDBEvent("DepositPerformed", eventDate, version, _, _) => DepositPerformed(version, eventDate)
      case MongoDBEvent("WithdrawalPerformed", eventDate, version, _, _) => WithdrawalPerformed(version, eventDate)
    }).asInstanceOf[Event[S]]

    event.data = objectMapper.readValue(mongoDBEvent.data.toJson(), classOf[Map[String, Any]])
    event
  }
}
