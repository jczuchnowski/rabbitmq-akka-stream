package io.scalac.rabbit

import io.scalac.amqp.{Direct, Exchange, Queue}


object RabbitRegistry {

  val inboundExchange = Exchange("censorship.inbound.exchange", Direct, true)
  val inboundQueue = Queue("censorship.inbound.queue")
    
  val outboundExchange = Exchange("censorship.outbound.exchange", Direct, true)
  val outOkQueue = Queue("censorship.ok.queue")
  val outNokQueue = Queue("censorship.nok.queue")
}
