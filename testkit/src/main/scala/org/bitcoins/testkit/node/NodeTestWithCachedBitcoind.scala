package org.bitcoins.testkit.node

import akka.actor.ActorSystem
import org.bitcoins.asyncutil.AsyncUtil
import org.bitcoins.core.api.node.{NodeType, Peer}
import org.bitcoins.node.Node
import org.bitcoins.rpc.client.common.BitcoindRpcClient
import org.bitcoins.server.BitcoinSAppConfig
import org.bitcoins.testkit.node.NodeUnitTest
import org.bitcoins.testkit.node.fixture.{
  NeutrinoNodeConnectedWithBitcoind,
  NeutrinoNodeConnectedWithBitcoinds
}
import org.bitcoins.testkit.rpc.{
  CachedBitcoind,
  CachedBitcoindNewest,
  CachedBitcoindNewestNoP2pBlockFilters,
  CachedBitcoindPairNewest
}
import org.bitcoins.testkit.tor.CachedTor
import org.bitcoins.testkit.wallet.BitcoinSWalletTest
import org.bitcoins.wallet.callback.WalletCallbacks
import org.scalatest.FutureOutcome

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/** Test trait for using a bitcoin-s [[Node]] that requires a cached bitcoind.
  * The cached bitcoind will be share across tests in the test suite that extends
  * this trait.
  */
trait NodeTestWithCachedBitcoind extends BaseNodeTest with CachedTor {
  _: CachedBitcoind[_] =>

  def withNeutrinoNodeConnectedToBitcoindCached(
      test: OneArgAsyncTest,
      bitcoind: BitcoindRpcClient)(implicit
      system: ActorSystem,
      appConfig: BitcoinSAppConfig): FutureOutcome = {
    val nodeWithBitcoindBuilder: () => Future[
      NeutrinoNodeConnectedWithBitcoind] = { () =>
      require(appConfig.nodeConf.nodeType == NodeType.NeutrinoNode)
      for {
        peer <- NodeUnitTest.createPeer(bitcoind)
        node <- NodeUnitTest.createNeutrinoNode(peer, None)(system,
                                                            appConfig.chainConf,
                                                            appConfig.nodeConf)
        started <- node.start()
        _ <- AsyncUtil.retryUntilSatisfied(
          node.peerManager.connectedPeerCount == 1,
          interval = 1.second,
          maxTries = 30)
      } yield NeutrinoNodeConnectedWithBitcoind(started, bitcoind)
    }

    makeDependentFixture[NeutrinoNodeConnectedWithBitcoind](
      build = nodeWithBitcoindBuilder,
      { x: NeutrinoNodeConnectedWithBitcoind =>
        NodeUnitTest.destroyNode(x.node, appConfig)
      })(test)
  }

  def withNeutrinoNodeUnstarted(
      test: OneArgAsyncTest,
      bitcoind: BitcoindRpcClient)(implicit
      system: ActorSystem,
      appConfig: BitcoinSAppConfig): FutureOutcome = {
    val nodeWithBitcoindBuilder: () => Future[
      NeutrinoNodeConnectedWithBitcoind] = { () =>
      require(appConfig.nodeConf.nodeType == NodeType.NeutrinoNode)
      for {
        _ <- appConfig.walletConf.kmConf.start()
        node <- NodeUnitTest.createNeutrinoNode(bitcoind, None)(
          system,
          appConfig.chainConf,
          appConfig.nodeConf)
      } yield NeutrinoNodeConnectedWithBitcoind(node, bitcoind)
    }
    makeDependentFixture[NeutrinoNodeConnectedWithBitcoind](
      build = nodeWithBitcoindBuilder,
      { x: NeutrinoNodeConnectedWithBitcoind =>
        tearDownNode(x.node, appConfig)
      })(test)
  }

  def withNeutrinoNodeConnectedToBitcoinds(
      test: OneArgAsyncTest,
      bitcoinds: Vector[BitcoindRpcClient])(implicit
      system: ActorSystem,
      appConfig: BitcoinSAppConfig): FutureOutcome = {
    val nodeWithBitcoindBuilder: () => Future[
      NeutrinoNodeConnectedWithBitcoinds] = { () =>
      require(appConfig.nodeConf.nodeType == NodeType.NeutrinoNode)
      for {
        _ <- appConfig.walletConf.kmConf.start()
        node <- NodeUnitTest.createNeutrinoNode(bitcoinds, None)(
          system,
          appConfig.chainConf,
          appConfig.nodeConf)
        startedNode <- node.start()
        _ <- AsyncUtil.retryUntilSatisfied(
          node.peerManager.connectedPeerCount == 1,
          interval = 1.second,
          maxTries = 30)
      } yield NeutrinoNodeConnectedWithBitcoinds(startedNode, bitcoinds)
    }
    makeDependentFixture[NeutrinoNodeConnectedWithBitcoinds](
      build = nodeWithBitcoindBuilder,
      { x: NeutrinoNodeConnectedWithBitcoinds =>
        tearDownNode(x.node, appConfig)
      })(test)
  }

