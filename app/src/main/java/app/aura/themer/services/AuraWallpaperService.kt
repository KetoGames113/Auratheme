package app.aura.themer.services

import android.graphics.Canvas
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import app.aura.themer.data.Repo
import app.aura.themer.model.ThemeSchedule
import app.aura.themer.model.WallpaperSpec
import app.aura.themer.render.WallpaperRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.io.File

/** Animated / scheduled wallpapers drawn with Canvas (everything except video). */
class AuraWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = AuraEngine()

    inner class AuraEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private var visible = false
        private var w = 0
        private var h = 0
        private val begin = System.nanoTime()

        private val ticker = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) {
                    val animated = WallpaperRenderer.isAnimated(currentSpec())
                    handler.postDelayed(this, if (animated) 33L else 20_000L)
                }
            }
        }

        private fun currentSpec(): WallpaperSpec = ThemeSchedule.resolve(Repo.active.value)

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            scope.launch {
                Repo.active.drop(1).collect {
                    if (visible) {
                        handler.removeCallbacks(ticker)
                        handler.post(ticker)
                    }
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            handler.removeCallbacks(ticker)
            if (visible) handler.post(ticker)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            w = width
            h = height
            drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(ticker)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            visible = false
            handler.removeCallbacks(ticker)
            scope.cancel()
            super.onDestroy()
        }

        private fun drawFrame() {
            if (w == 0 || h == 0) return
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val t = (System.nanoTime() - begin) / 1_000_000_000f
                    WallpaperRenderer.draw(canvas, w, h, currentSpec(), t)
                }
            } catch (e: Exception) {
                // surface not ready
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
    }
}

/** Video wallpaper (kept separate because a surface can't mix Canvas and MediaPlayer). */
class AuraVideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private var player: MediaPlayer? = null
        private var playingPath: String? = null

        private fun videoPath(): String? =
            Repo.active.value.wallpaper.takeIf { it.type == "video" }?.path

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            scope.launch {
                Repo.active.drop(1).collect {
                    if (videoPath() != playingPath && isVisible) start(surfaceHolder)
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            start(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                if (player == null) start(surfaceHolder) else try { player?.start() } catch (e: Exception) { }
            } else {
                try { player?.pause() } catch (e: Exception) { }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            release()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            release()
            scope.cancel()
            super.onDestroy()
        }

        private fun start(holder: SurfaceHolder) {
            release()
            val path = videoPath() ?: return
            if (!File(path).exists()) return
            try {
                val mp = MediaPlayer()
                mp.setSurface(holder.surface)
                mp.setDataSource(path)
                mp.isLooping = true
                mp.setVolume(0f, 0f)
                mp.setOnPreparedListener {
                    try {
                        it.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                        if (isVisible) it.start()
                    } catch (e: Exception) { }
                }
                mp.prepareAsync()
                player = mp
                playingPath = path
            } catch (e: Exception) {
                release()
            }
        }

        private fun release() {
            try { player?.release() } catch (e: Exception) { }
            player = null
            playingPath = null
        }
    }
}
