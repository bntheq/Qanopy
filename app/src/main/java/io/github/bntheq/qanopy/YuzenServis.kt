package io.github.bntheq.qanopy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

private val YZ_KREM = Color.parseColor("#FFE8AF")
private val YZ_YESIL = Color.parseColor("#00A75C")
private val YZ_PANO = ArrayList<String>()

fun yuzenBaslat(c: Context) {
    thread {
        try {
            if (!Settings.canDrawOverlays(c)) {
                val p = ProcessBuilder(
                    "su", "-c",
                    "appops set " + c.packageName + " SYSTEM_ALERT_WINDOW allow; pm grant " + c.packageName + " android.permission.POST_NOTIFICATIONS"
                ).redirectErrorStream(true).start()
                p.waitFor(10, TimeUnit.SECONDS)
            }
        } catch (e: Exception) {
        }
        Handler(Looper.getMainLooper()).post {
            if (Settings.canDrawOverlays(c)) {
                try {
                    c.startForegroundService(Intent(c, YuzenServis::class.java))
                } catch (e: Exception) {
                }
            }
        }
    }
}

private fun yzKok(komut: String): String {
    return try {
        val p = ProcessBuilder("su", "-c", komut).redirectErrorStream(true).start()
        val s = p.inputStream.bufferedReader().readText()
        p.waitFor(10, TimeUnit.SECONDS)
        s
    } catch (e: Exception) {
        ""
    }
}

private fun yzKokAt(komut: String) {
    thread { yzKok(komut) }
}

private class YuzenTutamac(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()
        val r = RectF(1.5f * d, 1.5f * d, w + 12f * d, h - 1.5f * d)
        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        canvas.drawRoundRect(r, 8f * d, 8f * d, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f * d
        p.color = YZ_YESIL
        canvas.drawRoundRect(r, 8f * d, 8f * d, p)
        p.style = Paint.Style.FILL
        p.color = YZ_KREM
        for (i in -1..1) {
            canvas.drawCircle(w / 2f + 1f * d, h / 2f + i * 12f * d, 2f * d, p)
        }
    }
}

private class YuzenDugme(c: Context, var metin: String) : View(c) {
    var acik = false
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun dispatchSetPressed(pressed: Boolean) {
        super.dispatchSetPressed(pressed)
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val ters = isPressed != acik
        val r = RectF(1.5f * d, 1.5f * d, width - 1.5f * d, height - 1.5f * d)
        p.style = Paint.Style.FILL
        p.color = if (ters) YZ_KREM else Color.BLACK
        canvas.drawRoundRect(r, 10f * d, 10f * d, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f * d
        p.color = if (ters) YZ_KREM else YZ_YESIL
        canvas.drawRoundRect(r, 10f * d, 10f * d, p)
        p.style = Paint.Style.FILL
        p.color = if (ters) Color.BLACK else YZ_KREM
        p.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        p.textSize = 13f * d
        val tw = p.measureText(metin)
        val max = width - 20f * d
        if (tw > max) p.textSize = 13f * d * max / tw
        val tw2 = p.measureText(metin)
        val fm = p.fontMetrics
        canvas.drawText(metin, (width - tw2) / 2f, height / 2f - (fm.ascent + fm.descent) / 2f, p)
    }
}

private class YuzenKok(c: Context, val kapat: () -> Unit) : FrameLayout(c) {
    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.keyCode == KeyEvent.KEYCODE_BACK) {
            if (e.action == KeyEvent.ACTION_UP) kapat()
            return true
        }
        return super.dispatchKeyEvent(e)
    }
}

class YuzenServis : Service() {
    private var wm: WindowManager? = null
    private var tutamac: View? = null
    private var panel: View? = null
    private var fenerAcik = false
    private val qsListe = ArrayList<YuzenDugme>()

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int = START_STICKY

