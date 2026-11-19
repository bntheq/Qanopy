#!/system/bin/sh
D=/data/adb/qanopy
L=$D/boot.log
J=$D/journal.txt
X=/data/system/display_settings.xml
mkdir -p $D
up=$(cut -d' ' -f1 /proc/uptime)

# Ekran ayari geri alinacaksa (journal'da ekran| satiri var), sistem acilmadan once
# yogunluk dosyasini kenara al: ekran bozuksa sistem varsayilanla acilir.
ekran_kurtar() {
  if [ -s $J ] && grep -q '^ekran|' $J && [ -f $X ]; then
    if cp -p $X $D/display_settings.xml.yedek; then
      rm -f $X
      echo "$(date '+%F %T') ekran dosyasi kenara alindi (yogunluk varsayilana donecek)" >> $L
    else
      echo "$(date '+%F %T') ekran dosyasi kopyalanamadi, dokunulmadi" >> $L
    fi
  fi
}

if [ -f $D/safe_mode ]; then
  ekran_kurtar
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
  ekran_kurtar
fi
