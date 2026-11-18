package app.fluffy.patches.scene.fixes

import app.fluffy.patches.shared.Constants.COMPATIBILITY_SCENE
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch

/**
 * The root scene-daemon gates its "adb-monitor-*" ops (the live stats behind the
 * dashboard cards and monitoring overlays) behind a local license check: ~40s
 * after the app connects, a validator flips RESTRICT=1 (root environment
 * detected via a /data/scene-permission-test probe) and monitor ops start
 * answering empty. A MASTER=1 byte overrides every tier. Both bytes sit in the
 * daemon's writable data/bss pages, so the fix is a root-side poker appended to
 * up.sh (raw resource "up", zip entry res/Bp) which already runs as root on
 * every daemon launch.
 *
 * The poker is update-tolerant: instead of hardcoding gate offsets per daemon
 * build, it locates the AES IV string "scene.omarea.com" in the daemon's data
 * segment at runtime and derives the gate bytes relative to it
 * (MASTER = iv - 0x2D0, RESTRICT = iv + 0x4DF5A; deltas verified against daemon
 * build 2026.11.17 00:01). Writes are validated (byte-range check, read-back
 * verify, restore-on-mismatch) so a daemon rebuild that shifts the layout fails
 * safe back to stock behavior instead of corrupting memory.
 *
 * In POKER_TEMPLATE every literal dollar is a section sign (U+00A7) to keep
 * Kotlin string templates out of the shell code; execute() swaps them back.
 */
@Suppress("unused")
val daemonGateBypassPatch = rawResourcePatch(
    name = "Daemon gate bypass",
    description = "Keeps the root scene-daemon's monitor gate open so live stats " +
        "(dashboard cards, system/mini/FPS overlays) keep updating instead of " +
        "freezing ~40s after connect on rooted devices. Patches the daemon " +
        "launcher script (raw resource \"up\") to hold two bytes in the running " +
        "daemon's memory open; unknown daemon layouts fail safe. Requires root " +
        "(the daemon itself runs as root).",
) {
    compatibleWith(COMPATIBILITY_SCENE)

    execute {
        val up = get("res/Bp")
            ?: throw PatchException("res/Bp (raw resource 'up', the daemon launcher) not found")

        val text = up.readBytes().decodeToString()

        // Root-branch daemon-launch block; stable across 2026.10 Alpha13 /
        // 2026.11 PR1 / PR2. A rebuild that changes it fails loudly here
        // instead of shipping a broken script.
        val anchor = "renice -n -20 \u0024(pidof scene-daemon)\n    echo 'Scene-Daemon OK!'"
        if (!text.contains(anchor)) {
            throw PatchException(
                "daemon launch anchor not found in res/Bp - the launcher script " +
                    "layout changed, re-derive the insertion point",
            )
        }
        if (text.contains("gp.sh")) {
            throw PatchException("res/Bp already contains a gate poker - patched twice?")
        }

        val poker = POKER_TEMPLATE.trimIndent().replace('\u00A7', '$')
        val replacement = anchor.replace(
            "    echo 'Scene-Daemon OK!'",
            poker + "\n    echo 'Scene-Daemon OK!'",
        )
        up.writeBytes(text.replace(anchor, replacement).encodeToByteArray())
        println("Scene daemon gate bypass: poker inserted into res/Bp")
    }
}

/**
 * Inserted after the `renice` line in up.sh's root branch: writes gp.sh next to
 * the daemon, runs it once, then a 300s loop (10s cadence) covers the daemon's
 * one-shot 63s license revalidation. Notes for maintainers:
 * - all 64-bit address math goes through awk; hex strings are decoded by an
 *   explicit digit loop because awk cannot parse 0x literals, and $(( )) is
 *   32-bit in toybox/busybox sh while awk %.0f prints doubles exactly
 * - system pidof/dd only: busybox pidof cannot see scene-daemon and busybox dd
 *   lacks conv=notrunc
 * - the subshell loop is intentionally unguarded: concurrent loops poke
 *   idempotent bytes, and any guard marker would go stale across framework
 *   restarts (observed with soft reboots)
 */