  def withUnsyncedNeutrinoNodeConnectedToBitcoinds(
      test: OneArgAsyncTest,
      bitcoinds: Vector[BitcoindRpcClient])(implicit
      system: ActorSystem,
      appConfig: BitcoinSAppConfig): FutureOutcome = {
    val nodeWithBitcoindBuilder: () => Future[
      NeutrinoNodeConnectedWithBitcoinds] = { () =>
      require(appConfig.nodeConf.nodeType == NodeType.NeutrinoNode)
      for {
        _ <- appConfig.walletConf.kmConf.start()
        node <- NodeUnitTest.createNeutrinoNode(bitcoinds, None)(
          system,
          appConfig.chainConf,
          appConfig.nodeConf)
        startedNode <- node.start()
        _ <- AsyncUtil
          .retryUntilSatisfied(node.peerManager.connectedPeerCount == 2,
                               interval = 1.second,
                               maxTries = 30)
      } yield NeutrinoNodeConnectedWithBitcoinds(startedNode, bitcoinds)
    }
    makeDependentFixture[NeutrinoNodeConnectedWithBitcoinds](
      build = nodeWithBitcoindBuilder,
      { x: NeutrinoNodeConnectedWithBitcoinds =>
        tearDownNode(x.node, appConfig)
      })(test)
  }

  def withUnsyncedNeutrinoNodeConnectedToBitcoind(
      test: OneArgAsyncTest,
      bitcoind: BitcoindRpcClient)(implicit
      system: ActorSystem,
      appConfig: BitcoinSAppConfig): FutureOutcome = {
    val nodeWithBitcoindBuilder: () => Future[
      NeutrinoNodeConnectedWithBitcoind] = { () =>
      require(appConfig.nodeConf.nodeType == NodeType.NeutrinoNode)
      for {
        _ <- appConfig.walletConf.kmConf.start()
        node <- NodeUnitTest.createNeutrinoNode(bitcoind, None)(
          system,
          appConfig.chainConf,
          appConfig.nodeConf)
        startedNode <- node.start()
      } yield NeutrinoNodeConnectedWithBitcoind(startedNode, bitcoind)
    }
    makeDependentFixture[NeutrinoNodeConnectedWithBitcoind](
      build = nodeWithBitcoindBuilder,
      { x: NeutrinoNodeConnectedWithBitcoind =>
        tearDownNode(x.node, appConfig)
      })(test)
  }

  def withNeutrinoNodeFundedWalletBitcoind(
      test: OneArgAsyncTest,
      bitcoind: BitcoindRpcClient,
      walletCallbacks: WalletCallbacks = WalletCallbacks.empty)(implicit
      system: ActorSystem,
      appConfig: BitcoinSAppConfig): FutureOutcome = {
    makeDependentFixture[NeutrinoNodeFundedWalletBitcoind](
      build = () =>
        NodeUnitTest
          .createNeutrinoNodeFundedWalletFromBitcoind(
            bitcoind,
            walletCallbacks = walletCallbacks)(system, appConfig),
      { x: NeutrinoNodeFundedWalletBitcoind =>
        tearDownNodeWithBitcoind(x, appConfig)
      }
    )(test)
  }

  def withBitcoindPeer(
      test: OneArgAsyncTest,
      bitcoind: BitcoindRpcClient): FutureOutcome = {
    makeDependentFixture[Peer](
      () => NodeTestUtil.getBitcoindPeer(bitcoind),
      _ => Future.unit
    )(test)
  }

  private def tearDownNodeWithBitcoind(
      nodeWithBitcoind: NodeFundedWalletBitcoind,
      appConfig: BitcoinSAppConfig): Future[Unit] = {
    val node = nodeWithBitcoind.node
    val destroyNodeF = tearDownNode(node, appConfig)
    val destroyWalletF =
      BitcoinSWalletTest.destroyWallet(nodeWithBitcoind.wallet)
    for {
      _ <- destroyNodeF
      _ <- destroyWalletF
      _ <- BitcoinSWalletTest.destroyWalletAppConfig(appConfig.walletConf)
    } yield ()
  }

  private def tearDownNode(
      node: Node,
      appConfig: BitcoinSAppConfig): Future[Unit] = {
    val destroyNodeF = NodeUnitTest.destroyNode(node, appConfig)
    for {
      _ <- destroyNodeF
      //need to stop chainAppConfig too since this is a test
      _ <- node.chainAppConfig.stop()
    } yield ()
  }
}

trait NodeTestWithCachedBitcoindNewest
    extends NodeTestWithCachedBitcoind
    with CachedBitcoindNewest {

  override def afterAll(): Unit = {
    super[CachedBitcoindNewest].afterAll()
    super[NodeTestWithCachedBitcoind].afterAll()
  }
}

trait NodeTestWithCachedBitcoindPair
    extends NodeTestWithCachedBitcoind
    with CachedBitcoindPairNewest {

  override def afterAll(): Unit = {
    super[CachedBitcoindPairNewest].afterAll()
    super[NodeTestWithCachedBitcoind].afterAll()
  }
}

trait NodeTestWithCachedBitcoindNoP2pBlockFilters
    extends NodeTestWithCachedBitcoind
    with CachedBitcoindNewestNoP2pBlockFilters {

  override def afterAll(): Unit = {
    super[CachedBitcoindNewestNoP2pBlockFilters].afterAll()
    super[NodeTestWithCachedBitcoind].afterAll()
  }
}
