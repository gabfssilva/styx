package org.styx.store

import org.styx.handler.{EventFetcher, EventHandler}
import org.styx.state.State

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
trait EventStore[S <: State] extends EventHandler[S] with EventFetcher[S]
