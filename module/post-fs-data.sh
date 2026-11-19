#!/system/bin/sh
D=/data/adb/qanopy
L=$D/boot.log
mkdir -p $D
up=$(cut -d' ' -f1 /proc/uptime)

if [ -f $D/safe_mode ]; then
  echo "$(date '+%F %T') post-fs-data GUVENLI MOD aktif, hicbir sey uygulanmayacak uptime=$up" >> $L
  exit 0
fi

n=$(cat $D/boot_count 2>/dev/null)
case "$n" in ''|*[!0-9]*) n=0 ;; esac
n=$((n+1))
echo $n > $D/boot_count.tmp && mv $D/boot_count.tmp $D/boot_count
echo "$(date '+%F %T') post-fs-data sayac=$n uptime=$up" >> $L

if [ $n -ge 3 ]; then
  touch $D/safe_mode
  echo 0 > $D/boot_count.tmp && mv $D/boot_count.tmp $D/boot_count
  echo "$(date '+%F %T') ESIK ASILDI: safe_mode olusturuldu" >> $L
fi
