#!/system/bin/sh
D=/data/adb/qanopy
L=$D/boot.log
mkdir -p $D
echo "$(date '+%F %T') service basladi" >> $L
i=0
while [ "$(getprop sys.boot_completed)" != "1" ] && [ $i -lt 150 ]; do
  sleep 2
  i=$((i+1))
done
echo "$(date '+%F %T') boot_completed=$(getprop sys.boot_completed) bekleme=$((i*2))sn uptime=$(cut -d' ' -f1 /proc/uptime)" >> $L
if [ "$(wc -c < $L)" -gt 51200 ]; then
  tail -n 100 $L > $L.tmp && mv $L.tmp $L
fi
