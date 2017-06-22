package org.styx.bank.example.events

import java.util.Date

import org.styx.bank.example.state.BankAccount
import org.styx.model.Event
import org.styx.model.Event.Valid

case class BankAccountCreated(override val revision: Long, override val eventDate: Date = new Date()) extends Event[BankAccount](revision) {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(revision, account.aggregationId)
    newAccount.balance = 0
    newAccount.id = this.id
    newAccount.status = "ACTIVE"
    newAccount.owner = this.owner
    newAccount
  }

  override def canApply(state: BankAccount): Valid = validation(state.status == null, "this account is already created")
}