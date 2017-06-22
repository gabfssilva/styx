package org.styx.mongo

import java.util.concurrent.Executors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.styx.bank.example.events._
import org.styx.bank.example.state.BankAccount
import org.styx.handler.{EventFetcher, EventHandler}
import org.styx.model.Event

import scala.concurrent.ExecutionContext

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
object BankAccountEventHandler {
  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(30))

  val mapper = new ObjectMapper
  mapper.registerModule(DefaultScalaModule)

  def converter: MongoDBEvent => Event[BankAccount] = { mongoDBEvent =>
    val event = mongoDBEvent match {
      case MongoDBEvent("BankAccountCreated", eventDate, version, _, _) => BankAccountCreated(version, eventDate)
      case MongoDBEvent("BankAccountClosed", eventDate, version, _, _) => BankAccountClosed(version, eventDate)
      case MongoDBEvent("OwnerChanged", eventDate, version, _, _) => OwnerChanged(version, eventDate)
      case MongoDBEvent("DepositPerformed", eventDate, version, _, _) => DepositPerformed(version, eventDate)
      case MongoDBEvent("WithdrawalPerformed", eventDate, version, _, _) => WithdrawalPerformed(version, eventDate)
    }

    event.data = mapper.readValue(mongoDBEvent.data.toJson(), classOf[Map[String, Any]])
    event
  }

  implicit val eventHandler: EventHandler[BankAccount] with EventFetcher[BankAccount] = MongoDBEventHandlerFetcher(MongoD.collection, mapper, converter)
}
