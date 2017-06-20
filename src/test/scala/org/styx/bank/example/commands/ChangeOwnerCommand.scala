package org.styx.bank.example.commands

import org.styx.bank.example.events.OwnerChanged
import org.styx.bank.example.state.BankAccount
import org.styx.command.Command
import org.styx.model.Request
import org.styx.bank.example.store.BankAccountEventHandler._

import scala.concurrent.{ExecutionContext, Future}

class ChangeOwnerCommand(implicit override val executionContext: ExecutionContext) extends Command[Request, BankAccount] {
  override def execute: ExecutionProduce = (request) => (state) => Future.successful()

  override def event: EventProduce = (request) => (state) => Future {
    val event = OwnerChanged(state.lastEventVersion + 1)
    event.newOwner = request.newOwner
    event
  }
}