    override fun onCreate() {
        super.onCreate()
        QaVarlik.baglam = applicationContext
        QaAyar.yukle(applicationContext)
        bildirim()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        tutamacEkle()
        try {
            val cam = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cam.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    fenerAcik = enabled
                }
            }, null)
        } catch (e: Exception) {
        }
        try {
            val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cb.addPrimaryClipChangedListener { panoOku() }
        } catch (e: Exception) {
        }
    }

    override fun onDestroy() {
        panelKapat()
        try {
            tutamac?.let { wm?.removeView(it) }
        } catch (e: Exception) {
        }
        tutamac = null
        super.onDestroy()
    }

    private fun bildirim() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("yuzen", "Qanopy yuzen dugme", NotificationManager.IMPORTANCE_MIN))
        val n = Notification.Builder(this, "yuzen")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("Qanopy")
            .setContentText("Yuzen dugme acik")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    private fun tutamacEkle() {
        val d = resources.displayMetrics.density
        val v = YuzenTutamac(this)
        v.setOnClickListener {
            if (QaAyar.titresim) v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            panelAc()
        }
        val lp = WindowManager.LayoutParams(
            (16 * d).toInt(), (88 * d).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        try {
            wm?.addView(v, lp)
            tutamac = v
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun dugme(ad: String, tik: () -> Unit): YuzenDugme {
        val b = YuzenDugme(this, ad)
        b.setOnClickListener {
            if (QaAyar.titresim) b.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            tik()
        }
        return b
    }

    private fun riskli(ad: String, komut: String): YuzenDugme {
        val b = YuzenDugme(this, ad)
        var bekliyor = false
        b.setOnClickListener {
            if (QaAyar.titresim) b.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            if (QaAyar.emin && !bekliyor) {
                bekliyor = true
                b.metin = "EMİN MİSİN?"
                b.invalidate()
                b.postDelayed({
                    bekliyor = false
                    b.metin = ad
                    b.invalidate()
                }, 3000)
            } else {
                panelKapat()
                qaKokCalistir(komut)
            }
        }
        return b
    }

    private fun qs(ad: String, ac: String, kapa: String): YuzenDugme {
        val b = YuzenDugme(this, ad)
        b.setOnClickListener {
            if (QaAyar.titresim) b.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            b.acik = !b.acik
            b.invalidate()
            yzKokAt(if (b.acik) ac else kapa)
        }
        qsListe.add(b)
        return b
    }

    private fun fenerDegis() {
        try {
            val cam = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            var id: String? = null
            for (x in cam.cameraIdList) {
                val ch = cam.getCameraCharacteristics(x)
                if (ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                    id = x
                    break
                }
            }
            if (id != null) {
                fenerAcik = !fenerAcik
                cam.setTorchMode(id, fenerAcik)
            }
        } catch (e: Exception) {
        }
    }

    private fun medyaTusu(kod: Int) {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, kod))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, kod))
        } catch (e: Exception) {
        }
    }

    private fun sesDegis(artir: Boolean) {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (artir) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            )
        } catch (e: Exception) {
        }
    }

    private fun kisayol(komut: String) {
        panelKapat()
        Handler(Looper.getMainLooper()).postDelayed({ yzKokAt(komut) }, 400)
    }

    private fun etkinlik(i: Intent) {
        panelKapat()
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(i)
        } catch (e: Exception) {
        }
    }

    private fun panoOku() {
        try {
            val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val t = cb.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
            if (!t.isNullOrBlank()) {
                val s = t.take(500)
                YZ_PANO.remove(s)
                YZ_PANO.add(0, s)
                while (YZ_PANO.size > 4) YZ_PANO.removeAt(YZ_PANO.size - 1)
            }
        } catch (e: Exception) {
        }
    }

    private fun panoYaz(m: String) {
        try {
            val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cb.setPrimaryClip(ClipData.newPlainText("qanopy", m))
        } catch (e: Exception) {
        }
    }

    private fun panoSil() {
        YZ_PANO.clear()
        try {
            val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cb.clearPrimaryClip()
        } catch (e: Exception) {
        }
    }

    private fun baslik(m: String): TextView {
        val d = resources.displayMetrics.density
        val t = TextView(this)
        t.text = m
        t.setTextColor(YZ_YESIL)
        t.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        t.textSize = 11f
        t.setPadding(0, (10 * d).toInt(), 0, (4 * d).toInt())
        return t
    }

    private fun ekle(k: LinearLayout, v: View) {
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = (6 * resources.displayMetrics.density).toInt()
        k.addView(v, lp)
    }

    private fun satir(vararg l: YuzenDugme): LinearLayout {
        val d = resources.displayMetrics.density
        val s = LinearLayout(this)
        s.orientation = LinearLayout.HORIZONTAL
        for (i in l.indices) {
            val lp = LinearLayout.LayoutParams(0, (40 * d).toInt(), 1f)
            if (i < l.size - 1) lp.marginEnd = (6 * d).toInt()
            s.addView(l[i], lp)
        }
        return s
    }

    private fun panoYenile(k: LinearLayout) {
        val d = resources.displayMetrics.density
        k.removeAllViews()
        if (YZ_PANO.isEmpty()) {
            val t = TextView(this)
            t.text = "PANO BOŞ"
            t.setTextColor(YZ_KREM)
            t.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            t.textSize = 12f
            t.setPadding(0, 0, 0, (6 * d).toInt())
            k.addView(t)
        }
        for (m in YZ_PANO) {
            val bir = m.replace("\n", " ")
            val ad = if (bir.length > 22) bir.take(22) + "..." else bir
            ekle(k, satir(dugme(ad) {
                panoYaz(m)
                panelKapat()
            }))
        }
        ekle(k, satir(dugme("PANOYU SİL") {
            panoSil()
            panoYenile(k)
        }))
    }

    private fun panelAc() {
        if (panel != null) return
        val d = resources.displayMetrics.density
        val kok = YuzenKok(this) { panelKapat() }
        kok.setBackgroundColor(0x99000000.toInt())
        kok.setOnClickListener { panelKapat() }
        kok.isFocusableInTouchMode = true

        val kutu = LinearLayout(this)
        kutu.orientation = LinearLayout.VERTICAL
        qsListe.clear()

        kutu.addView(baslik("MEDYA"))
        ekle(kutu, satir(
            dugme("|<") { medyaTusu(KeyEvent.KEYCODE_MEDIA_PREVIOUS) },
            dugme(">/||") { medyaTusu(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) },
            dugme(">|") { medyaTusu(KeyEvent.KEYCODE_MEDIA_NEXT) }
        ))
        ekle(kutu, satir(
            dugme("SES -") { sesDegis(false) },
            dugme("SES +") { sesDegis(true) }
        ))

        kutu.addView(baslik("PANO"))
        val panoKutu = LinearLayout(this)
        panoKutu.orientation = LinearLayout.VERTICAL
        kutu.addView(panoKutu, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        panoYenile(panoKutu)

        kutu.addView(baslik("HIZLI AYARLAR"))
        val wifi = qs("WIFI", "svc wifi enable", "svc wifi disable")
        val bt = qs("BLUETOOTH", "svc bluetooth enable", "svc bluetooth disable")
        val ucak = qs("UÇAK", "cmd connectivity airplane-mode enable", "cmd connectivity airplane-mode disable")
        val veri = qs("MOBİL VERİ", "svc data enable", "svc data disable")
        val dondur = qs("DÖNDÜR", "settings put system accelerometer_rotation 1", "settings put system accelerometer_rotation 0")
        val karanlik = qs("KARANLIK", "cmd uimode night yes", "cmd uimode night no")
        val fener = YuzenDugme(this, "FENER")
        fener.acik = fenerAcik
        fener.setOnClickListener {
            if (QaAyar.titresim) fener.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            fenerDegis()
            fener.acik = fenerAcik
            fener.invalidate()
        }
        ekle(kutu, satir(wifi, bt))
        ekle(kutu, satir(ucak, veri))
        ekle(kutu, satir(fener, dondur))
        ekle(kutu, satir(karanlik, dugme("QS AÇ") {
            panelKapat()
            yzKokAt("cmd statusbar expand-settings")
        }))

        kutu.addView(baslik("KISAYOLLAR"))
        ekle(kutu, satir(
            dugme("EKRAN GÖRÜNTÜSÜ") { kisayol("input keyevent 120") },
            dugme("SON UYGULAMALAR") { kisayol("input keyevent 187") }
        ))
        ekle(kutu, satir(
            dugme("ANA EKRAN") { kisayol("input keyevent 3") },
            dugme("EKRAN KİLİTLE") { kisayol("input keyevent 223") }
        ))
        ekle(kutu, satir(
            dugme("GÜÇ MENÜSÜ") { kisayol("input keyevent --longpress 26") },
            dugme("AYARLAR") { etkinlik(Intent(Settings.ACTION_SETTINGS)) }
        ))
        ekle(kutu, satir(
            dugme("KAMERA") { etkinlik(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) },
            dugme("GELİŞTİRİCİ") { etkinlik(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
        ))

        kutu.addView(baslik("QANOPY"))
        ekle(kutu, satir(
            dugme("QANOPY AÇ") {
                panelKapat()
                val it2 = packageManager.getLaunchIntentForPackage(packageName)
                if (it2 != null) {
                    it2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(it2)
                }
            },
            dugme("MAGISK AÇ") {
                panelKapat()
                qaKokCalistir(QA_ISLEMLER[0].komut)
            }
        ))
        ekle(kutu, satir(riskli("SYSTEMUI YENİDEN BAŞLAT", QA_ISLEMLER[1].komut)))
        ekle(kutu, satir(dugme("KAPAT") {
            panelKapat()
            stopSelf()
        }))

        val pad = (12 * d).toInt()
        val dis = FrameLayout(this)
        dis.setPadding(pad, pad, pad, pad)
        val bg = GradientDrawable()
        bg.setColor(Color.BLACK)
        bg.setStroke((3 * d).toInt(), YZ_YESIL)
        bg.cornerRadius = 14f * d
        dis.background = bg
        dis.isClickable = true
        val sc = ScrollView(this)
        sc.isVerticalScrollBarEnabled = false
        sc.addView(kutu, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        dis.addView(sc, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val klp = FrameLayout.LayoutParams((284 * d).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        klp.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        klp.marginEnd = (28 * d).toInt()
        klp.topMargin = (24 * d).toInt()
        klp.bottomMargin = (24 * d).toInt()
        kok.addView(dis, klp)

        val lp = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        try {
            wm?.addView(kok, lp)
            panel = kok
            kok.requestFocus()
        } catch (e: Exception) {
            return
        }

        kok.postDelayed({
            if (panel === kok) {
                panoOku()
                panoYenile(panoKutu)
            }
        }, 350)

        thread {
            val cikti = yzKok("settings get global wifi_on; settings get global bluetooth_on; settings get global airplane_mode_on; settings get global mobile_data; settings get system accelerometer_rotation; cmd uimode night").trim().lines()
            if (cikti.size >= 6) {
                Handler(Looper.getMainLooper()).post {
                    if (panel === kok && qsListe.size >= 6) {
                        for (i in 0..4) qsListe[i].acik = cikti[i].trim() == "1"
                        qsListe[5].acik = cikti[5].contains("yes")
                        for (b in qsListe) b.invalidate()
                    }
                }
            }
        }
    }

    private fun panelKapat() {
        val p = panel ?: return
        panel = null
        try {
            wm?.removeView(p)
        } catch (e: Exception) {
        }
    }
}
