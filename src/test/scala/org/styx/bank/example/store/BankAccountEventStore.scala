package org.styx.bank.example.store

import java.util.concurrent.Executors

import org.styx.bank.example.state.BankAccount
import org.styx.store.EventStore

import scala.concurrent.ExecutionContext

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
object BankAccountEventStore {
  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(30))

//    implicit val eventStore: EventStore[BankAccount] = new InMemoryEventStore[BankAccount]
  implicit val eventStore: EventStore[BankAccount] = new MongoDBEventStore[BankAccount]
}
