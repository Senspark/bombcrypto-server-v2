plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "bombcrypto"
include("Common")
// include("ApiExtension")
include("BombChainExtension")
include("SmartFoxLibs")
include("ClientModule")