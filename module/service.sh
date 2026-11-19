#!/system/bin/sh
D=/data/adb/qanopy
L=$D/boot.log
J=$D/journal.txt
mkdir -p $D

log() { echo "$(date '+%F %T') $1 uptime=$(cut -d' ' -f1 /proc/uptime)" >> $L; }

geri_al() {
  if [ ! -s $J ]; then
    log "geri alinacak degisiklik yok"
    return
  fi
  sleep 5
  while IFS='|' read -r t k v; do
    [ -n "$t" ] && [ -n "$k" ] || continue
    case "$t" in system|secure|global) ;; *) log "atlandi, gecersiz tablo: $t"; continue ;; esac
    case "$k" in *[!A-Za-z0-9_.]*) log "atlandi, gecersiz anahtar: $k"; continue ;; esac
    content delete --uri content://settings/$t --where "name='$k'" >/dev/null 2>&1
    if [ "$v" != "NULL" ]; then
      content insert --uri content://settings/$t --bind "name:s:$k" --bind "value:s:$v" >/dev/null 2>&1
    fi
    r=$(content query --uri content://settings/$t --where "name='$k'" 2>&1 | head -n 1)
    log "geri alindi: $t/$k -> $v | kontrol: $r"
  done < $J
  mv $J $J.eski
}

log "service basladi"
i=0
while [ "$(getprop sys.boot_completed)" != "1" ] && [ $i -lt 150 ]; do
  sleep 2
  i=$((i+1))
done

if [ "$(getprop sys.boot_completed)" != "1" ]; then
  log "boot_completed 300sn icinde gelmedi, sayac sifirlanmadi"
  exit 0
fi
log "boot_completed=1 bekleme=$((i*2))sn"

if [ "$(wc -c < $L)" -gt 51200 ]; then
  tail -n 100 $L > $L.tmp && mv $L.tmp $L
fi

if [ -f $D/safe_mode ]; then
  log "GUVENLI MOD: onaylanmamis degisiklikler geri aliniyor"
  geri_al
  log "guvenli mod islemi bitti"
  exit 0
fi

sleep 90
echo 0 > $D/boot_count.tmp && mv $D/boot_count.tmp $D/boot_count
if [ -s $J ]; then
  mv $J $J.onayli
  log "degisiklikler onaylandi"
fi
log "90sn sorunsuz gecti, sayac sifirlandi"
