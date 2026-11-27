package io.github.bntheq.qanopy

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.text.TextPaint
import android.text.TextUtils
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

private val PB_KREM = Color.parseColor("#FFE8AF")
private val PB_YESIL = Color.parseColor("#00A75C")

// olculer 1080 px genislikli ekrana gore (piksel). Delik merkezi: x 540, y yaklasik 55
private const val PB_KUCUK_W = 330
private const val PB_KUCUK_H = 100
private const val PB_BUYUK_W = 960
private const val PB_BUYUK_H = 370
private const val PB_UST = 5

class QanopyDinleyici : NotificationListenerService()

private fun pbKok(komut: String): String {
    return try {
        val p = ProcessBuilder("su", "-c", komut).redirectErrorStream(true).start()
        val s = p.inputStream.bufferedReader().readText()
        p.waitFor(10, TimeUnit.SECONDS)
        s
    } catch (e: Exception) {
        ""
    }
}

fun punchyBaslat(c: Context) {
    thread {
        try {
            val pk = c.packageName
            if (!Settings.canDrawOverlays(c)) {
                pbKok("appops set " + pk + " SYSTEM_ALERT_WINDOW allow; pm grant " + pk + " android.permission.POST_NOTIFICATIONS")
            }
            val erisim = pk + "/" + QanopyErisim::class.java.name
            val ea = pbKok("settings get secure enabled_accessibility_services").trim()
            if (!ea.contains(erisim)) {
                val tk2 = "\u0027"
                val y2 = if (ea.isEmpty() || ea == "null") erisim else ea + ":" + erisim
                pbKok("settings put secure enabled_accessibility_services " + tk2 + y2 + tk2)
                pbKok("settings put secure accessibility_enabled 1")
            }
            val bilesen = pk + "/" + QanopyDinleyici::class.java.name
            var l = pbKok("settings get secure enabled_notification_listeners").trim()
            if (!l.contains(bilesen)) {
                pbKok("cmd notification allow_listener " + bilesen)
                l = pbKok("settings get secure enabled_notification_listeners").trim()
                if (!l.contains(bilesen)) {
                    val tk = "\u0027"
                    val y = if (l.isEmpty() || l == "null") bilesen else l + ":" + bilesen
                    pbKok("settings put secure enabled_notification_listeners " + tk + y + tk)
                }
            }
        } catch (e: Exception) {
        }
        Handler(Looper.getMainLooper()).post {
            if (Settings.canDrawOverlays(c)) {
                try {
                    c.startForegroundService(Intent(c, PunchyServis::class.java))
                } catch (e: Exception) {
                }
            }
        }
    }
}

