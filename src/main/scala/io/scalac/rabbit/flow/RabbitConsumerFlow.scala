package io.scalac.rabbit.flow

import akka.stream._
import akka.stream.scaladsl._

object RabbitConsumerFlow {
  type RabbitProcessing[Out] = Flow[RabbitMessage] => Flow[Out]
}

trait RabbitConsumerFlow {
  def startProcessing(): Unit
}