package org.bitcoins.server.util

import akka.actor.Cancellable
import grizzled.slf4j.Logging

case class BitcoindPollingCancellable(
    blockPollingCancellable: Cancellable,
    mempoolPollingCancelable: Cancellable)
    extends Cancellable
    with Logging {

  override def cancel(): Boolean = {
    logger.info(s"Cancelling bitcoind polling jobs")
    blockPollingCancellable.cancel() && mempoolPollingCancelable.cancel()
  }

  override def isCancelled: Boolean =
    blockPollingCancellable.isCancelled && mempoolPollingCancelable.cancel()
}

object BitcoindPollingCancellabe {

  val none: BitcoindPollingCancellable = BitcoindPollingCancellable(
    Cancellable.alreadyCancelled,
    Cancellable.alreadyCancelled)
}
