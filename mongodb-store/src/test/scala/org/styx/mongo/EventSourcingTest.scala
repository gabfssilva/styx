package org.styx.mongo

import java.util.UUID
import java.util.concurrent.Executors

import org.scalatest.{FeatureSpec, Matchers}
import org.styx.bank.example.state.BankAccount
import org.styx.exceptions.InvalidExecutionException
import org.styx.model.Request

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Success

import org.styx.mongo.BankAccountEventHandler.eventHandler
import org.styx.mongo.BankAccountCommands._
import org.styx.player.EventPlayer._

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
class EventSourcingTest extends FeatureSpec with Matchers {
  scenario("withdrawing more money than the balance has should throw an exception") {
    implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(3))

    val aggregationId = UUID.randomUUID().toString

    val result = List.range(0, 500).map { i =>
      val eventualBankAccount = for {
        account <- createAccount(Request("owner" -> "John Doe", "id" -> 123))(BankAccount(0, aggregationId))
        account <- deposit(Request("amount" -> 20))(account)
        account <- changeOwner(Request("newOwner" -> "Jane Doe"))(account)
        account <- withdrawal(Request("amount" -> 10))(account)
        account <- withdrawal(Request("amount" -> 10))(account)
        account <- withdrawal(Request("amount" -> 10))(account)
        account <- close(Request("reason" -> "Unavailable address"))(account)
      } yield account

      an[InvalidExecutionException] should be thrownBy Await.result(eventualBankAccount, 1000 millis)

      val eventualSeq = Await.result(eventHandler.get(aggregationId), 1 minute)
      val state = eventualSeq.play(BankAccount(0, aggregationId))

      state.balance[Int] shouldBe 0
      state.status[String] shouldNot be("CLOSED")
    }
  }

  feature("Creating an account") {
    scenario("assert that replaying restores the actual state of the BankAccount object") {
      implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(20))

      val result = List.range(0, 20).map { i =>
        val aggregationId = UUID.randomUUID().toString

        val eventualBankAccount = for {
          account <- createAccount(Request("owner" -> "John Doe", "id" -> 123))(BankAccount(0, aggregationId))
          account <- deposit(Request("amount" -> 20))(account)
          account <- changeOwner(Request("newOwner" -> "Jane Doe"))(account)
          account <- withdrawal(Request("amount" -> 10))(account)
          account <- close(Request("reason" -> "Unavailable address"))(account)
        } yield account

        val result: Future[BankAccount] = eventualBankAccount.andThen {
          case Success(state) => eventHandler.get(aggregationId).play(BankAccount(0, aggregationId))
        }

        eventualBankAccount -> result
      }

      result.foreach(f => {
        val (eventualActualState, eventualPlayedState) = f

        val playedState = Await.result(eventualPlayedState, 60 minutes)
        val actualState = Await.result(eventualActualState, 60 minutes)

        playedState shouldBe actualState
      })
    }
  }


}
