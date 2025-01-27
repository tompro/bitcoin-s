package org.bitcoins.core.api.node

import org.bitcoins.core.protocol.transaction.Transaction
import org.bitcoins.crypto.DoubleSha256Digest

import scala.concurrent.Future

/** Represent a bitcoin-s node API
  */
trait NodeApi {

  /** Broadcasts the given transaction over the P2P network
    */
  def broadcastTransaction(transaction: Transaction): Future[Unit] =
    broadcastTransactions(Vector(transaction))

  /** Broadcasts the given transactions over the P2P network
    */
  def broadcastTransactions(transactions: Vector[Transaction]): Future[Unit]

  /** Request the underlying node to download the given blocks from its peers and feed the blocks to [[org.bitcoins.node.NodeCallbacks]].
    */
  def downloadBlocks(blockHashes: Vector[DoubleSha256Digest]): Future[Unit]

  def getConnectionCount: Future[Int]
}
