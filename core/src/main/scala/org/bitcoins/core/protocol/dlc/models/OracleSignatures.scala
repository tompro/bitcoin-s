package org.bitcoins.core.protocol.dlc.models

import org.bitcoins.core.protocol.dlc.compute.CETCalculator
import org.bitcoins.core.protocol.tlv._
import org.bitcoins.core.util.SeqWrapper
import org.bitcoins.core.util.sorted.OrderedSchnorrSignatures
import org.bitcoins.crypto.{CryptoUtil, ECPrivateKey, SchnorrDigitalSignature}

/** Corresponds to a set of SchnorrDigitalSignatures given by a single oracle. */
sealed trait OracleSignatures extends SeqWrapper[SchnorrDigitalSignature] {

  /** This oracle's signatures */
  def sigs: Vector[SchnorrDigitalSignature]

  /** The SingleOracleInfo for the oracle whose signatures are stored here. */
  def oracle: SingleOracleInfo

  override def wrapped: Vector[SchnorrDigitalSignature] = sigs.toVector

  /** Verifies the signatures against a given outcome. */
  def verifySignatures(outcome: DLCOutcomeType): Boolean = {
    oracle.verifySigs(outcome, this)
  }

  /** Computes the sum of all signature s values. */
  def aggregateSig(outcome: DLCOutcomeType): ECPrivateKey = {
    sigs
      .take(outcome.serialized.length)
      .map(_.sig)
      .reduce(_.add(_))
      .toPrivateKey
  }

  /** Computes the full outcome that was signed */
  def getOutcome: DLCOutcomeType
}

object OracleSignatures {

  def apply(
      oracle: SingleOracleInfo,
      attestment: OracleAttestmentTLV): OracleSignatures = {
    oracle match {
      case info: EnumSingleOracleInfo =>
        require(attestment.sigs.length == 1,
                s"Expected one signature, got ${attestment.sigs.toVector}")
        EnumOracleSignature(info, attestment.sigs.head)
      case info: NumericSingleOracleInfo =>
        attestment match {
          case v0: OracleAttestmentV0TLV =>
            val sorted =
              OrderedSchnorrSignatures.fromUnsorted(v0.unsortedSignatures)
            if (v0.unsortedSignatures == sorted) {
              //means they are sorted
              NumericOracleSignaturesSorted(info, v0.sigs)
            } else {
              NumericOracleSignaturesUnsorted(info, v0.unsortedSignatures)
            }
        }
    }
  }

  def apply(
      oracle: SingleOracleInfo,
      sigs: Vector[SchnorrDigitalSignature]): OracleSignatures = {
    oracle match {
      case info: EnumSingleOracleInfo =>
        require(sigs.length == 1, s"Expected one signature, got ${sigs}")
        EnumOracleSignature(info, sigs.head)
      case info: NumericSingleOracleInfo =>
        val sorted =
          OrderedSchnorrSignatures.fromUnsorted(sigs)
        if (sigs == sorted) {
          //means they are sorted
          NumericOracleSignaturesSorted(info, sorted)
        } else {
          NumericOracleSignaturesUnsorted(info, sigs)
        }
    }
  }

  /** Computes the aggregate s value from the given signatures to be used
    * in the given outcome.
    *
    * This is what is used to decrypt a CET adaptor signature.
    */
  def computeAggregateSignature(
      outcome: OracleOutcome,
      sigs: Vector[OracleSignatures]): ECPrivateKey = {
    outcome match {
      case EnumOracleOutcome(_, enumOutcome) =>
        sigs.map(_.aggregateSig(enumOutcome)).reduce(_.add(_))
      case NumericOracleOutcome(oraclesAndOutcomes) =>
        sigs
          .map { sig =>
            val numericOutcome =
              oraclesAndOutcomes.find(_._1 == sig.oracle).get._2
            sig.aggregateSig(numericOutcome)
          }
          .reduce(_.add(_))
    }
  }
}

/** Wraps a single oracle signature of an Enum event. */
case class EnumOracleSignature(
    oracle: EnumSingleOracleInfo,
    sig: SchnorrDigitalSignature)
    extends OracleSignatures {
  override val sigs: Vector[SchnorrDigitalSignature] = Vector(sig)

  lazy val getOutcome: EnumOutcome = {
    // cast is safe, EnumSingleOracleInfo enforces this
    val potentialOutcomes = oracle.announcement.eventTLV.eventDescriptor
      .asInstanceOf[EnumEventDescriptorV0TLV]
      .outcomes

    val outcome = potentialOutcomes
      .find { potentialOutcome =>
        oracle.publicKey
          .verify(CryptoUtil
                    .sha256DLCAttestation(potentialOutcome.toString)
                    .bytes,
                  sig)
      }
      .getOrElse(throw new IllegalArgumentException(
        s"Signature $sig does not match any outcome $potentialOutcomes"))

    EnumOutcome(outcome)
  }

  override def toString: String =
    s"EnumOracleSignature(${oracle.announcement.publicKey}, $sig)"
}

sealed trait NumericOracleSignatures extends OracleSignatures {

  lazy val getOutcome: UnsignedNumericOutcome = {
    // cast is safe, NumericSingleOracleInfo enforces this
    val base = oracle.announcement.eventTLV.eventDescriptor
      .asInstanceOf[NumericEventDescriptorTLV]
      .base
      .toInt

    val digits = sigs.map { sig =>
      (0 until base)
        .find { possibleDigit =>
          oracle.publicKey
            .verify(CryptoUtil
                      .sha256DLCAttestation(possibleDigit.toString)
                      .bytes,
                    sig)
        }
        .getOrElse(throw new IllegalArgumentException(
          s"Signature $sig does not match any digit 0-${base - 1}"))
    }
    UnsignedNumericOutcome(digits.toVector)
  }

  /** Computes the NumericOutcome to which these signatures correspond. */
  def computeOutcome(possibleOutcomes: Vector[DLCOutcomeType]): Option[
    UnsignedNumericOutcome] = {
    val digitsSigned = getOutcome.digits

    CETCalculator.searchForNumericOutcome(digitsSigned, possibleOutcomes)
  }

  override def toString: String =
    s"${getClass.getSimpleName}(${oracle.announcement.publicKey}, $sigs)"
}

/** Wraps a set of oracle signatures of numeric digits that are sorted by nonces */
case class NumericOracleSignaturesSorted(
    oracle: NumericSingleOracleInfo,
    sortedSignatures: OrderedSchnorrSignatures)
    extends NumericOracleSignatures {
  override val sigs: Vector[SchnorrDigitalSignature] = sortedSignatures.toVector
}

/** Numeric oracle signatures that are not sorted by their nonce. This is needed for v0 attestations */
case class NumericOracleSignaturesUnsorted(
    oracle: NumericSingleOracleInfo,
    sigs: Vector[SchnorrDigitalSignature])
    extends NumericOracleSignatures
