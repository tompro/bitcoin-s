import java.nio.file._
import java.security.MessageDigest
import scala.util.Properties

name := "bitcoin-s-eclair-rpc"

libraryDependencies ++= Deps.eclairRpc

CommonSettings.prodSettings

TaskKeys.downloadEclair := {
  val logger = streams.value.log
  import scala.sys.process._

  val binaryDir = CommonSettings.binariesPath.resolve("eclair")

  if (Files.notExists(binaryDir)) {
    logger.info(s"Creating directory for Eclair binaires: $binaryDir")
    Files.createDirectories(binaryDir)
  }

  val version = "0.8.0"
  val commit = "0077471"

  logger.debug(s"(Maybe) downloading Eclair binaries for version: $version")

  val versionDir = binaryDir resolve version
  val location =
    s"https://github.com/ACINQ/eclair/releases/download/v$version/eclair-node-$version-$commit-bin.zip"

  if (Files.exists(versionDir)) {
    logger.debug(
      s"Directory $versionDir already exists, skipping download of Eclair $version")
  } else {
    logger.info(s"Creating directory $version")
    Files.createDirectories(versionDir)

    val archiveLocation = versionDir resolve s"eclair-node-$version-$commit.zip"
    logger.info(
      s"Downloading Eclair $version from location: $location, to destination: $archiveLocation")
    (url(location) #> archiveLocation.toFile).!!

    val bytes = Files.readAllBytes(archiveLocation)
    val hash = MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x" format _)
      .mkString

    val expectedHash =
      "d279317de25ba86b275183160d83acd064647371c446a35601397ae87ee04abb"

    val success = hash.equalsIgnoreCase(expectedHash)
    if (success) {
      logger.info(s"Download complete and verified, unzipping result")

      val extractCommand = s"unzip $archiveLocation -d $versionDir"
      logger.info(s"Extracting archive with command: $extractCommand")
      extractCommand.!!
    } else {
      logger.error(
        s"Downloaded invalid version of eclair, got $hash, expected $expectedHash")
    }

    logger.info(s"Deleting archive")
    Files.delete(archiveLocation)

    if (!success) {
      throw new RuntimeException(s"Failed to download eclair v$version")
    }

    logger.info(s"Download complete")
  }
}
