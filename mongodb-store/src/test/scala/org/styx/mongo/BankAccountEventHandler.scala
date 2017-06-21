package org.styx.mongo

import java.util.concurrent.Executors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.mongodb.scala.MongoCollection
import org.styx.bank.example.events._
import org.styx.bank.example.state.BankAccount
import org.styx.handler.{EventFetcher, EventHandler}
import org.styx.model.Event
import org.styx.state.State

import scala.concurrent.ExecutionContext

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
object BankAccountEventHandler {
  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(30))

  val mapper = new ObjectMapper
  mapper.registerModule(DefaultScalaModule)

  def converter[S <: State]: MongoDBEvent => Event[S] = { mongoDBEvent =>
    val event: Event[S] = (mongoDBEvent match {
      case MongoDBEvent("BankAccountCreated", eventDate, version, _, _) => BankAccountCreated(version, eventDate)
      case MongoDBEvent("BankAccountClosed", eventDate, version, _, _) => BankAccountClosed(version, eventDate)
      case MongoDBEvent("OwnerChanged", eventDate, version, _, _) => OwnerChanged(version, eventDate)
      case MongoDBEvent("DepositPerformed", eventDate, version, _, _) => DepositPerformed(version, eventDate)
      case MongoDBEvent("WithdrawalPerformed", eventDate, version, _, _) => WithdrawalPerformed(version, eventDate)
    }).asInstanceOf[Event[S]]

    event.data = mapper.readValue(mongoDBEvent.data.toJson(), classOf[Map[String, Any]])
    event
  }

  implicit val eventHandler: EventHandler[BankAccount] with EventFetcher[BankAccount] = new MongoDBEventHandler[BankAccount] with MongoDBEventFetcher[BankAccount] {
    override val collection: MongoCollection[MongoDBEvent] = org.styx.mongo.MongoD.collection
    override val objectMapper: ObjectMapper = mapper
    override val converter: (MongoDBEvent) => Event[BankAccount] = org.styx.mongo.BankAccountEventHandler.converter
  }
}
