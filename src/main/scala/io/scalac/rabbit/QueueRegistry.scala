package io.scalac.rabbit

import io.scalac.rabbit.flow.RabbitBinding


object QueueRegistry {

  val INBOUND_EXCHANGE = "censorship.inbound.exchange"
  val INBOUND_QUEUE = "censorship.inbound.queue"
    
  val OUT_OK_EXCHANGE = "censorship.ok.exchange"
  val OUT_OK_QUEUE = "censorship.ok.queue"
  
  val OUT_NOK_EXCHANGE = "censorship.nok.exchange"
  val OUT_NOK_QUEUE = "censorship.nok.queue"
  
  val IN_BINDING = RabbitBinding(INBOUND_EXCHANGE, INBOUND_QUEUE)
  val OUT_OK_BINDING = RabbitBinding(OUT_OK_EXCHANGE, OUT_OK_QUEUE)
  val OUT_NOK_BINDING = RabbitBinding(OUT_NOK_EXCHANGE, OUT_NOK_QUEUE)
}