private class PunchyGorunum(c: Context, val s: Float, val kucukW: Int, val buyukW: Int) : View(c) {
    var baslik = ""
    var sanatci = ""
    var caliyor = false
    var acikMi = false
    var onToggle: () -> Unit = {}
    var onKontrol: (Int) -> Unit = {}
    private var bas = -1
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val yol = Path()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val il = ((w - kucukW) / (buyukW - kucukW).toFloat()).coerceIn(0f, 1f)
        val cx = w / 2f
        val rad = 50f * s
        val ic = 2f * s
        val r = RectF(ic, ic, w - ic, h - ic)
        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        canvas.drawRoundRect(r, rad, rad, p)
        if (il > 0f) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 4f * s
            p.color = PB_YESIL
            p.alpha = (255 * il).toInt()
            canvas.drawRoundRect(r, rad, rad, p)
        }
        val a1 = (1f - il * 2f).coerceIn(0f, 1f)
        val a2 = (il * 2f - 1f).coerceIn(0f, 1f)
        if (a1 > 0f) kucukCiz(canvas, cx, h, a1)
        if (a2 > 0f) buyukCiz(canvas, cx, a2)
    }

    private fun kucukCiz(canvas: Canvas, cx: Float, h: Float, a1: Float) {
        val cy = h / 2f
        val al = (255 * a1).toInt()
        val gx = cx - 100f * s
        p.style = Paint.Style.FILL
        p.color = PB_KREM
        p.alpha = al
        if (caliyor) {
            yol.reset()
            yol.moveTo(gx - 9f * s, cy - 14f * s)
            yol.lineTo(gx - 9f * s, cy + 14f * s)
            yol.lineTo(gx + 14f * s, cy)
            yol.close()
            canvas.drawPath(yol, p)
        } else {
            canvas.drawRect(gx - 12f * s, cy - 14f * s, gx - 4f * s, cy + 14f * s, p)
            canvas.drawRect(gx + 4f * s, cy - 14f * s, gx + 12f * s, cy + 14f * s, p)
        }
        p.color = PB_YESIL
        p.alpha = al
        val t = System.currentTimeMillis().toFloat()
        for (i in 0..3) {
            val bh = if (caliyor) (10f + (Math.sin((t / (130f + i * 55f)).toDouble()).toFloat() + 1f) * 14f) * s else 8f * s
            val bx = cx + 74f * s + i * 14f * s
            canvas.drawRect(bx, cy - bh / 2f, bx + 8f * s, cy + bh / 2f, p)
        }
        if (caliyor) postInvalidateDelayed(50)
    }

    private fun sigdir(m: String, genislik: Float): String {
        return TextUtils.ellipsize(m, tp, genislik, TextUtils.TruncateAt.END).toString()
    }

    private fun dugmeRect(cx: Float, i: Int): RectF {
        val l = cx - 249f * s + i * 174f * s
        return RectF(l, 250f * s, l + 150f * s, 330f * s)
    }

    private fun buyukCiz(canvas: Canvas, cx: Float, a2: Float) {
        val yarim = 440f * s
        val al = (255 * a2).toInt()
        tp.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        tp.color = PB_YESIL
        tp.alpha = al
        tp.textSize = 26f * s
        tp.textAlign = Paint.Align.LEFT
        canvas.drawText("PUNCHY BLOCK", cx - yarim, 64f * s, tp)
        tp.textAlign = Paint.Align.RIGHT
        canvas.drawText(if (caliyor) "ÇALIYOR" else "DURAKLATILDI", cx + yarim, 64f * s, tp)
        tp.textAlign = Paint.Align.LEFT
        tp.color = PB_KREM
        tp.alpha = al
        tp.textSize = 38f * s
        canvas.drawText(sigdir(if (baslik.isEmpty()) "BİLİNMİYOR" else baslik, 2f * yarim), cx - yarim, 175f * s, tp)
        tp.color = PB_YESIL
        tp.alpha = al
        tp.textSize = 28f * s
        canvas.drawText(sigdir(sanatci, 2f * yarim), cx - yarim, 220f * s, tp)
        for (i in 0..2) {
            val r = dugmeRect(cx, i)
            val ters = bas == i
            p.style = Paint.Style.FILL
            p.color = if (ters) PB_KREM else Color.BLACK
            p.alpha = al
            canvas.drawRoundRect(r, 22f * s, 22f * s, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 4f * s
            p.color = if (ters) PB_KREM else PB_YESIL
            p.alpha = al
            canvas.drawRoundRect(r, 22f * s, 22f * s, p)
            p.style = Paint.Style.FILL
            p.color = if (ters) Color.BLACK else PB_KREM
            p.alpha = al
            simge(canvas, i, r.centerX(), r.centerY())
        }
    }

    private fun simge(canvas: Canvas, i: Int, gx: Float, gy: Float) {
        val u = 18f * s
        yol.reset()
        if (i == 0) {
            canvas.drawRect(gx - u, gy - u, gx - u + 6f * s, gy + u, p)
            yol.moveTo(gx + u, gy - u)
            yol.lineTo(gx + u, gy + u)
            yol.lineTo(gx - u + 10f * s, gy)
            yol.close()
            canvas.drawPath(yol, p)
        } else if (i == 2) {
            canvas.drawRect(gx + u - 6f * s, gy - u, gx + u, gy + u, p)
            yol.moveTo(gx - u, gy - u)
            yol.lineTo(gx - u, gy + u)
            yol.lineTo(gx + u - 10f * s, gy)
            yol.close()
            canvas.drawPath(yol, p)
        } else if (caliyor) {
            canvas.drawRect(gx - 14f * s, gy - u, gx - 4f * s, gy + u, p)
            canvas.drawRect(gx + 4f * s, gy - u, gx + 14f * s, gy + u, p)
        } else {
            yol.moveTo(gx - u * 0.7f, gy - u)
            yol.lineTo(gx - u * 0.7f, gy + u)
            yol.lineTo(gx + u, gy)
            yol.close()
            canvas.drawPath(yol, p)
        }
    }

    private fun hedef(x: Float, y: Float): Int {
        val cx = width / 2f
        for (i in 0..2) {
            if (dugmeRect(cx, i).contains(x, y)) return i
        }
        return -1
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_OUTSIDE -> {
                if (acikMi) onToggle()
            }
            MotionEvent.ACTION_DOWN -> {
                bas = if (acikMi) hedef(e.x, e.y) else -1
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                val h = if (acikMi) hedef(e.x, e.y) else -1
                val b = bas
                bas = -1
                invalidate()
                if (!acikMi) {
                    onToggle()
                } else if (h >= 0 && h == b) {
                    onKontrol(h)
                } else if (h < 0 && e.y < 110f * s) {
                    onToggle()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                bas = -1
                invalidate()
            }
        }
        return true
    }
}

