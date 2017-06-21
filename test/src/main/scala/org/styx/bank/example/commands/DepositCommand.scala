package org.styx.bank.example.commands

import org.styx.bank.example.events.DepositPerformed
import org.styx.bank.example.state.BankAccount
import org.styx.command.Command
import org.styx.handler.EventHandler
import org.styx.model.Request

import scala.concurrent.{ExecutionContext, Future}

class DepositCommand(implicit override val executionContext: ExecutionContext,
                     implicit override val eventHandler: EventHandler[BankAccount]) extends Command[Request, BankAccount] {
  override def event: EventProduce = (request) => (state) => {
    Future {
      val event = DepositPerformed(state.lastEventVersion + 1)
      event.amount = request.amount
      event
    }
  }

  override def execute: ExecutionProduce = (request) => (state) => Future.successful()
}