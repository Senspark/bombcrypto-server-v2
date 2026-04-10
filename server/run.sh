#!/bin/bash
set -e

echo "Rebuild project"
#  ./gradlew clean
./gradlew build -x test

cd ..
pwd
echo "Copy JAR files"
mkdir -p server/deploy/extensions_volume/__lib__
cp server/Common/build/libs/Common.jar server/deploy/extensions_volume/__lib__
cp server/Common/build/dependencies/*.jar server/deploy/extensions_volume/__lib__

cp server/BombChainExtension/build/libs/BombChainExtension.jar server/deploy/extensions_volume/ZoneExtension
cp server/BombChainExtension/build/libs/BombChainExtension.jar server/deploy/extensions_volume/PVPZoneExtension
cp server/BombChainExtension/build/dependencies/*.jar server/deploy/extensions_volume/__lib__

echo "Run Game server"
docker restart sfs-game-1
echo "Run Pvp server"
docker restart sfs-pvp-1
