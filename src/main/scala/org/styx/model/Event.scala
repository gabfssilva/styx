package org.styx.model

import java.util.Date

import org.styx.model.Event.Valid
import org.styx.state.State

object Event {
  case class Valid(value: Boolean, message: () => String)
}

abstract class Event[S <: State](val version: Long,
                                 val eventDate: Date = new Date()) extends DynamicData {
  def validation(validExpression: Boolean, message: => String) = Valid(validExpression, () => message)

  override def toString: String = s"${this.getClass.getSimpleName}${super.toString}"

  def canApply(state: S): Valid = validation(validExpression = true, "")

  def applyTo(state: S): S

  override def canEqual(other: Any): Boolean = other.isInstanceOf[Event[S]]

  override def equals(other: Any): Boolean = other match {
    case that: Event[S] => (that canEqual this) && version == that.version
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(super.hashCode(), version)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}