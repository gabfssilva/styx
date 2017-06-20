package org.styx.bank.example.store

import java.util.concurrent.Executors

import org.styx.bank.example.state.BankAccount
import org.styx.handler.EventHandler

import scala.concurrent.ExecutionContext

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
object BankAccountEventHandler {
  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(30))

    implicit val eventHandler: EventHandler[BankAccount] = new InMemoryEventHandler[BankAccount]
//  implicit val eventHandler: EventHandler[BankAccount] = new MongoDBEventHandler[BankAccount]
}
