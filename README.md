# styx
A simple event sourcing library written in Scala.


## What is Event Sourcing?

E.S. is the idea of persisting immutable events of a domain object instead of its actual state: 

Although you can save and get the last state into a database, you can also easily replay the events of the stream to get the state of your domain object. 


![Example](http://i.imgur.com/FAaGUPo.png)


There are a few problems with E.S., such as concurrency and side-effects while replaying your events. Styx can help you build your application using E.S., thus avoiding these common problems. ;) 

![Overview](https://cdn-images-1.medium.com/max/800/1*B__xOZumACYVsfe-YnJLQw.png)

## Example


The following example is based on: https://ookami86.github.io/event-sourcing-in-practice/



## The state

```scala
case class BankAccount(aggregationId: AggregationId) extends DynamicData with State
```

## The event handler

```scala
object BankAccountEventHander {
  //you can always change the event store implementation, for instance:
  // implicit val eventHandler: EventHander[BankAccount] = new MongoDBEventHander[BankAccount]
  implicit val eventHandler: EventHander[BankAccount] = new InMemoryEventHander[BankAccount]
}
```

## The events

```scala
case class BankAccountCreated(override val version: Long, override val eventDate: Date = new Date()) extends Event[BankAccount](version) {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(version, account.aggregationId)
    newAccount.balance = 0
    newAccount.id = this.id
    newAccount.status = "ACTIVE"
    newAccount.owner = this.owner
    newAccount
  }

  override def canApply(state: BankAccount): Valid = validation(state.status == null, "this account is already created")
}
```

```scala
case class DepositPerformed(override val version: Long, override val eventDate: Date = new Date()) extends Event[BankAccount](version) {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(version, account.aggregationId)
    account copyTo newAccount
    newAccount.balance = account.balance[Int] + this.amount[Int]
    newAccount
  }
}
```

```scala
case class OwnerChanged(override val version: Long, override val eventDate: Date = new Date()) extends Event[BankAccount](version) {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(version, account.aggregationId)
    account copyTo newAccount
    newAccount.owner = this.newOwner
    newAccount
  }
}
```

```scala
case class WithdrawalPerformed(override val version: Long, override val eventDate: Date = new Date()) extends Event[BankAccount](version) {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(version, account.aggregationId)
    account copyTo newAccount
    newAccount.balance = account.balance[Int] - this.amount[Int]
    newAccount
  }

  override def canApply(state: BankAccount): Event.Valid = validation((state.balance[Int] - this.amount[Int]) >= 0,
    s"the account cannot have a balance lower than zero. current balance: ${state.balance[Int]}, withdrawal amount: ${this.amount[Int]}")
}
```

```scala
case class BankAccountClosed(override val version: Long, override val eventDate: Date = new Date()) extends Event[BankAccount](version) {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(version, account.aggregationId)
    account copyTo newAccount
    newAccount.closeReason = this.closeReason
    newAccount.status = "CLOSED"
    newAccount
  }

  override def canApply(state: BankAccount): Event.Valid = validation(!state.status.equals("CLOSED"), "this account is already closed")
}
```

## The commands

```scala
class CreateAccountCommand(implicit override val executionContext: ExecutionContext) extends Command[Request, BankAccount] {
  override def event: EventProduce = (request) => (state) => {
    Future {
      val event = BankAccountCreated(state.lastEventVersion + 1)
      event.id = request.id
      event.owner = request.owner
      event
    }
  }

  override def execute: ExecutionProduce = (request) => (state) => Future.successful()
}
```

```scala
class DepositCommand(implicit override val executionContext: ExecutionContext) extends Command[Request, BankAccount] {
  override def event: EventProduce = (request) => (state) => {
    Future {
      val event = DepositPerformed(state.lastEventVersion + 1)
      event.amount = request.amount
      event
    }
  }

  override def execute: ExecutionProduce = (request) => (state) => Future.successful()
}
```

```scala
class ChangeOwnerCommand(implicit override val executionContext: ExecutionContext) extends Command[Request, BankAccount] {
  override def execute: ExecutionProduce = (request) => (state) => Future.successful()

  override def event: EventProduce = (request) => (state) => Future {
    val event = OwnerChanged(state.lastEventVersion + 1)
    event.newOwner = request.newOwner
    event
  }
}
```

```scala
class WithdrawalCommand(implicit override val executionContext: ExecutionContext) extends Command[Request, BankAccount] {
  override def event: EventProduce = (request) => (state) => {
    Future {
      val event = WithdrawalPerformed(state.lastEventVersion + 1)
      event.amount = request.amount
      event
    }
  }

  override def execute: ExecutionProduce = (request) => (state) => Future.successful()
}
```

```scala
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

  scenario("withdrawing more money than the balance has should throw an exception") {
    implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(3))

    val aggregationId = UUID.randomUUID().toString

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
```

