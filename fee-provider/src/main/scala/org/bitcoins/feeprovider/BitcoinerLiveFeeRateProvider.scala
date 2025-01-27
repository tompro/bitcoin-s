package org.bitcoins.feeprovider

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import org.bitcoins.commons.jsonmodels.wallet.BitcoinerLiveResult
import org.bitcoins.commons.serializers.JsonSerializers._
import org.bitcoins.core.api.tor.Socks5ProxyParams
import org.bitcoins.core.wallet.fee.SatoshisPerVirtualByte
import org.bitcoins.feeprovider.BitcoinerLiveFeeRateProvider.validMinutes
import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.util.{Failure, Success, Try}

case class BitcoinerLiveFeeRateProvider(
    minutes: Int,
    proxyParams: Option[Socks5ProxyParams])(implicit
    override val system: ActorSystem)
    extends CachedHttpFeeRateProvider[SatoshisPerVirtualByte] {

  require(validMinutes.contains(minutes),
          s"$minutes is not a valid selection, must be from $validMinutes")

  override val uri: Uri =
    Uri("https://bitcoiner.live/api/fees/estimates/latest")

  override def converter(str: String): Try[SatoshisPerVirtualByte] = {
    val json = Json.parse(str)
    json.validate[BitcoinerLiveResult] match {
      case JsSuccess(response, _) =>
        Success(response.estimates(minutes).sat_per_vbyte)
      case JsError(error) =>
        Failure(
          new RuntimeException(
            s"Unexpected error when parsing response: $error"))
    }
  }
}

object BitcoinerLiveFeeRateProvider
    extends FeeProviderFactory[BitcoinerLiveFeeRateProvider] {

  final val validMinutes =
    Vector(30, 60, 120, 180, 360, 720, 1440)

  override def fromBlockTarget(
      blocks: Int,
      proxyParams: Option[Socks5ProxyParams])(implicit
      system: ActorSystem): BitcoinerLiveFeeRateProvider = {
    require(blocks > 0,
            s"Cannot have a negative or zero block target, got $blocks")

    val blockTargets = validMinutes.map(_ / 10)

    // Find closest
    val target = blockTargets.minBy(target => Math.abs(target - blocks))

    BitcoinerLiveFeeRateProvider(target, proxyParams)
  }
}
