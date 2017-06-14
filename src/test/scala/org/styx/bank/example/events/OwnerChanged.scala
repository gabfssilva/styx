package org.styx.bank.example.events

import org.styx.bank.example.state.BankAccount
import org.styx.model.Event

case class OwnerChanged() extends Event[BankAccount] {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(account.aggregationId)
    account copyTo newAccount
    newAccount.owner = this.newOwner
    newAccount
  }
}