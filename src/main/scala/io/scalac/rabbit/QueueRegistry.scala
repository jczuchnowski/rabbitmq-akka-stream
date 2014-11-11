package io.scalac.rabbit

import io.scalac.amqp.{Direct, Exchange, Queue}


object DeclarationsRegistry {

  val inboundExchange = Exchange("censorship.inbound.exchange", Direct, true, false, false)
  val inboundQueue = Queue("censorship.inbound.queue")
    
  val outboundExchange = Exchange("censorship.outbound.exchange", Direct, true, false, false)
  val outOkQueue = Queue("censorship.ok.queue")
  val outNokQueue = Queue("censorship.nok.queue")
}
