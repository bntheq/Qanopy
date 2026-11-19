#!/system/bin/sh
D=/data/adb/qanopy
L=$D/boot.log
J=$D/journal.txt
mkdir -p $D

log() { echo "$(date '+%F %T') $1 uptime=$(cut -d' ' -f1 /proc/uptime)" >> $L; }

# cmd window komutlari terminal girdisi olmadan (< /dev/null) calisir.
yogunluk_yaz() {
  yv="$1"
  case "$yv" in
    reset) ;;
    ''|*[!0-9]*) log "ekran: gecersiz yogunluk: $yv"; return 1 ;;
    *) if [ "$yv" -lt 200 ] || [ "$yv" -gt 800 ]; then log "ekran: yogunluk aralik disi: $yv"; return 1; fi ;;
  esac
  yn=0
  yo=""
  while [ $yn -lt 3 ]; do
    cmd window density "$yv" < /dev/null >/dev/null 2>&1
    sleep 2
    yo=$(cmd window density < /dev/null 2>&1 | tr '\n' ' ')
    yok=0
    if [ "$yv" = "reset" ]; then
      case "$yo" in *Override*) ;; *) yok=1 ;; esac
    else
      case "$yo" in
        *"Override density: $yv "*) yok=1 ;;
        *Override*) ;;
        *"Physical density: $yv "*) yok=1 ;;
      esac
    fi
    if [ $yok -eq 1 ]; then
      log "ekran: yogunluk $yv tamam | $yo"
      return 0
    fi
    yn=$((yn+1))
  done
  log "ekran: yogunluk dogrulanamadi ($yv) | $yo"
  return 1
}

geri_al() {
  if [ ! -s $J ]; then
    log "geri alinacak degisiklik yok"
    return
  fi
  sleep 5
  while IFS='|' read -r t k v; do
    [ -n "$t" ] && [ -n "$k" ] || continue
    if [ "$t" = "ekran" ]; then
      case "$k" in
        density) yogunluk_yaz "$v" ;;
        *) log "atlandi, gecersiz ekran anahtari: $k" ;;
      esac
      continue
    fi
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

# Ekran sifirlama bayragi (kurtarma zip'i veya elle birakilir)
if [ -f $D/ekran_sifirla ]; then
  log "ekran sifirlama bayragi bulundu"
  sleep 5
  yogunluk_yaz reset
  cmd window size reset < /dev/null >/dev/null 2>&1
  content delete --uri content://settings/system --where "name='font_scale'" < /dev/null >/dev/null 2>&1
  content insert --uri content://settings/system --bind name:s:font_scale --bind value:s:1.0 < /dev/null >/dev/null 2>&1
  fs=$(content query --uri content://settings/system --where "name='font_scale'" < /dev/null 2>&1 | head -n 1)
  log "ekran: yazi olcegi | $fs"
  rm -f $D/ekran_sifirla
  log "ekran sifirlama bitti"
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
