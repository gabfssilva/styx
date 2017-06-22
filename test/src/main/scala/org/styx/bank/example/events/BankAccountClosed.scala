package org.styx.bank.example.events

import java.util.Date

import org.styx.bank.example.state.BankAccount
import org.styx.model.Event

case class BankAccountClosed(override val revision: Long, override val eventDate: Date = new Date()) extends Event[BankAccount](revision) {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(revision, account.aggregationId)
    account copyTo newAccount
    newAccount.closeReason = this.closeReason
    newAccount.status = "CLOSED"
    newAccount
  }

  override def canApply(state: BankAccount): Event.Valid = validation(!state.status.equals("CLOSED"), "this account is already closed")
}