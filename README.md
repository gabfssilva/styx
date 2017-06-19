# styx
A simple event sourcing library written in Scala.

The following example is based on: https://ookami86.github.io/event-sourcing-in-practice/


## The state

```scala
case class BankAccount(aggregationId: AggregationId) extends DynamicData with State
```

## The event store

```scala
object BankAccountEventStore {
  //you can always change the event store implementation, for instance:
  // implicit val eventStore: EventStore[BankAccount] = new MongoDBEventStore[BankAccount]
  implicit val eventStore: EventStore[BankAccount] = new InMemoryEventStore[BankAccount]
}
```

## The events

```scala
case class BankAccountCreated(override val eventDate: Date = new Date()) extends Event[BankAccount] {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(account.aggregationId)

    newAccount.balance = 0
    newAccount.id = this.id
    newAccount.status = "ACTIVE"
    newAccount.owner = this.owner

    newAccount
  }
}
```

```scala
case class DepositPerformed(override val eventDate: Date = new Date()) extends Event[BankAccount] {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(account.aggregationId)
    account copyTo newAccount
    newAccount.balance = account.balance[Int] + this.amount[Int]
    newAccount
  }
}
```

```scala
case class OwnerChanged(override val eventDate: Date = new Date()) extends Event[BankAccount] {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(account.aggregationId)
    account copyTo newAccount
    newAccount.owner = this.newOwner
    newAccount
  }
}
```

```scala
case class WithdrawalPerformed(override val eventDate: Date = new Date()) extends Event[BankAccount] {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(account.aggregationId)
    account copyTo newAccount
    newAccount.balance = account.balance[Int] - this.amount[Int]
    newAccount
  }
}
```

```scala
case class BankAccountClosed(override val eventDate: Date = new Date()) extends Event[BankAccount] {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(account.aggregationId)
    account copyTo newAccount
    newAccount.closeReason = this.closeReason
    newAccount.status = "CLOSED"
    newAccount
  }
}
```

## The commands

```scala
class CreateAccountCommand(implicit override val executionContext: ExecutionContext) extends Command[Request, BankAccount] {
  override def execute: ExecutionProduce = (request) => (_) => {
    Future {
      val event = BankAccountCreated()
      event.id = request.id
      event.owner = request.owner
      event
    }
  }

  override def validate: ValidationProduce =
    request => state => validation(state.status == null, "this account is already created")
}
```

```scala
class DepositCommand(implicit override val executionContext: ExecutionContext) extends Command[Request, BankAccount] {
  override def execute: ExecutionProduce = (request) => (_) => {
    Future {
      val event = DepositPerformed()
      event.amount = request.amount
      event
    }
  }
}
```

```scala
class ChangeOwnerCommand(implicit override val executionContext: ExecutionContext) extends Command[Request, BankAccount] {
  override def execute: ExecutionProduce = (request) => (_) => {
    Future {
      val event = OwnerChanged()
      event.newOwner = request.newOwner
      event
    }
  }
}
```

```scala
class WithdrawalCommand(implicit override val executionContext: ExecutionContext) extends Command[Request, BankAccount] {
  override def execute: ExecutionProduce = (request) => (_) => {
    Future {
      val event = WithdrawalPerformed()
      event.amount = request.amount
      event
    }
  }

  override def validate: ValidationProduce =
    request => state => validation((state.balance[Int] - request.amount[Int]) >= 0,
      s"the account cannot have a balance lower than zero. current balance: ${state.balance[Int]}, withdrawal amount: ${request.amount[Int]}")
}
```

```scala
class CloseCommand(implicit override val executionContext: ExecutionContext) extends Command[Request, BankAccount] {
  override def execute: ExecutionProduce = (request) => (_) => {
    Future {
      val event = BankAccountClosed()
      event.closeReason = request.reason
      event
    }
  }

  override def validate: ValidationProduce =
    request => state => validation(!state.status.equals("CLOSED"), "this account is already closed")
}
```

#### Commands as functions

```scala
object BankAccountCommands {
  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  val createAccount: ExecutionRequest[Request, BankAccount] = new CreateAccountCommand
  val withdrawal: ExecutionRequest[Request, BankAccount] = new WithdrawalCommand
  val deposit: ExecutionRequest[Request, BankAccount] = new DepositCommand
  val changeOwner: ExecutionRequest[Request, BankAccount] = new ChangeOwnerCommand
  val close: ExecutionRequest[Request, BankAccount] = new CloseCommand
}
```

## Running

```scala
class EventSourcingTest extends FeatureSpec with Matchers {
  feature("Creating an account") {
    scenario("assert that replaying restores the actual state of the BankAccount object") {
      implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(20))

      val result = List.range(0, 2000).map { i =>
        val aggregationId = UUID.randomUUID().toString

        val eventualBankAccount = for {
          account <- createAccount(Request("owner" -> "John Doe", "id" -> 123))(BankAccount(aggregationId))
          account <- deposit(Request("amount" -> 20))(account)
          account <- changeOwner(Request("newOwner" -> "Jane Doe"))(account)
          account <- withdrawal(Request("amount" -> 10))(account)
          account <- close(Request("reason" -> "Unavailable address"))(account)
        } yield account

        val result: Future[BankAccount] = eventualBankAccount
          .andThen({
            case Success(state) => eventStore.get(aggregationId).play(BankAccount(aggregationId))
          })

        eventualBankAccount -> result
      }

      result.foreach(f => {
        val eventualActualState = f._1
        val eventualPlayedState = f._2

        val playedState = Await.result(eventualPlayedState, 60 seconds)
        val actualState = Await.result(eventualActualState, 60 seconds)

        playedState shouldBe actualState
      })
    }
  }

  scenario("withdrawing more money than the balance has should throw an exception") {
    implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(3))

    val aggregationId = UUID.randomUUID().toString

    val eventualBankAccount = for {
      account <- createAccount(Request("owner" -> "John Doe", "id" -> 123))(BankAccount(aggregationId))
      account <- deposit(Request("amount" -> 20))(account)
      account <- changeOwner(Request("newOwner" -> "Jane Doe"))(account)
      account <- withdrawal(Request("amount" -> 10))(account)
      account <- withdrawal(Request("amount" -> 10))(account)
      account <- withdrawal(Request("amount" -> 10))(account)
      account <- close(Request("reason" -> "Unavailable address"))(account)
    } yield account

    an[InvalidExecutionException] should be thrownBy Await.result(eventualBankAccount, 1000 millis)
  }
}
```