private const val POKER_TEMPLATE = """
    GPF=§(current_dir)/gp.sh
    cat > "§GPF" << 'GPEOF'
#!/system/bin/sh
P=§(/system/bin/pidof scene-daemon 2>/dev/null | tr -d '\r\n')
[ -z "§P" ] && exit 0
grep 'r-xs' /proc/§P/maps 2>/dev/null | grep memfd > /dev/.gpm.§§
IMG=""
while read -r LN; do
  R=§(echo "§LN" | cut -d' ' -f1)
  S=§{R%%-*}; E=§{R##*-}
  if [ §((0x§E - 0x§S)) -gt 1048576 ]; then IMG=§S; break; fi
done < /dev/.gpm.§§
rm -f /dev/.gpm.§§
[ -z "§IMG" ] && exit 0
grep ' rw-p ' /proc/§P/maps 2>/dev/null | while read -r LN; do
  R=§(echo "§LN" | cut -d' ' -f1)
  S=§{R%%-*}; E=§{R##*-}
  SZ=§((0x§E - 0x§S))
  if [ "§SZ" -gt 4096 ] && [ "§SZ" -le 262144 ]; then echo "§S §SZ"; fi
done > /dev/.gpr.§§
IV=""
while read -r RS RZ; do
  [ -z "§RS" ] && continue
  SDS=§(awk "BEGIN{b=\"§RS\";v=0;for(i=1;i<=length(b);i++){c=substr(b,i,1);d=index(\"0123456789abcdef\",c)-1;if(d<0)d=index(\"0123456789ABCDEF\",c)-1;v=v*16+d};printf \"%.0f\", v}")
  /system/bin/dd if=/proc/§P/mem of=/dev/.gpw.§§ bs=1 iflag=skip_bytes,count_bytes skip=§SDS count=§RZ 2>/dev/null
  HIT=§(grep -a -b -o -m1 'scene.omarea.com' /dev/.gpw.§§ | head -1 | cut -d: -f1)
  rm -f /dev/.gpw.§§
  if [ -n "§HIT" ]; then IV=§(awk "BEGIN{printf \"%.0f\", §SDS+§HIT}"); break; fi
done < /dev/.gpr.§§
rm -f /dev/.gpr.§§
[ -z "§IV" ] && exit 0
MA=§(awk "BEGIN{printf \"%.0f\", §IV-720}")
RA=§(awk "BEGIN{printf \"%.0f\", §IV+319578}")
MB=§(/system/bin/dd if=/proc/§P/mem bs=1 iflag=skip_bytes,count_bytes skip=§MA count=1 2>/dev/null | od -An -tx1 | tr -d ' \n')
RB=§(/system/bin/dd if=/proc/§P/mem bs=1 iflag=skip_bytes,count_bytes skip=§RA count=1 2>/dev/null | od -An -tx1 | tr -d ' \n')
case "§MB§RB" in
  0000|0001|0100|0101) ;;
  *) exit 0 ;;
esac
printf '\x01' | /system/bin/dd of=/proc/§P/mem bs=1 seek=§MA count=1 conv=notrunc 2>/dev/null
printf '\x00' | /system/bin/dd of=/proc/§P/mem bs=1 seek=§RA count=1 conv=notrunc 2>/dev/null
VB=§(/system/bin/dd if=/proc/§P/mem bs=1 iflag=skip_bytes,count_bytes skip=§MA count=1 2>/dev/null | od -An -tx1 | tr -d ' \n')
if [ "§VB" != "01" ]; then
  printf "\\x§MB" | /system/bin/dd of=/proc/§P/mem bs=1 seek=§MA count=1 conv=notrunc 2>/dev/null
  printf "\\x§RB" | /system/bin/dd of=/proc/§P/mem bs=1 seek=§RA count=1 conv=notrunc 2>/dev/null
  exit 0
fi
if [ "$(cat /dev/.gp_last 2>/dev/null)" != "§P" ]; then
  echo "$(date '+%m-%d %H:%M:%S') gp daemon §P gate open" >> "$(dirname "§0")/gp.log"
  echo "§P" > /dev/.gp_last
fi
GPEOF
    chmod 755 "§GPF"
    sh "§GPF" 2>/dev/null
    (
      i=0
      while [ §i -lt 30 ]; do
        sleep 10
        sh "§GPF" 2>/dev/null
        i=§((i+1))
      done
    ) &
"""
