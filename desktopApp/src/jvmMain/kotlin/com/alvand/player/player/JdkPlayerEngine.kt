package com.alvand.player.player

import com.alvand.player.audio.DspSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * موتور پخش بر پایهٔ JDK.
 *
 * این موتور همهٔ قواعد رابط [PlayerEngine] را رعایت می‌کند، و منطقِ لبه‌هایش در
 * [PlaybackQueue] و [ErrorSkipPolicy] جدا شده تا قابل تست باشد. این کلاس فقط
 * **چسب** آن‌هاست.
 *
 * نکتهٔ مهم معماری: نخِ پخش صدا از نخ UI جداست. هیچ‌وقت روی نخ UI صدا پخش
 * نمی‌شود — همین قاعده‌ای است که پرامپت «بدون کار سنگین روی نخ UI» می‌نامد.
 */
class JdkPlayerEngine(
    private val scope: CoroutineScope,
    private val outputFactory: (AudioFormat) -> AudioOutputStream = { fmt ->
        AudioOutputs.create(fmt)
    },
    private val decoderFactory: (String, String?) -> AudioDecoder? = { uri, mime ->
        DecoderFactory.create(uri, mime)
    }
) : PlayerEngine {

    private val _state = MutableStateFlow(EngineState())
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    private val queue = PlaybackQueue()
    private val errorPolicy = ErrorSkipPolicy()

    /** صفی که قبل از آماده‌شدن موتور تنظیم شده — «آخرین برنده» */
    @Volatile
    private var pendingQueue: PendingQueue? = null

    private var positionJob: Job? = null
    private var pumpThread: Thread? = null

    private var decoder: AudioDecoder? = null
    private var output: AudioOutputStream? = null
    @Volatile
    private var dsp = DspSpec()
    @Volatile
    private var volume = 1f

    @Volatile
    private var outputDevice: String? = null

    @Volatile
    private var reopenOutputRequested = false

    /** آیتمی که رمزگشا/خروجیِ فعلی واقعاً برای آن ساخته شده است. */
    @Volatile
    private var preparedItem: PlayableItem? = null

    /**
     * قفلِ گذارهای پخش.
     *
     * کلیک‌های سریع (مثلاً چند بار next پشت‌سرهم) هر کدام در یک کوروتین جدا اجرا
     * می‌شوند؛ بدون این قفل، دو گذار می‌توانند روی یک رمزگشا/خروجی قاطی کنند و
     * آهنگ‌ها جا بیفتند یا بپرد.
     */
    private val controlMutex = Mutex()

    /**
     * نسلِ نخ پخش.
     *
     * وقتی آهنگ عوض می‌شود (next/previous/انتخاب کارت) یا pause می‌کنیم، نخ
     * قبلی بلافاصله نمی‌میرد — ممکن است وسط `write` خوابیده باشد. بدون این
     * شمارنده، نخِ مرده می‌توانست بعد از بیدار شدن موقعیتِ آهنگ جدید را با
     * عددِ آهنگ قبلی بازنویسی کند، یا بدتر: با رسیدن به تهِ رمزگشایِ بسته‌شده،
     * `onTrackFinished` را صدا بزند و صف را یک خانهٔ اضافه جلو ببرد (همان
     * «یهو سه‌چهار آهنگ رد می‌شود»). هر نخ فقط وقتی به state دست می‌زند که
     * نسلش هنوز جاری باشد.
     */
    private val pumpGeneration = AtomicLong(0)

    @Volatile
    private var seekRequestMs: Long = -1L

    /**
     * حلقهٔ A–B (میلی‌ثانیه) — مثل جت‌آدیو و VLC.
     * با عوض شدن آهنگ پاک می‌شود؛ مال همین آهنگ است نه کل صف.
     */
    @Volatile
    private var abLoopA: Long? = null

    @Volatile
    private var abLoopB: Long? = null

    /** بهرهٔ خطی ReplayGain آهنگ جاری — ۱ یعنی بدون تگ، بدون تغییر. */
    @Volatile
    private var replayGainLinear = 1f

    /**
     * مقصدِ آخرین seek که نخ پخش اعمالش کرده و هنوز هیچ بلوکی بعد از آن نخوانده‌ایم.
     *
     * اگر بلافاصله بعد از seek، رمزگشا ته کشید (`readBlock() == null`) یعنی یا
     * واقعاً به انتهای فایل رسیده‌ایم یا تخمینِ مدت‌زمان از دادهٔ واقعی بلندتر
     * بوده. در هر دو حالت، وقتی مقصد وسط آهنگ بوده، نباید بپریم آهنگ بعدی؛
     * همان‌جا در انتها می‌ایستیم. فقط seekِ چسبیده به انتها (یا پایان طبیعی)
     * مسیر عادی `onTrackFinished` را می‌رود.
     */
    @Volatile
    private var seekLandingMs: Long? = null

    @Volatile
    private var prepared = false

    /** تزریق‌شده برای تست */
    internal var clock: () -> Long = { System.nanoTime() / 1_000_000 }

    // ---------------- صف ----------------

    override suspend fun setQueue(items: List<PlayableItem>, startIndex: Int, autoPlay: Boolean) {
        controlMutex.withLock {
            val valid = items.isNotEmpty() && startIndex in items.indices
            if (!valid) {
                _state.value = _state.value.copy(
                    queue = items, index = -1, current = null,
                    isPlaying = false, positionMs = 0, durationMs = 0, error = null
                )
                return@withLock
            }

            if (!prepared) {
                // هنوز آماده نیست: آخرین درخواست برنده است
                pendingQueue = PendingQueue(items, startIndex, autoPlay)
                _state.value = _state.value.copy(queue = items, index = startIndex, current = items[startIndex])
                return@withLock
            }

            queue.setQueue(items, startIndex)
            errorPolicy.reset()
            _state.value = _state.value.copy(
                queue = items,
                index = startIndex,
                current = items[startIndex],
                error = null,
                shuffle = queue.shuffle,
                repeatMode = queue.repeat
            )
            if (autoPlay) playInternal() else prepareCurrent()
        }
    }

    /**
     * بارگذاری اولیهٔ صف از کتابخانهٔ تازه اسکن‌شده.
     *
     * این متد فقط وقتی کاری می‌کند که صفِ موتور هنوز خالی باشد؛ وگرنه آهنگِ در
     * حال پخش یا انتخابِ کاربر هرگز با نتیجهٔ یک اسکن تازه بازنویسی نمی‌شود.
     * بدون پخش خودکار است: کارت‌ها باید دیده شوند، ولی صدا نباید خودش شروع شود.
     */
    suspend fun primeQueueFromLibrary(items: List<PlayableItem>): Boolean {
        if (items.isEmpty() || _state.value.queue.isNotEmpty()) return false
        setQueue(items, 0, autoPlay = false)
        return _state.value.queue.isNotEmpty()
    }

    /** صدا زدن از بیرون پس از آماده‌شدن موتور */
    fun onReady() {
        prepared = true
        val pending = pendingQueue ?: return
        pendingQueue = null
        if (pending.isValid()) {
            scope.launch { setQueue(pending.items, pending.startIndex, pending.autoPlay) }
        }
    }

    // ---------------- کنترل ----------------

    override suspend fun play() = controlMutex.withLock { playInternal() }

    private suspend fun playInternal() {
        // اگر آهنگِ «current» با چیزی که رمزگشا از قبل آماده کرده فرق دارد،
        // باید اول همان آماده شود — وگرنه setQueue(autoPlay=true) که در حال
        // پخش صدا زده می‌شود فقط برچسبِ آهنگ را عوض می‌کرد و رمزگشای قدیمی
        // همچنان می‌خواند (باگِ «آهنگ جدید پخش نمی‌شود»).
        val cur = _state.value.current
        if (cur != null && preparedItem?.id != cur.id) {
            prepareCurrent()
            if (_state.value.current == null || output == null) return
        }
        if (_state.value.current == null) {
            prepareCurrent()
            if (_state.value.current == null) return
        }
        val out = output ?: run {
            prepareCurrent()
            output ?: return
        }
        // اگر نخ پخش از قبل مرده است (مثلاً بعد از pause) دوباره راه‌اندازی می‌شود
        if (pumpThread?.isAlive != true) startPumpThread()
        withContext(Dispatchers.IO) {
            if (!out.isOpen.get()) out.start()
        }
        _state.value = _state.value.copy(isPlaying = true, error = null)
        errorPolicy.onPlayingStarted()
        ensurePositionPump()
    }

    override suspend fun pause() {
        controlMutex.withLock { pauseLocked() }
    }

    override suspend fun toggle() {
        controlMutex.withLock {
            if (_state.value.isPlaying) pauseLocked() else playInternal()
        }
    }

    private fun pauseLocked() {
        _state.value = _state.value.copy(isPlaying = false)
        positionJob?.cancel()
        positionJob = null
        // نخ پخش هم باید متوقف شود، وگرنه بلوکی که در حال پردازش است
        // باز هم به دستگاه می‌رود و صدا بعد از مکث ادامه پیدا می‌کند
        stopPumpThread()
    }

    override suspend fun next() {
        controlMutex.withLock {
            val nextIndex = queue.nextIndex()
            if (nextIndex < 0) {
                // انتهای صف با تکرار خاموش: هیچ اتفاقی نمی‌افتد
                _state.value = _state.value.copy(isPlaying = false)
                return@withLock
            }
            queue.moveTo(nextIndex)
            errorPolicy.reset()
            seekRequestMs = 0L
            publishCurrent()
            switchToCurrentAndPlay()
        }
    }

override suspend fun previous() {
    controlMutex.withLock {
        val oldIndex = _state.value.index
        val prevIndex = queue.previousIndex(_state.value.positionMs)
        queue.moveTo(prevIndex)
        errorPolicy.reset()
        seekRequestMs = 0L
        publishCurrent()
        // برگشت به ابتدای همان آهنگ باید روی همان رمزگشا seek شود، نه اینکه خروجی
        // از نو ساخته شود؛ وگرنه صدا قطع‌ و وصل می‌شود.
        if (prevIndex != oldIndex) {
            switchToCurrentAndPlay()
        } else {
            playInternal()
        }
    }
   }

   /**
    * پخش مستقیمِ یک آیتم مشخص از صف — وقتی کاربر روی کارتِ یک آهنگ در ردیف
    * کاورها می‌زند.
    *
    * جدا از `next`/`previous` است چون آن‌ها فقط یک خانه جلو/عقب می‌روند، ولی
    * انتخاب کارت می‌تواند به هر فاصله‌ای بپرد.
    */
   suspend fun playIndex(target: Int) {
    controlMutex.withLock {
        if (target < 0 || target >= queue.size) return@withLock
        // زدنِ دوبارهٔ همان کارت نباید رمزگشا را از نو بسازد؛ فقط باید مطمئن شویم
        // همان آهنگ در حال پخش است. در غیر این صورت هر کلیک، پخش را از اول می‌انداخت.
        if (target == queue.currentIndex && preparedItem?.id == queue.current()?.id && output != null) {
            playInternal()
            return@withLock
        }
        queue.moveTo(target)
        errorPolicy.reset()
        seekRequestMs = 0L
        publishCurrent()
        switchToCurrentAndPlay()
    }
   }

    /**
     * چرخهٔ حلقهٔ A–B: نقطهٔ A ← نقطهٔ B ← پاک شدن.
     *
     * اگر B جلوتر از A نباشد، A جابه‌جا می‌شود (شروع دوباره). بدون Exception،
     * مثل بقیهٔ API موتور.
     */
    suspend fun cycleAbLoop() {
        controlMutex.withLock {
            val pos = _state.value.positionMs
            val a = abLoopA
            when {
                a == null -> abLoopA = pos
                abLoopB == null && pos > a -> abLoopB = pos
                else -> {
                    abLoopA = if (abLoopB == null) pos else null
                    if (abLoopA == null) abLoopB = null
                }
            }
            publishLoopState()
        }
    }

    /** پاک کردن صریح حلقه (مثلاً از دیالوگ). */
    suspend fun clearAbLoop() {
        controlMutex.withLock {
            abLoopA = null
            abLoopB = null
            publishLoopState()
        }
    }

    /** نقطهٔ شروع حلقه روی موقعیت فعلی. اگر از B جلوتر باشد، B پاک می‌شود. */
    suspend fun setLoopStart() {
        controlMutex.withLock {
            val pos = _state.value.positionMs
            abLoopA = pos
            if (abLoopB != null && abLoopB!! <= pos) abLoopB = null
            publishLoopState()
        }
    }

    /**
     * نقطهٔ پایان حلقه روی موقعیت فعلی. اگر جلوتر از A نباشد، به‌جایش A جابه‌جا
     * می‌شود (همان قانون چرخه).
     */
    suspend fun setLoopEnd() {
        controlMutex.withLock {
            val pos = _state.value.positionMs
            val a = abLoopA
            if (a == null || pos <= a) {
                abLoopA = pos
                abLoopB = null
            } else {
                abLoopB = pos
            }
            publishLoopState()
        }
    }

    private fun publishLoopState() {
        _state.value = _state.value.copy(loopA = abLoopA, loopB = abLoopB)
    }

    /**
     * تعویض واقعی آهنگ.
     *
     * فقط به‌روزرسانی `current` در UI کافی نیست؛ رمزگشا و خروجی قبلی باید بسته
     * و برای آهنگ تازه ساخته شوند. بدون این مرحله، کارت و عنوان جلو می‌روند ولی
     * همان بایت‌های آهنگ قبلی به بلندگو می‌رود — دقیقاً همان باگی که گزارش شد.
     */
    private suspend fun switchToCurrentAndPlay() {
        prepareCurrent()
        if (_state.value.error != null) return
        playInternal()
    }


    override suspend fun seekTo(ms: Long) {
        controlMutex.withLock {
            val st = _state.value
            if (!st.hasKnownDuration) return@withLock   // استریم زنده کشیدن ندارد
            val target = ms.coerceIn(0L, st.durationMs)
            seekRequestMs = target
            _state.value = st.copy(positionMs = target)
        }
    }

    override suspend fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
    }

    override fun setShuffle(enabled: Boolean) {
        queue.setShuffle(enabled)
        _state.value = _state.value.copy(shuffle = queue.shuffle)
    }

    /** جابه‌جایی آیتم صف — وقتی صف shuffle است معنا ندارد و نادیده گرفته می‌شود */
    override fun moveInQueue(from: Int, to: Int) {
        if (queue.shuffle) return
        val moved = queue.move(from, to)
        if (moved >= 0) {
            _state.value = _state.value.copy(
                queue = queue.items(),
                index = queue.currentIndex
            )
        }
    }

    override fun addToQueue(item: PlayableItem) {
        queue.append(item)
        _state.value = _state.value.copy(
            queue = queue.items(),
            index = queue.currentIndex
        )
    }

    override fun removeFromQueue(index: Int) {
        scope.launch {
            controlMutex.withLock {
                // ایندکس «فعلی» باید **داخل** قفل خوانده شود وگرنه دو حذفِ سریع
                // پشت‌سرهم هر دو همان `before` قدیمی را می‌گیرند و ردیفِ اشتباه
                // حذف می‌شود (باگ مسابقهٔ audit).
                val before = _state.value.index
                if (index !in queue.items().indices) return@withLock
                val wasCurrent = index == before && before >= 0
                if (!queue.removeAt(index)) return@withLock
                if (queue.isEmpty()) {
                    // صف خالی شد — توقف کامل
                    stopPumpThread()
                    decoder = null
                    _state.value = _state.value.copy(
                        current = null, isPlaying = false, queue = emptyList(), index = -1,
                        positionMs = 0L, durationMs = 0L, spectrum = emptyList()
                    )
                } else if (wasCurrent) {
                    // آهنگ در حال پخش حذف شد — همان جایگاه (آهنگ بعدی) پخش می‌شود
                    switchToCurrentAndPlay()
                    _state.value = _state.value.copy(
                        queue = queue.items(),
                        index = queue.currentIndex
                    )
                } else {
                    _state.value = _state.value.copy(
                        queue = queue.items(),
                        index = queue.currentIndex
                    )
                }
            }
        }
    }

    override fun clearQueue() {
        scope.launch {
            controlMutex.withLock {
                queue.clear()
                stopPumpThread()
                decoder = null
                _state.value = _state.value.copy(
                    current = null, isPlaying = false, queue = emptyList(), index = -1,
                    positionMs = 0L, durationMs = 0L, spectrum = emptyList()
                )
            }
        }
    }

    override fun cycleRepeat() {
        val mode = queue.cycleRepeat()
        _state.value = _state.value.copy(repeatMode = mode)
    }

    override fun setDsp(spec: DspSpec) {
        dsp = spec
    }

    /**
     * تغییر دستگاه خروجی — خط در اولین فرصت داخل حلقهٔ پخش بسته و دوباره با
     * میکسر تازه باز می‌شود؛ پخش فعلی بدون مکث محسوس ادامه می‌یابد.
     */
    fun setOutputDevice(name: String?) {
        outputDevice = name
        reopenOutputRequested = true
    }

    override fun release() {
        positionJob?.cancel()
        stopPumpThread()
        runCatching { decoder?.close() }
        runCatching { output?.close() }
        decoder = null
        output = null
        preparedItem = null
    }

    // ---------------- داخلی ----------------

    private fun publishCurrent() {
        seekLandingMs = null
        // حلقهٔ A–B مال آهنگ قبلی است؛ با عوض شدن آهنگ پاک می‌شود
        abLoopA = null
        abLoopB = null
        val item = queue.current()
        _state.value = _state.value.copy(
            current = item,
            index = queue.currentIndex,
            durationMs = item?.durationMs ?: 0L,
            positionMs = 0L,
            error = null,
            loopA = null,
            loopB = null
        )
    }

    private fun prepareCurrent() {
        val item = queue.current() ?: return
        preparedItem = null
        val dec = decoderFactory(item.uri, item.mimeHint)
        if (dec == null) {
            _state.value = _state.value.copy(error = "cannot-decode", current = item)
            maybeSkipOnError()
            return
        }
        runCatching { decoder?.close() }
        decoder = dec

        runCatching { output?.close() }
        val fmt = dec.format
        reopenOutputRequested = false
        val out = runCatching { outputFactory(fmt) }.getOrNull()
        if (out == null) {
            _state.value = _state.value.copy(error = "no-audio-output", current = item)
            maybeSkipOnError()
            return
        }
        output = out

        // بهرهٔ ReplayGain این آهنگ (اگر تگ دارد) — مثل جت‌آدیو و VLC
        replayGainLinear = item.replayGainDb?.let { db ->
            (10.0.pow(db.toDouble() / 20.0)).toFloat().coerceIn(0.05f, 5f)
        } ?: 1f

        _state.value = _state.value.copy(
            current = item,
            index = queue.currentIndex,
            durationMs = if (dec.totalDurationMs > 0) dec.totalDurationMs else item.durationMs,
            positionMs = 0L,
            isLiveStream = dec.isLive(),
            error = null
        )
        preparedItem = item

        startPumpThread()
    }

    private fun maybeSkipOnError() {
        val hasNext = queue.nextIndex() >= 0 && queue.currentIndex < queue.size - 1
        if (errorPolicy.shouldSkip(hasNext)) {
            scope.launch { next() }
        } else {
            _state.value = _state.value.copy(isPlaying = false)
        }
    }

    /** نخِ پخش — از نخ UI جداست */
    private fun startPumpThread() {
        stopPumpThread()
        val gen = pumpGeneration.get()
        pumpThread = thread(name = "alvand-pump", isDaemon = true) {
            pumpLoop(gen)
        }
    }

    private fun stopPumpThread() {
        // نسل را همین‌جا بالا می‌بریم تا نوشته‌هایِ دیرهنگامِ نخِ در حال مرگ
        // (موقعیت قدیمی یا پایانِ کاذبِ آهنگ) نادیده گرفته شوند.
        pumpGeneration.incrementAndGet()
        pumpThread?.interrupt()
        pumpThread = null
    }

    private fun pumpLoop(gen: Long) {
        var dec = decoder ?: return
        var out = output ?: return
        var chain = DspChain(dsp, dec.format, volume)
        var appliedVolume = volume
        var appliedDsp = dsp

        try {
            out.start()
        } catch (t: Throwable) {
            if (pumpGeneration.get() != gen) return
            _state.value = _state.value.copy(error = "output-failed", isPlaying = false)
            return
        }

        // نکتهٔ مهم: این نخ **خودش** isPlaying را true نمی‌کند. آماده‌سازی یک آهنگ
        // با «در حال پخش بودن» فرق دارد — اگر اینجا true می‌کردیم، آماده کردنِ یک
        // آهنگ برای پخش بعدی، وضعیت پخش را هم عوض می‌کرد و دکمهٔ play/pause
        // برعکس عمل می‌کرد. مالکیت این پرچم فقط با play/pause است.
        while (!Thread.currentThread().isInterrupted && pumpGeneration.get() == gen) {
            if (!_state.value.isPlaying) {
                try {
                    Thread.sleep(20)
                } catch (_: InterruptedException) {
                    break
                }
                continue
            }

            // تغییر دستگاه خروجی: خط را با میکسر تازه باز می‌کنیم
            if (reopenOutputRequested) {
                reopenOutputRequested = false
                if (pumpGeneration.get() != gen) return
                runCatching { out.close() }
                val reopened = runCatching { outputFactory(dec.format) }.getOrNull()
                if (reopened == null) {
                    if (pumpGeneration.get() != gen) return
                    _state.value = _state.value.copy(error = "output-failed", isPlaying = false)
                    return
                }
                output = reopened
                out = reopened
                try {
                    out.start()
                } catch (t: Throwable) {
                    if (pumpGeneration.get() != gen) return
                    _state.value = _state.value.copy(error = "output-failed", isPlaying = false)
                    return
                }
                out.resetPosition(_state.value.positionMs)
            }

            // seek خواسته‌شده
            val seekTo = seekRequestMs
            if (seekTo >= 0) {
                seekRequestMs = -1L
                val frame = if (dec.format.bytesPerSecond > 0) {
                    seekTo * dec.format.sampleRate / 1000L
                } else 0L
                runCatching { dec.seekTo(frame) }
                // بایت‌های پخش‌شده از نو شمرده می‌شوند تا موقعیت با جای جدید بخواند
                out.resetPosition(seekTo)
                seekLandingMs = seekTo
                _state.value = _state.value.copy(positionMs = seekTo)
            }

            // ولوم و DSP زنده را با موتور همگام کن.
            //
            // قبلاً زنجیره فقط موقع ساخته شدن (شروع آهنگ) ولوم را می‌گرفت و
            // `setVolume` وسط پخش هیچ اثری نداشت — اسلایدر ولوم عملاً خراب بود.
            // مسیر ارزان (فقط ولوم) فیلترها را بازسازی نمی‌کند؛ مسیر spec جدید
            // فقط وقتی که spec واقعاً عوض شده باشد. بهرهٔ ReplayGain هم همین‌جا
            // با ولوم ترکیب می‌شود.
            val wantVolume = (volume * replayGainLinear).coerceIn(0f, 1f)
            val wantDsp = dsp
            if (wantDsp != appliedDsp) {
                chain.updateSpec(wantDsp, wantVolume)
                appliedDsp = wantDsp
                appliedVolume = wantVolume
            } else if (wantVolume != appliedVolume) {
                chain.updateVolume(wantVolume)
                appliedVolume = wantVolume
            }

            val block = try {
                dec.readBlock()
            } catch (t: Throwable) {
                null
            }

            if (block == null) {
                if (pumpGeneration.get() != gen) return
                val landing = seekLandingMs
                seekLandingMs = null
                if (landing != null && landing < _state.value.durationMs) {
                    // وسط آهنگ seek کردیم ولی داده‌ای نیست (مثلاً تخمین مدت‌زمان
                    // از فایل واقعی بلندتر بوده). همان‌جا می‌ایستیم، نه آهنگ بعدی.
                    _state.value = _state.value.copy(
                        positionMs = _state.value.durationMs,
                        isPlaying = false
                    )
                    stopPumpThread()
                    return
                }

                // مسیر gapless: آهنگ تمام شد ولی آهنگ بعدی موجود و هم‌قالب است؛
                // دیکودر را بدون بستن خط صدا عوض می‌کنیم تا هیچ شکاف صدایی نباشد.
                if (tryGaplessSwap()) {
                    dec = decoder ?: return
                    // زنجیره برای دیکودر تازه و بهرهٔ ReplayGain تازه ساخته می‌شود
                    chain = DspChain(dsp, dec.format, volume)
                    appliedVolume = volume
                    appliedDsp = dsp
                    continue
                }

                onTrackFinished()
                return
            }
            seekLandingMs = null

            val processed = chain.process(block.bytes)

            // بین خواندن و نوشتن ممکن است کاربر مکث کرده باشد
            if (!_state.value.isPlaying) continue

            out.write(processed)
            if (pumpGeneration.get() != gen) return
            errorPolicy.onPlayingStarted()

            // به‌روزرسانی دوره‌ای موقعیت، بر اساس بایت‌های واقعاً پخش‌شده
            val played = out.playedMs()
            // حلقهٔ A–B: رسیدیم به B، برمی‌گردیم به A (مسیر seek معمولی)
            val loopA = abLoopA
            val loopB = abLoopB
            if (loopA != null && loopB != null && loopB > loopA && played >= loopB) {
                seekRequestMs = loopA
            }
            // طیف لحظه‌ای برای ویژوالایزر — از همین بایت‌های تازه
            val spectrum = runCatching {
                com.alvand.player.audio.SpectrumAnalyzer.analyze(
                    com.alvand.player.audio.SpectrumAnalyzer.mixMonoTail(
                        processed, dec.format.channels
                    ),
                    dec.format.sampleRate
                )
            }.getOrNull().orEmpty()
            if (played != _state.value.positionMs || spectrum.isNotEmpty()) {
                _state.value = _state.value.copy(positionMs = played, spectrum = spectrum)
            }
        }
    }

    /**
     * تعویض بدون شکستگی صدا (gapless): اگر آهنگ بعدی موجود و هم‌قالب با فعلی
     * باشد، دیکودر عوض و صف جلو می‌رود ولی خط صدا باز می‌ماند.
     * در قالب متفاوت (نرخ/کانال) یا خطای دیکودر، false برمی‌گردد تا مسیر عادی
     * با باز شدن دوبارهٔ خط برود — شکاف کوچک فقط همان‌جا.
     */
    private fun tryGaplessSwap(): Boolean {
        if (!_state.value.isPlaying) return false
        val nextIndex = queue.nextIndex()
        if (nextIndex < 0 || nextIndex == queue.currentIndex) return false // تکرار یک‌باره مسیر عادی
        val nextItem = queue.peekAt(nextIndex) ?: return false
        val nextDec = decoderFactory(nextItem.uri, nextItem.mimeHint) ?: return false
        val curFormat = decoder?.format
        if (curFormat == null || nextDec.format != curFormat) {
            runCatching { nextDec.close() }
            return false
        }
        val oldDec = decoder
        decoder = nextDec
        runCatching { oldDec?.close() }
        queue.moveTo(nextIndex)
        replayGainLinear = nextItem.replayGainDb?.let { db ->
            (10.0.pow(db.toDouble() / 20.0)).toFloat().coerceIn(0.05f, 5f)
        } ?: 1f
        preparedItem = nextItem
        output?.resetPosition(0)
        publishCurrent()
        _state.value = _state.value.copy(
            durationMs = if (nextDec.totalDurationMs > 0) nextDec.totalDurationMs else nextItem.durationMs
        )
        return true
    }

    private fun onTrackFinished() {
        // رسیدیم به انتهای آهنگ فعلی
        _state.value = _state.value.copy(
            positionMs = _state.value.durationMs,
            isPlaying = false
        )
        val nextIndex = queue.nextIndex()
        when {
            nextIndex >= 0 -> scope.launch { next() }
            queue.repeat == RepeatMode.ONE -> {
                // تکرار یک‌باره: از اول همان آهنگ
                scope.launch {
                    seekRequestMs = 0L
                    controlMutex.withLock { switchToCurrentAndPlay() }
                }
            }
            else -> stopPumpThread()
        }
    }

    /**
     * پمپ موقعیت — ۲ هرتز، همان cadence نسخهٔ اندروید (هر ۵۰۰ میلی‌ثانیه).
     *
     * دلیل: UI همه‌چیز را از همین یک StateFlow می‌خواند و ۲ هرتز برای نوار پیشرفت
     * کاملاً نرم است، در حالی که ۶۰ بار در ثانیه فقط بار پردازنده را الکی می‌سوزاند.
     */
    private fun ensurePositionPump() {
        if (positionJob?.isActive == true) return
            positionJob = scope.launch {
                while (isActive) {
                    delay(500)
                    val gen = pumpGeneration.get()
                    val out = output ?: continue
                    val played = out.playedMs()
                    // اگر وسط خواندن، خروجی عوض شده باشد عددِ آهنگ قبلی است
                    if (out !== output || pumpGeneration.get() != gen) continue
                    val st = _state.value
                    if (st.hasKnownDuration) {
                        val clamped = played.coerceAtMost(st.durationMs)
                        if (clamped != st.positionMs) {
                            _state.value = st.copy(positionMs = clamped)
                        }
                    } else if (played != st.positionMs) {
                        _state.value = st.copy(positionMs = played)
                    }
                }
            }
    }
}
