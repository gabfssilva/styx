package org.styx.bank.example.events

import org.styx.bank.example.state.BankAccount
import org.styx.model.Event

case class BankAccountCreated() extends Event[BankAccount] {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(account.aggregationId)

    newAccount.balance = 0
    newAccount.id = this.id
    newAccount.status = "ACTIVE"
    newAccount.owner = this.owner

    newAccount
  }
}