package org.styx.bank.example.commands

import org.styx.bank.example.events.BankAccountClosed
import org.styx.bank.example.state.BankAccount
import org.styx.command.Command
import org.styx.model.Request
import org.styx.bank.example.store.BankAccountEventStore._

import scala.concurrent.{ExecutionContext, Future}

class CloseCommand(implicit override val executionContext: ExecutionContext) extends Command[Request, BankAccount] {
  override def event: EventProduce = (request) => (state) => {
    Future {
      val event = BankAccountClosed(state.lastEventVersion + 1)
      event.closeReason = request.reason
      event
    }
  }

  override def execute: ExecutionProduce = (request) => (state) => Future.successful()
}