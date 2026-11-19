#!/system/bin/sh
D=/data/adb/qanopy
mkdir -p $D
echo "$(date '+%F %T') post-fs-data uptime=$(cut -d' ' -f1 /proc/uptime)" >> $D/boot.log
