# NBTGuard 1.0.1 (for 1.16.5/Java 8+)

Fixes client disconnects like:
```
io.netty.handler.codec.EncoderException: java.io.UTFDataFormatException: encoded string too long
at net.minecraft.network.play.server.SWindowItemsPacket / SSetSlotPacket
```
by trimming oversized ItemStack meta (display name, lore) and removing too-large STRING entries from PDC **before** packets are sent.

## Build
- Java 8 or 11
- Maven 3.8+
```
mvn -U -DskipTests package
```
If ProtocolLib cannot be resolved, ensure JitPack repository is reachable.

## Install
Drop the built jar into `/plugins`, make sure **ProtocolLib 4.8.0** is installed on the server, restart.
