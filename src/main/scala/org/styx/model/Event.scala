package org.styx.model

import java.util.Date

import org.styx.state.State

abstract class Event[S <: State](val eventDate: Date = new Date()) extends DynamicData {
  override def toString: String = s"${this.getClass.getSimpleName}${super.toString}"

  def applyTo(state: S): S

  override def equals(other: Any): Boolean = other match {
    case that: Event[S] =>
      super.equals(that) &&
        (that canEqual this) &&
        eventDate == that.eventDate
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(super.hashCode(), eventDate)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}