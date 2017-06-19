package org.styx.bank.example.commands

import org.styx.bank.example.events.DepositPerformed
import org.styx.bank.example.state.BankAccount
import org.styx.command.Command
import org.styx.model.Request
import org.styx.bank.example.store.BankAccountEventStore._

import scala.concurrent.{ExecutionContext, Future}

class DepositCommand(implicit override val executionContext: ExecutionContext) extends Command[Request, BankAccount] {
  override def execute: ExecutionProduce = (request) => (_) => {
    Future {
      val event = DepositPerformed()
      event.amount = request.amount
      event
    }
  }
}