class PunchyServis : Service() {
    companion object {
        @Volatile
        var kendisi: PunchyServis? = null
    }
    private var aktifWm: WindowManager? = null
    private var wm: WindowManager? = null
    private var gorunum: PunchyGorunum? = null
    private var lp: WindowManager.LayoutParams? = null
    private var anim: ValueAnimator? = null
    private var acik = false
    private var denetleyici: MediaController? = null
    private var deneme = 0
    private val ana = Handler(Looper.getMainLooper())
    private var s = 1f
    private var kucukW = 0
    private var kucukH = 0
    private var buyukW = 0
    private var buyukH = 0
    private var ust = 0

    private val cb = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            guncelle()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            guncelle()
        }
    }

    private val oturumDinleyici = MediaSessionManager.OnActiveSessionsChangedListener { l -> sec(l) }

    private val bagla = object : Runnable {
        override fun run() {
            if (!medyaBagla() && deneme < 30) {
                deneme++
                ana.postDelayed(this, 3000)
            }
        }
    }

    private val gizleGorev = Runnable {
        val st = denetleyici?.playbackState?.state
        if (!acik && st != PlaybackState.STATE_PLAYING) gizle()
    }

    override fun onBind(i: Intent?): IBinder? = null

    fun yenidenKur() {
        gizle()
        guncelle()
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int = START_STICKY

    override fun onCreate() {
        super.onCreate()
        QaVarlik.baglam = applicationContext
        QaAyar.yukle(applicationContext)
        bildirim()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        s = resources.displayMetrics.widthPixels / 1080f
        kucukW = (PB_KUCUK_W * s).toInt()
        kucukH = (PB_KUCUK_H * s).toInt()
        buyukW = (PB_BUYUK_W * s).toInt()
        buyukH = (PB_BUYUK_H * s).toInt()
        ust = (PB_UST * s).toInt()
        kendisi = this
        ana.post(bagla)
    }

    override fun onDestroy() {
        kendisi = null
        ana.removeCallbacks(bagla)
        ana.removeCallbacks(gizleGorev)
        try {
            val m = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            m.removeOnActiveSessionsChangedListener(oturumDinleyici)
        } catch (e: Exception) {
        }
        try {
            denetleyici?.unregisterCallback(cb)
        } catch (e: Exception) {
        }
        gizle()
        super.onDestroy()
    }

    private fun bildirim() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("punchy", "Punchy Block", NotificationManager.IMPORTANCE_MIN))
        val n = Notification.Builder(this, "punchy")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("Qanopy")
            .setContentText("Punchy Block acik")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    private fun medyaBagla(): Boolean {
        return try {
            val m = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val c = ComponentName(this, QanopyDinleyici::class.java)
            val liste = m.getActiveSessions(c)
            m.addOnActiveSessionsChangedListener(oturumDinleyici, c)
            sec(liste)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun sec(l: List<MediaController>?) {
        var secilen: MediaController? = null
        if (l != null) {
            for (c in l) {
                if (c.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    secilen = c
                    break
                }
            }
            if (secilen == null) {
                for (c in l) {
                    if (c.playbackState?.state == PlaybackState.STATE_PAUSED) {
                        secilen = c
                        break
                    }
                }
            }
        }
        if (secilen?.sessionToken != denetleyici?.sessionToken) {
            try {
                denetleyici?.unregisterCallback(cb)
            } catch (e: Exception) {
            }
            denetleyici = secilen
            try {
                secilen?.registerCallback(cb)
            } catch (e: Exception) {
            }
        }
        guncelle()
    }

    private fun guncelle() {
        val c = denetleyici
        val st = c?.playbackState?.state
        if (c == null || (st != PlaybackState.STATE_PLAYING && st != PlaybackState.STATE_PAUSED)) {
            gizle()
            return
        }
        val md = c.metadata
        goster()
        val v = gorunum ?: return
        v.baslik = md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        v.sanatci = md?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
        v.caliyor = st == PlaybackState.STATE_PLAYING
        v.invalidate()
        ana.removeCallbacks(gizleGorev)
        if (st == PlaybackState.STATE_PAUSED && !acik) ana.postDelayed(gizleGorev, 10000)
    }

    private fun goster() {
        if (gorunum != null) return
        val v = PunchyGorunum(this, s, kucukW, buyukW)
        v.onToggle = { gecis(!acik) }
        v.onKontrol = { kontrol(it) }
        val ak = QanopyErisim.ornek
        val yw = if (ak != null) (ak.getSystemService(Context.WINDOW_SERVICE) as WindowManager) else wm
        val tur = if (ak != null) WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        val l = WindowManager.LayoutParams(
            kucukW, kucukH,
            tur,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        l.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        l.y = ust
        if (Build.VERSION.SDK_INT >= 30) {
            l.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        try {
            yw?.addView(v, l)
            aktifWm = yw
            gorunum = v
            lp = l
        } catch (e: Exception) {
        }
    }

    private fun gizle() {
        ana.removeCallbacks(gizleGorev)
        anim?.cancel()
        anim = null
        acik = false
        val v = gorunum ?: return
        gorunum = null
        lp = null
        try {
            (aktifWm ?: wm)?.removeView(v)
        } catch (e: Exception) {
        }
    }

    private fun gecis(ac: Boolean) {
        val v = gorunum ?: return
        val l = lp ?: return
        acik = ac
        v.acikMi = ac
        if (QaAyar.titresim) v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        anim?.cancel()
        val w0 = l.width
        val h0 = l.height
        val w1 = if (ac) buyukW else kucukW
        val h1 = if (ac) buyukH else kucukH
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 320
        a.interpolator = if (ac) OvershootInterpolator(1.2f) else DecelerateInterpolator()
        a.addUpdateListener {
            val t = it.animatedValue as Float
            l.width = (w0 + (w1 - w0) * t).toInt().coerceAtLeast(kucukW)
            l.height = (h0 + (h1 - h0) * t).toInt().coerceAtLeast(kucukH)
            try {
                (aktifWm ?: wm)?.updateViewLayout(v, l)
            } catch (e: Exception) {
            }
        }
        anim = a
        a.start()
        if (!ac) guncelle()
    }

    private fun kontrol(i: Int) {
        val c = denetleyici ?: return
        val tc = c.transportControls
        if (i == 0) {
            tc.skipToPrevious()
        } else if (i == 2) {
            tc.skipToNext()
        } else if (c.playbackState?.state == PlaybackState.STATE_PLAYING) {
            tc.pause()
        } else {
            tc.play()
        }
    }
}
